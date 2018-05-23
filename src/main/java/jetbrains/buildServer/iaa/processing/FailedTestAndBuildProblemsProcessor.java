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
import org.jetbrains.annotations.NotNull;

public class FailedTestAndBuildProblemsProcessor {
  @NotNull private ResponsibleUserFinder myResponsibleUserFinder;
  @NotNull private final List<ContextFilter> myContextFilters;
  @NotNull private final List<ResponsibilityAssigner> myResponsibilityAssigners;

  private static final Logger LOGGER = Logger.getInstance(FailedTestAndBuildProblemsProcessor.class.getName());

  public FailedTestAndBuildProblemsProcessor(@NotNull final ResponsibleUserFinder responsibleUserFinder,
                                             @NotNull final List<ContextFilter> contextFilters,
                                             @NotNull final List<ResponsibilityAssigner> responsibilityAssigners) {
    myResponsibleUserFinder = responsibleUserFinder;
    myContextFilters = contextFilters;
    myResponsibilityAssigners = responsibilityAssigners;
  }

  public Boolean processBuild(final FailedBuildInfo failedBuildInfo) {
    SBuild sBuild = failedBuildInfo.getSBuild();
    boolean shouldDelete = sBuild.isFinished();
    Integer threshold = CustomParameters.getMaxTestsPerBuildThreshold(sBuild);
    if (failedBuildInfo.processed >= threshold) return shouldDelete;

    List<STestRun> failedTests = requestBrokenTestsWithStats(sBuild);

    HeuristicContext heuristicContext =
      new HeuristicContext(failedBuildInfo, ((BuildEx)sBuild).getBuildProblems(), failedTests);

    for (ContextFilter contextFilter: myContextFilters) {
      heuristicContext = contextFilter.apply(heuristicContext);
    }

    HeuristicResult heuristicsResult = myResponsibleUserFinder.findResponsibleUser(heuristicContext);

    for (ResponsibilityAssigner assigner: myResponsibilityAssigners) {
      assigner.apply(heuristicsResult, heuristicContext);
    }

    return shouldDelete;
  }

  private List<STestRun> requestBrokenTestsWithStats(final SBuild build) {
    BuildStatisticsOptions options = new BuildStatisticsOptions(
      BuildStatisticsOptions.FIRST_FAILED_IN_BUILD | BuildStatisticsOptions.FIXED_IN_BUILD, -1);
    BuildStatistics stats = build.getBuildStatistics(options);

    return stats.getFailedTests();
  }

}
