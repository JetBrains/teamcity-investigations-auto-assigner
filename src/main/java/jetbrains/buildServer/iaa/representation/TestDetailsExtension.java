/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

package jetbrains.buildServer.iaa.representation;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import jetbrains.buildServer.iaa.common.Constants;
import jetbrains.buildServer.iaa.common.Responsibility;
import jetbrains.buildServer.iaa.utils.AssignerArtifactDao;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PlaceId;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.SimplePageExtension;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.iaa.common.Constants.TEST_RUN_IN_REQUEST;

public class TestDetailsExtension extends SimplePageExtension {
  private final AssignerArtifactDao myAssignerArtifactDao;

  public TestDetailsExtension(@NotNull final PagePlaces pagePlaces,
                              @NotNull final PluginDescriptor descriptor,
                              @NotNull final AssignerArtifactDao assignerArtifactDao) {
    super(pagePlaces,
          PlaceId.TEST_DETAILS_BLOCK,
          Constants.BUILD_FEATURE_TYPE,
          descriptor.getPluginResourcesPath("testDetailsExtension.jsp"));
    myAssignerArtifactDao = assignerArtifactDao;
  }

  @Override
  public void fillModel(@NotNull final Map<String, Object> model, @NotNull final HttpServletRequest request) {
    final Object testRunObject = request.getAttribute(TEST_RUN_IN_REQUEST);

    if (testRunObject instanceof STestRun) {
      STestRun sTestRun = (STestRun)testRunObject;
      Responsibility responsibility = myAssignerArtifactDao.get(sTestRun);
      if (responsibility != null) {
        model.put("responsibility", responsibility);
      }
    }
  }
}
