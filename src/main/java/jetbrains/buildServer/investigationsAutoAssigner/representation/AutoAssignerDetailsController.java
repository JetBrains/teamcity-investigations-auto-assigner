package jetbrains.buildServer.investigationsAutoAssigner.representation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.persistent.AssignerArtifactDao;
import jetbrains.buildServer.investigationsAutoAssigner.persistent.StatisticsReporter;
import jetbrains.buildServer.investigationsAutoAssigner.utils.CustomParameters;
import jetbrains.buildServer.investigationsAutoAssigner.utils.FlakyTestDetector;
import jetbrains.buildServer.investigationsAutoAssigner.utils.InvestigationsManager;
import jetbrains.buildServer.responsibility.TestNameResponsibilityEntry;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.auth.AuthorityHolder;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.stat.FirstFailedInFixedInCalculator;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import static jetbrains.buildServer.investigationsAutoAssigner.common.Constants.SHOULD_PERSIST_FILTERED_TESTS_DESCRIPTION;
import static jetbrains.buildServer.serverSide.BuildStatisticsOptions.ALL_TESTS_NO_DETAILS;

/**
 * Controller that handles the details of test assignments and investigations for auto-assigning responsibilities.
 * <p>
 * This class is responsible for handling requests to the '/autoAssignerController.html' URL, where it determines
 * whether a specific test's responsibility should be assigned and provides the details of the assignment.
 * The controller interacts with various services such as the statistics provider, artifact DAO, and test
 * investigation manager to retrieve and display relevant information to the user.
 * </p>
 */
public class AutoAssignerDetailsController extends BaseController {

  private final FirstFailedInFixedInCalculator statisticsProvider;
  private final AssignerArtifactDao assignerArtifactDao;
  private final String dynamicTestDetailsExtensionPath;
  private final String cssPath;
  @NotNull private final InvestigationsManager investigationsManager;
  private final FlakyTestDetector flakyTestDetector;
  private final StatisticsReporter statisticsReporter;
  private final CustomParameters customParameters;
  @NotNull private final SecurityContextEx securityContext;

  /**
   * Constructs an instance of the AutoAssignerDetailsController.
   *
   * @param server                The TeamCity server instance.
   * @param statisticsProvider    The provider for calculating first failed and fixed builds.
   * @param assignerArtifactDao   The DAO responsible for retrieving and storing assignment data.
   * @param controllerManager     The manager for registering controllers.
   * @param descriptor            The plugin descriptor for retrieving resource paths.
   * @param flakyTestDetector     The service for detecting flaky tests.
   * @param investigationsManager The service for managing test investigations.
   * @param statisticsReporter    The service for reporting statistics.
   * @param customParameters      The custom parameters' configuration.
   * @param securityContext       The security context for checking user permissions.
   */
  public AutoAssignerDetailsController(final SBuildServer server,
                                       @NotNull final FirstFailedInFixedInCalculator statisticsProvider,
                                       @NotNull final AssignerArtifactDao assignerArtifactDao,
                                       @NotNull final WebControllerManager controllerManager,
                                       @NotNull final PluginDescriptor descriptor,
                                       @NotNull final FlakyTestDetector flakyTestDetector,
                                       @NotNull final InvestigationsManager investigationsManager,
                                       @NotNull final StatisticsReporter statisticsReporter,
                                       @NotNull final CustomParameters customParameters,
                                       @NotNull final SecurityContextEx securityContext) {
    super(server);
    this.statisticsProvider = statisticsProvider;
    this.assignerArtifactDao = assignerArtifactDao;
    this.flakyTestDetector = flakyTestDetector;
    this.dynamicTestDetailsExtensionPath = descriptor.getPluginResourcesPath("dynamicTestDetailsExtension.jsp");
    this.cssPath = descriptor.getPluginResourcesPath("testDetailsExtension.css");
    this.investigationsManager = investigationsManager;
    this.statisticsReporter = statisticsReporter;
    this.customParameters = customParameters;
    this.securityContext = securityContext;
    controllerManager.registerController("/autoAssignerController.html", this);
  }

