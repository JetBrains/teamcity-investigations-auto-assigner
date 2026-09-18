

package jetbrains.buildServer.investigationsAutoAssigner.representation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.investigationsAutoAssigner.persistent.StatisticsReporter;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

public class ClickAssignButtonReportController extends BaseController {

  private final StatisticsReporter myStatisticsReporter;
  private final static String CONTROLLER_URL = "/autoAssignerStatisticsReporter.html";

  public ClickAssignButtonReportController(@NotNull final SBuildServer server,
                                           @NotNull final WebControllerManager controllerManager,
                                           @NotNull final StatisticsReporter statisticsReporter) {
    super(server);
    myStatisticsReporter = statisticsReporter;
    controllerManager.registerController(CONTROLLER_URL, this);
  }

  @Nullable
  @Override
  protected ModelAndView doHandle(@NotNull final HttpServletRequest request,
                                  @NotNull final HttpServletResponse response) {
    myStatisticsReporter.reportClickedButton();
    return null;
  }
}
