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
import jetbrains.buildServer.investigationsAutoAssigner.persistent.AssignerArtifactDao;
import jetbrains.buildServer.investigationsAutoAssigner.utils.CustomParameters;
import jetbrains.buildServer.serverSide.BuildEx;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import org.jetbrains.annotations.NotNull;

public class FailedTestAndBuildProblemsProcessor extends BaseProcessor {

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
    SProject sProject = getProject(sBuild);
    if (sProject == null) return;

    LOGGER.debug("Start processing build #" + sBuild.getBuildId() + ".");
    int threshold = CustomParameters.getMaxTestsPerBuildThreshold(sBuild);
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

    List<STestRun> testsForAssign = myFailedTestFilter.applyBeforeAssign(failedBuildInfo, sProject, applicableFailedTests);
    List<BuildProblem> problemsForAssign =
      myBuildProblemsFilter.applyBeforeAssign(failedBuildInfo, sProject, applicableProblems);
    logChangedProblemsNumber(sBuild, applicableFailedTests, testsForAssign, applicableProblems, problemsForAssign);

    myAssignerArtifactDao.appendHeuristicsResult(sBuild, testsForAssign, heuristicsResult);

    if (CustomParameters.isBuildFeatureEnabled(sBuild) && !failedBuildInfo.shouldDelayAssignments) {
      myFailedTestAssigner.assign(heuristicsResult, sProject, sBuild, testsForAssign);
      myBuildProblemsAssigner.assign(heuristicsResult, sProject, sBuild, problemsForAssign);
    } else if (LOGGER.isDebugEnabled() && !failedBuildInfo.getHeuristicsResult().isEmpty()) {
      if (!CustomParameters.isBuildFeatureEnabled(sBuild)) {
        LOGGER.debug("Found investigations but build feature is not configured.");
      } else if (failedBuildInfo.shouldDelayAssignments) {
        LOGGER.debug("Found investigations but assignments should be delayed.");
      }
    }

    failedBuildInfo.addHeuristicsResult(heuristicsResult);
  }
}
