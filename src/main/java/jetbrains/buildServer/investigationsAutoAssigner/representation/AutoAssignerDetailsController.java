

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

public class AutoAssignerDetailsController extends BaseController {

  private final FirstFailedInFixedInCalculator myStatisticsProvider;
  private final AssignerArtifactDao myAssignerArtifactDao;
  private final String myDynamicTestDetailsExtensionPath;
  private final String myCssPath;
  @NotNull private final InvestigationsManager myInvestigationsManager;
  private final FlakyTestDetector myFlakyTestDetector;
  private final StatisticsReporter myStatisticsReporter;
  private final CustomParameters myCustomParameters;
  @NotNull private final SecurityContextEx mySecurityContext;

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
    myStatisticsProvider = statisticsProvider;
    myAssignerArtifactDao = assignerArtifactDao;
    myFlakyTestDetector = flakyTestDetector;
    myDynamicTestDetailsExtensionPath = descriptor.getPluginResourcesPath("dynamicTestDetailsExtension.jsp");
    myCssPath = descriptor.getPluginResourcesPath("testDetailsExtension.css");
    myInvestigationsManager = investigationsManager;
    myStatisticsReporter = statisticsReporter;
    myCustomParameters = customParameters;
    mySecurityContext = securityContext;
    controllerManager.registerController("/autoAssignerController.html", this);
  }

  @Nullable
  @Override
  protected ModelAndView doHandle(@NotNull final HttpServletRequest request,
                                  @NotNull final HttpServletResponse response) {
    final long buildId = Long.parseLong(request.getParameter("buildId"));
    final int testId = Integer.parseInt(request.getParameter("testId"));

    final SBuild build = myServer.findBuildInstanceById(buildId);
    if (build == null ||
        !userHasPermissions(build) ||
        !myCustomParameters.isDefaultSilentModeEnabled(build)) {
      return null;
    }

    @Nullable
    Branch branch = build.getBranch();
    boolean isDefaultBranch = branch == null || branch.isDefaultBranch();

    STestRun sTestRun = build.getBuildStatistics(ALL_TESTS_NO_DETAILS).findTestByTestRunId(testId);
    if (sTestRun == null) {
      return null;
    }

    boolean assignShouldNotBeShow = !isDefaultBranch ||
                                     myFlakyTestDetector.isFlaky(sTestRun.getTest().getTestNameId()) ||
                                     isUnderInvestigation(build, sTestRun.getTest());
    if (assignShouldNotBeShow &&
        !TeamCityProperties.getBoolean(SHOULD_PERSIST_FILTERED_TESTS_DESCRIPTION)) {
      return null;
    }

    final FirstFailedInFixedInCalculator.FFIData ffiData = myStatisticsProvider.calculateFFIData(sTestRun);

    @Nullable SBuild firstFailedBuild = myServer.findBuildInstanceById(ffiData.getFirstFailedInId());
    Responsibility responsibility = myAssignerArtifactDao.get(firstFailedBuild, sTestRun);

    if (responsibility != null) {
      return createModelAndView(request, responsibility, buildId, build, sTestRun, assignShouldNotBeShow, firstFailedBuild);
    }


    return null;
  }

  private boolean userHasPermissions(final SBuild build) {
    AuthorityHolder authorityHolder = mySecurityContext.getAuthorityHolder();
    @Nullable String projectId = build.getProjectId();

    return build.isFinished() ||
           (projectId != null &&
            authorityHolder.getPermissionsGrantedForProject(projectId).contains(Permission.VIEW_BUILD_RUNTIME_DATA));
  }

  private boolean isUnderInvestigation(SBuild sBuild, STest sTest) {
    SBuildType sBuildType = sBuild.getBuildType();
    if (sBuildType == null) return false;
    SProject sProject = sBuildType.getProject();

    @Nullable
    TestNameResponsibilityEntry investigationEntry = myInvestigationsManager.getInvestigation(sProject, sBuild, sTest);

    return investigationEntry != null;
  }

  @Nullable
  private ModelAndView createModelAndView(HttpServletRequest request,
                                          Responsibility responsibility,
                                          long buildId,
                                          SBuild build,
                                          STestRun sTestRun,
                                          boolean assignShouldNotBeShow,
                                          @Nullable SBuild firstFailedBuild) {
    final ModelAndView modelAndView = new ModelAndView(myDynamicTestDetailsExtensionPath);

    boolean isFilteredTestDescription = TeamCityProperties.getBoolean(SHOULD_PERSIST_FILTERED_TESTS_DESCRIPTION) &&
                                        responsibility.getDescription().startsWith(Constants.ASSIGNEE_FILTERED_DESCRIPTION_PREFIX);

    if (assignShouldNotBeShow && !isFilteredTestDescription) {
      return null;
    }

    modelAndView.getModel().put("isFilteredDescription", isFilteredTestDescription);
    modelAndView.getModel().put("userId", responsibility.getUser().getId());
    modelAndView.getModel().put("userName", responsibility.getUser().getDescriptiveName());

    String shownDescription = responsibility.getDescription();
    if (firstFailedBuild != null &&
        firstFailedBuild.getBuildId() != buildId &&
        shownDescription.endsWith("build")) {
      shownDescription = shownDescription + " with the first test failure";
    }

    modelAndView.getModel().put("shownDescription", shownDescription);
    modelAndView.getModel().put("investigationDescription", responsibility.getDescription());
    modelAndView.getModel().put("buildId", buildId);
    modelAndView.getModel().put("projectId", build.getProjectId());
    modelAndView.getModel().put("test", sTestRun.getTest());
    modelAndView.getModel().put("myCssPath", request.getContextPath() + myCssPath);

    myStatisticsReporter.reportShownButton();

    return modelAndView;
  }



}

