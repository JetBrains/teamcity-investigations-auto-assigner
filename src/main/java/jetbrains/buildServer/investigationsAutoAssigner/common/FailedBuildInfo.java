/*
 * Copyright 2000-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

  private final SBuild mySBuild;
  private final int myThreshold;
  private Set<Integer> processedTests = new HashSet<>();
  private Set<Integer> processedBuildProblems = new HashSet<>();
  private HeuristicResult myHeuristicResult = new HeuristicResult();
  private final boolean myShouldDelayAssignments;
  private int myProcessedCount = 0;

  public FailedBuildInfo(final SBuild sBuild) {
    mySBuild = sBuild;
    myShouldDelayAssignments = CustomParameters.shouldDelayAssignments(sBuild);
    myThreshold = CustomParameters.getMaxTestsPerBuildThreshold(sBuild);
  }

  @NotNull
  public SBuild getBuild() {
    return mySBuild;
  }

  public void addProcessedTestRuns(@NotNull Collection<STestRun> tests) {
    for (STestRun testRun : tests) {
      processedTests.add(testRun.getTestRunId());
    }
  }

  public void addProcessedBuildProblems(@NotNull Collection<BuildProblem> buildProblems) {
    for (BuildProblem buildProblem : buildProblems) {
      processedBuildProblems.add(buildProblem.getId());
    }
  }

  public boolean checkNotProcessed(STestRun sTestRun) {
    return !processedTests.contains(sTestRun.getTestRunId());
  }

  public boolean checkNotProcessed(final BuildProblem buildProblem) {
    return !processedBuildProblems.contains(buildProblem.getId());
  }

  public void addHeuristicsResult(final HeuristicResult heuristicsResult) {
    myHeuristicResult.merge(heuristicsResult);
  }

  public HeuristicResult getHeuristicsResult() {
    return myHeuristicResult;
  }

  public boolean shouldDelayAssignments() {
    return myShouldDelayAssignments;
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