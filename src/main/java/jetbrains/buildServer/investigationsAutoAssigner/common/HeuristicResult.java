package jetbrains.buildServer.investigationsAutoAssigner.common;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the result of a heuristic analysis, storing responsibilities
 * associated with test runs and build problems.
 * <p>
 * This class allows mapping of {@link STestRun} and {@link BuildProblem} objects
 * to their respective {@link Responsibility} assignments. Key methods include:
 *
 * <ul>
 *     <li>{@link #addResponsibility(STestRun, Responsibility)} - Adds a responsibility for a test run.</li>
 *     <li>{@link #addResponsibility(BuildProblem, Responsibility)} - Adds a responsibility for a build problem.</li>
 *     <li>{@link #getResponsibility(STestRun)} - Retrieves the responsibility for a specific test run.</li>
 *     <li>{@link #merge(HeuristicResult)} - Merges responsibilities from another HeuristicResult.</li>
 * </ul>
 * <p>
 * This class is used to manage and track heuristic results in a test and build problem context.
 */
public class HeuristicResult {

  private final HashMap<Integer, Responsibility> testRun2Responsibility = new HashMap<>();
  private final HashMap<Integer, Responsibility> buildProblem2Responsibility = new HashMap<>();

  /**
   * Adds a responsibility for a test run.
   *
   * @param sTestRun       the test run associated with the responsibility
   * @param responsibility the responsibility assigned to the test run
   */
  public void addResponsibility(final STestRun sTestRun, final Responsibility responsibility) {
    this.testRun2Responsibility.put(sTestRun.getTestRunId(), responsibility);
  }

  /**
   * Adds a responsibility for a build problem.
   *
   * @param buildProblem   the build problem associated with the responsibility
   * @param responsibility the responsibility assigned to the build problem
   */
  public void addResponsibility(final BuildProblem buildProblem, final Responsibility responsibility) {
    this.buildProblem2Responsibility.put(buildProblem.getId(), responsibility);
  }

  /**
   * Retrieves the responsibility for a specific test run.
   *
   * @param sTestRun the test run for which the responsibility is retrieved
   * @return the responsibility associated with the test run, or null if no responsibility exists
   */
  @Nullable
  public Responsibility getResponsibility(final STestRun sTestRun) {
    return this.testRun2Responsibility.get(sTestRun.getTestRunId());
  }

  /**
   * Retrieves the responsibility for a specific build problem.
   *
   * @param buildProblem the build problem for which the responsibility is retrieved
   * @return the responsibility associated with the build problem, or null if no responsibility exists
   */
  @Nullable
  public Responsibility getResponsibility(final BuildProblem buildProblem) {
    return this.buildProblem2Responsibility.get(buildProblem.getId());
  }

  /**
   * Merges the responsibilities from another HeuristicResult into this one.
   *
   * @param heuristicResult the HeuristicResult to merge
   */
  public void merge(final HeuristicResult heuristicResult) {
    this.testRun2Responsibility.putAll(heuristicResult.testRun2Responsibility);
    this.buildProblem2Responsibility.putAll(heuristicResult.buildProblem2Responsibility);
  }

  /**
   * Checks if the result is empty, i.e., no responsibilities are assigned to test runs or build problems.
   *
   * @return true if there are no responsibilities, false otherwise
   */
  public boolean isEmpty() {
    return this.testRun2Responsibility.isEmpty() && this.buildProblem2Responsibility.isEmpty();
  }

  /**
   * Retrieves all responsibilities in a combined list from both test runs and build problems.
   *
   * @return a list of all responsibilities
   */
  public List<Responsibility> getAllResponsibilities() {
    return Stream.concat(this.testRun2Responsibility.values().stream(),
                         this.buildProblem2Responsibility.values().stream()).collect(Collectors.toList());
  }
}
