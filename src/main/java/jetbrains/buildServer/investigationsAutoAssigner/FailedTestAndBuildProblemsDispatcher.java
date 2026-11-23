

package jetbrains.buildServer.investigationsAutoAssigner;

import com.intellij.openapi.diagnostic.Logger;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;
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
import jetbrains.buildServer.serverSide.problems.BuildProblemInfo;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.util.NamedThreadFactory;
import jetbrains.buildServer.util.ThreadUtil;
import jetbrains.buildServer.util.executors.ExecutorsFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

public class FailedTestAndBuildProblemsDispatcher {
  private static final Logger LOGGER = Constants.LOGGER;

  @NotNull
  private final FailedTestAndBuildProblemsProcessor myProcessor;
  private final DelayedAssignmentsProcessor myDelayedAssignmentsProcessor;
  @NotNull private final AggregationLogger myAggregationLogger;
  private final ServerResponsibility myServerResponsibility;
  private final StatisticsReporter myStatisticsReporter;
  private final CustomParameters myCustomParameters;
  @NotNull
  private final Set<Long> myFailedBuilds = ConcurrentHashMap.newKeySet();
  @NotNull
  private final ConcurrentHashMap<String, Long> myDelayedAssignments = new ConcurrentHashMap<>();
  @NotNull
  private final ScheduledExecutorService myExecutor;
  private final BuildsManager myBuildsManager;

  public FailedTestAndBuildProblemsDispatcher(@NotNull final BuildServerListenerEventDispatcher buildServerListenerEventDispatcher,
                                              @NotNull final FailedTestAndBuildProblemsProcessor processor,
                                              @NotNull final DelayedAssignmentsProcessor delayedAssignmentsProcessor,
                                              @NotNull final AggregationLogger aggregationLogger,
                                              @NotNull final StatisticsReporter statisticsReporter,
                                              @NotNull final CustomParameters customParameters,
                                              @NotNull final BuildsManager buildsManager,
                                              @NotNull final ServerResponsibility serverResponsibility) {
    myProcessor = processor;
    myDelayedAssignmentsProcessor = delayedAssignmentsProcessor;
    myAggregationLogger = aggregationLogger;
    myStatisticsReporter = statisticsReporter;
    myCustomParameters = customParameters;
    myBuildsManager = buildsManager;
    myServerResponsibility = serverResponsibility;
    myExecutor = ExecutorsFactory.newFixedScheduledDaemonExecutor(Constants.BUILD_FEATURE_TYPE, 1);
    myExecutor.scheduleWithFixedDelay(this::processBrokenBuildsOneThread,
                                      CustomParameters.getProcessingDelayInSeconds(),
                                      CustomParameters.getProcessingDelayInSeconds(),
                                      TimeUnit.SECONDS);

    buildServerListenerEventDispatcher.addListener(new BuildServerAdapter() {
      @Override
      public void buildProblemsChanged(@NotNull SBuild sBuild,
                                       @NotNull List<BuildProblemData> before,
                                       @NotNull List<BuildProblemData> after) {
        if (canSendNotifications()) return;

        if (myFailedBuilds.contains(sBuild.getBuildId()) || shouldIgnore(sBuild)) {
          return;
        }

        myFailedBuilds.add(sBuild.getBuildId());
      }

      @Override
      public void buildInterrupted(@NotNull final SRunningBuild build) {
        myFailedBuilds.remove(build.getBuildId());
      }

      @Override
      public void buildFinished(@NotNull SRunningBuild build) {
        if (shouldIgnore(build) || canSendNotifications()) {
          myFailedBuilds.remove(build.getBuildId());
          return;
        }

        try {
          scheduleDelayedAssignmentProcessing(build);

          if (myFailedBuilds.remove(build.getBuildId())) {
            scheduleFinishedBuildProcessing(build);
          }
        } catch (RejectedExecutionException e) {
          LOGGER.infoAndDebugDetails("Could not schedule automatic assignment investigations for the finishing build " + build, e);
          myFailedBuilds.remove(build.getBuildId());
        }
      }

      @Override
      public void responsibleChanged(@NotNull final SProject project,
                                     @NotNull final Collection<TestName> testNames,
                                     @NotNull final ResponsibilityEntry entry,
                                     final boolean isUserAction) {
        if (isUserAction && shouldBeReportedAsWrong(entry)) {
          myStatisticsReporter.reportWrongInvestigation(testNames.size());
        }
      }

      private boolean shouldBeReportedAsWrong(@Nullable final ResponsibilityEntry entry) {
        return entry != null &&
               entry.getReporterUser() != null &&
               (entry.getState() == ResponsibilityEntry.State.GIVEN_UP ||
                entry.getState() == ResponsibilityEntry.State.TAKEN) &&
               entry.getComment().startsWith(Constants.ASSIGN_DESCRIPTION_PREFIX);
      }

      @Override
      public void responsibleChanged(@NotNull final SProject project,
                                     @NotNull final Collection<BuildProblemInfo> buildProblems,
                                     @Nullable final ResponsibilityEntry entry) {
        if (shouldBeReportedAsWrong(entry)) {
          myStatisticsReporter.reportWrongInvestigation(buildProblems.size());
        }
      }

      @Override
      public void serverShutdown() {
        ThreadUtil.shutdownGracefully(myExecutor, "Investigator-Auto-Assigner Daemon");
      }
    });
  }

  private void scheduleFinishedBuildProcessing(@NotNull SRunningBuild build) {
    long buildId = build.getBuildId();
    myExecutor.execute(() -> {
      // can't pass the running build right to the scheduled task to avoid its leaking, see https://youtrack.jetbrains.com/issue/TW-90428
      SBuild currentBuild = myBuildsManager.findBuildInstanceById(buildId);
      if (currentBuild == null) return;
      processFinishedBuild(new FailedBuildInfo(currentBuild));
    });
  }

