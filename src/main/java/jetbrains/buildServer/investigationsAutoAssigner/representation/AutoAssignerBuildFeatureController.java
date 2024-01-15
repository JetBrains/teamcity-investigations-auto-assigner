

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

public class AutoAssignerBuildFeatureController extends BaseController {
  public static final String CONTROLLER_URL = "editAutoAssigner.html";
  public AutoAssignerBuildFeatureController(@NotNull final SBuildServer server,
                                            @NotNull final WebControllerManager controllerManager,
                                            @NotNull final PluginDescriptor descriptor) {
    super(server);
    controllerManager.registerController(descriptor.getPluginResourcesPath(CONTROLLER_URL), this);
  }

  @Nullable
  @Override
  protected ModelAndView doHandle(@NotNull final HttpServletRequest request,
                                  @NotNull final HttpServletResponse response) {
    return new ModelAndView("autoAssignerBuildFeature.jsp");
  }
}