

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