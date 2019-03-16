/*
 * Copyright 2000-2019 JetBrains s.r.o.
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import static jetbrains.buildServer.investigationsAutoAssigner.common.Constants.MY_AUTO_ASSIGNER_CONTROLLER_URL;

public class AutoAssignerBuildFeatureController extends BaseController {
  @NotNull private final ProjectManager myProjectManager;
  @NotNull private static final String BT_PREFIX = "buildType:";
  @NotNull private static final String TEMPLATE_PREFIX = "template:";

  public AutoAssignerBuildFeatureController(@NotNull final SBuildServer server,
                                            @NotNull final WebControllerManager controllerManager,
                                            @NotNull final PluginDescriptor descriptor,
                                            @NotNull final ProjectManager projectManager) {
    super(server);
    myProjectManager = projectManager;
    controllerManager.registerController(descriptor.getPluginResourcesPath(MY_AUTO_ASSIGNER_CONTROLLER_URL), this);
  }

  @Nullable
  @Override
  protected ModelAndView doHandle(@NotNull final HttpServletRequest request,
                                  @NotNull final HttpServletResponse response) {
    ModelAndView mv = new ModelAndView("autoAssignerBuildFeature.jsp");
    BuildTypeSettings buildTypeOrTemplate = getBuildTypeOrTemplateId(request);
    List<SProject> projectAndParents = new ArrayList<>();

    if (buildTypeOrTemplate != null) {
      String rootProjectId = myProjectManager.getRootProject().getProjectId();

      SPersistentEntity project = buildTypeOrTemplate.getProject();
      while (project != null && !((SProject)project).getProjectId().equals(rootProjectId)) {
        projectAndParents.add((SProject)project);
        project = project.getParent();
      }

      projectAndParents.sort(Comparator.comparingInt(o -> o.getFullName().length()));
    }

    mv.addObject("projectAndParents", projectAndParents);
    return mv;
  }

  private BuildTypeSettings getBuildTypeOrTemplateId(@NotNull final HttpServletRequest request) {
    String buildTypeOrTemplateId = request.getParameter("id");
    if (buildTypeOrTemplateId == null) {
      return null;
    }

    if (isBuildTypeId(buildTypeOrTemplateId)) {
     return myProjectManager.findBuildTypeByExternalId(extractBuildTypeId(buildTypeOrTemplateId));
    } else if (isTemplateId(buildTypeOrTemplateId)) {
      return myProjectManager.findBuildTypeTemplateByExternalId(extractTemplateId(buildTypeOrTemplateId));
    }

    return null;
  }

  private static String extractBuildTypeId(final String id) {
    return id.substring(BT_PREFIX.length());
  }

  private static String extractTemplateId(final String id) {
    return id.substring(TEMPLATE_PREFIX.length());
  }

  private static boolean isTemplateId(@NotNull final String id) {
    return id.startsWith(TEMPLATE_PREFIX);
  }

  private static boolean isBuildTypeId(@NotNull final String id) {
    return id.startsWith(BT_PREFIX);
  }
}
