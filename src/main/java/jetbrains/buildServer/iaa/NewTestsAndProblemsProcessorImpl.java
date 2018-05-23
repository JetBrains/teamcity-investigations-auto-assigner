/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package jetbrains.buildServer.iaa;

import com.intellij.openapi.diagnostic.Logger;
import java.util.List;
import java.util.stream.Collectors;
import jetbrains.buildServer.iaa.utils.CustomParameters;
import jetbrains.buildServer.responsibility.BuildProblemResponsibilityFacade;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.responsibility.ResponsibilityEntryFactory;
import jetbrains.buildServer.responsibility.TestNameResponsibilityFacade;
import jetbrains.buildServer.responsibility.impl.BuildProblemResponsibilityEntryImpl;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.impl.problems.BuildProblemImpl;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.util.Dates;
import org.jetbrains.annotations.NotNull;

public class NewTestsAndProblemsProcessorImpl implements NewTestsAndProblemsProcessor {
  @NotNull private final TestNameResponsibilityFacade myTestNameResponsibilityFacade;
  @NotNull private final BuildProblemResponsibilityFacade myBuildProblemResponsibilityFacade;
  @NotNull private ResponsibleUserFinder myResponsibleUserFinder;
  @NotNull private BuildApplicabilityChecker myBuildApplicabilityChecker;
  @NotNull private final TestApplicabilityChecker myTestApplicabilityChecker;

  private static final Logger LOGGER = Logger.getInstance(NewTestsAndProblemsProcessorImpl.class.getName());

  NewTestsAndProblemsProcessorImpl(@NotNull final TestNameResponsibilityFacade testNameResponsibilityFacade,
                                   @NotNull final BuildProblemResponsibilityFacade buildProblemResponsibilityFacade,
                                   @NotNull final ResponsibleUserFinder responsibleUserFinder,
                                   @NotNull final BuildApplicabilityChecker buildApplicabilityChecker,
                                   @NotNull final TestApplicabilityChecker testApplicabilityChecker) {
    myTestNameResponsibilityFacade = testNameResponsibilityFacade;
    myBuildProblemResponsibilityFacade = buildProblemResponsibilityFacade;
    myResponsibleUserFinder = responsibleUserFinder;
    myBuildApplicabilityChecker = buildApplicabilityChecker;
    myTestApplicabilityChecker = testApplicabilityChecker;
  }

  @Override
  public Boolean processBuild(final FailedBuildInfo failedBuildInfo) {
    SBuild build = failedBuildInfo.getSBuild();
    final SBuildType buildType = build.getBuildType();
    assert buildType != null;

    Integer threshold = CustomParameters.getMaxTestsPerBuildThreshold(build);
    boolean shouldDelete = build.isFinished();
    if (failedBuildInfo.processed >= threshold) return shouldDelete;

    List<STestRun> failedTests = requestBrokenTestsWithStats(build);

    List<STestRun> applicableTestRuns = failedTests.stream()
                                                   .filter(failedBuildInfo::checkNotProcessed)
                                                   .filter(testRun -> myTestApplicabilityChecker
                                                     .isApplicable(buildType.getProject(), build, testRun))
                                                   .limit(threshold - failedBuildInfo.processed)
                                                   .collect(Collectors.toList());
    failedBuildInfo.addProcessedTestRuns(failedTests);
    failedBuildInfo.processed += applicableTestRuns.size();

    assert build instanceof BuildEx;
    final List<BuildProblem> buildProblems = ((BuildEx)build).getBuildProblems();
    BuildProblemImpl.fillIsNew(build.getBuildPromotion(), buildProblems);

    List<BuildProblem> applicableBuildProblems = buildProblems.stream()
                                                              .filter(failedBuildInfo::checkNotProcessed)
                                                              .filter(buildProblem -> myBuildApplicabilityChecker
                                                                .isApplicable(buildType.getProject(), build,
                                                                              buildProblem))
                                                              .limit(threshold - failedBuildInfo.processed)
                                                              .collect(Collectors.toList());

    failedBuildInfo.addProcessedBuildProblems(buildProblems);
    failedBuildInfo.processed += buildProblems.size();

    processFailedBuild(build, applicableBuildProblems, applicableTestRuns);
    return shouldDelete;
  }

  private List<STestRun> requestBrokenTestsWithStats(final SBuild build) {
    BuildStatisticsOptions options = new BuildStatisticsOptions(
      BuildStatisticsOptions.FIRST_FAILED_IN_BUILD | BuildStatisticsOptions.FIXED_IN_BUILD, -1);
    BuildStatistics stats = build.getBuildStatistics(options);

    return stats.getFailedTests();
  }

  private void processFailedBuild(SBuild sBuild, List<BuildProblem> buildProblems, List<STestRun> sTestRuns) {
    final SBuildType buildType = sBuild.getBuildType();
    assert buildType != null;
    final SProject project = buildType.getProject();

    HeuristicResult heuristicsResult = myResponsibleUserFinder.findResponsibleUser(sBuild, buildProblems, sTestRuns);

    for (STestRun sTestRun : sTestRuns) {
      Responsibility responsibility = heuristicsResult.getResponsibility(sTestRun);
      if (responsibility != null) {
        final STest test = sTestRun.getTest();
        final TestName testName = test.getName();

        myTestNameResponsibilityFacade.setTestNameResponsibility(
          testName, project.getProjectId(),
          ResponsibilityEntryFactory.createEntry(
            testName, test.getTestNameId(), ResponsibilityEntry.State.TAKEN, responsibility.getUser(), null,
            Dates.now(), responsibility.getDescription(), project, ResponsibilityEntry.RemoveMethod.WHEN_FIXED
          )
        );
      }
    }

    for (BuildProblem buildProblem : buildProblems) {
      Responsibility responsibility = heuristicsResult.getResponsibility(buildProblem);
      if (responsibility != null) {
        myBuildProblemResponsibilityFacade.setBuildProblemResponsibility(
          buildProblem, project.getProjectId(),
          new BuildProblemResponsibilityEntryImpl(
            ResponsibilityEntry.State.TAKEN, responsibility.getUser(), null, Dates.now(),
            responsibility.getDescription(), ResponsibilityEntry.RemoveMethod.WHEN_FIXED, project, buildProblem.getId()
          )
        );
      }
    }
  }
}
