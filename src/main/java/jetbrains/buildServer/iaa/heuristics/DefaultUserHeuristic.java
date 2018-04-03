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
import java.util.Collection;
import jetbrains.buildServer.iaa.ProblemInfo;
import jetbrains.buildServer.iaa.common.Constants;
import jetbrains.buildServer.iaa.utils.UserModelProxy;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.impl.UserEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultUserHeuristic extends Heuristic {

  @NotNull private UserModelProxy myUserModel;

  public DefaultUserHeuristic(@NotNull UserModelProxy userModel) {
    myUserModel = userModel;
  }

  @Override
  public long getUniqueOrder() {
    return 999;
  }

  @Override
  @NotNull
  public String getName() {
    return "Default User Heuristic";
  }

  @Override
  @NotNull
  public String getDescription() {
    return "Assign an investigation to a default responsible user.";
  }

  @Override
  @Nullable
  public Pair<SUser, String> findResponsibleUser(@NotNull ProblemInfo problemInfo) {
    SBuild build = problemInfo.mySBuild;
    Collection<SBuildFeatureDescriptor> descriptors = build.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE);
    if (descriptors.isEmpty()) return null;

    final SBuildFeatureDescriptor sBuildFeature = (SBuildFeatureDescriptor)descriptors.toArray()[0];
    String defaultResponsible = String.valueOf(sBuildFeature.getParameters().get(Constants.DEFAULT_RESPONSIBLE));

    if (defaultResponsible == null) return null;
    UserEx responsibleUser = myUserModel.findUserAccount(defaultResponsible);

    if (responsibleUser == null) return null;
    return Pair.create(responsibleUser,
              Constants.REASON_PREFIX + " you were selected as default responsible for following build: " +
              build.getFullName() + " #" + build.getBuildNumber());
  }
}
