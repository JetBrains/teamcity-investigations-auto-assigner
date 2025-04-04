package jetbrains.buildServer.investigationsAutoAssigner;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.common.FailedBuildInfo;
import jetbrains.buildServer.investigationsAutoAssigner.persistent.StatisticsReporter;
import jetbrains.buildServer.investigationsAutoAssigner.processing.DelayedAssignmentsProcessor;
import jetbrains.buildServer.investigationsAutoAssigner.processing.FailedTestAndBuildProblemsProcessor;
import jetbrains.buildServer.investigationsAutoAssigner.utils.CustomParameters;
import jetbrains.buildServer.investigationsAutoAssigner.utils.AggregationLogger;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.util.NamedThreadFactory;
import jetbrains.buildServer.util.ThreadUtil;
import jetbrains.buildServer.util.executors.ExecutorsFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import jetbrains.buildServer.serverSide.problems.BuildProblemInfo;


import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;

/**
 * Dispatcher that processes failed test and build problems and assigns investigations.
 */
public class FailedTestAndBuildProblemsDispatcher {
  private static final Logger LOGGER = Constants.LOGGER;

  @NotNull
  private final FailedTestAndBuildProblemsProcessor processor;
  private final DelayedAssignmentsProcessor delayedAssignmentsProcessor;
  @NotNull private final AggregationLogger aggregationLogger;
  private final ServerResponsibility serverResponsibility;
  private final StatisticsReporter statisticsReporter;
  private final CustomParameters customParameters;
  @NotNull
  private final Set<Long> failedBuilds = ConcurrentHashMap.newKeySet();
  @NotNull
  private final ConcurrentHashMap<String, Long> delayedAssignments = new ConcurrentHashMap<>();
  @NotNull
  private final ScheduledExecutorService executor;
  private final BuildsManager buildsManager;

  public FailedTestAndBuildProblemsDispatcher(@NotNull final BuildServerListenerEventDispatcher buildServerListenerEventDispatcher,
                                              @NotNull final FailedTestAndBuildProblemsProcessor processor,
                                              @NotNull final DelayedAssignmentsProcessor delayedAssignmentsProcessor,
                                              @NotNull final AggregationLogger aggregationLogger,
                                              @NotNull final StatisticsReporter statisticsReporter,
                                              @NotNull final CustomParameters customParameters,
                                              @NotNull final BuildsManager buildsManager,
                                              @NotNull final ServerResponsibility serverResponsibility) {
    this.processor = processor;
    this.delayedAssignmentsProcessor = delayedAssignmentsProcessor;
    this.aggregationLogger = aggregationLogger;
    this.statisticsReporter = statisticsReporter;
    this.customParameters = customParameters;
    this.buildsManager = buildsManager;
    this.serverResponsibility = serverResponsibility;
    this.executor = ExecutorsFactory.newFixedScheduledDaemonExecutor(Constants.BUILD_FEATURE_TYPE, 1);
    scheduleBrokenBuildProcessing();

    buildServerListenerEventDispatcher.addListener(new BuildServerAdapter() {
      @Override
      public void buildProblemsChanged(@NotNull SBuild sBuild, @NotNull List<BuildProblemData> before, @NotNull List<BuildProblemData> after) {
        handleBuildProblemsChanged(sBuild);
      }

      @Override
      public void buildInterrupted(@NotNull final SRunningBuild build) {
        handleBuildInterrupted(build);
      }

      @Override
      public void buildFinished(@NotNull SRunningBuild build) {
        handleBuildFinished(build);
      }

      @Override
      public void responsibleChanged(@NotNull final SProject project, @NotNull final Collection<TestName> testNames, @NotNull final ResponsibilityEntry entry, final boolean isUserAction) {
        handleResponsibleChanged(testNames, entry, isUserAction);
      }

      @Override
      public void responsibleChanged(@NotNull final SProject project, @NotNull final Collection<BuildProblemInfo> buildProblems, @Nullable final ResponsibilityEntry entry) {
        handleResponsibleChanged(buildProblems, entry);
      }

      @Override
      public void serverShutdown() {
        shutdownExecutor();
      }
    });
  }

  /**
   * Schedules the processing of broken builds periodically.
   */
  private void scheduleBrokenBuildProcessing() {
    this.executor.scheduleWithFixedDelay(this::processBrokenBuildsOneThread,
                                    CustomParameters.getProcessingDelayInSeconds(),
                                    CustomParameters.getProcessingDelayInSeconds(),
                                    TimeUnit.SECONDS);
  }

  /**
   * Handles the case when build problems have changed.
   * @param sBuild the build whose problems have changed.
   */
  private void handleBuildProblemsChanged(SBuild sBuild) {
    if (!canSendNotifications() || this.failedBuilds.contains(sBuild.getBuildId()) || shouldIgnore(sBuild)) {
      return;
    }
    this.failedBuilds.add(sBuild.getBuildId());
  }

  /**
   * Handles the case when a build is interrupted.
   * @param build the interrupted build.
   */
  private void handleBuildInterrupted(@NotNull final SRunningBuild build) {
    this.failedBuilds.remove(build.getBuildId());
  }

