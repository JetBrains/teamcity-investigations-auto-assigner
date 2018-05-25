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

import com.intellij.openapi.diagnostic.Logger;
import java.util.Collection;
import jetbrains.buildServer.iaa.common.Constants;
import jetbrains.buildServer.iaa.common.HeuristicResult;
import jetbrains.buildServer.iaa.common.Responsibility;
import jetbrains.buildServer.iaa.processing.HeuristicContext;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.users.UserModelEx;
import jetbrains.buildServer.users.impl.UserEx;
import org.jetbrains.annotations.NotNull;

public class DefaultUserHeuristic implements Heuristic {

  private static final Logger LOGGER = Logger.getInstance(DefaultUserHeuristic.class.getName());

  @NotNull private UserModelEx myUserModel;

  DefaultUserHeuristic(@NotNull final UserModelEx userModel) {
    myUserModel = userModel;
  }

  @Override
  @NotNull
  public String getName() {
    return "Default User Heuristic";
  }

  @Override
  @NotNull
  public String getDescription() {
    return "Assign an investigation to the default responsible user.";
  }

  @Override
  public HeuristicResult findResponsibleUser(@NotNull HeuristicContext heuristicContext) {
    HeuristicResult result = new HeuristicResult();

    SBuild build = heuristicContext.getBuild();
    Collection<SBuildFeatureDescriptor> descriptors = build.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE);
    if (descriptors.isEmpty()) return result;

    final SBuildFeatureDescriptor sBuildFeature = (SBuildFeatureDescriptor)descriptors.toArray()[0];
    String defaultResponsible = String.valueOf(sBuildFeature.getParameters().get(Constants.DEFAULT_RESPONSIBLE));

    if (defaultResponsible == null || defaultResponsible.isEmpty()) return result;
    UserEx responsibleUser = myUserModel.findUserAccount(null, defaultResponsible);

    if (responsibleUser == null) {
      LOGGER.warn(String.format("There is specified default user %s, but the user is not in a user model. " +
                                "Failed build #%s", defaultResponsible, build.getBuildId()));
      return result;
    }

    Responsibility responsibility =
      new Responsibility(responsibleUser, Constants.REASON_PREFIX + " you're the default responsible " +
                                          "user for the build: " + build.getFullName() + " #" + build.getBuildNumber());
    heuristicContext.getBuildProblems()
                    .forEach(buildProblem -> result.addResponsibility(buildProblem, responsibility));
    heuristicContext.getTestRuns().forEach(testRun -> result.addResponsibility(testRun, responsibility));

    return result;
  }
}
