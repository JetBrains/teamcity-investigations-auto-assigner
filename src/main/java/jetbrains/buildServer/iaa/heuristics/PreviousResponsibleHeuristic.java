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

package jetbrains.buildServer.iaa.heuristics;

import com.intellij.openapi.util.Pair;
import jetbrains.buildServer.iaa.BuildProblemInfo;
import jetbrains.buildServer.iaa.ProblemInfo;
import jetbrains.buildServer.iaa.TestProblemInfo;
import jetbrains.buildServer.iaa.common.Constants;
import jetbrains.buildServer.iaa.utils.InvestigationsManager;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STest;
import jetbrains.buildServer.serverSide.impl.problems.BuildProblemImpl;
import jetbrains.buildServer.users.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PreviousResponsibleHeuristic implements Heuristic {

  private InvestigationsManager myInvestigationsManager;

  PreviousResponsibleHeuristic(InvestigationsManager investigationsManager) {
    myInvestigationsManager = investigationsManager;
  }

  @NotNull
  @Override
  public String getName() {
    return "Previous Responsible Heuristic";
  }

  @NotNull
  @Override
  public String getDescription() {
    return "Assign an investigation to a user if the user was responsible previous time.";
  }

  @Nullable
  @Override
  public Pair<User, String> findResponsibleUser(@NotNull ProblemInfo problemInfo) {
    User responsibleUser = null;
    String description = null;
    SProject sProject = problemInfo.getSProject();
    SBuild sBuild = problemInfo.getSBuild();
    if (problemInfo instanceof TestProblemInfo) {
      STest sTest = ((TestProblemInfo)problemInfo).getSTest();
      responsibleUser = myInvestigationsManager.findPreviousResponsible(sProject, sBuild, sTest);
      description = String.format("%s you were responsible for the test: %s in build %s previous time",
                                  Constants.REASON_PREFIX, sTest.getTestNameId(), sBuild.getFullName());
    } else if (problemInfo instanceof BuildProblemInfo) {
      BuildProblemImpl buildProblem = ((BuildProblemInfo)problemInfo).getBuildProblem();
      responsibleUser = myInvestigationsManager.findPreviousResponsible(sProject, sBuild, buildProblem);
      String buildProblemType = buildProblem.getBuildProblemData().getType();
      description = String.format("%s you were responsible for the build problem: %s in build %s previous time",
                                  Constants.REASON_PREFIX, buildProblemType, sBuild.getFullName());
    }

    if (responsibleUser == null) {
      return null;
    }

    return Pair.create(responsibleUser, description);
  }
}
