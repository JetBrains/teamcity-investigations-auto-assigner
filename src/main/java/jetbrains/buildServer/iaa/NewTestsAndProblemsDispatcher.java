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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.iaa.common.Constants;
import jetbrains.buildServer.iaa.utils.CustomParameters;
import jetbrains.buildServer.iaa.utils.InvestigationsManager;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.impl.problems.BuildProblemImpl;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.serverSide.stat.BuildTestsListener;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.util.executors.ExecutorsFactory;
import org.jetbrains.annotations.NotNull;

public class NewTestsAndProblemsDispatcher {
  @NotNull private final NewTestsAndProblemsProcessor myProcessor;
  @NotNull private final BuildsManager myBuildsManager;
  @NotNull private final TestApplicabilityChecker myTestApplicabilityChecker;
  @NotNull private final InvestigationsManager myInvestigationsManager;
  // Map isn't synchronized because we work with it from synchronized method
  @NotNull private final FailedBuildManager myFailedBuildManager;
  @NotNull private final ScheduledExecutorService myDaemon;

  public NewTestsAndProblemsDispatcher(@NotNull final BuildTestsEventDispatcher buildTestsEventDispatcher,
                                       @NotNull final BuildServerListenerEventDispatcher buildServerListenerEventDispatcher,
                                       @NotNull final NewTestsAndProblemsProcessor processor,
                                       @NotNull final BuildsManager buildsManager,
                                       @NotNull final TestApplicabilityChecker testApplicabilityChecker,
                                       @NotNull final InvestigationsManager investigationsManager) {
    myProcessor = processor;
    myBuildsManager = buildsManager;
    myTestApplicabilityChecker = testApplicabilityChecker;
    myInvestigationsManager = investigationsManager;
    myFailedBuildManager = new FailedBuildManager();
    myDaemon = ExecutorsFactory.newFixedScheduledDaemonExecutor("Investigator-Auto-Assigner-", 1);
    myDaemon.scheduleWithFixedDelay(this::processBrokenBuildsOneThread, 2, 2, TimeUnit.MINUTES);

    buildTestsEventDispatcher.addListener(new BuildTestsListener() {
      public void testPassed(@NotNull SRunningBuild sRunningBuild, @NotNull List<Long> list) {
        //
      }

      public void testFailed(@NotNull SRunningBuild build, @NotNull List<Long> testNameIds) {
        if (shouldIgnore(build)) return;
        myFailedBuildManager.addFailedBuild(build);
      }


      public void testIgnored(@NotNull SRunningBuild sRunningBuild, @NotNull List<Long> list) {
        //
      }
    });

    buildServerListenerEventDispatcher.addListener(new BuildServerAdapter() {
      @Override
      public void buildProblemsChanged(@NotNull SBuild sBuild,
                                       @NotNull List<BuildProblemData> before,
                                       @NotNull List<BuildProblemData> after) {
        if (shouldIgnore(sBuild) || !(sBuild instanceof BuildEx)) return;

        final List<BuildProblemData> newProblems = new ArrayList<>(after);
        newProblems.removeAll(before);
        for (BuildProblemData newProblem : newProblems) {
          onBuildProblemOccurred((BuildEx)sBuild, newProblem);
        }
      }

      @Override
      public void serverShutdown() {
        myDaemon.shutdown();
      }
    });
  }

  private void processBrokenBuildsOneThread() {
    for (Long buildId : myFailedBuildManager.getBuilds()) {
      SBuild build = myBuildsManager.findBuildInstanceById(buildId);
      if (build == null) continue;
      processBuildOneThread(build);
    }
  }

  private void processBuildOneThread(final SBuild build) {
    Integer threshold = CustomParameters.getMaxTestsPerBuildThreshold(build);
    final SBuildType buildType = build.getBuildType();
    if (buildType == null) return;
    FailedBuildInfo buildInfo = myFailedBuildManager.getFailedBuildInfo(build);
    if (buildInfo.processed >= threshold) return;

    SProject project = buildType.getProject();
    boolean shouldDelete = build.isFinished();
    List<STestRun> failedTests = requestBrokenTestsWithStats(build);

    List<STestRun> applicableTestRuns = failedTests.stream()
                                                   .filter(buildInfo::checkNotProcessed)
                                                   .filter(testRun -> myTestApplicabilityChecker
                                                     .isApplicable(buildType.getProject(), build, testRun))
                                                   .limit(threshold - buildInfo.processed)
                                                   .collect(Collectors.toList());
    List<STest> applicableTests = applicableTestRuns.stream().map(STestRun::getTest).collect(Collectors.toList());
    HashMap<Long, User> testId2Responsible = myInvestigationsManager.findInAudit(applicableTests, project);

    buildInfo.processed += applicableTestRuns.size();
    for (STestRun testRun : applicableTestRuns) {
      final STest test = testRun.getTest();
      final TestName testName = test.getName();
      final String problemText = testName.getAsString() + " " + testRun.getFullText();
      TestProblemInfo problemInfo =
        new TestProblemInfo(test, build, buildType.getProject(), problemText, testId2Responsible);
      myProcessor.processFailedTest(build, testRun, problemInfo);
    }

    if (shouldDelete) {
      myFailedBuildManager.removeBuild(build);
    } else {
      buildInfo.addProcessedTestRuns(failedTests);
    }
  }

  private List<STestRun> requestBrokenTestsWithStats(final SBuild build) {
    BuildStatisticsOptions options = new BuildStatisticsOptions(
      BuildStatisticsOptions.FIRST_FAILED_IN_BUILD | BuildStatisticsOptions.FIXED_IN_BUILD,
      -1);
    BuildStatistics stats = build.getBuildStatistics(options);

    return stats.getFailedTests();
  }

  private void onBuildProblemOccurred(@NotNull final BuildEx build, @NotNull final BuildProblemData problem) {
    myDaemon.submit(() -> {
      final List<BuildProblem> buildProblems = build.getBuildProblems();
      BuildProblemImpl.fillIsNew(build.getBuildPromotion(), buildProblems);
      for (BuildProblem buildProblem : buildProblems) {
        if (buildProblem.getBuildProblemData().equals(problem)) {
          if (buildProblem instanceof BuildProblemImpl) {
            myProcessor.onBuildProblemOccurred(build, (BuildProblemImpl)buildProblem);
          }
          break;
        }
      }
    });
  }

  private static boolean shouldIgnore(@NotNull SBuild build) {
    return checkFeatureDisabled(build) || build.isPersonal();
  }

  private static boolean checkFeatureDisabled(@NotNull SBuild build) {
    Collection<SBuildFeatureDescriptor> descriptors = build.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE);

    return descriptors.isEmpty();
  }
}
