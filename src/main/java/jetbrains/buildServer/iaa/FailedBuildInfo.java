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

package jetbrains.buildServer.iaa;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import org.jetbrains.annotations.NotNull;

class FailedBuildInfo {

  private SBuild mySBuild;
  private Set<Integer> processedTests = new HashSet<>();
  private Set<Integer> processedBuildProblems = new HashSet<>();
  int processed = 0;

  FailedBuildInfo(final SBuild sBuild) {
    mySBuild = sBuild;
  }

  public SBuild getSBuild() {
    return mySBuild;
  }

  void addProcessedTestRuns(@NotNull Collection<STestRun> tests) {
    for (STestRun testRun : tests) {
      processedTests.add(testRun.getTestRunId());
    }
  }

  void addProcessedBuildProblems(@NotNull Collection<BuildProblem> buildProblems) {
    for (BuildProblem buildProblem : buildProblems) {
      processedBuildProblems.add(buildProblem.getId());
    }
  }

  boolean checkNotProcessed(STestRun sTestRun) {
    return !processedTests.contains(sTestRun.getTestRunId());
  }

  boolean checkNotProcessed(final BuildProblem buildProblem) {
    return !processedBuildProblems.contains(buildProblem.getId());
  }
}