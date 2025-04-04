package jetbrains.buildServer.investigationsAutoAssigner.representation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.investigationsAutoAssigner.utils.TargetProjectFinder;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.responsibility.ResponsibilityEntryEx;
import jetbrains.buildServer.responsibility.TestNameResponsibilityFacade;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.auth.AuthorityHolder;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.auth.SecurityContext;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.users.UserModelEx;
import jetbrains.buildServer.util.Dates;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.SessionUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller for assigning investigations to users based on specific test failures or issues.
 * Handles user authentication, validation, and the assignment process.
 */
public class AssignInvestigationController extends BaseController {

  private final SecurityContext securityContext;
  private final TestNameResponsibilityFacade testNameResponsibilityFacade;
  private final ProjectManager projectManager;
  private final STestManager testManager;
  private final UserModelEx userModel;
  private final TargetProjectFinder targetProjectFinder;

  /**
   * Constructor for initializing the AssignInvestigationController.
   *
   * @param server                       the build server instance.
   * @param controllerManager            the WebControllerManager for registering the controller.
   * @param testNameResponsibilityFacade the facade for handling test name responsibility.
   * @param userModelEx                  the user model used to find users.
   * @param sTestManager                 the test manager for handling test-related operations.
   * @param securityContext              the security context for handling user permissions.
   * @param projectManager               the project manager for handling project-related operations.
   * @param targetProjectFinder          the target project finder for locating preferred projects for users.
   */
  public AssignInvestigationController(@NotNull final SBuildServer server,
                                       @NotNull final WebControllerManager controllerManager,
                                       @NotNull final TestNameResponsibilityFacade testNameResponsibilityFacade,
                                       @NotNull final UserModelEx userModelEx,
                                       @NotNull final STestManager sTestManager,
                                       @NotNull final SecurityContext securityContext,
                                       @NotNull final ProjectManager projectManager,
                                       @NotNull final TargetProjectFinder targetProjectFinder) {
    super(server);
    this.securityContext = securityContext;
    this.projectManager = projectManager;
    this.targetProjectFinder = targetProjectFinder;
    controllerManager.registerController("/assignInvestigation.html", this);
    this.testNameResponsibilityFacade = testNameResponsibilityFacade;
    this.userModel = userModelEx;
    this.testManager = sTestManager;
  }

  /**
   * Handles the HTTP request for assigning an investigation to a user.
   * Validates the request parameters, checks user permissions, and assigns the investigation.
   *
   * @param request  the HTTP request containing parameters.
   * @param response the HTTP response.
   * @return a ModelAndView representing the response to the client.
   * @throws IllegalAccessException if the current user doesn't have permission to assign investigations.
   */
  @Nullable
  @Override
  protected ModelAndView doHandle(@NotNull final HttpServletRequest request,
                                  @NotNull final HttpServletResponse response) throws IllegalAccessException {

    // Parse and validate request parameters
    final long userId = getRequestParameterAsLong(request, "userId");
    final long testNameId = getRequestParameterAsLong(request, "testNameId");
    final int buildId = getRequestParameterAsInt(request, "buildId");
    final String description = request.getParameter("description");
    validateDescription(description);

    AuthorityHolder authorityHolder = this.securityContext.getAuthorityHolder();
    User reporterUser = getReporterUser(authorityHolder, request);

    SBuild build = getBuildById(buildId);
    SProject project = getProjectById(build);
    User responsibleUser = getResponsibleUserById(userId);
    STest sTest = getTestById(testNameId, build.getProjectId());

    checkPermissions(project.getProjectId());

    if (reporterUser instanceof SUser) {
      SProject preferredProject =
        this.targetProjectFinder.getPreferredInvestigationProject(project, (SUser)reporterUser);
      if (preferredProject != null) {
        project = preferredProject;
      }
    }

    assignInvestigation(sTest, project, responsibleUser, reporterUser, description);

    return null;
  }

  /**
   * Retrieves a long request parameter value and parses it. Throws an IllegalArgumentException if invalid.
   *
   * @param request   the HTTP request.
   * @param parameter the parameter name.
   * @return the parsed long value.
   * @throws IllegalArgumentException if the parameter is not a valid long.
   */
  private long getRequestParameterAsLong(HttpServletRequest request, String parameter) throws IllegalArgumentException {
    try {
      return Long.parseLong(request.getParameter(parameter));
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Provided parameter " + parameter + " is not valid", ex);
    }
  }

