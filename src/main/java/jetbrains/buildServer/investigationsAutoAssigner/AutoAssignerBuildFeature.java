/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

package jetbrains.buildServer.investigationsAutoAssigner;

import java.util.Arrays;
import java.util.Map;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.representation.AutoAssignerBuildFeatureController;
import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AutoAssignerBuildFeature extends BuildFeature {
  private final String myEditUrl;

  public AutoAssignerBuildFeature(@NotNull final PluginDescriptor descriptor) {
    myEditUrl = descriptor.getPluginResourcesPath(AutoAssignerBuildFeatureController.CONTROLLER_URL);
  }

  @NotNull
  @Override
  public String getType() {
    return Constants.BUILD_FEATURE_TYPE;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return Constants.BUILD_FEATURE_DISPLAY_NAME;
  }

  @Nullable
  @Override
  public String getEditParametersUrl() {
    return myEditUrl;
  }

  @NotNull
  @Override
  public String describeParameters(@NotNull Map<String, String> params) {
    final String userName = params.get(Constants.DEFAULT_RESPONSIBLE);
    final String usersToIgnore = params.get(Constants.USERS_TO_IGNORE);
    final String shouldDelayAssignments = params.get(Constants.ASSIGN_ON_SECOND_FAILURE);

    StringBuilder sb = new StringBuilder();
    if (StringUtil.isTrue(shouldDelayAssignments)) {
      sb.append("On second failure strategy").append("\n");
    }
    if (StringUtil.isNotEmpty(userName)) {
      sb.append("Default assignee: ").append(userName).append("\n");
    }
    if (StringUtil.isNotEmpty(usersToIgnore)) {
      String usersToIgnoreOneLine = StringUtil.join(", ", Arrays.asList(usersToIgnore.split("\n")));
      sb.append("Users to ignore: ").append(usersToIgnoreOneLine);
    }
    return sb.toString().trim();
  }

  @Override
  public boolean isMultipleFeaturesPerBuildTypeAllowed() {
    return false;
  }

  @Override
  public boolean isRequiresAgent() { return false; }
}
