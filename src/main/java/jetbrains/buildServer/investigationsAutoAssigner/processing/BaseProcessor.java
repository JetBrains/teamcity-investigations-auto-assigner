package jetbrains.buildServer.investigationsAutoAssigner.processing;

import com.intellij.openapi.diagnostic.Logger;
import java.util.List;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract base class for processing build-related data.
 * <p>
 * This class provides methods for:
 * <ul>
 *   <li>Retrieving test statistics from builds</li>
 *   <li>Logging changes in test and problem counts</li>
 *   <li>Fetching project details associated with a build</li>
 * </ul>
 * </p>
 *
 * @see jetbrains.buildServer.investigationsAutoAssigner.processing.BaseAssigner
 */
abstract class BaseProcessor {
  private static final Logger LOGGER = Constants.LOGGER;

  /**
   * Retrieves the list of failed tests associated with a build, along with their statistics.
   *
   * @param build the build for which the failed tests are to be retrieved
   * @return a list of {@link STestRun} objects representing the failed tests
   */
  @NotNull
  protected List<STestRun> requestBrokenTestsWithStats(@NotNull final SBuild build) {
    BuildStatisticsOptions options = new BuildStatisticsOptions(BuildStatisticsOptions.FIXED_IN_BUILD, 0);
    return build.getBuildStatistics(options).getFailedTests();
  }

  /**
   * Logs the change in the number of applicable tests and problems before and after filtering.
   *
   * @param sBuild the build associated with the tests and problems
   * @param beforeFilteringTests the list of tests before filtering
   * @param afterFilteringTests the list of tests after filtering
   * @param beforeFilteringProblems the list of problems before filtering
   * @param afterFilteringProblems the list of problems after filtering
   */
  protected void logChangedProblemsNumber(@NotNull SBuild sBuild,
                                          @NotNull final List<STestRun> beforeFilteringTests,
                                          @NotNull final List<STestRun> afterFilteringTests,
                                          @NotNull final List<BuildProblem> beforeFilteringProblems,
                                          @NotNull final List<BuildProblem> afterFilteringProblems) {

    if (!LOGGER.isDebugEnabled()) {
      return;
    }

    int testDifference = beforeFilteringTests.size() - afterFilteringTests.size();
    if (testDifference != 0) {
      LOGGER.debug("Build #" + sBuild.getBuildId() + ": number of applicable tests changed by " + testDifference);
    }

    int problemDifference = beforeFilteringProblems.size() - afterFilteringProblems.size();
    if (problemDifference != 0) {
      LOGGER.debug("Build #" + sBuild.getBuildId() + ": number of applicable problems changed by " + problemDifference);
    }
  }

  /**
   * Logs the number of applicable tests and problems found after filtering.
   *
   * @param sBuild the build associated with the tests and problems
   * @param afterFilteringTests the list of tests after filtering
   * @param afterFilteringProblems the list of problems after filtering
   */
  protected void logProblemsNumber(@NotNull SBuild sBuild,
                                   @NotNull final List<STestRun> afterFilteringTests,
                                   @NotNull final List<BuildProblem> afterFilteringProblems) {

    if (!LOGGER.isDebugEnabled()) {
      return;
    }

    LOGGER.debug(
      "Build #" + sBuild.getBuildId() + ": found " + afterFilteringProblems.size() + " applicable build problems and " +
      afterFilteringTests.size() + " applicable failed tests.");
  }

  /**
   * Retrieves the project associated with a build.
   * <p>
   * If the build is part of a virtual project, the method will traverse upwards to find the real parent project.
   * </p>
   *
   * @param sBuild the build for which the project is to be retrieved
   * @return the {@link SProject} associated with the build, or {@code null} if no project is found
   */
  @Nullable
  protected SProject getProject(@NotNull final SBuild sBuild) {
    SBuildType sBuildType = sBuild.getBuildType();
    if (sBuildType == null) {
      LOGGER.debug("Build #" + sBuild.getBuildId() + " doesn't have a build type. Stopping processing.");
      return null;
    }

    SProject project = sBuildType.getProject();
    while (project != null && project.isVirtual()) {
      project = project.getParentProject();
    }
    return project;
  }
}
