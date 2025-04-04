package jetbrains.buildServer.investigationsAutoAssigner.common;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import jetbrains.buildServer.investigationsAutoAssigner.utils.CustomParameters;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import org.jetbrains.annotations.NotNull;

/**
 * Holds information about a failed build, including processed tests, build problems, and heuristic results.
 * <p>
 * Key methods include:
 * <ul>
 *     <li>{@link #addProcessedTestRuns(Collection)} - Adds a collection of processed test runs.</li>
 *     <li>{@link #addProcessedBuildProblems(Collection)} - Adds a collection of processed build problems.</li>
 *     <li>{@link #checkNotProcessed(STestRun)} - Checks if a test run has been processed.</li>
 *     <li>{@link #checkNotProcessed(BuildProblem)} - Checks if a build problem has been processed.</li>
 *     <li>{@link #addHeuristicsResult(HeuristicResult)} - Merges heuristic results into the current build info.</li>
 *     <li>{@link #increaseProcessedNumber(int)} - Increases the number of processed problems.</li>
 * </ul>
 */
public class FailedBuildInfo {

  private final SBuild build;
  private final int threshold;
  private final Set<Integer> processedTests = new HashSet<>();
  private final Set<Integer> processedBuildProblems = new HashSet<>();
  private final HeuristicResult heuristicResult = new HeuristicResult();
  private int processedCount = 0;

  /**
   * Constructs a new FailedBuildInfo instance for the given build.
   *
   * @param build the build associated with the failed build information
   */
  public FailedBuildInfo(@NotNull final SBuild build) {
    this.build = build;
    this.threshold = CustomParameters.getMaxTestsPerBuildThreshold(build);
  }

  /**
   * Adds a collection of processed test runs to the information.
   *
   * @param tests the collection of test runs to add
   */
  public void addProcessedTestRuns(@NotNull Collection<STestRun> tests) {
    this.processedTests.addAll(tests.stream().map(STestRun::getTestRunId).collect(Collectors.toSet()));
  }

  /**
   * Adds a collection of processed build problems to the information.
   *
   * @param buildProblems the collection of build problems to add
   */
  public void addProcessedBuildProblems(@NotNull Collection<BuildProblem> buildProblems) {
    this.processedBuildProblems.addAll(buildProblems.stream().map(BuildProblem::getId).collect(Collectors.toSet()));
  }

  /**
   * Checks if a test run has not been processed.
   *
   * @param sTestRun the test run to check
   * @return true if the test run has not been processed, false otherwise
   */
  public boolean checkNotProcessed(STestRun sTestRun) {
    return !this.processedTests.contains(sTestRun.getTestRunId());
  }

  /**
   * Checks if a build problem has not been processed.
   *
   * @param buildProblem the build problem to check
   * @return true if the build problem has not been processed, false otherwise
   */
  public boolean checkNotProcessed(final BuildProblem buildProblem) {
    return !this.processedBuildProblems.contains(buildProblem.getId());
  }

  /**
   * Merges heuristic results into the current build information.
   *
   * @param heuristicsResult the heuristic result to merge
   */
  public void addHeuristicsResult(final HeuristicResult heuristicsResult) {
    this.heuristicResult.merge(heuristicsResult);
  }

  /**
   * Retrieves the heuristic results associated with the failed build.
   *
   * @return the heuristic results
   */
  public HeuristicResult getHeuristicsResult() {
    return this.heuristicResult;
  }

  /**
   * Checks if assignments should be delayed based on custom parameters.
   *
   * @return true if assignments should be delayed, false otherwise
   */
  public boolean shouldDelayAssignments() {
    return CustomParameters.shouldDelayAssignments(this.build);
  }

  /**
   * Checks if the number of processed problems has reached the threshold.
   *
   * @return true if the processed problems count has reached or exceeded the threshold, false otherwise
   */
  public boolean isOverProcessedProblemsThreshold() {
    return this.processedCount >= this.threshold;
  }

  /**
   * Retrieves the limit of problems that can still be processed, based on the threshold.
   *
   * @return the remaining number of problems that can be processed
   */
  public int getLimitToProcess() {
    return Math.max(0, this.threshold - this.processedCount);
  }

  /**
   * Increases the number of processed problems.
   *
   * @param numberOfProcessedProblems the number of problems to increase the processed count by
   * @throws IllegalArgumentException if the number of processed problems is negative
   */
  public void increaseProcessedNumber(final int numberOfProcessedProblems) {
    if (numberOfProcessedProblems < 0) {
      throw new IllegalArgumentException("Processed problems count cannot be negative.");
    }
    this.processedCount = Math.addExact(this.processedCount, numberOfProcessedProblems); // Prevents overflow
  }

  /**
   * Retrieves the build associated with this failed build information.
   *
   * @return the build associated with this information
   */
  @NotNull
  public SBuild getBuild() {
    return this.build;
  }

  /**
   * Retrieves the build ID associated with this failed build information.
   *
   * @return the ID of the build
   */
  public long getBuildId() {
    return this.build.getBuildId();
  }
}
