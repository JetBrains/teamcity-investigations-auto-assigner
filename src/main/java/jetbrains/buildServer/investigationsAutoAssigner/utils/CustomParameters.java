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

/**
 * Utility class for retrieving and processing custom parameters from builds.
 */
public class CustomParameters {
  private static final int MINIMAL_PROCESSING_DELAY = 5;
  private static final int DEFAULT_PROCESSING_DELAY_IN_SECONDS = 30;

  /**
   * Retrieves the default responsible user from the build feature descriptor.
   *
   * @param build the build instance
   * @return the default responsible user or null if not specified
   */
  @Nullable
  public static String getDefaultResponsible(@NotNull final SBuild build) {
    return getBuildFeatureParameter(build, Constants.DEFAULT_RESPONSIBLE);
  }

  /**
   * Retrieves a set of users to ignore from the build feature descriptor.
   *
   * @param build the build instance
   * @return a set of users to ignore
   */
  @NotNull
  public static Set<String> getUsersToIgnore(@NotNull final SBuild build) {
    String usersToIgnore = getBuildFeatureParameter(build, Constants.USERS_TO_IGNORE);
    if (usersToIgnore == null) {
      return Collections.emptySet();
    }
    return Arrays.stream(usersToIgnore.split("\n"))
                 .map(String::trim)
                 .collect(Collectors.toSet());
  }

  /**
   * Determines if the default silent mode is enabled for a given build.
   *
   * @param build the build instance
   * @return true if silent mode is enabled, false otherwise
   */
  public boolean isDefaultSilentModeEnabled(@NotNull final SBuild build) {
    String enabledInBuild = build.getBuildOwnParameters().get(Constants.DEFAULT_SILENT_MODE_ENABLED);
    if (StringUtil.isTrue(enabledInBuild)) {
      return true;
    } else if ("false".equals(enabledInBuild)) {
      return false;
    }
    return isBuildFeatureEnabled(build) || TeamCityProperties.getBooleanOrTrue(Constants.DEFAULT_SILENT_MODE_ENABLED);
  }

  /**
   * Retrieves the processing delay in seconds, ensuring it meets the minimum threshold.
   *
   * @return the processing delay in seconds
   */
  public static int getProcessingDelayInSeconds() {
    int delay = TeamCityProperties.getInteger(Constants.PROCESSING_DELAY_IN_SECONDS, DEFAULT_PROCESSING_DELAY_IN_SECONDS);
    return Math.max(delay, MINIMAL_PROCESSING_DELAY);
  }

  /**
   * Retrieves the maximum number of tests per build threshold.
   *
   * @param build the build instance
   * @return the maximum number of tests per build
   */
  public static int getMaxTestsPerBuildThreshold(@NotNull final SBuild build) {
    String maxTests = build.getBuildOwnParameters().get(Constants.MAX_TESTS_PER_BUILD_NUMBER);
    return StringUtil.isNotEmpty(maxTests) ? parseThreshold(maxTests)
                                           : TeamCityProperties.getInteger(Constants.MAX_TESTS_PER_BUILD_NUMBER, Constants.DEFAULT_TEST_COUNT_THRESHOLD);
  }

  /**
   * Determines if assignments should be delayed based on build feature settings.
   *
   * @param build the build instance
   * @return true if assignments should be delayed, false otherwise
   */
  public static boolean shouldDelayAssignments(@NotNull final SBuild build) {
    return StringUtil.isTrue(getBuildFeatureParameter(build, Constants.ASSIGN_ON_SECOND_FAILURE));
  }

  /**
   * Checks if build feature is enabled.
   *
   * @param build the build instance
   * @return true if feature is enabled, false otherwise
   */
  public boolean isBuildFeatureEnabled(@NotNull final SBuild build) {
    return !build.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE).isEmpty();
  }

  /**
   * Determines if feature branches should be supported.
   *
   * @param build the build instance
   * @return true if feature branches support is enabled, false otherwise
   */
  public static boolean shouldRunForFeatureBranches(@NotNull final SBuild build) {
    String enabledInBuild = build.getBuildOwnParameters().get(Constants.ENABLE_FEATURE_BRANCHES_SUPPORT);
    if (StringUtil.isTrue(enabledInBuild)) {
      return true;
    } else if ("false".equals(enabledInBuild)) {
      return false;
    }
    return TeamCityProperties.getBoolean(Constants.ENABLE_FEATURE_BRANCHES_SUPPORT);
  }

  /**
   * Retrieves the build problem types to ignore.
   *
   * @param build the build instance
   * @return a list of ignored build problem types
   */
  @NotNull
  public List<String> getBuildProblemTypesToIgnore(@NotNull final SBuild build) {
    List<String> ignoredProblems = new ArrayList<>();
    SBuildFeatureDescriptor feature = getBuildFeatureDescriptor(build);

    if (feature != null) {
      if (Boolean.parseBoolean(feature.getParameters().get(Constants.SHOULD_IGNORE_COMPILATION_PROBLEMS))) {
        ignoredProblems.add(BuildProblemTypes.TC_COMPILATION_ERROR_TYPE);
      }
      if (Boolean.parseBoolean(feature.getParameters().get(Constants.SHOULD_IGNORE_EXITCODE_PROBLEMS))) {
        ignoredProblems.add(BuildProblemTypes.TC_EXIT_CODE_TYPE);
      }
    }
    return ignoredProblems;
  }

  /**
   * Determines if a specific heuristic is disabled.
   *
   * @param heuristicId the heuristic identifier
   * @return true if the heuristic is disabled, false otherwise
   */
  public boolean isHeuristicsDisabled(@NotNull final String heuristicId) {
    String propertyName = "teamcity.investigationsAutoAssigner.heuristics." + heuristicId + ".enabled";
    return !TeamCityProperties.getBooleanOrTrue(propertyName);
  }

  // Private helper methods

  /**
   * Retrieves the first build feature descriptor for the given build.
   *
   * @param build the build instance
   * @return the first found build feature descriptor or null if none found
   */
  @Nullable
  private static SBuildFeatureDescriptor getBuildFeatureDescriptor(@NotNull final SBuild build) {
    return build.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE).stream().findFirst().orElse(null);
  }

  /**
   * Retrieves a specific parameter from the build feature descriptor.
   *
   * @param build      the build instance
   * @param parameter  the parameter key to retrieve
   * @return the parameter value or null if not found
   */
  @Nullable
  private static String getBuildFeatureParameter(@NotNull final SBuild build, @NotNull final String parameter) {
    SBuildFeatureDescriptor feature = getBuildFeatureDescriptor(build);
    return feature != null ? feature.getParameters().get(parameter) : null;
  }

  /**
   * Parses an integer threshold value from a string, ensuring it's non-negative.
   *
   * @param value the string value to parse
   * @return the parsed integer threshold, or Integer.MAX_VALUE if invalid
   */
  private static int parseThreshold(@NotNull String value) {
    int parsedValue = StringUtil.parseInt(value, Constants.DEFAULT_TEST_COUNT_THRESHOLD);
    return parsedValue >= 0 ? parsedValue : Integer.MAX_VALUE;
  }
}
