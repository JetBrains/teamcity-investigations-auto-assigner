/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

package jetbrains.buildServer.investigationsAutoAssigner.utils;

import java.util.*;
import java.util.stream.Collectors;
import jetbrains.buildServer.BuildProblemTypes;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CustomParameters {
  private final static Integer MINIMAL_PROCESSING_DELAY = 5;
  private final static Integer DEFAULT_PROCESSING_DELAY_IN_SECONDS = 30;

  @Nullable
  public static String getDefaultResponsible(final SBuild build) {
    final SBuildFeatureDescriptor sBuildFeature = getBuildFeatureDescriptor(build);
    if (sBuildFeature == null) return null;
    return sBuildFeature.getParameters().get(Constants.DEFAULT_RESPONSIBLE);
  }

  @NotNull
  public static Set<String> getUsersToIgnore(final SBuild build) {
    final SBuildFeatureDescriptor sBuildFeature = getBuildFeatureDescriptor(build);
    if (sBuildFeature == null) {
      return Collections.emptySet();
    }

    String usersToIgnore = sBuildFeature.getParameters().get(Constants.USERS_TO_IGNORE);
    if (usersToIgnore == null) {
      return Collections.emptySet();
    }

    return Arrays.stream(usersToIgnore.split("\n")).map(String::trim).collect(Collectors.toSet());
  }

  public boolean isDefaultSilentModeEnabled(final SBuild build) {
    @Nullable
    String enabledInBuild = build.getBuildOwnParameters().get(Constants.DEFAULT_SILENT_MODE_ENABLED);
    if (StringUtil.isTrue(enabledInBuild)) {
      return true;
    } else if ("false".equals(enabledInBuild)) {
      return false;
    }

    if (isBuildFeatureEnabled(build)) {
      return true;
    }

    return TeamCityProperties.getBooleanOrTrue(Constants.DEFAULT_SILENT_MODE_ENABLED);
  }

  @Nullable
  private static SBuildFeatureDescriptor getBuildFeatureDescriptor(final SBuild build) {
    Collection<SBuildFeatureDescriptor> descriptors = build.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE);
    if (descriptors.isEmpty()) return null;
    return descriptors.iterator().next();
  }

  public static int getProcessingDelayInSeconds() {
    int value =
      TeamCityProperties.getInteger(Constants.PROCESSING_DELAY_IN_SECONDS, DEFAULT_PROCESSING_DELAY_IN_SECONDS);
    return value < MINIMAL_PROCESSING_DELAY ? MINIMAL_PROCESSING_DELAY : value;
  }

  public static int getMaxTestsPerBuildThreshold(SBuild build) {
    @Nullable
    String maxTestsPerBuildNumber = build.getBuildOwnParameters().get(Constants.MAX_TESTS_PER_BUILD_NUMBER);
    if (StringUtil.isNotEmpty(maxTestsPerBuildNumber)) {
      return parseThreshold(maxTestsPerBuildNumber);
    }

    return TeamCityProperties.getInteger(Constants.MAX_TESTS_PER_BUILD_NUMBER, Constants.DEFAULT_TEST_COUNT_THRESHOLD);
  }

  private static int parseThreshold(@NotNull String value) {
    int parsedValue = StringUtil.parseInt(value, Constants.DEFAULT_TEST_COUNT_THRESHOLD);
    return parsedValue >= 0 ? parsedValue : Integer.MAX_VALUE;
  }

  public boolean shouldDelayAssignments(final SBuild sBuild) {
    final SBuildFeatureDescriptor sBuildFeature = getBuildFeatureDescriptor(sBuild);
    if (sBuildFeature == null) {
      return false;
    }

    @Nullable
    String shouldDelayAssignments = sBuildFeature.getParameters().get(Constants.ASSIGN_ON_SECOND_FAILURE);
    return StringUtil.isTrue(shouldDelayAssignments);
  }

  public boolean isBuildFeatureEnabled(@NotNull SBuild sBuild) {
    Collection<SBuildFeatureDescriptor> descriptors = sBuild.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE);

    return !descriptors.isEmpty();
  }

  public static boolean shouldRunForFeatureBranches(SBuild build) {
    @Nullable
    String enabledInBuild = build.getBuildOwnParameters().get(Constants.ENABLE_FEATURE_BRANCHES_SUPPORT);
    if (StringUtil.isTrue(enabledInBuild)) {
      return true;
    } else if ("false".equals(enabledInBuild)) {
      return false;
    }

    return TeamCityProperties.getBoolean(Constants.ENABLE_FEATURE_BRANCHES_SUPPORT);
  }

  @NotNull
  public List<String> getBuildProblemTypesToIgnore(final SBuild sBuild) {
    final SBuildFeatureDescriptor sBuildFeature = getBuildFeatureDescriptor(sBuild);
    if (sBuildFeature == null) {
      return Collections.emptyList();
    }

    boolean shouldIgnoreCompilation = "true".equals(sBuildFeature.getParameters().get(Constants.SHOULD_IGNORE_COMPILATION_PROBLEMS));
    boolean shouldIgnoreExitCode = "true".equals(sBuildFeature.getParameters().get(Constants.SHOULD_IGNORE_EXITCODE_PROBLEMS));

    if (shouldIgnoreExitCode || shouldIgnoreCompilation) {
      ArrayList<String> result = new ArrayList<>();
      if (shouldIgnoreCompilation) {
        result.add(BuildProblemTypes.TC_COMPILATION_ERROR_TYPE);
      }
      if (shouldIgnoreExitCode) {
        result.add(BuildProblemTypes.TC_EXIT_CODE_TYPE);
      }

      return result;
    }

    return Collections.emptyList();
  }

  public boolean  isHeuristicsDisabled(@NotNull final String heuristicId) {
    String propertyName = "teamcity.investigationsAutoAssigner.heuristics." + heuristicId + ".enabled";
    return !TeamCityProperties.getBooleanOrTrue(propertyName);
  }
}
