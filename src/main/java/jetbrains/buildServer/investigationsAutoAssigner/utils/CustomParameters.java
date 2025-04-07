

package jetbrains.buildServer.investigationsAutoAssigner.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
  private static final Integer MINIMAL_PROCESSING_DELAY = 5;
  private static final Integer DEFAULT_PROCESSING_DELAY_IN_SECONDS = 30;

  @Nullable
  public static String getDefaultResponsible(final SBuild build) {
    final SBuildFeatureDescriptor sBuildFeature = getBuildFeatureDescriptor(build);
    return sBuildFeature == null ? null : sBuildFeature.getParameters().get(Constants.DEFAULT_RESPONSIBLE);
  }

  @NotNull
  public static Set<String> getUsersToIgnore(final SBuild build) {
    final SBuildFeatureDescriptor sBuildFeature = getBuildFeatureDescriptor(build);
    if (sBuildFeature == null) return Collections.emptySet();

    String usersToIgnore = sBuildFeature.getParameters().get(Constants.USERS_TO_IGNORE);
    if (usersToIgnore == null) return Collections.emptySet();

    return Arrays.stream(usersToIgnore.split("\n")).map(String::trim).collect(Collectors.toSet());
  }

  public static boolean isDefaultSilentModeEnabled(final SBuild build) {
    @Nullable
    String enabledInBuild = build.getBuildOwnParameters().get(Constants.DEFAULT_SILENT_MODE_ENABLED);
    if (StringUtil.isTrue(enabledInBuild)) return true;
    else if ("false".equals(enabledInBuild)) return false;

    if (isBuildFeatureEnabled(build)) return true;

    return TeamCityProperties.getBooleanOrTrue(Constants.DEFAULT_SILENT_MODE_ENABLED);
  }

  @Nullable
  private static SBuildFeatureDescriptor getBuildFeatureDescriptor(final SBuild build) {
    Collection<SBuildFeatureDescriptor> descriptors = build.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE);
    return !descriptors.isEmpty() ? descriptors.iterator().next() : null;
  }

  public static int getProcessingDelayInSeconds() {
    int value =
      TeamCityProperties.getInteger(Constants.PROCESSING_DELAY_IN_SECONDS, DEFAULT_PROCESSING_DELAY_IN_SECONDS);
    return value < MINIMAL_PROCESSING_DELAY ? MINIMAL_PROCESSING_DELAY : value;
  }

  public static int getMaxTestsPerBuildThreshold(SBuild build) {
    @Nullable
    String maxTestsPerBuildNumber = build.getBuildOwnParameters().get(Constants.MAX_TESTS_PER_BUILD_NUMBER);
    if (StringUtil.isNotEmpty(maxTestsPerBuildNumber)) return parseThreshold(maxTestsPerBuildNumber);

    return TeamCityProperties.getInteger(Constants.MAX_TESTS_PER_BUILD_NUMBER, Constants.DEFAULT_TEST_COUNT_THRESHOLD);
  }

  private static int parseThreshold(@NotNull String value) {
    return Math.max(StringUtil.parseInt(value, Constants.DEFAULT_TEST_COUNT_THRESHOLD), 0);
  }

  public static boolean shouldDelayAssignments(final SBuild sBuild) {
    final SBuildFeatureDescriptor sBuildFeature = getBuildFeatureDescriptor(sBuild);

    return sBuildFeature != null &&
           StringUtil.isTrue(sBuildFeature.getParameters().get(Constants.ASSIGN_ON_SECOND_FAILURE));
  }

  public static boolean isBuildFeatureEnabled(@NotNull SBuild sBuild) {
    return !sBuild.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE).isEmpty();
  }

  public static boolean shouldRunForFeatureBranches(SBuild build) {
    @Nullable
    String enabledInBuild = build.getBuildOwnParameters().get(Constants.ENABLE_FEATURE_BRANCHES_SUPPORT);
    if (StringUtil.isTrue(enabledInBuild)) return true;
    else if ("false".equals(enabledInBuild)) return false;

    return TeamCityProperties.getBoolean(Constants.ENABLE_FEATURE_BRANCHES_SUPPORT);
  }

  @NotNull
  public List<String> getBuildProblemTypesToIgnore(final SBuild sBuild) {
    final SBuildFeatureDescriptor sBuildFeature = getBuildFeatureDescriptor(sBuild);
    if (sBuildFeature == null) return Collections.emptyList();

    boolean shouldIgnoreCompilation = "true".equals(sBuildFeature.getParameters().get(Constants.SHOULD_IGNORE_COMPILATION_PROBLEMS));
    boolean shouldIgnoreExitCode = "true".equals(sBuildFeature.getParameters().get(Constants.SHOULD_IGNORE_EXITCODE_PROBLEMS));

    if (shouldIgnoreExitCode || shouldIgnoreCompilation) {
      ArrayList<String> result = new ArrayList<>();
      if (shouldIgnoreCompilation) result.add(BuildProblemTypes.TC_COMPILATION_ERROR_TYPE);
      if (shouldIgnoreExitCode) result.add(BuildProblemTypes.TC_EXIT_CODE_TYPE);
      return result;
    }

    return Collections.emptyList();
  }

  public boolean isHeuristicsDisabled(@NotNull final String heuristicId) {
    String propertyName = "teamcity.investigationsAutoAssigner.heuristics." + heuristicId + ".enabled";
    return !TeamCityProperties.getBooleanOrTrue(propertyName);
  }
}