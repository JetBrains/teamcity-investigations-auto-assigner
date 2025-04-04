package jetbrains.buildServer.investigationsAutoAssigner.representation;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.web.openapi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller responsible for handling test details extension for Sakura.
 * This controller renders the details page for the test in the Sakura UI.
 */
public class SakuraTestDetailsController extends BaseController {
  private final SimplePageExtension extension;
  private final PluginDescriptor pluginDescriptor;

  /**
   * Constructs the controller and registers the URL endpoint for the test details' extension.
   *
   * @param pagePlaces        the page places context for the page extensions
   * @param descriptor        the plugin descriptor to access plugin resources
   * @param controllerManager the WebControllerManager used to register this controller
   */
  public SakuraTestDetailsController(@NotNull final PagePlaces pagePlaces,
                                     @NotNull final PluginDescriptor descriptor,
                                     @NotNull final WebControllerManager controllerManager) {
    String url = "/sakuraTestDetailsExtension.html";
    this.extension =
      new SimplePageExtension(pagePlaces, new PlaceId("SAKURA_TEST_DETAILS_ACTIONS"), Constants.BUILD_FEATURE_TYPE,
                              url);
    this.pluginDescriptor = descriptor;
    controllerManager.registerController(url, this);
  }

  /**
   * Registers the test details extension to be used in the Sakura UI.
   */
  public void register() {
    this.extension.register();
  }

  /**
   * Unregisters the test details extension from the Sakura UI.
   */
  public void unregister() {
    this.extension.unregister();
  }

  /**
   * Handles the request for the test details' extension, rendering the details page for a specific test.
   *
   * @param request  the HTTP request containing parameters related to the test and build
   * @param response the HTTP response to be sent back to the client
   * @return a ModelAndView object containing the path to the JSP page for the test details extension
   * @throws Exception if there is an error processing the request
   */
  @Nullable
  @Override
  protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) {
    final ModelAndView mv = new ModelAndView(this.pluginDescriptor.getPluginResourcesPath("testDetailsExtension.jsp"));
    final Map<String, Object> model = mv.getModel();
    PluginUIContext pluginUIContext = PluginUIContext.getFromRequest(request);
    model.put("buildId", pluginUIContext.getBuildId());
    model.put("testId", pluginUIContext.getTestRunId());
    return mv;
  }
}
