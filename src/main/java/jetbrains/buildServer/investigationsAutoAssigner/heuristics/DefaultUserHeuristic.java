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

package jetbrains.buildServer.investigationsAutoAssigner.heuristics;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.common.DefaultUserResponsibility;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.processing.HeuristicContext;
import jetbrains.buildServer.investigationsAutoAssigner.utils.CustomParameters;
import jetbrains.buildServer.log.LogUtil;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.users.UserModelEx;
import jetbrains.buildServer.users.impl.UserEx;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.investigationsAutoAssigner.processing.BuildProblemsFilter.notSupportedEverywhereTypes;

public class DefaultUserHeuristic implements Heuristic {

  private static final Logger LOGGER = Constants.LOGGER;

  @NotNull private UserModelEx myUserModel;

  public DefaultUserHeuristic(@NotNull final UserModelEx userModel) {
    myUserModel = userModel;
  }

  @Override
  @NotNull
  public String getId() {
    return "DefaultUser";
  }

  @NotNull
  @Override
  public HeuristicResult findResponsibleUser(@NotNull HeuristicContext heuristicContext) {
    HeuristicResult result = new HeuristicResult();

    SBuild build = heuristicContext.getBuild();
    String defaultResponsible = CustomParameters.getDefaultResponsible(build);
    if (StringUtil.isEmpty(defaultResponsible)) return result;

    UserEx responsibleUser = myUserModel.findUserAccount(null, defaultResponsible);
    if (responsibleUser == null) {
      LOGGER.warn("Ignoring heuristic \"DefaultUser\" as there is no TeamCity user with the username \"" +
                  defaultResponsible + "\" specified in the Investigations Auto-Assigner settings in the build: " +
                  LogUtil.describe(build) + "Affected build configuration: " +
                  LogUtil.describe(build.getBuildType()));
      return result;
    }

    Responsibility responsibility = new DefaultUserResponsibility(responsibleUser);
    heuristicContext.getBuildProblems()
                    .stream()
                    .filter(buildProblem -> !notSupportedEverywhereTypes.contains(buildProblem.getBuildProblemData().getType()))
                    .forEach(buildProblem -> result.addResponsibility(buildProblem, responsibility));
    heuristicContext.getTestRuns().forEach(testRun -> result.addResponsibility(testRun, responsibility));

    return result;
  }
}
