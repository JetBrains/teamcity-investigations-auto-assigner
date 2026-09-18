

package jetbrains.buildServer.investigationsAutoAssigner.utils;


import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.impl.ProjectEx;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.investigationsAutoAssigner.common.Constants.PREFERRED_INVESTIGATION_PROJECT;
import static jetbrains.buildServer.investigationsAutoAssigner.common.Constants.USE_PREFERRED_PROJECT;

public class TargetProjectFinder {

  private final @NotNull ProjectManager myProjectManager;

  public TargetProjectFinder(@NotNull final ProjectManager projectManager) {
    myProjectManager = projectManager;
  }

  @Nullable
  public SProject getPreferredInvestigationProject(@NotNull SProject baseProject, @Nullable SUser currentUser) {
    final boolean tryDetectPreferredProject = ((ProjectEx)baseProject).getBooleanInternalParameterOrTrue(USE_PREFERRED_PROJECT);
    final String preferredProjectExtId = ((ProjectEx)baseProject).getInternalParameterValue(PREFERRED_INVESTIGATION_PROJECT, "");

    if (tryDetectPreferredProject && StringUtil.isNotEmpty(preferredProjectExtId)) {
      final SProject p = myProjectManager.findProjectByExternalId(preferredProjectExtId);
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