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

import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import org.jetbrains.annotations.Nullable;

public class FailedBuildContext {
  public final SBuild sBuild;
  public final Iterable<BuildProblem> buildProblems;
  public final Iterable<STestRun> sTestRuns;

  private final HashMap<Integer, Responsibility> testRun2Responsibility;
  private final HashMap<Integer, Responsibility> buildProblem2Responsibility;

  public FailedBuildContext(SBuild sBuild, Iterable<BuildProblem> buildProblems, Iterable<STestRun> sTestRuns) {
    this.sBuild = sBuild;
    this.buildProblems = buildProblems;
    this.sTestRuns = sTestRuns;
    testRun2Responsibility = new HashMap<>();
    buildProblem2Responsibility = new HashMap<>();
  }

  public Iterable<BuildProblem> getBuildProblems() {
    return StreamSupport.stream(buildProblems.spliterator(), false)
                        .filter(buildProblem -> buildProblem2Responsibility.get(buildProblem.getId()) == null)
                        .collect(Collectors.toList());
  }

  public Iterable<STestRun> getTestRuns() {
    return StreamSupport.stream(sTestRuns.spliterator(), false)
                        .filter(testRun -> testRun2Responsibility.get(testRun.getTestRunId()) == null)
                        .collect(Collectors.toList());
  }

  public void addResponsibility(final STestRun sTestRun, final Responsibility responsibility) {
    testRun2Responsibility.put(sTestRun.getTestRunId(), responsibility);
  }

  public void addResponsibility(final BuildProblem buildProblem, final Responsibility responsibility) {
    buildProblem2Responsibility.put(buildProblem.getId(), responsibility);
  }

  @Nullable
  Responsibility getResponsibility(final STestRun sTestRun) {
    return testRun2Responsibility.get(sTestRun.getTestRunId());
  }

  @Nullable
  Responsibility getResponsibility(final BuildProblem buildProblem) {
    return testRun2Responsibility.get(buildProblem.getId());
  }
}

