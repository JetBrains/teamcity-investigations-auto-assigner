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

import jetbrains.buildServer.serverSide.SBuild;
import org.jetbrains.annotations.Nullable;

public class CustomParameters {
  public static Integer getMaxTestsPerBuildThreshold(SBuild build) {
    return parseThreshold(build.getBuildOwnParameters().get("autoassigner.maxTestsPerBuildNumber"));
  }

  private static int parseThreshold(@Nullable String value) {
    final Integer DEFAULT_TEST_COUNT_THRESHOLD = 100;
    if (value == null) {
      return DEFAULT_TEST_COUNT_THRESHOLD;
    }
    try {
      Integer parsedValue = Integer.parseInt(value);
      return parsedValue >= 0 ? parsedValue : Integer.MAX_VALUE;
    } catch (NumberFormatException e) {
      return DEFAULT_TEST_COUNT_THRESHOLD;
    }
  }
}
