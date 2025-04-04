package jetbrains.buildServer.investigationsAutoAssigner.utils;

import com.intellij.openapi.diagnostic.Logger;
import java.util.List;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.common.FailedBuildInfo;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import org.jetbrains.annotations.NotNull;

/**
 * Logs aggregated heuristic results for failed builds, including test failures and build problems.
 */
public class AggregationLogger {
  private static final Logger LOGGER = Constants.AGGREGATION_LOGGER;
  private final CustomParameters customParameters;
  private final WebLinks webLinks;


  /**
   * Constructs an AggregationLogger with the required dependencies.
   *
   * @param webLinks         WebLinks instance for generating build URLs.
   * @param customParameters Custom parameters for checking build feature states.
   */
  public AggregationLogger(@NotNull WebLinks webLinks, @NotNull CustomParameters customParameters) {
    this.webLinks = webLinks;
    this.customParameters = customParameters;
  }

  /**
   * Logs the heuristic results if logging is enabled and the results should be logged.
   *
   * @param failedBuildInfo Information about the failed build.
   */
  public void logResults(@NotNull FailedBuildInfo failedBuildInfo) {
    if (!shouldLog(failedBuildInfo) || !LOGGER.isDebugEnabled()) {
      return;
    }
    LOGGER.debug(getTitle(failedBuildInfo) + ". " +
                 generateReport(failedBuildInfo.getBuild(), failedBuildInfo.getHeuristicsResult()));
  }

  /**
   * Determines whether the heuristic results should be logged.
   *
   * @param failedBuildInfo Information about the failed build.
   * @return true if logging should proceed, false otherwise.
   */
  private boolean shouldLog(@NotNull FailedBuildInfo failedBuildInfo) {
    return !failedBuildInfo.getHeuristicsResult().isEmpty()
           && this.customParameters.isBuildFeatureEnabled(failedBuildInfo.getBuild())
           && !failedBuildInfo.shouldDelayAssignments();
  }

  /**
   * Logs delayed assignment results for a failed build.
   *
   * @param sBuild            The original failed build.
   * @param nextBuild         The build that triggered the assignment.
   * @param heuristicResult   The heuristic results.
   * @param testsForAssign    The tests marked for assignment.
   * @param problemsForAssign The build problems marked for assignment.
   */
  public void logDelayedResults(@NotNull SBuild sBuild, @NotNull SBuild nextBuild,
                                @NotNull HeuristicResult heuristicResult,
                                @NotNull List<STestRun> testsForAssign,
                                @NotNull List<BuildProblem> problemsForAssign) {
    if (!LOGGER.isDebugEnabled() || (testsForAssign.isEmpty() && problemsForAssign.isEmpty())) {
      return;
    }

    String assignTriggerInfo = String.format(
      "Assign was triggered by build '%s'#%s (url: %s).",
      sBuild.getBuildTypeName(), sBuild.getBuildId(), this.webLinks.getViewResultsUrl(nextBuild));

    LOGGER.debug(
      getTitle(new FailedBuildInfo(sBuild)) + ". " + generateReport(sBuild, heuristicResult) + assignTriggerInfo +
      "\n");
  }

  /**
   * Generates a report containing heuristic results for a failed build.
   *
   * @param sBuild           The failed build.
   * @param heuristicsResult The heuristic results.
   * @return A formatted string containing details of test failures and build problems.
   */
  @NotNull
  private String generateReport(@NotNull SBuild sBuild, @NotNull HeuristicResult heuristicsResult) {
    return String.format(
      "Build '%s'#%s (url: %s). Found %s entries:\n%s%s",
      sBuild.getBuildTypeName(), sBuild.getBuildId(), this.webLinks.getViewResultsUrl(sBuild),
      heuristicsResult.getAllResponsibilities().size(),
      generateForFailedTests(sBuild, heuristicsResult),
      generateForBuildProblems(sBuild, heuristicsResult));
  }

  /**
   * Generates a string describing failed test cases assigned to users.
   *
   * @param sBuild           The failed build.
   * @param heuristicsResult The heuristic results.
   * @return A formatted string listing failed test assignments.
   */
  private String generateForFailedTests(@NotNull SBuild sBuild, @NotNull HeuristicResult heuristicsResult) {
    StringBuilder sb = new StringBuilder();
    List<STestRun> testRuns = sBuild.getBuildStatistics(new BuildStatisticsOptions()).getFailedTests();

    for (STestRun testRun : testRuns) {
      Responsibility responsibility = heuristicsResult.getResponsibility(testRun);
      if (responsibility != null) {
        sb.append(formatTestEntry(testRun, responsibility, sBuild));
      }
    }
    return sb.toString();
  }

  /**
   * Generates a string describing build problems assigned to users.
   *
   * @param sBuild           The failed build.
   * @param heuristicsResult The heuristic results.
   * @return A formatted string listing assigned build problems.
   */
  private String generateForBuildProblems(@NotNull SBuild sBuild, @NotNull HeuristicResult heuristicsResult) {
    StringBuilder sb = new StringBuilder();
    List<BuildProblem> allBuildProblems = ((BuildEx)sBuild).getBuildProblems();

    for (BuildProblem buildProblem : allBuildProblems) {
      Responsibility responsibility = heuristicsResult.getResponsibility(buildProblem);
      if (responsibility != null) {
        sb.append(formatBuildProblemEntry(responsibility));
      }
    }
    return sb.toString();
  }

  /**
   * Constructs the title for the log entry based on the build's state.
   *
   * @param failedBuildInfo Information about the failed build.
   * @return A formatted title string.
   */
  private String getTitle(@NotNull FailedBuildInfo failedBuildInfo) {
    SBuild sBuild = failedBuildInfo.getBuild();
    String status = failedBuildInfo.shouldDelayAssignments() ? "New delayed assignment"
                                                             : this.customParameters.isBuildFeatureEnabled(sBuild)
                                                               ? "New assignments"
                                                               : "New suggestions";

    SBuildType sBuildType = sBuild.getBuildType();
    String projectInfo = (sBuildType != null) ? " for project '" + sBuildType.getProject().getFullName() + "'" : "";

    return status + projectInfo;
  }

  /**
   * Formats an entry for a failed test.
   *
   * @param testRun        The failed test run.
   * @param responsibility The assigned responsibility.
   * @param sBuild         The failed build.
   * @return A formatted string describing the test assignment.
   */
  private String formatTestEntry(@NotNull STestRun testRun,
                                 @NotNull Responsibility responsibility,
                                 @NotNull SBuild sBuild) {
    return String.format("* Test entry (url: %s#testNameId%s) for %s. The user %s.\n",
                         this.webLinks.getViewResultsUrl(sBuild), testRun.getTest().getTestNameId(),
                         responsibility.getUser().getDescriptiveName(), responsibility.getDescription());
  }

  /**
   * Formats an entry for a build problem.
   *
   * @param responsibility The assigned responsibility.
   * @return A formatted string describing the build problem assignment.
   */
  private String formatBuildProblemEntry(@NotNull Responsibility responsibility) {
    return String.format("* Build problem entry for %s. The user %s.\n",
                         responsibility.getUser().getDescriptiveName(), responsibility.getDescription());
  }
}
