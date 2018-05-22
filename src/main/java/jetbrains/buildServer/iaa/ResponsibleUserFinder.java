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

import com.intellij.openapi.diagnostic.Logger;
import java.util.List;
import java.util.stream.Collectors;
import jetbrains.buildServer.iaa.heuristics.Heuristic;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import org.jetbrains.annotations.NotNull;

public class ResponsibleUserFinder {
  private static final Logger LOGGER = Logger.getInstance(ResponsibleUserFinder.class.getName());
  private List<Heuristic> myOrderedHeuristics;

  ResponsibleUserFinder(@NotNull final List<Heuristic> orderedHeuristics) {
    myOrderedHeuristics = orderedHeuristics;
  }

  HeuristicResult findResponsibleUser(SBuild sBuild, List<BuildProblem> buildProblems, List<STestRun> sTestRuns) {
    HeuristicResult heuristicResult = new HeuristicResult();

    for (Heuristic heuristic : myOrderedHeuristics) {
      List<STestRun> actualSTestRuns = sTestRuns
        .stream()
        .filter(sTestRun -> heuristicResult.getResponsibility(sTestRun) == null)
        .collect(Collectors.toList());

      List<BuildProblem> actualBuildProblems = buildProblems
        .stream()
        .filter(buildProblem -> heuristicResult.getResponsibility(buildProblem) == null)
        .collect(Collectors.toList());

      FailedBuildContext buildContext = new FailedBuildContext(sBuild, actualBuildProblems, actualSTestRuns);

      heuristicResult.merge(heuristic.findResponsibleUser(buildContext));
    }

    return heuristicResult;
  }
}
