

package jetbrains.buildServer.investigationsAutoAssigner.utils;

import com.intellij.openapi.diagnostic.Logger;
import java.util.List;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.common.FailedBuildInfo;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.serverSide.BuildEx;
import jetbrains.buildServer.serverSide.BuildStatisticsOptions;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.WebLinks;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AggregationLogger {
  private static final Logger LOGGER = Constants.AGGREGATION_LOGGER;
  @NotNull private final WebLinks myWebLinks;

  public AggregationLogger(@NotNull WebLinks webLinks) {
    this.myWebLinks = webLinks;
  }

  public void logResults(FailedBuildInfo failedBuildInfo) {
    SBuild sBuild = failedBuildInfo.getBuild();
    HeuristicResult heuristicsResult = failedBuildInfo.getHeuristicsResult();
    if (shouldLog(failedBuildInfo) && LOGGER.isDebugEnabled()) {
      LOGGER.debug(getTitle(failedBuildInfo) + ". " + generateReport(sBuild, heuristicsResult));
    }
  }

  private boolean shouldLog(FailedBuildInfo failedBuildInfo) {
    SBuild sBuild = failedBuildInfo.getBuild();
    HeuristicResult heuristicsResult = failedBuildInfo.getHeuristicsResult();

    return !heuristicsResult.isEmpty() &&
           CustomParameters.isBuildFeatureEnabled(sBuild) &&
           !failedBuildInfo.shouldDelayAssignments();
  }

  public void logDelayedResults(@NotNull final SBuild sBuild,
                                @NotNull final SBuild nextBuild,
                                @NotNull final HeuristicResult heuristicResult,
                                @NotNull final List<STestRun> testsForAssign,
                                @NotNull final List<BuildProblem> problemsForAssign) {
    if (!LOGGER.isDebugEnabled() || (testsForAssign.isEmpty() && problemsForAssign.isEmpty())) {
      return;
    }

    final FailedBuildInfo failedBuildInfo = new FailedBuildInfo(sBuild);
    String assignTriggeredBy = String.format("Assign was triggered by build '%s'#%s (url: %s).",
                                             sBuild.getBuildTypeName(),
                                             sBuild.getBuildId(),
                                             myWebLinks.getViewResultsUrl(nextBuild));
    LOGGER.debug(getTitle(failedBuildInfo) + ". " + generateReport(sBuild, heuristicResult) + assignTriggeredBy + "\n");
  }

  private String getTitle(final FailedBuildInfo failedBuildInfo) {
    SBuild sBuild = failedBuildInfo.getBuild();

    StringBuilder sb = new StringBuilder();
    if (failedBuildInfo.shouldDelayAssignments()) sb.append("New delayed assignment");
    else if (CustomParameters.isBuildFeatureEnabled(sBuild)) sb.append("New assignments");
    else sb.append("New suggestions");
    sb.append(" for ");

    @Nullable
    SBuildType sBuildType = sBuild.getBuildType();
    if (sBuildType != null) sb.append("project '").append(sBuildType.getProject().getFullName()).append("'");

    return sb.toString();
  }

  @NotNull
  private String generateReport(final SBuild sBuild, final HeuristicResult heuristicsResult) {
    return String.format("Build '%s'#%s (url: %s). Found %s entries: %n%s%s",
                         sBuild.getBuildTypeName(),
                         sBuild.getBuildId(),
                         myWebLinks.getViewResultsUrl(sBuild),
                         heuristicsResult.getAllResponsibilities().size(),
                         generateForFailedTests(sBuild, heuristicsResult),
                         generateForBuildProblems(sBuild, heuristicsResult));
  }

  private String generateForFailedTests(SBuild sBuild, HeuristicResult heuristicsResult) {
    StringBuilder sb = new StringBuilder();
    String buildRunResultsUrl = myWebLinks.getViewResultsUrl(sBuild);

    List<STestRun> testRuns = sBuild.getBuildStatistics(new BuildStatisticsOptions()).getFailedTests();

    for (STestRun testRun : testRuns) {
      Responsibility responsibility = heuristicsResult.getResponsibility(testRun);
      if (responsibility == null) continue;

      sb.append(String.format("* test entry (url: %s) for %s. The user %s. %n",
                              buildRunResultsUrl + "#testNameId" + testRun.getTest().getTestNameId(),
                              responsibility.getUser().getDescriptiveName(),
                              responsibility.getDescription()));
    }

    return sb.toString();
  }

  private String generateForBuildProblems(final SBuild sBuild, final HeuristicResult heuristicsResult) {
    StringBuilder sb = new StringBuilder();
    List<BuildProblem> allBuildProblems = ((BuildEx)sBuild).getBuildProblems();
    for (BuildProblem buildProblem : allBuildProblems) {
      Responsibility responsibility = heuristicsResult.getResponsibility(buildProblem);
      if (responsibility == null) continue;

      sb.append(String.format("* build problem entry for %s. The user %s. %n",
                              responsibility.getUser().getDescriptiveName(),
                              responsibility.getDescription()));
    }

    return sb.toString();
  }
}