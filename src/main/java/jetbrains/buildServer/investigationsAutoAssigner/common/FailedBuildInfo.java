

package jetbrains.buildServer.investigationsAutoAssigner.common;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import jetbrains.buildServer.investigationsAutoAssigner.utils.CustomParameters;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import org.jetbrains.annotations.NotNull;

public class FailedBuildInfo {

  private final SBuild myBuild;
  private final int myThreshold;
  private final Set<Integer> myProcessedTests = new HashSet<>();
  private final Set<Integer> myProcessedBuildProblems = new HashSet<>();
  private final HeuristicResult myHeuristicResult = new HeuristicResult();
  private int myProcessedCount = 0;

  public FailedBuildInfo(@NotNull final SBuild build) {
    myBuild = build;
    myThreshold = CustomParameters.getMaxTestsPerBuildThreshold(build);
  }

  @NotNull
  public SBuild getBuild() {
    return myBuild;
  }

  public long getBuildId() {
    return myBuild.getBuildId();
  }

  public void addProcessedTestRuns(@NotNull Collection<STestRun> tests) {
    for (STestRun testRun : tests) {
      myProcessedTests.add(testRun.getTestRunId());
    }
  }

  public void addProcessedBuildProblems(@NotNull Collection<BuildProblem> buildProblems) {
    for (BuildProblem buildProblem : buildProblems) {
      myProcessedBuildProblems.add(buildProblem.getId());
    }
  }

  public boolean checkNotProcessed(STestRun sTestRun) {
    return !myProcessedTests.contains(sTestRun.getTestRunId());
  }

  public boolean checkNotProcessed(final BuildProblem buildProblem) {
    return !myProcessedBuildProblems.contains(buildProblem.getId());
  }

  public void addHeuristicsResult(final HeuristicResult heuristicsResult) {
    myHeuristicResult.merge(heuristicsResult);
  }

  public HeuristicResult getHeuristicsResult() {
    return myHeuristicResult;
  }

  public boolean shouldDelayAssignments() {
    return CustomParameters.shouldDelayAssignments(myBuild);
  }

  public boolean isOverProcessedProblemsThreshold() {
    return getLimitToProcess() <= 0;
  }

  public int getLimitToProcess() {
    return myThreshold - myProcessedCount;
  }

  public void increaseProcessedNumber(final int numberOfProcessedProblems) {
    myProcessedCount += numberOfProcessedProblems;
  }
}