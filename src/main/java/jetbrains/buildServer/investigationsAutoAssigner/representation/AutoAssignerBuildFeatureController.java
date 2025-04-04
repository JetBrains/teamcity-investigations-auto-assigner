package jetbrains.buildServer.investigationsAutoAssigner.representation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller for handling AutoAssigner build feature configurations.
 * Registers the controller with the appropriate path and responds with the configuration JSP.
 */
public class AutoAssignerBuildFeatureController extends BaseController {

  /**
   * The URL path for accessing the AutoAssigner configuration page.
   */
  public static final String CONTROLLER_URL = "editAutoAssigner.html";

  /**
   * Constructor that initializes the controller and registers it with the controller manager.
   *
   * @param server            the build server instance.
   * @param controllerManager the WebControllerManager for registering the controller.
   * @param descriptor        the plugin descriptor containing resource paths.
   */
  public AutoAssignerBuildFeatureController(@NotNull final SBuildServer server,
                                            @NotNull final WebControllerManager controllerManager,
                                            @NotNull final PluginDescriptor descriptor) {
    super(server);
    registerController(controllerManager, descriptor);
  }

  /**
   * Registers the controller with the given WebControllerManager and plugin descriptor.
   *
   * @param controllerManager the WebControllerManager for registering the controller.
   * @param descriptor        the plugin descriptor containing the resource path for the controller.
   */
  private void registerController(@NotNull final WebControllerManager controllerManager,
                                  @NotNull final PluginDescriptor descriptor) {
    controllerManager.registerController(descriptor.getPluginResourcesPath(CONTROLLER_URL), this);
  }

  /**
   * Handles the HTTP request and responds with the AutoAssigner build feature configuration JSP.
   *
   * @param request  the HTTP request.
   * @param response the HTTP response.
   * @return a ModelAndView representing the AutoAssigner build feature configuration page.
   */
  @Nullable
  @Override
  protected ModelAndView doHandle(@NotNull final HttpServletRequest request,
                                  @NotNull final HttpServletResponse response) {
    return new ModelAndView("autoAssignerBuildFeature.jsp");
  }
}
