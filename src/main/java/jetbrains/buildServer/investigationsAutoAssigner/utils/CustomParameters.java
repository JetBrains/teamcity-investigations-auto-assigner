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

package jetbrains.buildServer.investigationsAutoAssigner.utils;

import java.util.*;
import java.util.stream.Collectors;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CustomParameters {
  private final static Integer MINIMAL_PROCESSING_DELAY = 5;
  private final static Integer DEFAULT_PROCESSING_DELAY_IN_SECONDS = 10 * 60;

  @NotNull
  public static List<String> getDefaultResponsible(final SBuild build) {
    final SBuildFeatureDescriptor sBuildFeature = getBuildFeatureDescriptor(build);
    if (sBuildFeature == null) return Collections.emptyList();
    String defaultResponsible = sBuildFeature.getParameters().get(Constants.DEFAULT_RESPONSIBLE);
    if (defaultResponsible == null) {
      return Collections.emptyList();
    }
    return Arrays.stream(defaultResponsible.split(",")).map(String::trim).collect(Collectors.toList());
  }

  @NotNull
  public static List<String> getUsersToIgnore(final SBuild build) {
    final SBuildFeatureDescriptor sBuildFeature = getBuildFeatureDescriptor(build);
    if (sBuildFeature == null) {
      return Collections.emptyList();
    }

    String usersToIgnore = sBuildFeature.getParameters().get(Constants.USERS_TO_IGNORE);
    if (usersToIgnore == null) {
      return Collections.emptyList();
    }

    return Arrays.stream(usersToIgnore.split("\n")).map(String::trim).collect(Collectors.toList());
  }

  public static boolean isDefaultSilentModeEnabled(final SBuild build) {
    @Nullable
    String enabledInBuild = build.getBuildOwnParameters().get(Constants.DEFAULT_SILENT_MODE_ENABLED);
    if ("true".equals(enabledInBuild)) {
      return true;
    } else if ("false".equals(enabledInBuild)) {
      return false;
    }

    if (isBuildFeatureEnabled(build)) {
      return true;
    }

    return Boolean.valueOf(TeamCityProperties.getProperty(Constants.DEFAULT_SILENT_MODE_ENABLED, "true"));
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

    return parseThreshold(TeamCityProperties.getProperty(Constants.MAX_TESTS_PER_BUILD_NUMBER,
                                                         String.valueOf(Constants.DEFAULT_TEST_COUNT_THRESHOLD)));
  }

  private static int parseThreshold(@NotNull String value) {
    int parsedValue = StringUtil.parseInt(value, Constants.DEFAULT_TEST_COUNT_THRESHOLD);
    return parsedValue >= 0 ? parsedValue : Integer.MAX_VALUE;
  }

  @Nullable
  String getEmailForEmailReporter() {
    return TeamCityProperties.getPropertyOrNull(Constants.INTERNAL_REPORTER_EMAIL);
  }

  public static boolean isBuildFeatureEnabled(@NotNull SBuild sBuild) {
    Collection<SBuildFeatureDescriptor> descriptors = sBuild.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE);

    return !descriptors.isEmpty();
  }
}
