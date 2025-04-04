package jetbrains.buildServer.investigationsAutoAssigner.processing;

import com.intellij.openapi.diagnostic.Logger;
import java.util.List;
import java.util.stream.Collectors;
import jetbrains.buildServer.BuildProblemTypes;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.common.FailedBuildInfo;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.utils.AggregationLogger;
import jetbrains.buildServer.serverSide.BuildEx;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import org.jetbrains.annotations.NotNull;

/**
 * Processes delayed assignments of build problems and failed tests by verifying their continued
 * applicability in subsequent builds before assignment.
 */
public class DelayedAssignmentsProcessor extends BaseProcessor {
  private static final Logger LOGGER = Constants.LOGGER;

  private final BuildProblemsFilter buildProblemsFilter;
  private final FailedTestFilter failedTestFilter;
  private final AggregationLogger aggregationLogger;
  private final BuildProblemsAssigner buildProblemsAssigner;
  private final FailedTestAssigner failedTestAssigner;

  /**
   * Constructs a DelayedAssignmentsProcessor with required dependencies.
   *
   * @param buildProblemsAssigner handles assignment of build problems
   * @param failedTestAssigner    handles assignment of failed tests
   * @param buildProblemsFilter   filters applicable build problems
   * @param failedTestFilter      filters applicable failed tests
   * @param aggregationLogger     logs aggregation results
   */
  public DelayedAssignmentsProcessor(@NotNull final BuildProblemsAssigner buildProblemsAssigner,
                                     @NotNull final FailedTestAssigner failedTestAssigner,
                                     @NotNull final BuildProblemsFilter buildProblemsFilter,
                                     @NotNull final FailedTestFilter failedTestFilter,
                                     @NotNull final AggregationLogger aggregationLogger) {
    this.buildProblemsAssigner = buildProblemsAssigner;
    this.failedTestAssigner = failedTestAssigner;
    this.buildProblemsFilter = buildProblemsFilter;
    this.failedTestFilter = failedTestFilter;
    this.aggregationLogger = aggregationLogger;
  }

  /**
   * Processes delayed assignments for a failed build by checking which problems/tests
   * from the original build are still applicable in the next build.
   *
   * @param failedBuildInfo contains information about the failed build
   * @param nextBuild       the subsequent build to check for problem continuity
   */
  public void processBuild(@NotNull final FailedBuildInfo failedBuildInfo, @NotNull final SBuild nextBuild) {
    final SBuild build = failedBuildInfo.getBuild();
    final SProject project = getProject(build);
    if (project == null) {
      return;
    }

    logDebug("Start processing delayed assignments for build #" + build.getBuildId() + ".");

    final HeuristicResult heuristicsResult = failedBuildInfo.getHeuristicsResult();
    final List<STestRun> applicableFailedTests = getApplicableFailedTests(build, heuristicsResult);
    final List<BuildProblem> applicableProblems = getApplicableBuildProblems(build, nextBuild, heuristicsResult);

    logProblemsNumber(build, applicableFailedTests, applicableProblems);

    final List<STestRun> testsForAssign =
      this.failedTestFilter.getStillApplicable(failedBuildInfo, project, applicableFailedTests);
    final List<BuildProblem> problemsForAssign =
      this.buildProblemsFilter.getStillApplicable(failedBuildInfo, project, applicableProblems);

    logChangedProblemsNumber(build, applicableFailedTests, testsForAssign, applicableProblems, problemsForAssign);

    assignProblemsAndTests(heuristicsResult, project, build, testsForAssign, problemsForAssign);
    this.aggregationLogger.logDelayedResults(build, nextBuild, heuristicsResult, testsForAssign, problemsForAssign);
  }

  /**
   * Retrieves applicable failed tests that have assigned responsibilities.
   *
   * @param build            the build to check
   * @param heuristicsResult the heuristic results containing responsibility assignments
   * @return list of applicable failed tests
   */
  @NotNull
  private List<STestRun> getApplicableFailedTests(@NotNull final SBuild build,
                                                  @NotNull final HeuristicResult heuristicsResult) {
    return requestBrokenTestsWithStats(build).stream().filter(
      failedTest -> heuristicsResult.getResponsibility(failedTest) != null).collect(Collectors.toList());
  }

  /**
   * Retrieves applicable build problems that are still present in the next build.
   *
   * @param build            the original build
   * @param nextBuild        the subsequent build
   * @param heuristicsResult the heuristic results containing responsibility assignments
   * @return list of applicable build problems
   */
  @NotNull
  private List<BuildProblem> getApplicableBuildProblems(@NotNull final SBuild build,
                                                        @NotNull final SBuild nextBuild,
                                                        @NotNull final HeuristicResult heuristicsResult) {
    final List<String> nextBuildProblemIdentities = getBuildProblemIdentities((BuildEx)nextBuild);

    return ((BuildEx)build).getBuildProblems().stream().filter(
                             buildProblem -> isProblemApplicable(buildProblem, heuristicsResult, nextBuildProblemIdentities))
                           .collect(Collectors.toList());
  }

  /**
   * Checks if a build problem is still applicable in the next build.
   *
   * @param buildProblem               the build problem to check
   * @param heuristicsResult           the heuristic results
   * @param nextBuildProblemIdentities identities of problems in the next build
   * @return true if the problem is still applicable, false otherwise
   */
  private boolean isProblemApplicable(@NotNull final BuildProblem buildProblem,
                                      @NotNull final HeuristicResult heuristicsResult,
                                      @NotNull final List<String> nextBuildProblemIdentities) {
    return heuristicsResult.getResponsibility(buildProblem) != null &&
           nextBuildProblemIdentities.contains(buildProblem.getBuildProblemData().getIdentity()) &&
           BuildProblemTypes.TC_EXIT_CODE_TYPE.equals(buildProblem.getBuildProblemData().getType());
  }

  /**
   * Extracts build problem identities from a build.
   *
   * @param build the build to extract from
   * @return list of build problem identities
   */
  @NotNull
  private List<String> getBuildProblemIdentities(@NotNull final BuildEx build) {
    return build.getBuildProblems().stream().map(buildProblem -> buildProblem.getBuildProblemData().getIdentity())
                .collect(Collectors.toList());
  }

  /**
   * Assigns problems and tests to responsible users.
   *
   * @param heuristicsResult  the heuristic results
   * @param project           the current project
   * @param build             the current build
   * @param testsForAssign    tests to assign
   * @param problemsForAssign problems to assign
   */
  private void assignProblemsAndTests(@NotNull final HeuristicResult heuristicsResult,
                                      @NotNull final SProject project,
                                      @NotNull final SBuild build,
                                      @NotNull final List<STestRun> testsForAssign,
                                      @NotNull final List<BuildProblem> problemsForAssign) {
    this.failedTestAssigner.assign(heuristicsResult, project, build, testsForAssign);
    this.buildProblemsAssigner.assign(heuristicsResult, project, build, problemsForAssign);
  }

  /**
   * Helper method for debug logging that checks debug level before logging.
   *
   * @param message the message to log
   */
  private void logDebug(@NotNull final String message) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(message);
    }
  }
}