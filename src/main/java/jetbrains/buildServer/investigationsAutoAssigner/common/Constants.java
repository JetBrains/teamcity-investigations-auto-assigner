

package jetbrains.buildServer.investigationsAutoAssigner.common;

import com.intellij.openapi.diagnostic.Logger ;
import jetbrains.buildServer.ArtifactsConstants;

public class Constants {
  // Plugin's ids
  public static final String BUILD_FEATURE_TYPE = "InvestigationsAutoAssigner";
  public static final String BUILD_FEATURE_DISPLAY_NAME = "Investigations Auto Assigner";

  // Build feature parameters
  public static final String DEFAULT_RESPONSIBLE = "defaultAssignee.username";
  public static final String USERS_TO_IGNORE = "excludeAssignees.usernames";
  public static final String SHOULD_IGNORE_COMPILATION_PROBLEMS = "ignoreBuildProblems.compilation";
  public static final String SHOULD_IGNORE_EXITCODE_PROBLEMS = "ignoreBuildProblems.exitCode";
  public static final String ASSIGN_ON_SECOND_FAILURE = "assignOnSecondFailure";

  // Build configuration parameter and internal property
  public static final String DEFAULT_SILENT_MODE_ENABLED = "teamcity.investigationsAutoAssigner.suggestions.enabledByDefault";
  public static final String ENABLE_FEATURE_BRANCHES_SUPPORT = "teamcity.investigationsAutoAssigner.enableFeatureBranchesSupport";
  public static final String MAX_TESTS_PER_BUILD_NUMBER = "teamcity.investigationsAutoAssigner.maxTestsFailuresToProcessPerBuild";
  public static final String SHOULD_PERSIST_FILTERED_TESTS_DESCRIPTION = "teamcity.investigationsAutoAssigner.persistFilteredTests";
  public static final String SHOULD_ASSIGN_RESOLVE_MANUALLY = "investigationsAutoAssigner.assignResolveManually";
  public static final String USER_AUTOMATICALLY_MARK_IMPORTANT_BUILDS_AS_FAVORITE_INTERNAL_PROPERTY = "automaticallyMarkImportantBuildsAsFavorite";

  // Server internal properties
  public static final String PROCESSING_DELAY_IN_SECONDS = "teamcity.investigationsAutoAssigner.scheduledTaskInterval.seconds";

  // Server internal properties (debug use only)
  public static final String STATISTICS_ENABLED = "teamcity.investigationsAutoAssigner.statisticsEnabled";

  public static final String MAX_COMPILE_ERRORS_TO_PROCESS = "teamcity.investigationsAutoAssigner.maxCompileErrorsToProcess";

  public static final String IGNORE_SETUP_TEARDOWN_METHODS = "teamcity.investigationsAutoAssigner.ignoreSetupAndTearDown";

  public static final String PREFERRED_INVESTIGATION_PROJECT = "teamcity.internal.preferredInvestigationProject";
  /**
   * When set (default), Auto-assigner will check {@link Constants#PREFERRED_INVESTIGATION_PROJECT} parameter
   * to find out target project for the auto-assigned investigation.
   *
   * @since 2022.2
   * @see "https://youtrack.jetbrains.com/issue/TW-74512"
   */
  public static final String USE_PREFERRED_PROJECT = "teamcity.internal.investigationsAutoAssigner.usePreferredProject";
  public static final String IGNORE_SNAPSHOT_DEPENDENCY_ERRORS_IN_DEFAULT_HEURISTIC = "teamcity.internal.investigationsAutoAssigner.defaultUserHeuristic.ignoreSnapshotDependencyErrors";

  //Constants
  public final static String TEAMCITY_DIRECTORY = ArtifactsConstants.TEAMCITY_ARTIFACTS_DIR;
  public final static String TEST_RUN_IN_REQUEST = "loadedTestRun";
  public final static int DEFAULT_TEST_COUNT_THRESHOLD = 100;
  public final static String ARTIFACT_DIRECTORY = "investigationsAutoAssigner";
  public static final String PLUGIN_DATA_DIR = ARTIFACT_DIRECTORY;
  public static final String ARTIFACT_FILENAME = "suggestions.json";
  public static final String STATISTICS_FILE_NAME = "statistics.json";
  public static final String STATISTICS_FILE_VERSION = "1.6";
  public static final String ASSIGN_DESCRIPTION_PREFIX = "Investigation was automatically assigned to";
  public static final String ASSIGNEE_FILTERED_LITERAL = "-";
  public static final String ASSIGNEE_FILTERED_DESCRIPTION_PREFIX = "This failed test was filtered by investigation auto assigner because it ";

  public static final Logger LOGGER = Logger.getInstance("jetbrains.buildServer.investigationsAutoAssigner");
  public static final Logger AGGREGATION_LOGGER = Logger.getInstance("InvestigationsAutoAssignerAggregation");
}