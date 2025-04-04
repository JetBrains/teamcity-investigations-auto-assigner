package jetbrains.buildServer.investigationsAutoAssigner.processing;

import com.intellij.openapi.diagnostic.Logger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jetbrains.buildServer.BuildProblemTypes;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.common.FailedBuildInfo;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.persistent.AssignerArtifactDao;
import jetbrains.buildServer.investigationsAutoAssigner.utils.CustomParameters;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.investigationsAutoAssigner.common.Constants.SHOULD_PERSIST_FILTERED_TESTS_DESCRIPTION;

/**
 * Processes failed tests and build problems by identifying responsible users
 * and managing investigation assignments based on project configuration and thresholds.
 */
public class FailedTestAndBuildProblemsProcessor extends BaseProcessor {

  private static final Logger LOGGER = Constants.LOGGER;
  private final FailedTestFilter failedTestFilter;
  private final BuildProblemsFilter buildProblemsFilter;
  private final FailedTestAssigner failedTestAssigner;
  private final BuildProblemsAssigner buildProblemsAssigner;
  private final AssignerArtifactDao assignerArtifactDao;
  private final CustomParameters customParameters;
  private final ResponsibleUserFinder responsibleUserFinder;

  /**
   * Creates an instance of the processor with required dependencies.
   *
   * @param responsibleUserFinder  Responsible user identification service.
   * @param failedTestFilter       Filters applicable failed tests.
   * @param failedTestAssigner     Assigns investigations for failed tests.
   * @param buildProblemsFilter    Filters applicable build problems.
   * @param buildProblemsAssigner  Assigns investigations for build problems.
   * @param assignerArtifactDao    Persists assignment artifacts.
   * @param customParameters       Provides custom configuration parameters.
   */
  public FailedTestAndBuildProblemsProcessor(
    @NotNull final ResponsibleUserFinder responsibleUserFinder,
    @NotNull final FailedTestFilter failedTestFilter,
    @NotNull final FailedTestAssigner failedTestAssigner,
    @NotNull final BuildProblemsFilter buildProblemsFilter,
    @NotNull final BuildProblemsAssigner buildProblemsAssigner,
    @NotNull final AssignerArtifactDao assignerArtifactDao,
    @NotNull final CustomParameters customParameters) {
    this.responsibleUserFinder = responsibleUserFinder;
    this.failedTestFilter = failedTestFilter;
    this.failedTestAssigner = failedTestAssigner;
    this.buildProblemsFilter = buildProblemsFilter;
    this.buildProblemsAssigner = buildProblemsAssigner;
    this.assignerArtifactDao = assignerArtifactDao;
    this.customParameters = customParameters;
  }

  /**
   * Processes a failed build, identifying responsible users and managing
   * investigation assignments based on configured thresholds.
   *
   * @param failedBuildInfo The information about the failed build.
   */
  public void processBuild(@NotNull final FailedBuildInfo failedBuildInfo) {
    SBuild build = failedBuildInfo.getBuild();
    SProject project = getProject(build);
    if (project == null) {
      return;
    }

    logBuildProcessingStart(build, failedBuildInfo);

    if (failedBuildInfo.isOverProcessedProblemsThreshold()) {
      LOGGER.debug("Stop processing build #" + build.getBuildId() + " as the threshold was exceeded.");
      return;
    }

    Map<Long, String> notApplicableTestsDescription = new HashMap<>();
    List<BuildProblem> applicableProblems = filterBuildProblems(failedBuildInfo, project, build);
    List<STestRun> applicableFailedTests = filterFailedTests(failedBuildInfo, project, build, notApplicableTestsDescription);
    logProblemsNumber(build, applicableFailedTests, applicableProblems);

    HeuristicResult heuristicsResult = this.responsibleUserFinder.findResponsibleUser(build, project, applicableProblems, applicableFailedTests);

    List<STestRun> testsForAssign = this.failedTestFilter.getStillApplicable(failedBuildInfo, project, applicableFailedTests, notApplicableTestsDescription);
    List<BuildProblem> problemsForAssign = this.buildProblemsFilter.getStillApplicable(failedBuildInfo, project, applicableProblems);
    logChangedProblemsNumber(build, applicableFailedTests, testsForAssign, applicableProblems, problemsForAssign);

    persistResults(build, testsForAssign, heuristicsResult, notApplicableTestsDescription);

    if (!heuristicsResult.isEmpty()) {
      handleAssignments(build, project, failedBuildInfo, heuristicsResult, testsForAssign, problemsForAssign);
    }
  }

