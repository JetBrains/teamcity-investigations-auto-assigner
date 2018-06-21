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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jetbrains.buildServer.iaa.common.Constants;
import jetbrains.buildServer.iaa.common.FailedBuildInfo;
import jetbrains.buildServer.iaa.utils.CustomParameters;
import jetbrains.buildServer.iaa.utils.InvestigationsManager;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.impl.problems.BuildProblemImpl;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
class BuildProblemsFilter {

  private static final Logger LOGGER = Logger.getInstance(BuildProblemsFilter.class.getName());
  private InvestigationsManager myInvestigationsManager;
  private final Set<String> supportedTypes =
    Collections.unmodifiableSet(Collections.singleton(Constants.TC_COMPILATION_ERROR_TYPE));


  BuildProblemsFilter(@NotNull final InvestigationsManager investigationsManager) {
    myInvestigationsManager = investigationsManager;
  }

  List<BuildProblem> apply(final FailedBuildInfo failedBuildInfo,
                           final SProject sProject,
                           final List<BuildProblem> buildProblems) {
    SBuild sBuild = failedBuildInfo.getBuild();
    Integer threshold = CustomParameters.getMaxTestsPerBuildThreshold(sBuild);

    BuildProblemImpl.fillIsNew(sBuild.getBuildPromotion(), buildProblems);

    List<BuildProblem> filteredBuildProblems = buildProblems.stream()
                                                            .filter(failedBuildInfo::checkNotProcessed)
                                                            .filter(problem -> isApplicable(sProject, sBuild, problem))
                                                            .limit(threshold - failedBuildInfo.processed)
                                                            .collect(Collectors.toList());

    failedBuildInfo.addProcessedBuildProblems(buildProblems);
    failedBuildInfo.processed += filteredBuildProblems.size();

    return filteredBuildProblems;
  }

  private boolean isApplicable(@NotNull final SProject project,
                               @NotNull final SBuild sBuild,
                               @NotNull final BuildProblem problem) {
    String reason = null;
    if (problem.isMuted()) {
      reason = "is muted";
    } else if (!isNew(problem)) {
      reason = "occurs not for the first time";
    } else if (!supportedTypes.contains(problem.getBuildProblemData().getType())) {
      reason = String.format("has not supported type %s. Supported types: %s",
                             problem.getBuildProblemData().getType(), supportedTypes);
    } else if (myInvestigationsManager.checkUnderInvestigation(project, sBuild, problem)) {
      reason = "is already under an investigation";
    }

    boolean isApplicable = reason == null;
    LOGGER.debug(String.format("Build problem %s:%s is %s. Reason: this build problem %s.",
                               sBuild.getBuildId(),
                               problem.getTypeDescription(),
                               (isApplicable ? "applicable" : " not applicable"),
                               reason));

    return isApplicable;
  }

  private static boolean isNew(@NotNull final BuildProblem problem) {
    if (problem instanceof BuildProblemImpl) {
      final Boolean isNew = ((BuildProblemImpl)problem).isNew();
      return isNew != null && isNew;
    }

    return true;
  }
}
