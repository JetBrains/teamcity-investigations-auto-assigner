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
  private final FailedTestFilter myFailedTestFilter;
  private final BuildProblemsFilter myBuildProblemsFilter;
  private final FailedTestAssigner myFailedTestAssigner;
  private final BuildProblemsAssigner myBuildProblemsAssigner;
  @NotNull private ResponsibleUserFinder myResponsibleUserFinder;

  private static final Logger LOGGER = Logger.getInstance(FailedTestAndBuildProblemsProcessor.class.getName());

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
    SProject sProject = failedBuildInfo.getProject();
    boolean shouldDelete = sBuild.isFinished();
    Integer threshold = CustomParameters.getMaxTestsPerBuildThreshold(sBuild);
    if (failedBuildInfo.processed >= threshold) return shouldDelete;

    List<BuildProblem> allBuildProblems = ((BuildEx)sBuild).getBuildProblems();
    List<STestRun> allFailedTests = requestBrokenTestsWithStats(sBuild);

    List<BuildProblem> applicableBuildProblems = myBuildProblemsFilter.apply(failedBuildInfo, allBuildProblems);
    List<STestRun> applicableFailedTests = myFailedTestFilter.apply(failedBuildInfo, allFailedTests);

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
