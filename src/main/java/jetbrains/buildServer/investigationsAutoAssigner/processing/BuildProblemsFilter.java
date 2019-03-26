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
import java.util.*;
import java.util.stream.Collectors;
import jetbrains.buildServer.BuildProblemTypes;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.common.FailedBuildInfo;
import jetbrains.buildServer.investigationsAutoAssigner.utils.BuildProblemUtils;
import jetbrains.buildServer.investigationsAutoAssigner.utils.CustomParameters;
import jetbrains.buildServer.investigationsAutoAssigner.utils.InvestigationsManager;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class BuildProblemsFilter {

  private static final Logger LOGGER = Constants.LOGGER;
  private final BuildProblemUtils myBuildProblemUtils;
  private CustomParameters myCustomParameters;
  private InvestigationsManager myInvestigationsManager;
  private final Set<String> supportedTypes = Collections.unmodifiableSet(
    new HashSet<>(Arrays.asList(BuildProblemTypes.TC_COMPILATION_ERROR_TYPE, BuildProblemTypes.TC_EXIT_CODE_TYPE)));

  public BuildProblemsFilter(@NotNull final InvestigationsManager investigationsManager,
                             @NotNull final BuildProblemUtils buildProblemUtils,
                             @NotNull final CustomParameters customParameters) {
    myInvestigationsManager = investigationsManager;
    myBuildProblemUtils = buildProblemUtils;
    myCustomParameters = customParameters;
  }

  List<BuildProblem> apply(final FailedBuildInfo failedBuildInfo,
                           final SProject sProject,
                           final List<BuildProblem> buildProblems) {
    SBuild sBuild = failedBuildInfo.getBuild();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(String.format("Filtering of build problems for build id:%s started", sBuild.getBuildId()));
    }

    List<BuildProblem> filteredBuildProblems = buildProblems.stream()
                                                            .filter(failedBuildInfo::checkNotProcessed)
                                                            .filter(problem -> isApplicable(sProject, sBuild, problem))
                                                            .limit(failedBuildInfo.getLimitToProcess())
                                                            .collect(Collectors.toList());

    failedBuildInfo.addProcessedBuildProblems(buildProblems);
    failedBuildInfo.increaseProcessedNumber(filteredBuildProblems.size());

    return filteredBuildProblems;
  }


  List<BuildProblem> getStillApplicable(final FailedBuildInfo failedBuildInfo,
                                        final SProject sProject,
                                        final List<BuildProblem> allBuildProblems) {
    SBuild sBuild = failedBuildInfo.getBuild();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(String.format("Filtering before assign of build problems for build id:%s started", sBuild.getBuildId()));
    }

    return allBuildProblems.stream()
                           .filter(buildProblem -> isApplicable(sProject, sBuild, buildProblem))
                           .collect(Collectors.toList());
  }

  private boolean isApplicable(@NotNull final SProject project,
                               @NotNull final SBuild sBuild,
                               @NotNull final BuildProblem problem) {
    String reason = null;
    String buildProblemType = problem.getBuildProblemData().getType();

    if (problem.isMuted()) {
      reason = "is muted";
    } else if (!myBuildProblemUtils.isNew(problem)) {
      reason = "occurs not for the first time";
    } else if (!supportedTypes.contains(buildProblemType)) {
      reason = String.format("has an unsupported type %s. Supported types: %s",
                             problem.getBuildProblemData().getType(), supportedTypes);
    } else if (myInvestigationsManager.checkUnderInvestigation(project, sBuild, problem)) {
      reason = "is already under an investigation";
    } else if (myCustomParameters.getBuildProblemTypesToIgnore(sBuild).contains(buildProblemType)) {
      reason = "is among build problem types to ignore";
    }

    boolean isApplicable = reason == null;
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(String.format("Build problem id:%s:%s is %s.%s",
                                 sBuild.getBuildId(),
                                 problem.getTypeDescription(),
                                 (isApplicable ? "applicable" : "not applicable"),
                                 (isApplicable ? "" : String.format(" Reason: this build problem %s.", reason))
      ));
    }

    return isApplicable;
  }
}
