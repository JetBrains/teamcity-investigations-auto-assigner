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

import jetbrains.buildServer.iaa.utils.InvestigationsManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.impl.problems.BuildProblemImpl;
import org.jetbrains.annotations.NotNull;

public class BuildApplicabilityChecker {

  private InvestigationsManager myInvestigationsManager;

  BuildApplicabilityChecker(@NotNull final InvestigationsManager investigationsManager) {
    myInvestigationsManager = investigationsManager;
  }

  boolean check(@NotNull final SProject project, @NotNull final BuildProblemImpl problem) {
    return (!problem.isMuted() &&
            isNew(problem) &&
            !myInvestigationsManager.checkUnderInvestigation(project, problem));
  }

  private static boolean isNew(@NotNull final BuildProblemImpl problem) {
    final Boolean isNew = problem.isNew();
    return isNew != null && isNew;
  }
}
