/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

package jetbrains.buildServer.investigationsAutoAssigner.utils;

import jetbrains.buildServer.serverSide.Parameter;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.users.SUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TargetProjectFinder {
  private static final String PREFERRED_INVESTIGATION_PROJECT = "teamcity.internal.preferredInvestigationProject";

  private final @NotNull ProjectManager myProjectManager;

  public TargetProjectFinder(@NotNull final ProjectManager projectManager) {
    myProjectManager = projectManager;
  }

  @Nullable
  public SProject getPreferredInvestigationProject(@NotNull SProject baseProject, @Nullable SUser currentUser) {
    final Parameter preferredProjectParameter = baseProject.getParameter(PREFERRED_INVESTIGATION_PROJECT);
    if (preferredProjectParameter != null) {
      final SProject p = myProjectManager.findProjectByExternalId(preferredProjectParameter.getValue());
      if (p != null && !p.isRootProject() && (currentUser == null || hasModifyPermission(currentUser, p))) {
        return p;
      }
    }
    return null;
  }

  private static boolean hasModifyPermission(final @NotNull SUser user, final SProject project) {
    return user.isPermissionGrantedForProject(project.getProjectId(), Permission.ASSIGN_INVESTIGATION) ||
           user.isPermissionGrantedForProject(project.getProjectId(), Permission.MANAGE_BUILD_PROBLEMS);
  }

}
