/*
 * Copyright 2000-2019 JetBrains s.r.o.
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

package jetbrains.buildServer.investigationsAutoAssigner.processing;

import com.intellij.openapi.diagnostic.Logger;
import java.util.List;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import org.jetbrains.annotations.Nullable;

abstract class BaseProcessor {
  private static final Logger LOGGER = Constants.LOGGER;


  protected List<STestRun> requestBrokenTestsWithStats(final SBuild build) {
    BuildStatisticsOptions options = new BuildStatisticsOptions(
      BuildStatisticsOptions.FIRST_FAILED_IN_BUILD | BuildStatisticsOptions.FIXED_IN_BUILD, -1);
    BuildStatistics stats = build.getBuildStatistics(options);

    return stats.getFailedTests();
  }

  protected void logChangedProblemsNumber(SBuild sBuild,
                                          final List<STestRun> beforeFilteringTests,
                                          final List<STestRun> afterFilteringTests,
                                          final List<BuildProblem> beforeFilteringProblems,
                                          final List<BuildProblem> afterFilteringProblems) {
    if (!LOGGER.isDebugEnabled()) {
      return;
    }

    if (beforeFilteringTests.size() != afterFilteringTests.size()) {
      LOGGER.debug("Build #" + sBuild.getBuildId() + ": number of applicable tests changed because " +
                   (beforeFilteringTests.size() - afterFilteringTests.size()) + " became not applicable");
    }
    if (beforeFilteringProblems.size() != afterFilteringProblems.size()) {
      LOGGER.debug("Build #" + sBuild.getBuildId() + ": number of applicable problems changed because " +
                   (beforeFilteringProblems.size() - beforeFilteringProblems.size()) + " became not applicable");
    }
  }

  protected void logProblemsNumber(SBuild sBuild,
                                   final List<STestRun> afterFilteringTests,
                                   final List<BuildProblem> afterFilteringProblems) {
    if (!LOGGER.isDebugEnabled()) {
      return;
    }

    LOGGER.debug("Build #" + sBuild.getBuildId() + ": found " + afterFilteringProblems.size() +
                 " applicable build problems and " + afterFilteringTests.size() + " applicable failed tests.");
  }

  @Nullable
  protected SProject getProject(final SBuild sBuild) {
    SBuildType sBuildType = sBuild.getBuildType();
    if (sBuildType == null) {
      LOGGER.debug("Build #" + sBuild.getBuildId() + " doesn't have a build type. Stop processing.");
      return null;
    }

    return sBuildType.getProject();
  }
}