  /**
   * Logs the start of build processing.
   *
   * @param build           The build being processed.
   * @param failedBuildInfo Information about the failed build.
   */
  private void logBuildProcessingStart(SBuild build, FailedBuildInfo failedBuildInfo) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Start processing build #" + build.getBuildId() + ". " +
                   "Delay assignment: " + failedBuildInfo.shouldDelayAssignments());
    }
  }

  /**
   * Filters build problems applicable for further processing.
   *
   * @param failedBuildInfo The failed build info.
   * @param project         The associated project.
   * @param build           The failed build.
   * @return List of applicable build problems.
   */
  private List<BuildProblem> filterBuildProblems(FailedBuildInfo failedBuildInfo, SProject project, SBuild build) {
    return this.buildProblemsFilter.apply(failedBuildInfo, project, ((BuildEx) build).getBuildProblems());
  }

  /**
   * Filters applicable failed tests.
   *
   * @param failedBuildInfo            The failed build info.
   * @param project                    The associated project.
   * @param build                       The failed build.
   * @param notApplicableTestsDescription A map to store reasons for filtered-out tests.
   * @return List of applicable failed tests.
   */
  private List<STestRun> filterFailedTests(FailedBuildInfo failedBuildInfo, SProject project, SBuild build, Map<Long, String> notApplicableTestsDescription) {
    return this.failedTestFilter.apply(failedBuildInfo, project, requestBrokenTestsWithStats(build), notApplicableTestsDescription);
  }

  /**
   * Persists heuristic results and filtered test descriptions if enabled.
   *
   * @param build                       The build being processed.
   * @param testsForAssign              Tests marked for assignment.
   * @param heuristicsResult            The heuristic result of the assignment.
   * @param notApplicableTestsDescription A map of filtered-out test descriptions.
   */
  private void persistResults(SBuild build, List<STestRun> testsForAssign, HeuristicResult heuristicsResult, Map<Long, String> notApplicableTestsDescription) {
    this.assignerArtifactDao.appendHeuristicsResult(build, testsForAssign, heuristicsResult);
    if (TeamCityProperties.getBoolean(SHOULD_PERSIST_FILTERED_TESTS_DESCRIPTION)) {
      this.assignerArtifactDao.appendNotApplicableTestsDescription(build, notApplicableTestsDescription);

    }
  }

  /**
   * Handles the assignment of investigations based on configuration.
   *
   * @param build            The failed build.
   * @param project          The associated project.
   * @param failedBuildInfo  The failed build info.
   * @param heuristicsResult The heuristic result of responsible users.
   * @param testsForAssign   The tests ready for assignment.
   * @param problemsForAssign The build problems ready for assignment.
   */
  private void handleAssignments(SBuild build, SProject project, FailedBuildInfo failedBuildInfo, HeuristicResult heuristicsResult, List<STestRun> testsForAssign, List<BuildProblem> problemsForAssign) {
    if (this.customParameters.isBuildFeatureEnabled(build) && !failedBuildInfo.shouldDelayAssignments()) {
      performImmediateAssignment(heuristicsResult, project, build, failedBuildInfo, testsForAssign, problemsForAssign);
    } else {
      handleDelayedOrSkippedAssignments(build, heuristicsResult, problemsForAssign, failedBuildInfo);
    }
  }

  /**
   * Performs immediate assignment of investigations to the responsible users.
   *
   * @param heuristicsResult The heuristic result containing identified responsible users.
   * @param project          The associated project.
   * @param build            The build being processed.
   * @param failedBuildInfo  The failed build info.
   * @param testsForAssign   The tests to be assigned for investigation.
   * @param problemsForAssign The build problems to be assigned for investigation.
   */
  private void performImmediateAssignment(HeuristicResult heuristicsResult, SProject project, SBuild build, FailedBuildInfo failedBuildInfo, List<STestRun> testsForAssign, List<BuildProblem> problemsForAssign) {
    this.failedTestAssigner.assign(heuristicsResult, project, build, testsForAssign);
    this.buildProblemsAssigner.assign(heuristicsResult, project, build, problemsForAssign);
    failedBuildInfo.addHeuristicsResult(heuristicsResult);
  }

  /**
   * Handles delayed or skipped assignments based on build feature settings and assignment delay flags.
   *
   * @param build            The build being processed.
   * @param heuristicsResult The heuristic result containing identified responsible users.
   * @param problemsForAssign The list of build problems considered for assignment.
   * @param failedBuildInfo  The failed build info.
   */
  private void handleDelayedOrSkippedAssignments(SBuild build, HeuristicResult heuristicsResult, List<BuildProblem> problemsForAssign, FailedBuildInfo failedBuildInfo) {
    if (!this.customParameters.isBuildFeatureEnabled(build)) {
      LOGGER.debug(String.format("Build id:%s. Found investigations but build feature is not configured.", build.getBuildId()));
    } else if (failedBuildInfo.shouldDelayAssignments()) {
      List<BuildProblem> forcedAssignInstantlyProblems = problemsForAssign.stream()
                                                                          .filter(problem -> !BuildProblemTypes.TC_EXIT_CODE_TYPE.equals(problem.getBuildProblemData().getType()))
                                                                          .collect(Collectors.toList());

      if (!forcedAssignInstantlyProblems.isEmpty()) {
        this.buildProblemsAssigner.assign(heuristicsResult, getProject(build), build, forcedAssignInstantlyProblems);
      }

      LOGGER.debug(String.format("Build id:%s. Found investigations but assignments should be delayed.", build.getBuildId()));
    }

    failedBuildInfo.addHeuristicsResult(heuristicsResult);
  }
}