  private void scheduleDelayedAssignmentProcessing(@NotNull SRunningBuild build) {
    long buildId = build.getBuildId();

    myExecutor.execute(() -> {
      // can't pass the running build right to the scheduled task to avoid its leaking, see https://youtrack.jetbrains.com/issue/TW-90428
      SBuild currentBuild = myBuildsManager.findBuildInstanceById(buildId);
      if (currentBuild == null) return;
      processDelayedAssignmentsOneThread(currentBuild);
    });
  }

  private void processBrokenBuildsOneThread() {
    String description = String.format("Investigations auto-assigner: processing %s builds in background",
                                       myFailedBuilds.size());
    NamedThreadFactory.executeWithNewThreadName(description, this::processBrokenBuilds);
  }

  private void processDelayedAssignmentsOneThread(@NotNull SBuild nextBuild) {
    @Nullable
    SBuildType buildType = nextBuild.getBuildType();
    if (buildType != null) {
      Long delayedAssignmentsBuildId = myDelayedAssignments.get(buildType.getInternalId());

      if ((delayedAssignmentsBuildId == null) || (delayedAssignmentsBuildId == nextBuild.getBuildId())) return;

      SBuild delayedAssignmentsBuild = myBuildsManager.findBuildInstanceById(delayedAssignmentsBuildId);
      if (delayedAssignmentsBuild == null) {
        myDelayedAssignments.remove(buildType.getInternalId());
        return;
      }

      if (nextBuild.getBuildPromotion().isLaterThan(delayedAssignmentsBuild.getBuildPromotion())) {
        myDelayedAssignments.remove(buildType.getInternalId());
        processDelayedAssignments(new FailedBuildInfo(delayedAssignmentsBuild), nextBuild);
      }



    }
  }

  private void processDelayedAssignments(@NotNull final FailedBuildInfo delayedAssignmentsBuildInfo, @NotNull SBuild nextBuild) {
    String description = String.format("Investigations auto-assigner: processing delayed assignments for build %s" +
                                       " in background", delayedAssignmentsBuildInfo.getBuild().getBuildId());
    NamedThreadFactory.executeWithNewThreadName(
      description, () -> myDelayedAssignmentsProcessor.processBuild(delayedAssignmentsBuildInfo, nextBuild));
  }

  private void processFinishedBuild(@NotNull final FailedBuildInfo failedBuildInfo) {
    String description = String.format("Investigations auto-assigner: processing finished build %s in background",
                                       failedBuildInfo.getBuild().getBuildId());
    NamedThreadFactory.executeWithNewThreadName(description, () -> this.processBrokenBuild(failedBuildInfo));
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Build #" + failedBuildInfo.getBuild().getBuildId() + " will be removed from processing.");
    }

    if (failedBuildInfo.shouldDelayAssignments() && !failedBuildInfo.getHeuristicsResult().isEmpty()) {
      putIntoDelayAssignments(failedBuildInfo);
    }

    if (!failedBuildInfo.getHeuristicsResult().isEmpty() && myCustomParameters.isBuildFeatureEnabled(failedBuildInfo.getBuild())) {
      int numberOfChanges = failedBuildInfo.getBuild().getContainingChanges().size();
      myStatisticsReporter.reportProcessedBuildWithChanges(numberOfChanges);
    }

    myAggregationLogger.logResults(failedBuildInfo);
  }

  private void putIntoDelayAssignments(@NotNull final FailedBuildInfo currentFailedBuildInfo) {
    @Nullable
    SBuildType buildType = currentFailedBuildInfo.getBuild().getBuildType();
    if (buildType == null) {
      return;
    }

    Long previouslyAddedBuildId = myDelayedAssignments.get(buildType.getInternalId());
    SBuild previouslyAddedBuild = previouslyAddedBuildId == null ? null : myBuildsManager.findBuildInstanceById(previouslyAddedBuildId);
    if (previouslyAddedBuild == null) {
      myDelayedAssignments.put(buildType.getInternalId(), currentFailedBuildInfo.getBuildId());
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

  private void processOlderAndDelayNew(@NotNull SBuildType buildType, @NotNull FailedBuildInfo older, @NotNull FailedBuildInfo newer) {
    processDelayedAssignments(older, newer.getBuild());
    myDelayedAssignments.put(buildType.getInternalId(), newer.getBuildId());
  }

  private void processBrokenBuilds() {
    if (canSendNotifications()) {
      myFailedBuilds.clear();
      return;
    }

    for (SBuild build: myBuildsManager.findBuildInstances(myFailedBuilds)) {
      processBrokenBuild(new FailedBuildInfo(build));
    }
  }

  private boolean canSendNotifications() {
    return !myServerResponsibility.canSendNotifications();
  }

  private synchronized void processBrokenBuild(final FailedBuildInfo failedBuildInfo) {
    myProcessor.processBuild(failedBuildInfo);
  }

  /*
    We should ignore personal builds, builds for feature branches (by default),
    and handle the case when investigation suggestions are disabled.
   */
  private boolean shouldIgnore(@NotNull SBuild build) {
    @Nullable
    Branch branch = build.getBranch();
    boolean isDefaultBranch = branch == null || branch.isDefaultBranch();

    if (build.isPersonal() ||
        build.getBuildType() == null ||
        !(isDefaultBranch || CustomParameters.shouldRunForFeatureBranches(build))) {
      return true;
    }

    return !(myCustomParameters.isBuildFeatureEnabled(build) || myCustomParameters.isDefaultSilentModeEnabled(build));
  }

  @TestOnly
  @NotNull
  public Set<Long> getRememberedFailedBuilds() {
    return myFailedBuilds;
  }
}