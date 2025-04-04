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

  /**
   * Constructs a TargetProjectFinder.
   *
   * @param projectManager the ProjectManager to be used for project lookups
   */
  public TargetProjectFinder(@NotNull final ProjectManager projectManager) {
    myProjectManager = projectManager;
  }

  /**
   * Retrieves the preferred investigation project for the given base project and user.
   *
   * @param baseProject the base project from which to determine the preferred investigation project
   * @param currentUser the current user requesting the preferred project
   * @return the preferred investigation project, or null if not found or the user lacks permission
   */
  @Nullable
  public SProject getPreferredInvestigationProject(@NotNull SProject baseProject, @Nullable SUser currentUser) {
    boolean tryDetectPreferredProject = shouldTryDetectPreferredProject(baseProject);
    String preferredProjectExtId = getPreferredProjectExtId(baseProject);

    if (tryDetectPreferredProject && StringUtil.isNotEmpty(preferredProjectExtId)) {
      return findPreferredProject(preferredProjectExtId, currentUser);
    }
    return null;
  }

  /**
   * Determines whether the preferred project detection should be attempted based on the base project configuration.
   *
   * @param baseProject the base project to check
   * @return true if preferred project detection should be attempted, false otherwise
   */
  private boolean shouldTryDetectPreferredProject(@NotNull SProject baseProject) {
    return ((ProjectEx) baseProject).getBooleanInternalParameterOrTrue(USE_PREFERRED_PROJECT);
  }

  /**
   * Retrieves the preferred project external ID from the base project.
   *
   * @param baseProject the base project to check
   * @return the preferred project external ID or an empty string if not found
   */
  private String getPreferredProjectExtId(@NotNull SProject baseProject) {
    return ((ProjectEx) baseProject).getInternalParameterValue(PREFERRED_INVESTIGATION_PROJECT, "");
  }

  /**
   * Finds the preferred project by external ID and checks the user's permissions.
   *
   * @param preferredProjectExtId the external ID of the preferred project
   * @param currentUser the current user requesting the preferred project
   * @return the preferred project if found and the user has permission, otherwise null
   */
  @Nullable
  private SProject findPreferredProject(@NotNull String preferredProjectExtId, @Nullable SUser currentUser) {
    SProject preferredProject = myProjectManager.findProjectByExternalId(preferredProjectExtId);
    if (preferredProject != null && !preferredProject.isRootProject() && userHasPermission(currentUser, preferredProject)) {
      return preferredProject;
    }
    return null;
  }

  /**
   * Checks whether the user has modified permissions for the specified project.
   *
   * @param user the user to check permissions for
   * @param project the project to check permissions for
   * @return true if the user has modified permissions, false otherwise
   */
  private static boolean userHasPermission(@Nullable SUser user, @NotNull SProject project) {
    return user != null && (
      user.isPermissionGrantedForProject(project.getProjectId(), Permission.ASSIGN_INVESTIGATION) ||
      user.isPermissionGrantedForProject(project.getProjectId(), Permission.MANAGE_BUILD_PROBLEMS)
    );
  }
}