  /**
   * Handles the case when a build has finished.
   * @param build the finished build.
   */
  private void handleBuildFinished(@NotNull SRunningBuild build) {
    if (shouldIgnore(build) || !canSendNotifications()) {
      this.failedBuilds.remove(build.getBuildId());
      return;
    }

    try {
      scheduleDelayedAssignmentProcessing(build);
      if (this.failedBuilds.remove(build.getBuildId())) {
        scheduleFinishedBuildProcessing(build);
      }
    } catch (RejectedExecutionException e) {
      LOGGER.infoAndDebugDetails("Could not schedule automatic assignment investigations for the finishing build " + build, e);
      this.failedBuilds.remove(build.getBuildId());
    }
  }

  /**
   * Handles the responsible change for tests.
   * @param testNames the collection of test names.
   * @param entry the responsibility entry.
   * @param isUserAction whether the change is user-initiated.
   */
  private void handleResponsibleChanged(@NotNull final Collection<TestName> testNames, @NotNull final ResponsibilityEntry entry, final boolean isUserAction) {
    if (isUserAction && shouldBeReportedAsWrong(entry)) {
      this.statisticsReporter.reportWrongInvestigation(testNames.size());
    }
  }

  /**
   * Handles the responsible change for build problems.
   * @param buildProblems the collection of build problems.
   * @param entry the responsibility entry.
   */
  private void handleResponsibleChanged(@NotNull final Collection<BuildProblemInfo> buildProblems, @Nullable final ResponsibilityEntry entry) {
    if (shouldBeReportedAsWrong(entry)) {
      this.statisticsReporter.reportWrongInvestigation(buildProblems.size());
    }
  }

  /**
   * Checks if the responsibility entry should be reported as wrong.
   * @param entry the responsibility entry.
   * @return true if it should be reported as wrong, false otherwise.
   */
  private boolean shouldBeReportedAsWrong(@Nullable final ResponsibilityEntry entry) {
    return entry != null &&
           entry.getReporterUser() != null &&
           (entry.getState() == ResponsibilityEntry.State.GIVEN_UP || entry.getState() == ResponsibilityEntry.State.TAKEN) &&
           entry.getComment().startsWith(Constants.ASSIGN_DESCRIPTION_PREFIX);
  }

  /**
   * Handles server shutdown and gracefully shuts down the executor.
   */
  private void shutdownExecutor() {
    ThreadUtil.shutdownGracefully(this.executor, "Investigator-Auto-Assigner Daemon");
  }

  /**
   * Schedules the processing of finished build.
   * @param build the build to be processed.
   */
  private void scheduleFinishedBuildProcessing(@NotNull SRunningBuild build) {
    long buildId = build.getBuildId();
    this.executor.execute(() -> {
      SBuild currentBuild = this.buildsManager.findBuildInstanceById(buildId);
      if (currentBuild == null) return;
      processFinishedBuild(new FailedBuildInfo(currentBuild));
    });
  }

  /**
   * Schedules the processing of delayed assignments for a given build.
   * @param build the build for which delayed assignments need to be processed.
   */
  private void scheduleDelayedAssignmentProcessing(@NotNull SRunningBuild build) {
    long buildId = build.getBuildId();
    this.executor.execute(() -> {
      SBuild currentBuild = this.buildsManager.findBuildInstanceById(buildId);
      if (currentBuild == null) return;
      processDelayedAssignmentsOneThread(currentBuild);
    });
  }

  /**
   * Processes broken builds in a separate thread.
   */
  private void processBrokenBuildsOneThread() {
    String description = String.format("Investigations auto-assigner: processing %s builds in background", this.failedBuilds.size());
    NamedThreadFactory.executeWithNewThreadName(description, this::processBrokenBuilds);
  }

  /**
   * Processes delayed assignments for a given build in a separate thread.
   * @param nextBuild the build to process delayed assignments for.
   */
  private void processDelayedAssignmentsOneThread(@NotNull SBuild nextBuild) {
    @Nullable SBuildType buildType = nextBuild.getBuildType();
    if (buildType == null) return;

    Long delayedAssignmentsBuildId = this.delayedAssignments.get(buildType.getInternalId());
    if (delayedAssignmentsBuildId == null || delayedAssignmentsBuildId == nextBuild.getBuildId()) return;

    SBuild delayedAssignmentsBuild = this.buildsManager.findBuildInstanceById(delayedAssignmentsBuildId);
    if (delayedAssignmentsBuild == null) {
      this.delayedAssignments.remove(buildType.getInternalId());
      return;
    }

    if (nextBuild.getBuildPromotion().isLaterThan(delayedAssignmentsBuild.getBuildPromotion())) {
      this.delayedAssignments.remove(buildType.getInternalId());
      processDelayedAssignments(new FailedBuildInfo(delayedAssignmentsBuild), nextBuild);
    }
  }

