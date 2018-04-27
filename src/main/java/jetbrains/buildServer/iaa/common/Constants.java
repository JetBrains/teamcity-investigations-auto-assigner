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
  public static final String BUILD_FEATURE_TYPE = "investigations-auto-assigner";
  public static final String BUILD_FEATURE_DISPLAY_NAME = "Investigations Auto Assigner";

  // Parameter names
  public static final String DEFAULT_RESPONSIBLE = "teamcity.iaa.defaultResponsible";

  //Message constants
  public static final String REASON_PREFIX = "Auto-assigned investigation:";

  //Constants
  public static final String TC_COMPILATION_ERROR_TYPE = "TC_COMPILATION_ERROR";
}
