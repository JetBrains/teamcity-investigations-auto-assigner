package jetbrains.buildServer.investigationsAutoAssigner.processing;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.common.FailedBuildInfo;
import jetbrains.buildServer.serverSide.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Filters failed tests before they are assigned for investigation.
 */
@Component
public class FailedTestFilter {

  private static final Logger LOGGER = Constants.LOGGER;
  private final TestRunFilter testRunFilter;

  /**
   * Constructs a new {@code FailedTestFilter}.
   *
   * @param testRunFilter The filter responsible for determining applicable test runs.
   */
  public FailedTestFilter(@NotNull TestRunFilter testRunFilter) {
    this.testRunFilter = testRunFilter;
  }

  public List<STestRun> apply(@NotNull FailedBuildInfo failedBuildInfo,
                              @NotNull SProject project,
                              @NotNull List<STestRun> testRuns) {
    return apply(failedBuildInfo, project, testRuns, new HashMap<>());
  }

  public List<STestRun> apply(@NotNull FailedBuildInfo failedBuildInfo,
                              @NotNull SProject project,
                              @NotNull List<STestRun> testRuns,
                              @NotNull Map<Long, String> notApplicableTestDescription) {
    SBuild build = failedBuildInfo.getBuild();

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(String.format("Filtering of failed tests for build id:%s started", build.getBuildId()));
    }

    List<STestRun> filteredTestRuns = this.testRunFilter.filterApplicableTests(project, build, testRuns, notApplicableTestDescription);

    failedBuildInfo.addProcessedTestRuns(testRuns);
    failedBuildInfo.increaseProcessedNumber(filteredTestRuns.size());

    return filteredTestRuns;
  }

  public List<STestRun> getStillApplicable(@NotNull FailedBuildInfo failedBuildInfo,
                                           @NotNull SProject project,
                                           @NotNull List<STestRun> testRuns) {
    return getStillApplicable(failedBuildInfo, project, testRuns, new HashMap<>());
  }

  public List<STestRun> getStillApplicable(@NotNull FailedBuildInfo failedBuildInfo,
                                           @NotNull SProject project,
                                           @NotNull List<STestRun> testRuns,
                                           @NotNull Map<Long, String> notApplicableTestDescription) {
    SBuild build = failedBuildInfo.getBuild();

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(String.format("Filtering before assign of failed tests for build id:%s started", build.getBuildId()));
    }

    return this.testRunFilter.filterApplicableTests(project, build, testRuns, notApplicableTestDescription);
  }
}