  /**
   * Processes delayed assignments for a given build.
   * @param delayedAssignmentsBuildInfo the delayed assignments build info.
   * @param nextBuild the next build to process delayed assignments for.
   */
  private void processDelayedAssignments(@NotNull final FailedBuildInfo delayedAssignmentsBuildInfo, @NotNull SBuild nextBuild) {
    String description = String.format("Investigations auto-assigner: processing delayed assignments for build %s in background", delayedAssignmentsBuildInfo.getBuild().getBuildId());
    NamedThreadFactory.executeWithNewThreadName(description, () -> this.delayedAssignmentsProcessor.processBuild(delayedAssignmentsBuildInfo, nextBuild));
  }

  /**
   * Processes a finished build.
   * @param failedBuildInfo the failed build info.
   */
  private void processFinishedBuild(@NotNull final FailedBuildInfo failedBuildInfo) {
    String description = String.format("Investigations auto-assigner: processing finished build %s in background", failedBuildInfo.getBuild().getBuildId());
    NamedThreadFactory.executeWithNewThreadName(description, () -> this.processBrokenBuild(failedBuildInfo));
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Build #" + failedBuildInfo.getBuild().getBuildId() + " will be removed from processing.");
    }

    if (failedBuildInfo.shouldDelayAssignments() && !failedBuildInfo.getHeuristicsResult().isEmpty()) {
      putIntoDelayAssignments(failedBuildInfo);
    }

    if (!failedBuildInfo.getHeuristicsResult().isEmpty() && this.customParameters.isBuildFeatureEnabled(failedBuildInfo.getBuild())) {
      int numberOfChanges = failedBuildInfo.getBuild().getContainingChanges().size();
      this.statisticsReporter.reportProcessedBuildWithChanges(numberOfChanges);
    }

    this.aggregationLogger.logResults(failedBuildInfo);
  }

  /**
   * Puts a failed build into delayed assignments.
   * @param currentFailedBuildInfo the current failed build info.
   */
  private void putIntoDelayAssignments(@NotNull final FailedBuildInfo currentFailedBuildInfo) {
    @Nullable SBuildType buildType = currentFailedBuildInfo.getBuild().getBuildType();
    if (buildType == null) return;

    Long previouslyAddedBuildId = this.delayedAssignments.get(buildType.getInternalId());
    SBuild previouslyAddedBuild = previouslyAddedBuildId == null ? null : this.buildsManager.findBuildInstanceById(previouslyAddedBuildId);
    if (previouslyAddedBuild == null) {
      this.delayedAssignments.put(buildType.getInternalId(), currentFailedBuildInfo.getBuildId());
      return;
    }

    BuildPromotion currentBuildPromotion = currentFailedBuildInfo.getBuild().getBuildPromotion();
    BuildPromotion previouslyAddedPromotion = previouslyAddedBuild.getBuildPromotion();
    if (currentBuildPromotion.isLaterThan(previouslyAddedPromotion)) {
      processOlderAndDelayNew(buildType, new FailedBuildInfo(previouslyAddedBuild), currentFailedBuildInfo);
    } else {
      processOlderAndDelayNew(buildType, currentFailedBuildInfo, new FailedBuildInfo(previouslyAddedBuild));
    }
  }

  /**
   * Processes older build and delays new build.
   * @param buildType the build type.
   * @param older the older failed build info.
   * @param newer the newer failed build info.
   */
  private void processOlderAndDelayNew(@NotNull SBuildType buildType, @NotNull FailedBuildInfo older, @NotNull FailedBuildInfo newer) {
    processDelayedAssignments(older, newer.getBuild());
    this.delayedAssignments.put(buildType.getInternalId(), newer.getBuildId());
  }

  /**
   * Processes broken builds.
   */
  private void processBrokenBuilds() {
    if (!canSendNotifications()) {
      this.failedBuilds.clear();
      return;
    }

    for (SBuild build : this.buildsManager.findBuildInstances(this.failedBuilds)) {
      processBrokenBuild(new FailedBuildInfo(build));
    }
  }

  /**
   * Checks if notifications can be sent.
   * @return true if notifications can be sent, false otherwise.
   */
  private boolean canSendNotifications() {
    return this.serverResponsibility.canSendNotifications();
  }

  /**
   * Processes a broken build.
   * @param failedBuildInfo the failed build info.
   */
  private synchronized void processBrokenBuild(final FailedBuildInfo failedBuildInfo) {
    this.processor.processBuild(failedBuildInfo);
  }

  /**
   * Determines whether a build should be ignored.
   * @param build the build.
   * @return true if the build should be ignored, false otherwise.
   */
  private boolean shouldIgnore(@NotNull SBuild build) {
    @Nullable Branch branch = build.getBranch();
    boolean isDefaultBranch = branch == null || branch.isDefaultBranch();

    return build.isPersonal() ||
           build.getBuildType() == null ||
           !(isDefaultBranch || CustomParameters.shouldRunForFeatureBranches(build)) ||
           !(this.customParameters.isBuildFeatureEnabled(build) || this.customParameters.isDefaultSilentModeEnabled(build));
  }

  @TestOnly
  @NotNull
  public Set<Long> getRememberedFailedBuilds() {
    return this.failedBuilds;
  }
}
