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

package jetbrains.buildServer.iaa.common;

public class Constants {
  // Plugin's ids
  public static final String BUILD_FEATURE_TYPE = "InvestigationAutoAssigner";
  public static final String BUILD_FEATURE_DISPLAY_NAME = "Investigation Auto Assigner";

   // Build feature parameters
  public static final String DEFAULT_RESPONSIBLE = "defaultAssignee.username";
  public static final String USERS_TO_IGNORE = "excludeAssignees.usernames";
  
  // Build configuration parameter and internal property
  public static final String DEFAULT_SILENT_MODE_ENABLED = "teamcity.investigationAutoAssigner.enabledByDefault";
  
  // Server internal properties (debug use only)
  public static final String INTERNAL_REPORTER_EMAIL = "teamcity.investigationAutoAssigner.copyReport.emailAddress";
  public static final String PROCESSING_DELAY_IN_SECONDS = "teamcity.investigationAutoAssigner.scheduledTaskInterval.seconds";
  
  // Build parameter
  public static final String MAX_TESTS_PER_BUILD_NUMBER = "teamcity.investigationAutoAssigner.maxTestsFailuresToProcessPerBuild";

  //TeamCity API constants
  public static final String TC_COMPILATION_ERROR_TYPE = "TC_COMPILATION_ERROR";
  public final static String TEAMCITY_DIRECTORY = ".teamcity";
  public final static String TEST_RUN_IN_REQUEST = "loadedTestRun";

}
