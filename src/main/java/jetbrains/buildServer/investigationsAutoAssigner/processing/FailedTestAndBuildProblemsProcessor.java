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

package jetbrains.buildServer.investigationsAutoAssigner.processing;

import com.intellij.openapi.diagnostic.Logger;
import java.util.List;
import jetbrains.buildServer.investigationsAutoAssigner.common.FailedBuildInfo;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.utils.AssignerArtifactDao;
import jetbrains.buildServer.investigationsAutoAssigner.utils.CustomParameters;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import org.jetbrains.annotations.NotNull;

public class FailedTestAndBuildProblemsProcessor {

  private static final Logger LOGGER = Logger.getInstance(FailedTestAndBuildProblemsProcessor.class.getName());
  private final FailedTestFilter myFailedTestFilter;
  private final BuildProblemsFilter myBuildProblemsFilter;
  private final FailedTestAssigner myFailedTestAssigner;
  private final BuildProblemsAssigner myBuildProblemsAssigner;
  @NotNull private final CustomParameters myCustomParameters;
  @NotNull private final AssignerArtifactDao myAssignerArtifactDao;
  @NotNull private ResponsibleUserFinder myResponsibleUserFinder;


  public FailedTestAndBuildProblemsProcessor(@NotNull final ResponsibleUserFinder responsibleUserFinder,
                                             @NotNull final FailedTestFilter failedTestFilter,
                                             @NotNull final FailedTestAssigner failedTestAssigner,
                                             @NotNull final BuildProblemsFilter buildProblemsFilter,
                                             @NotNull final BuildProblemsAssigner buildProblemsAssigner,
                                             @NotNull final CustomParameters customParameters,
                                             @NotNull final AssignerArtifactDao assignerArtifactDao) {
    myResponsibleUserFinder = responsibleUserFinder;
    myFailedTestFilter = failedTestFilter;
    myFailedTestAssigner = failedTestAssigner;
    myBuildProblemsFilter = buildProblemsFilter;
    myBuildProblemsAssigner = buildProblemsAssigner;
    myCustomParameters = customParameters;
    myAssignerArtifactDao = assignerArtifactDao;
  }

  public void processBuild(final FailedBuildInfo failedBuildInfo) {
    SBuild sBuild = failedBuildInfo.getBuild();
    SBuildType sBuildType = sBuild.getBuildType();
    if (sBuildType == null) {
      LOGGER.warn("Build #" + sBuild.getBuildId() + " doesn't have a build type.");
      return;
    }
    LOGGER.debug("Start processing build #" + sBuild.getBuildId() + ".");

    SProject sProject = sBuildType.getProject();
    Integer threshold = CustomParameters.getMaxTestsPerBuildThreshold(sBuild);
    if (failedBuildInfo.processed >= threshold) {
      LOGGER.debug("Stop processing build #" + sBuild.getBuildId() + " as the threshold was exceeded.");
      return;
    }

    List<BuildProblem> allBuildProblems = ((BuildEx)sBuild).getBuildProblems();
    List<STestRun> allFailedTests = requestBrokenTestsWithStats(sBuild);
    List<BuildProblem> applicableProblems = myBuildProblemsFilter.apply(failedBuildInfo, sProject, allBuildProblems);
    List<STestRun> applicableFailedTests = myFailedTestFilter.apply(failedBuildInfo, sProject, allFailedTests);
    logProblemsNumber(sBuild, applicableFailedTests, applicableProblems);

    HeuristicResult heuristicsResult =
      myResponsibleUserFinder.findResponsibleUser(sBuild, sProject, applicableProblems, applicableFailedTests);

    List<STestRun> testsForAssign = myFailedTestFilter.applyBeforeAssign(failedBuildInfo, sProject, allFailedTests);
    List<BuildProblem> problemsForAssign =
      myBuildProblemsFilter.applyBeforeAssign(failedBuildInfo, sProject, allBuildProblems);
    logChangedProblemsNumber(sBuild, applicableFailedTests, testsForAssign, applicableProblems, problemsForAssign);

    boolean silentModeOn = myCustomParameters.isSilentModeOn(sBuild);
    myAssignerArtifactDao.appendHeuristicsResult(sBuild, testsForAssign, heuristicsResult);
    myFailedTestAssigner.assign(heuristicsResult, sProject, testsForAssign, silentModeOn);
    myBuildProblemsAssigner.assign(heuristicsResult, sProject, problemsForAssign, silentModeOn);

    failedBuildInfo.addHeuristicsResult(heuristicsResult);
  }

  private void logProblemsNumber(SBuild sBuild,
                                 final List<STestRun> afterFilteringTests,
                                 final List<BuildProblem> afterFilteringProblems) {
    if (!LOGGER.isDebugEnabled()) {
      return;
    }

    LOGGER.debug("Build #" + sBuild.getBuildId() + ": found " + afterFilteringProblems.size() +
                 " applicable build problems and " + afterFilteringTests.size() + " applicable failed tests.");
  }

  private void logChangedProblemsNumber(SBuild sBuild,
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

  private List<STestRun> requestBrokenTestsWithStats(final SBuild build) {
    BuildStatisticsOptions options = new BuildStatisticsOptions(
      BuildStatisticsOptions.FIRST_FAILED_IN_BUILD | BuildStatisticsOptions.FIXED_IN_BUILD, -1);
    BuildStatistics stats = build.getBuildStatistics(options);

    return stats.getFailedTests();
  }

}
