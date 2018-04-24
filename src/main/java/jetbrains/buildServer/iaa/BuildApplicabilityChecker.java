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
import jetbrains.buildServer.BuildProject;
import jetbrains.buildServer.iaa.common.Constants;
import jetbrains.buildServer.responsibility.BuildProblemResponsibilityEntry;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.impl.problems.BuildProblemImpl;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import org.jetbrains.annotations.NotNull;

public class BuildApplicabilityChecker {
  private final Set<String> supportedTypes =
    Collections.unmodifiableSet(Collections.singleton(Constants.TC_COMPILATION_ERROR_TYPE));

  boolean check(@NotNull final SProject project, @NotNull final BuildProblemImpl problem) {
    return (!problem.isMuted() &&
            isNew(problem) &&
            supportedTypes.contains(problem.getBuildProblemData().getType()) &&
            !isInvestigated(problem, project));
  }

  private static boolean isNew(@NotNull final BuildProblemImpl problem) {
    final Boolean isNew = problem.isNew();
    return isNew != null && isNew;
  }

  private static boolean isInvestigated(@NotNull final BuildProblem problem, @NotNull final SProject project) {
    for (BuildProblemResponsibilityEntry entry : problem.getAllResponsibilities()) {
      if (isActiveOrFixed(entry) && isSameOrParent(entry.getProject(), project)) return true;
    }
    return false;
  }

  private static boolean isActiveOrFixed(@NotNull final ResponsibilityEntry entry) {
    final ResponsibilityEntry.State state = entry.getState();
    return state.isActive() || state.isFixed();
  }

  private static boolean isSameOrParent(@NotNull final BuildProject parent, @NotNull final BuildProject project) {
    if (parent.getProjectId().equals(project.getProjectId())) return true;
    final BuildProject parentProject = project.getParentProject();
    return parentProject != null && isSameOrParent(parent, parentProject);
  }


}
