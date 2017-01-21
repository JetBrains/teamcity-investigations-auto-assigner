/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Med_PC on 11/6/2016.
 */
public class AutoAssignNewTestsAndProblemsFeature extends BuildFeature {
  //region Constants
  @NotNull static final String AutoAssignTestsAndProblemsFeature = "AutoAssignTestsAndProblemsFeature";
  @NotNull private static final String AUTO_ASSIGN_TESTS = "iaa.config.assign_tests";
  @NotNull private static final String AUTO_ASSIGN_BUILD_PROBLEMS = "iaa.config.assign_build_problems";

  //endregion
  //region Fields
  @NotNull private final String jsp;

  //endregion
  public AutoAssignNewTestsAndProblemsFeature(@NotNull final PluginDescriptor descriptor) {
    jsp = descriptor.getPluginResourcesPath("autoAssignerSettings.jsp");
  }

  @NotNull public String getType() {
    return AutoAssignTestsAndProblemsFeature;
  }

  @NotNull public String getDisplayName() {
    return "Auto assign new tests and build problems";
  }

  @NotNull public String getEditParametersUrl() {
    return jsp;
  }

  @NotNull @Override public Map<String, String> getDefaultParameters() {
    HashMap<String, String> parametersDescription = new HashMap<String, String>();
    parametersDescription.put(AUTO_ASSIGN_TESTS, Boolean.toString(true));
    parametersDescription.put(AUTO_ASSIGN_BUILD_PROBLEMS, Boolean.toString(true));
    return parametersDescription;
  }

  @Override public boolean isMultipleFeaturesPerBuildTypeAllowed() {
    return false;
  }

  @Nullable private static SBuildFeatureDescriptor getBuildFeature(@NotNull final SBuild build) {
    Collection<SBuildFeatureDescriptor> buildFeatures = build.getBuildFeaturesOfType(AutoAssignTestsAndProblemsFeature);
    if (buildFeatures.isEmpty())
      return null;
    return buildFeatures.iterator().next();
  }

  public static boolean isAutoAssignTestEnabledFor(@NotNull final SBuild build) {
    SBuildFeatureDescriptor buildFeature = getBuildFeature(build);
    if (buildFeature == null)
      return false;
    Map<String, String> parameters = buildFeature.getParameters();
    if (!parameters.containsKey(AUTO_ASSIGN_TESTS))
      return false;
    String value = parameters.get(AUTO_ASSIGN_TESTS);
    return value != null ? Boolean.parseBoolean(value) : false;
  }

  public static boolean isAutoAssignProblemsEnabledFor(@NotNull final SBuild build) {
    SBuildFeatureDescriptor buildFeature = getBuildFeature(build);
    if (buildFeature == null)
      return false;
    Map<String, String> parameters = buildFeature.getParameters();
    if (!parameters.containsKey(AUTO_ASSIGN_BUILD_PROBLEMS))
      return false;
    String value = parameters.get(AUTO_ASSIGN_BUILD_PROBLEMS);
    return value != null ? Boolean.parseBoolean(value) : false;
  }
}
