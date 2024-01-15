

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
import org.jetbrains.annotations.Nullable;

public class AggregationLogger {
  private static final Logger LOGGER = Constants.AGGREGATION_LOGGER;
  private final CustomParameters myCustomParameters;
  @NotNull private final WebLinks myWebLinks;

  public AggregationLogger(@NotNull WebLinks webLinks,
                           @NotNull CustomParameters customParameters) {
    myWebLinks = webLinks;
    myCustomParameters = customParameters;

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
           myCustomParameters.isBuildFeatureEnabled(sBuild) &&
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

    final FailedBuildInfo failedBuildInfo = new FailedBuildInfo(sBuild, true);
    String assignTriggeredBy = String.format("Assign was triggered by build '%s'#%s (url: %s).",
                                             sBuild.getBuildTypeName(),
                                             sBuild.getBuildId(),
                                             myWebLinks.getViewResultsUrl(nextBuild));
    LOGGER.debug(getTitle(failedBuildInfo) + ". " + generateReport(sBuild, heuristicResult) + assignTriggeredBy + "\n");
  }

  private String getTitle(final FailedBuildInfo failedBuildInfo) {
    SBuild sBuild = failedBuildInfo.getBuild();

    StringBuilder sb = new StringBuilder();
    if (failedBuildInfo.shouldDelayAssignments()) {
      sb.append("New delayed assignment");
    } else if (myCustomParameters.isBuildFeatureEnabled(sBuild)) {
      sb.append("New assignments");
    } else {
      sb.append("New suggestions");
    }
    sb.append(" for ");
    @Nullable
    SBuildType sBuildType = sBuild.getBuildType();
    if (sBuildType != null) {
      sb.append("project '").append(sBuildType.getProject().getFullName()).append("'");
    }

    return sb.toString();
  }

  @NotNull
  private String generateReport(final SBuild sBuild, final HeuristicResult heuristicsResult) {
    String buildRunResultsUrl = myWebLinks.getViewResultsUrl(sBuild);

    return String.format("Build '%s'#%s (url: %s). " +
                         "Found %s entries:\n" +
                         "%s%s",
                         sBuild.getBuildTypeName(),
                         sBuild.getBuildId(),
                         buildRunResultsUrl,
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
      if (responsibility == null) {
        continue;
      }

      sb.append(String.format("* test entry (url: %s) for %s. The user %s.\n",
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
      if (responsibility == null) {
        continue;
      }

      sb.append(String.format("* build problem entry for %s. The user %s.\n",
                              responsibility.getUser().getDescriptiveName(),
                              responsibility.getDescription()));
    }

    return sb.toString();
  }
}