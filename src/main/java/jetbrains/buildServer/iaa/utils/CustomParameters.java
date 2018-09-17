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

package jetbrains.buildServer.iaa.utils;

import java.util.*;
import java.util.stream.Collectors;
import jetbrains.buildServer.iaa.common.Constants;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CustomParameters {
  private final static Integer MINIMAL_PROCESSING_DELAY = 5;
  private final static Integer DEFAULT_PROCESSING_DELAY_IN_SECONDS = 120;

  @Nullable
  public static String getDefaultResponsible(final SBuild build) {
    final SBuildFeatureDescriptor sBuildFeature = getBuildFeatureDescriptor(build);
    if (sBuildFeature == null) return null;

    return String.valueOf(sBuildFeature.getParameters().get(Constants.DEFAULT_RESPONSIBLE));
  }

  @NotNull
  public static List<String> getBlackList(final SBuild build) {
    final SBuildFeatureDescriptor sBuildFeature = CustomParameters.getBuildFeatureDescriptor(build);
    if (sBuildFeature == null) {
      return Collections.emptyList();
    }

    return Arrays.stream(String.valueOf(sBuildFeature.getParameters().get(Constants.BLACK_LIST)).split(","))
      .map(String::trim).collect(Collectors.toList());
  }

  @Nullable
  private static SBuildFeatureDescriptor getBuildFeatureDescriptor(final SBuild build) {
    Collection<SBuildFeatureDescriptor> descriptors = build.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE);
    if (descriptors.isEmpty()) return null;
    return (SBuildFeatureDescriptor)descriptors.toArray()[0];
  }

  public static int getProcessingDelayInSeconds() {
    int value = TeamCityProperties
      .getInteger("teamcity.autoassigner.processingDelayInSeconds", DEFAULT_PROCESSING_DELAY_IN_SECONDS);
    return value < MINIMAL_PROCESSING_DELAY ? MINIMAL_PROCESSING_DELAY : value;
  }

  public static Integer getMaxTestsPerBuildThreshold(SBuild build) {
    return parseThreshold(build.getBuildOwnParameters().get("autoassigner.maxTestsPerBuildNumber"));
  }

  private static int parseThreshold(@Nullable String value) {
    final int DEFAULT_TEST_COUNT_THRESHOLD = 100;
    if (value == null) {
      return DEFAULT_TEST_COUNT_THRESHOLD;
    }

    int parsedValue = StringUtil.parseInt(value, DEFAULT_TEST_COUNT_THRESHOLD);
    return parsedValue >= 0 ? parsedValue : Integer.MAX_VALUE;
  }

  public boolean isSilentModeOn(@NotNull SBuild build) {
    Collection<SBuildFeatureDescriptor> descriptors = build.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE);
    if (descriptors.isEmpty()) throw new IllegalStateException("Descriptors should not be empty");

    final Optional<SBuildFeatureDescriptor> sBuildFeatureOptional = descriptors.stream().findFirst();
    return Boolean.valueOf(sBuildFeatureOptional.get().getParameters().get(Constants.SILENT_MODE_ON));
  }

  @Nullable
  String getEmailForEmailReporter() {
    return TeamCityProperties.getPropertyOrNull("teamcity.autoassigner.reporter.email");
  }
}