  /**
   * Retrieves an integer request parameter value and parses it. Throws an IllegalArgumentException if invalid.
   *
   * @param request   the HTTP request.
   * @param parameter the parameter name.
   * @return the parsed integer value.
   * @throws IllegalArgumentException if the parameter is not a valid integer.
   */
  private int getRequestParameterAsInt(HttpServletRequest request, String parameter) throws IllegalArgumentException {
    try {
      return Integer.parseInt(request.getParameter(parameter));
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Provided parameter " + parameter + " is not valid", ex);
    }
  }

  /**
   * Validates the description parameter.
   *
   * @param description the description string.
   * @throws IllegalArgumentException if description is null or empty.
   */
  private void validateDescription(String description) throws IllegalArgumentException {
    if (description == null || description.isEmpty()) {
      throw new IllegalArgumentException("Description is not specified");
    }
  }

  /**
   * Retrieves the user associated with the request, either from the security context or session.
   *
   * @param authorityHolder the security context.
   * @param request         the HTTP request.
   * @return the reporter user.
   */
  private User getReporterUser(AuthorityHolder authorityHolder, HttpServletRequest request) {
    User reporterUser = authorityHolder.getAssociatedUser();
    if (reporterUser == null) {
      reporterUser = SessionUser.getUser(request);
    }
    return reporterUser;
  }

  /**
   * Retrieves the build by its ID.
   *
   * @param buildId the ID of the build.
   * @return the build.
   * @throws IllegalStateException if the build is not found.
   */
  private SBuild getBuildById(int buildId) throws IllegalStateException {
    SBuild build = myServer.findBuildInstanceById(buildId);
    if (build == null) {
      throw new IllegalStateException("Build was not found by provided buildId");
    }
    return build;
  }

  /**
   * Retrieves the project by the build's project ID.
   *
   * @param build the build object.
   * @return the project.
   * @throws IllegalStateException if the project is not found.
   */
  private SProject getProjectById(SBuild build) throws IllegalStateException {
    String projectId = build.getProjectId();
    if (projectId == null) {
      throw new IllegalStateException("ProjectId is not specified on the build");
    }
    SProject project = this.projectManager.findProjectById(projectId);
    if (project == null) {
      throw new IllegalStateException("Cannot find project by ID " + projectId);
    }
    return project;
  }

  /**
   * Retrieves the responsible user by ID.
   *
   * @param userId the ID of the user.
   * @return the responsible user.
   * @throws IllegalStateException if the user is not found.
   */
  private User getResponsibleUserById(long userId) throws IllegalStateException {
    User responsibleUser = this.userModel.findUserById(userId);
    if (responsibleUser == null) {
      throw new IllegalStateException("Investigator was not found in the model by his id");
    }
    return responsibleUser;
  }

  /**
   * Retrieves the test by its ID.
   *
   * @param testNameId the ID of the test.
   * @param projectId  the project ID associated with the test.
   * @return the test.
   * @throws IllegalStateException if the test is not found.
   */
  private STest getTestById(long testNameId, String projectId) throws IllegalStateException {
    STest sTest = this.testManager.findTest(testNameId, projectId);
    if (sTest == null) {
      throw new IllegalStateException("Test was not found by provided testNameId");
    }
    return sTest;
  }

  /**
   * Checks if the current user has the necessary permissions to assign investigations.
   *
   * @param projectId the ID of the project.
   * @throws IllegalAccessException if the user does not have permission.
   */
  private void checkPermissions(String projectId) throws IllegalAccessException {
    if (!this.securityContext.getAuthorityHolder().getPermissionsGrantedForProject(projectId)
                             .contains(Permission.ASSIGN_INVESTIGATION)) {
      throw new IllegalAccessException("Current user doesn't have permissions to assign investigations");
    }
  }

  /**
   * Assigns the investigation responsibility to the responsible user.
   *
   * @param sTest           the test for which the responsibility is being assigned.
   * @param project         the project associated with the test.
   * @param responsibleUser the user being assigned the responsibility.
   * @param reporterUser    the user who is assigning the responsibility.
   * @param description     a description of the investigation.
   */
  private void assignInvestigation(STest sTest,
                                   SProject project,
                                   User responsibleUser,
                                   User reporterUser,
                                   String description) {
    this.testNameResponsibilityFacade.setTestNameResponsibility(sTest.getName(), project.getProjectId(),
                                                                new ResponsibilityEntryEx(
                                                                  ResponsibilityEntry.State.TAKEN,
                                                                  responsibleUser, reporterUser,
                                                                  Dates.now(), description,
                                                                  ResponsibilityEntry.RemoveMethod.WHEN_FIXED));
  }
}
