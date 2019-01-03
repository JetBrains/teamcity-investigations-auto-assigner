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

package jetbrains.buildServer.investigationsAutoAssigner.common;

import jetbrains.buildServer.ArtifactsConstants;
import jetbrains.buildServer.BuildProblemTypes;

public class Constants {
  // Plugin's ids
  public static final String BUILD_FEATURE_TYPE = "InvestigationsAutoAssigner";
  public static final String BUILD_FEATURE_DISPLAY_NAME = "Investigations Auto Assigner";

  // Build feature parameters
  public static final String DEFAULT_RESPONSIBLE = "defaultAssignee.username";
  public static final String USERS_TO_IGNORE = "excludeAssignees.usernames";
  public static final String SHOULD_DELAY_ASSIGNMENTS = "shouldDelayAssignments";

  // Build configuration parameter and internal property
  public static final String DEFAULT_SILENT_MODE_ENABLED = "teamcity.investigationsAutoAssigner.suggestions.enabledByDefault";
  public static final String ENABLE_FEATURE_BRANCHES_SUPPORT = "teamcity.investigationsAutoAssigner.enableFeatureBranchesSupport";
  public static final String MAX_TESTS_PER_BUILD_NUMBER = "teamcity.investigationsAutoAssigner.maxTestsFailuresToProcessPerBuild";

  // Server internal properties
  public static final String PROCESSING_DELAY_IN_SECONDS = "teamcity.investigationsAutoAssigner.scheduledTaskInterval.seconds";

  // Server internal properties (debug use only)
  public static final String INTERNAL_REPORTER_EMAIL = "teamcity.investigationsAutoAssigner.debugEmailAddress";

  //Constants
  public static final String TC_COMPILATION_ERROR_TYPE = BuildProblemTypes.TC_COMPILATION_ERROR_TYPE;
  public final static String TEAMCITY_DIRECTORY = ArtifactsConstants.TEAMCITY_ARTIFACTS_DIR;
  public final static String TEST_RUN_IN_REQUEST = "loadedTestRun";
  public final static int DEFAULT_TEST_COUNT_THRESHOLD = 100;
  public final static String ARTIFACT_DIRECTORY = "investigationsAutoAssigner";
  public static final String ARTIFACT_FILENAME = "suggestions.json";
}
