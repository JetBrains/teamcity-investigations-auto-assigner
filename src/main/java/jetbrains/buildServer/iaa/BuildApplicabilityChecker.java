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

package jetbrains.buildServer.iaa;

import java.util.Collections;
import java.util.Set;
import jetbrains.buildServer.iaa.common.Constants;
import jetbrains.buildServer.iaa.utils.InvestigationsManager;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.impl.problems.BuildProblemImpl;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import org.jetbrains.annotations.NotNull;

public class BuildApplicabilityChecker {
  private InvestigationsManager myInvestigationsManager;
  private final Set<String> supportedTypes =
    Collections.unmodifiableSet(Collections.singleton(Constants.TC_COMPILATION_ERROR_TYPE));


  BuildApplicabilityChecker(@NotNull final InvestigationsManager investigationsManager) {
    myInvestigationsManager = investigationsManager;
  }

  boolean isApplicable(@NotNull final SProject project,
                       @NotNull final SBuild sBuild,
                       @NotNull final BuildProblem problem) {
    return (!problem.isMuted() &&
            isNew(problem) &&
            supportedTypes.contains(problem.getBuildProblemData().getType()) &&
            !myInvestigationsManager.checkUnderInvestigation(project, sBuild, problem));
  }

  private static boolean isNew(@NotNull final BuildProblem problem) {
    if (problem instanceof BuildProblemImpl) {
      final Boolean isNew = ((BuildProblemImpl)problem).isNew();
      return isNew != null && isNew;
    }

    return true;
  }
}
