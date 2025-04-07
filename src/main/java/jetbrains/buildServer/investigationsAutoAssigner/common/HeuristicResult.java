

package jetbrains.buildServer.investigationsAutoAssigner.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HeuristicResult {
  private final Map<Integer, Responsibility> testRunToResponsibility = new HashMap<>();
  private final Map<Integer, Responsibility> buildProblemToResponsibility = new HashMap<>();

  public void addResponsibility(@NotNull final STestRun testRun, @NotNull final Responsibility responsibility) {
    testRunToResponsibility.putIfAbsent(testRun.getTestRunId(), responsibility);
  }

  public void addResponsibility(@NotNull final BuildProblem problem, @NotNull final Responsibility responsibility) {
    buildProblemToResponsibility.putIfAbsent(problem.getId(), responsibility);
  }

  @Nullable
  public Responsibility getResponsibility(@NotNull final STestRun testRun) {
    return testRunToResponsibility.get(testRun.getTestRunId());
  }

  @Nullable
  public Responsibility getResponsibility(@NotNull final BuildProblem problem) {
    return buildProblemToResponsibility.get(problem.getId());
  }

  public void merge(@NotNull final HeuristicResult other) {
    testRunToResponsibility.putAll(other.testRunToResponsibility);
    buildProblemToResponsibility.putAll(other.buildProblemToResponsibility);
  }

  public boolean isEmpty() {
    return testRunToResponsibility.isEmpty() && buildProblemToResponsibility.isEmpty();
  }

  public List<Responsibility> getAllResponsibilities() {
    return Stream.concat(testRunToResponsibility.values().stream(),
                         buildProblemToResponsibility.values().stream())
                 .collect(Collectors.toList());
  }

  public Map<Integer, Responsibility> getTestRunResponsibilities() {
    return Collections.unmodifiableMap(testRunToResponsibility);
  }

  public Map<Integer, Responsibility> getBuildProblemResponsibilities() {
    return Collections.unmodifiableMap(buildProblemToResponsibility);
  }
}