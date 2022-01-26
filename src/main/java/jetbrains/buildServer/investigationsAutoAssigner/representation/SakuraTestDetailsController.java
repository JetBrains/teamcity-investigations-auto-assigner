/*
 * Copyright 2000-2022 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

public class SakuraTestDetailsController extends BaseController {
  private final SimplePageExtension myExtension;
  private final PluginDescriptor myPluginDescriptor;

  public SakuraTestDetailsController(@NotNull final PagePlaces pagePlaces,
                                     @NotNull final PluginDescriptor descriptor,
                                     @NotNull final WebControllerManager controllerManager) {
    String url = "/sakuraTestDetailsExtension.html";
    myExtension = new SimplePageExtension(pagePlaces,
          new PlaceId("SAKURA_TEST_DETAILS_ACTIONS"),
          Constants.BUILD_FEATURE_TYPE,
          url);
    myPluginDescriptor = descriptor;
    controllerManager.registerController(url, this);
  }

  public void register() {
    myExtension.register();
  }

  public void unregister() {
    myExtension.unregister();
  }

  @Nullable
  @Override
  protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws Exception {
    final ModelAndView mv = new ModelAndView(myPluginDescriptor.getPluginResourcesPath("testDetailsExtension.jsp"));
    final Map<String, Object> model = mv.getModel();
    PluginUIContext pluginUIContext = PluginUIContext.getFromRequest(request);
    model.put("buildId", pluginUIContext.getBuildId());
    model.put("testId", pluginUIContext.getTestRunId());
    return mv;
  }
}
