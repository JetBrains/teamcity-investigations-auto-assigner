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

package jetbrains.buildServer.iaa.processing;

import jetbrains.buildServer.iaa.common.HeuristicResult;
import jetbrains.buildServer.iaa.common.Responsibility;
import jetbrains.buildServer.responsibility.BuildProblemResponsibilityFacade;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.responsibility.impl.BuildProblemResponsibilityEntryImpl;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.util.Dates;
import org.jetbrains.annotations.NotNull;

public class BuildProblemsAssigner implements ResponsibilityAssigner {

  @NotNull private final BuildProblemResponsibilityFacade myBuildProblemResponsibilityFacade;

  public BuildProblemsAssigner(@NotNull final BuildProblemResponsibilityFacade buildProblemResponsibilityFacade) {
    myBuildProblemResponsibilityFacade = buildProblemResponsibilityFacade;
  }

  @Override
  public void apply(HeuristicResult heuristicsResult, HeuristicContext heuristicContext) {
    SProject sProject = heuristicContext.getSProject();
    Iterable<BuildProblem> buildProblems = heuristicContext.getBuildProblems();
    for (BuildProblem buildProblem : buildProblems) {
      Responsibility responsibility = heuristicsResult.getResponsibility(buildProblem);

      if (responsibility != null) {
        myBuildProblemResponsibilityFacade.setBuildProblemResponsibility(
          buildProblem, sProject.getProjectId(),
          new BuildProblemResponsibilityEntryImpl(
            ResponsibilityEntry.State.TAKEN, responsibility.getUser(), null, Dates.now(),
            responsibility.getDescription(), ResponsibilityEntry.RemoveMethod.WHEN_FIXED, sProject, buildProblem.getId()
          )
        );
      }
    }
  }
}