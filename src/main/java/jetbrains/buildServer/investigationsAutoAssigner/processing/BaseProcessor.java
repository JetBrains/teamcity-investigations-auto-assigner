

package jetbrains.buildServer.investigationsAutoAssigner.processing;

import com.intellij.openapi.diagnostic.Logger;
import java.util.List;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.serverSide.BuildStatistics;
import jetbrains.buildServer.serverSide.BuildStatisticsOptions;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

abstract class BaseProcessor {
  private static final Logger LOGGER = Constants.LOGGER;
  private static final String TESTS_CHANGED_LOG = "Build #%d: number of applicable tests changed because %d became not applicable";
  private static final String PROBLEMS_CHANGED_LOG = "Build #%d: number of applicable problems changed because %d became not applicable";
  private static final String PROBLEMS_NUMBER_LOG = "Build #%d: found %d applicable build problems and %d applicable failed tests.";
  private static final String NO_BUILD_TYPE_LOG = "Build #%d doesn't have a build type. Stop processing.";

  protected List<STestRun> requestBrokenTestsWithStats(final SBuild build) {
    BuildStatisticsOptions options = new BuildStatisticsOptions(BuildStatisticsOptions.FIXED_IN_BUILD, 0);
    BuildStatistics stats = build.getBuildStatistics(options);

    return stats.getFailedTests();
  }

  protected void logChangedProblemsNumber(SBuild sBuild,
                                          final List<STestRun> beforeFilteringTests,
                                          final List<STestRun> afterFilteringTests,
                                          final List<BuildProblem> beforeFilteringProblems,
                                          final List<BuildProblem> afterFilteringProblems) {
    if (!LOGGER.isDebugEnabled()) return;

    if (beforeFilteringTests.size() != afterFilteringTests.size()) {
        LOGGER.debug(String.format(TESTS_CHANGED_LOG, sBuild.getBuildId(), beforeFilteringTests.size() - afterFilteringTests.size()));
    }
    if (beforeFilteringProblems.size() != afterFilteringProblems.size()) {
      LOGGER.debug(String.format(PROBLEMS_CHANGED_LOG, sBuild.getBuildId(), beforeFilteringProblems.size() - afterFilteringProblems.size()));
    }
  }

  protected void logProblemsNumber(SBuild sBuild,
                                   final List<STestRun> afterFilteringTests,
                                   final List<BuildProblem> afterFilteringProblems) {
    if (!LOGGER.isDebugEnabled()) return;
    LOGGER.debug(String.format(PROBLEMS_NUMBER_LOG, sBuild.getBuildId(), afterFilteringProblems.size(), afterFilteringTests.size()));
  }

  @Nullable
  protected SProject getProject(@NotNull final SBuild sBuild) {
    SBuildType sBuildType = sBuild.getBuildType();
    if (sBuildType == null) {
      LOGGER.debug(String.format(NO_BUILD_TYPE_LOG, sBuild.getBuildId()));
      return null;
    }

    SProject project = sBuildType.getProject();
    while (project != null && project.isVirtual()) {
      project = project.getParentProject();
    }
    return project;
  }
}