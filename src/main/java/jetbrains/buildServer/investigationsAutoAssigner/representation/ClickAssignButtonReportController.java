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

/**
 * Controller responsible for handling the click of the "Assign" button
 * and reporting statistics about the button click event.
 */
public class ClickAssignButtonReportController extends BaseController {

  private final StatisticsReporter statisticsReporter;

  /**
   * Constructs the controller and registers the URL endpoint for the report.
   *
   * @param server             the build server instance
   * @param controllerManager  the WebControllerManager used to register this controller
   * @param statisticsReporter the StatisticsReporter used to report the button click event
   */
  public ClickAssignButtonReportController(@NotNull final SBuildServer server,
                                           @NotNull final WebControllerManager controllerManager,
                                           @NotNull final StatisticsReporter statisticsReporter) {
    super(server);
    this.statisticsReporter = statisticsReporter;
    controllerManager.registerController("/autoAssignerStatisticsReporter.html", this);
  }

  /**
   * Handles the request to report the "Assign" button click.
   * This method records the click event and returns no view (null).
   *
   * @param request  the HTTP request containing the details of the button click
   * @param response the HTTP response to be sent back to the client
   * @return null, since this controller does not need to render any view
   */
  @Nullable
  @Override
  protected ModelAndView doHandle(@NotNull final HttpServletRequest request,
                                  @NotNull final HttpServletResponse response) {
    this.statisticsReporter.reportClickedButton();
    return null;
  }
}
