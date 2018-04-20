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

package jetbrains.buildServer.iaa.utils;

import jetbrains.buildServer.BuildProject;
import jetbrains.buildServer.responsibility.BuildProblemResponsibilityEntry;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.responsibility.TestNameResponsibilityEntry;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STest;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import org.jetbrains.annotations.NotNull;

public class InvestigationsManager {
  public boolean checkUnderInvestigation(@NotNull final SProject project, @NotNull final BuildProblem problem) {
    for (BuildProblemResponsibilityEntry entry : problem.getAllResponsibilities()) {
      if (isActiveOrFixed(entry) && belongSameProjectOrParent(entry.getProject(), project)) return true;
    }
    return false;
  }

  public boolean checkUnderInvestigation(@NotNull final SProject project, @NotNull final STest test) {
    for (TestNameResponsibilityEntry entry : test.getAllResponsibilities()) {
      if (isActiveOrFixed(entry) && belongSameProjectOrParent(entry.getProject(), project)) return true;
    }
    return false;
  }

  private boolean isActiveOrFixed(@NotNull final ResponsibilityEntry entry) {
    final ResponsibilityEntry.State state = entry.getState();
    return state.isActive() || state.isFixed();
  }

  private boolean belongSameProjectOrParent(@NotNull final BuildProject parent, @NotNull final BuildProject project) {
    if (parent.getProjectId().equals(project.getProjectId())) return true;
    final BuildProject parentProject = project.getParentProject();
    return parentProject != null && belongSameProjectOrParent(parent, parentProject);
  }
}