  /**
   * Handles the HTTP request for displaying the test assignment details.
   *
   * @param request  The HTTP request.
   * @param response The HTTP response.
   * @return A ModelAndView containing the view to render, or null if the request should be ignored.
   */
  @Nullable
  @Override
  protected ModelAndView doHandle(@NotNull final HttpServletRequest request,
                                  @NotNull final HttpServletResponse response) {
    final long buildId = Long.parseLong(request.getParameter("buildId"));
    final int testId = Integer.parseInt(request.getParameter("testId"));

    final SBuild build = myServer.findBuildInstanceById(buildId);
    if (build == null || !userHasPermissions(build) || !this.customParameters.isDefaultSilentModeEnabled(build)) {
      return null;
    }

    @Nullable Branch branch = build.getBranch();
    boolean isDefaultBranch = branch == null || branch.isDefaultBranch();

    STestRun sTestRun = build.getBuildStatistics(ALL_TESTS_NO_DETAILS).findTestByTestRunId(testId);
    if (sTestRun == null) {
      return null;
    }

    boolean assignShouldNotBeShow =
      !isDefaultBranch || this.flakyTestDetector.isFlaky(sTestRun.getTest().getTestNameId()) ||
      isUnderInvestigation(build, sTestRun.getTest());
    if (assignShouldNotBeShow && !TeamCityProperties.getBoolean(SHOULD_PERSIST_FILTERED_TESTS_DESCRIPTION)) {
      return null;
    }

    final FirstFailedInFixedInCalculator.FFIData ffiData = this.statisticsProvider.calculateFFIData(sTestRun);

    @Nullable SBuild firstFailedBuild = myServer.findBuildInstanceById(ffiData.getFirstFailedInId());
    Responsibility responsibility = this.assignerArtifactDao.get(firstFailedBuild, sTestRun);
    if (responsibility != null) {
      return createModelAndView(request, build, sTestRun, responsibility, firstFailedBuild);
    }

    return null;
  }

  /**
   * Creates a ModelAndView for displaying the test assignment details.
   *
   * @param request          The HTTP request.
   * @param build            The build for which the test assignment is being shown.
   * @param sTestRun         The test run for the specific test.
   * @param responsibility   The responsibility assigned to the test.
   * @param firstFailedBuild The build in which the test first failed.
   * @return The ModelAndView containing the view and relevant data for rendering.
   */
  private ModelAndView createModelAndView(HttpServletRequest request,
                                          SBuild build,
                                          STestRun sTestRun,
                                          Responsibility responsibility,
                                          SBuild firstFailedBuild) {
    final ModelAndView modelAndView = new ModelAndView(this.dynamicTestDetailsExtensionPath);
    boolean isFilteredTestDescription = shouldPersistFilteredDescription(responsibility);

    modelAndView.getModel().put("isFilteredDescription", isFilteredTestDescription);
    modelAndView.getModel().put("userId", responsibility.getUser().getId());
    modelAndView.getModel().put("userName", responsibility.getUser().getDescriptiveName());

    String shownDescription = getDescriptionForTest(firstFailedBuild, responsibility);
    modelAndView.getModel().put("shownDescription", shownDescription);
    modelAndView.getModel().put("investigationDescription", responsibility.getDescription());
    modelAndView.getModel().put("buildId", build.getBuildId());
    modelAndView.getModel().put("projectId", build.getProjectId());
    modelAndView.getModel().put("test", sTestRun.getTest());
    modelAndView.getModel().put("myCssPath", request.getContextPath() + this.cssPath);

    this.statisticsReporter.reportShownButton();
    return modelAndView;
  }

  /**
   * Checks if a responsibility's description should be persisted based on certain conditions.
   *
   * @param responsibility The responsibility whose description is to be checked.
   * @return true if the description should be persisted, false otherwise.
   */
  private boolean shouldPersistFilteredDescription(Responsibility responsibility) {
    return TeamCityProperties.getBoolean(SHOULD_PERSIST_FILTERED_TESTS_DESCRIPTION) &&
           responsibility.getDescription().startsWith(Constants.ASSIGNEE_FILTERED_DESCRIPTION_PREFIX);
  }

  /**
   * Gets the description for a test, adding specific details for the first failed build.
   *
   * @param firstFailedBuild The build in which the test first failed.
   * @param responsibility   The responsibility assigned to the test.
   * @return The description for the test.
   */
  private String getDescriptionForTest(SBuild firstFailedBuild, Responsibility responsibility) {
    String description = responsibility.getDescription();
    if (firstFailedBuild != null && firstFailedBuild.getBuildId() != responsibility.getUser().getId() &&
        description.endsWith("build")) {
      description = description + " with the first test failure";
    }
    return description;
  }

  /**
   * Checks if the current user has the necessary permissions to view the build's details.
   *
   * @param build The build for which the permissions are checked.
   * @return true if the user has the required permissions, false otherwise.
   */
  private boolean userHasPermissions(final SBuild build) {
    AuthorityHolder authorityHolder = this.securityContext.getAuthorityHolder();
    @Nullable String projectId = build.getProjectId();

    return build.isFinished() || (projectId != null && authorityHolder.getPermissionsGrantedForProject(projectId)
                                                                      .contains(Permission.VIEW_BUILD_RUNTIME_DATA));
  }

  /**
   * Checks if the given test is under investigation.
   *
   * @param sBuild The build instance for which the investigation is checked.
   * @param sTest  The test for which the investigation is checked.
   * @return true if the test is under investigation, false otherwise.
   */
  private boolean isUnderInvestigation(SBuild sBuild, STest sTest) {
    SBuildType sBuildType = sBuild.getBuildType();
    if (sBuildType == null) return false;
    SProject sProject = sBuildType.getProject();

    @Nullable TestNameResponsibilityEntry investigationEntry =
      this.investigationsManager.getInvestigation(sProject, sBuild, sTest);

    return investigationEntry != null;
  }
}
