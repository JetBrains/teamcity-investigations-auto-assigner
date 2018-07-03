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

package jetbrains.buildServer.iaa.processing;

import com.intellij.openapi.diagnostic.Logger;
import java.util.List;
import jetbrains.buildServer.iaa.common.FailedBuildInfo;
import jetbrains.buildServer.iaa.common.HeuristicResult;
import jetbrains.buildServer.iaa.utils.CustomParameters;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import org.jetbrains.annotations.NotNull;

public class FailedTestAndBuildProblemsProcessor {

  private static final Logger LOGGER = Logger.getInstance(FailedTestAndBuildProblemsProcessor.class.getName());
  private final FailedTestFilter myFailedTestFilter;
  private final BuildProblemsFilter myBuildProblemsFilter;
  private final FailedTestAssigner myFailedTestAssigner;
  private final BuildProblemsAssigner myBuildProblemsAssigner;
  @NotNull private ResponsibleUserFinder myResponsibleUserFinder;


  public FailedTestAndBuildProblemsProcessor(@NotNull final ResponsibleUserFinder responsibleUserFinder,
                                             @NotNull final FailedTestFilter failedTestFilter,
                                             @NotNull final FailedTestAssigner failedTestAssigner,
                                             @NotNull final BuildProblemsFilter buildProblemsFilter,
                                             @NotNull final BuildProblemsAssigner buildProblemsAssigner) {
    myResponsibleUserFinder = responsibleUserFinder;
    myFailedTestFilter = failedTestFilter;
    myFailedTestAssigner = failedTestAssigner;
    myBuildProblemsFilter = buildProblemsFilter;
    myBuildProblemsAssigner = buildProblemsAssigner;
  }

  public Boolean processBuild(final FailedBuildInfo failedBuildInfo) {
    SBuild sBuild = failedBuildInfo.getBuild();
    SBuildType sBuildType = sBuild.getBuildType();
    if (sBuildType == null) {
      LOGGER.warn("Build #" + sBuild.getBuildId() + " doesn't have a build type.");
      return true;
    }
    LOGGER.debug("Start processing build #" + sBuild.getBuildId() + ".");

    SProject sProject = sBuildType.getProject();
    boolean shouldDelete = sBuild.isFinished();
    Integer threshold = CustomParameters.getMaxTestsPerBuildThreshold(sBuild);
    if (failedBuildInfo.processed >= threshold) {
      LOGGER.debug("Stop processing build #" + sBuild.getBuildId() + " as the threshold was exceeded.");
      return shouldDelete;
    }

    List<BuildProblem> allBuildProblems = ((BuildEx)sBuild).getBuildProblems();
    List<STestRun> allFailedTests = requestBrokenTestsWithStats(sBuild);

    LOGGER.debug("Build #" + sBuild.getBuildId() + ": has " + allBuildProblems.size() +
                 " build problems and " + allFailedTests.size() + " failed tests.");

    List<BuildProblem> applicableBuildProblems =
      myBuildProblemsFilter.apply(failedBuildInfo, sProject, allBuildProblems);
    List<STestRun> applicableFailedTests = myFailedTestFilter.apply(failedBuildInfo, sProject, allFailedTests);

    LOGGER.debug("Build #" + sBuild.getBuildId() + ": found " + applicableBuildProblems.size() +
                 " applicable build problems and " + applicableFailedTests.size() + " applicable failed tests.");

    HeuristicResult heuristicsResult =
      myResponsibleUserFinder.findResponsibleUser(sBuild, sProject, applicableBuildProblems, applicableFailedTests);

    myFailedTestAssigner.assign(heuristicsResult, sProject, applicableFailedTests);
    myBuildProblemsAssigner.assign(heuristicsResult, sProject, applicableBuildProblems);

    return shouldDelete;
  }

  private List<STestRun> requestBrokenTestsWithStats(final SBuild build) {
    BuildStatisticsOptions options = new BuildStatisticsOptions(
      BuildStatisticsOptions.FIRST_FAILED_IN_BUILD | BuildStatisticsOptions.FIXED_IN_BUILD, -1);
    BuildStatistics stats = build.getBuildStatistics(options);

    return stats.getFailedTests();
  }

}
