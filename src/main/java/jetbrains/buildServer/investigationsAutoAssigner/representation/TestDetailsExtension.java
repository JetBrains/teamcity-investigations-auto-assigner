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
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.web.openapi.*;
import org.jetbrains.annotations.NotNull;

public class TestDetailsExtension extends SimplePageExtension {

  public TestDetailsExtension(@NotNull final PagePlaces pagePlaces,
                              @NotNull final PluginDescriptor descriptor) {
    super(pagePlaces,
          PlaceId.TEST_DETAILS_BLOCK,
          Constants.BUILD_FEATURE_TYPE,
          descriptor.getPluginResourcesPath("testDetailsExtension.jsp"));
  }

  @Override
  public void fillModel(@NotNull Map<String, Object> model, @NotNull HttpServletRequest request) {
    STestRun loadedTestRun = (STestRun)model.get("loadedTestRun");
    model.put("buildId", loadedTestRun.getBuildId());
    model.put("testId", loadedTestRun.getTestRunId());
  }
}
