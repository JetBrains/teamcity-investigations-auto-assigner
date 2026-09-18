

package jetbrains.buildServer.investigationsAutoAssigner.common;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import org.jetbrains.annotations.Nullable;

public class HeuristicResult {

  private final HashMap<Integer, Responsibility> testRun2Responsibility;
  private final HashMap<Integer, Responsibility> buildProblem2Responsibility;

  public HeuristicResult() {
    testRun2Responsibility = new HashMap<>();
    buildProblem2Responsibility = new HashMap<>();
  }

  public void addResponsibility(final STestRun sTestRun, final Responsibility responsibility) {
    testRun2Responsibility.put(sTestRun.getTestRunId(), responsibility);
  }

  public void addResponsibility(final BuildProblem buildProblem, final Responsibility responsibility) {
    buildProblem2Responsibility.put(buildProblem.getId(), responsibility);
  }

  @Nullable
  public Responsibility getResponsibility(final STestRun sTestRun) {
    return testRun2Responsibility.get(sTestRun.getTestRunId());
  }

  @Nullable
  public Responsibility getResponsibility(final BuildProblem buildProblem) {
    return buildProblem2Responsibility.get(buildProblem.getId());
  }

  public void merge(final HeuristicResult heuristicResult) {
    testRun2Responsibility.putAll(heuristicResult.testRun2Responsibility);
    buildProblem2Responsibility.putAll(heuristicResult.buildProblem2Responsibility);
  }

  public boolean isEmpty() {
    return testRun2Responsibility.isEmpty() && buildProblem2Responsibility.isEmpty();
  }

  public List<Responsibility> getAllResponsibilities() {
    return Stream.concat(testRun2Responsibility.values().stream(), buildProblem2Responsibility.values().stream())
                 .collect(Collectors.toList());
  }
}
