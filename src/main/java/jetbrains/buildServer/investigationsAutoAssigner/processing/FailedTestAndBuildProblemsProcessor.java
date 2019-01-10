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
import com.intellij.openapi.util.Pair;
import java.util.List;
import java.util.stream.Collectors;
import jetbrains.buildServer.investigationsAutoAssigner.common.FailedBuildInfo;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.persistent.AssignerArtifactDao;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FailedTestAndBuildProblemsProcessor {

  private static final Logger LOGGER = Logger.getInstance(FailedTestAndBuildProblemsProcessor.class.getName());
  private final FailedTestFilter myFailedTestFilter;
  private final BuildProblemsFilter myBuildProblemsFilter;
  private final FailedTestAssigner myFailedTestAssigner;
  private final BuildProblemsAssigner myBuildProblemsAssigner;
  @NotNull private final AssignerArtifactDao myAssignerArtifactDao;
  @NotNull private ResponsibleUserFinder myResponsibleUserFinder;


  public FailedTestAndBuildProblemsProcessor(@NotNull final ResponsibleUserFinder responsibleUserFinder,
                                             @NotNull final FailedTestFilter failedTestFilter,
                                             @NotNull final FailedTestAssigner failedTestAssigner,
                                             @NotNull final BuildProblemsFilter buildProblemsFilter,
                                             @NotNull final BuildProblemsAssigner buildProblemsAssigner,
                                             @NotNull final AssignerArtifactDao assignerArtifactDao) {
    myResponsibleUserFinder = responsibleUserFinder;
    myFailedTestFilter = failedTestFilter;
    myFailedTestAssigner = failedTestAssigner;
    myBuildProblemsFilter = buildProblemsFilter;
    myBuildProblemsAssigner = buildProblemsAssigner;
    myAssignerArtifactDao = assignerArtifactDao;
  }

  public void processBuild(final FailedBuildInfo failedBuildInfo) {
    SBuild sBuild = failedBuildInfo.getBuild();
    @Nullable
    SProject sProject = getProject(sBuild);
    if (!shouldBeProcessed(failedBuildInfo, sProject)) {
      return;
    }
    LOGGER.debug("Start processing build #" + failedBuildInfo.getBuildId() + ".");

    Pair<List<BuildProblem>, List<STestRun>> applicableProblems = getApplicableProblems(failedBuildInfo, sProject);
    HeuristicResult foundHeuristicsResult = myResponsibleUserFinder.findResponsibleUser(sBuild,
                                                                                        sProject,
                                                                                        applicableProblems.getFirst(),
                                                                                        applicableProblems.getSecond());

    Pair<List<BuildProblem>, List<STestRun>> stillApplicable = getStillApplicable(failedBuildInfo,
                                                                                  sProject,
                                                                                  applicableProblems);


    myAssignerArtifactDao.appendHeuristicsResult(sBuild, stillApplicable.getSecond(), foundHeuristicsResult);

    if (failedBuildInfo.shouldAssignInvestigations(foundHeuristicsResult)) {
      assignInvestigations(foundHeuristicsResult, sProject, sBuild, stillApplicable);
    } else if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(failedBuildInfo.getNotAssignReason(foundHeuristicsResult));
    }

    failedBuildInfo.addHeuristicsResult(foundHeuristicsResult);
  }

  @Nullable
  private SProject getProject(final SBuild sBuild) {
    SBuildType sBuildType = sBuild.getBuildType();
    if (sBuildType == null) {
      LOGGER.warn("Build #" + sBuild.getBuildId() + " doesn't have a build type.");
      return null;
    }

    return sBuildType.getProject();
  }

  private boolean shouldBeProcessed(final FailedBuildInfo failedBuildInfo, final SProject sProject) {
    if (sProject == null) return false;

    if (failedBuildInfo.isOverProcessedProblemsThreshold()) {
      LOGGER.debug("Stop processing build #" + failedBuildInfo.getBuildId() + " as the threshold was exceeded.");
      return false;
    }

    return true;
  }

  private Pair<List<BuildProblem>, List<STestRun>> getApplicableProblems(final FailedBuildInfo failedBuildInfo,
                                                                         final SProject sProject) {
    SBuild sBuild = failedBuildInfo.getBuild();
    List<BuildProblem> allBuildProblems = ((BuildEx)sBuild).getBuildProblems();
    List<STestRun> allFailedTests = requestBrokenTestsWithStats(sBuild);
    List<BuildProblem> applicableProblems = myBuildProblemsFilter.apply(failedBuildInfo, sProject, allBuildProblems);
    List<STestRun> applicableFailedTests = myFailedTestFilter.apply(failedBuildInfo, sProject, allFailedTests);
    logProblemsNumber(sBuild, applicableFailedTests, applicableProblems);

    return new Pair<>(allBuildProblems, allFailedTests);
  }

  private List<STestRun> requestBrokenTestsWithStats(final SBuild build) {
    BuildStatisticsOptions options = new BuildStatisticsOptions(
      BuildStatisticsOptions.FIRST_FAILED_IN_BUILD | BuildStatisticsOptions.FIXED_IN_BUILD, -1);
    BuildStatistics stats = build.getBuildStatistics(options);

    return stats.getFailedTests();
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

  private Pair<List<BuildProblem>, List<STestRun>> getStillApplicable(final FailedBuildInfo failedBuildInfo,
                                                                      final SProject sProject,
                                                                      final Pair<List<BuildProblem>, List<STestRun>> applicableProblems) {
    SBuild sBuild = failedBuildInfo.getBuild();
    List<STestRun> testsForAssign =
      myFailedTestFilter.getStillApplicable(failedBuildInfo, sProject, applicableProblems.getSecond());
    List<BuildProblem> problemsForAssign =
      myBuildProblemsFilter.getStillApplicable(failedBuildInfo, sProject, applicableProblems.getFirst());
    logChangedProblemsNumber(sBuild, applicableProblems.getSecond(), testsForAssign,
                             applicableProblems.getFirst(), problemsForAssign);

    return new Pair<>(problemsForAssign, testsForAssign);
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

  private void assignInvestigations(final HeuristicResult heuristicsResult,
                                    final SProject sProject,
                                    final SBuild sBuild,
                                    final Pair<List<BuildProblem>, List<STestRun>> stillApplicable) {
    myFailedTestAssigner.assign(heuristicsResult, sProject, sBuild, stillApplicable.getSecond());
    myBuildProblemsAssigner.assign(heuristicsResult, sProject, sBuild, stillApplicable.getFirst());

  }


  public void processDelayedAssignments(final FailedBuildInfo failedBuildInfo, SBuild nextBuild) {
    SBuild sBuild = failedBuildInfo.getBuild();
    SProject sProject = getProject(sBuild);
    if (sProject == null) return;

    LOGGER.debug("Start processing delayed assignments for build #" + sBuild.getBuildId() + ".");


    Pair<List<BuildProblem>, List<STestRun>> applicableProblems2 = getApplicableForDelayAssignments(failedBuildInfo,
                                                                                                    nextBuild);
    Pair<List<BuildProblem>, List<STestRun>> stillApplicable = getStillApplicable(failedBuildInfo,
                                                                                  sProject,
                                                                                  applicableProblems2);
    assignInvestigations(failedBuildInfo.getHeuristicsResult(), sProject, sBuild, stillApplicable);
  }

  private Pair<List<BuildProblem>, List<STestRun>> getApplicableForDelayAssignments(final FailedBuildInfo failedBuildInfo,
                                                                                    final SBuild nextBuild) {
    SBuild sBuild = failedBuildInfo.getBuild();
    HeuristicResult heuristicsResult = failedBuildInfo.getHeuristicsResult();

    List<STestRun> applicableFailedTests =
      requestBrokenTestsWithStats(sBuild).stream()
                                         .filter(failedTest -> heuristicsResult.getResponsibility(failedTest) != null)
                                         .collect(Collectors.toList());

    List<String> nextBuildProblemIdentities =
      ((BuildEx)nextBuild).getBuildProblems()
                          .stream()
                          .map(buildProblem -> buildProblem.getBuildProblemData().getIdentity())
                          .collect(Collectors.toList());

    List<BuildProblem> applicableProblems =
      ((BuildEx)sBuild).getBuildProblems()
                       .stream()
                       .filter(buildProblem -> heuristicsResult.getResponsibility(buildProblem) != null &&
                                               nextBuildProblemIdentities.contains(
                                                 buildProblem.getBuildProblemData().getIdentity()))
                       .collect(Collectors.toList());

    logProblemsNumber(sBuild, applicableFailedTests, applicableProblems);
    return new Pair<>(applicableProblems, applicableFailedTests);

  }
}
