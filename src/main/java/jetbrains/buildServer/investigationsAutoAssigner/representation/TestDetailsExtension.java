

package jetbrains.buildServer.investigationsAutoAssigner.representation;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.persistent.AssignerArtifactDao;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PlaceId;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.SimplePageExtension;
import org.jetbrains.annotations.NotNull;

public class TestDetailsExtension extends SimplePageExtension {

  private final AssignerArtifactDao myAssignerArtifactDao;

  public TestDetailsExtension(@NotNull final PagePlaces pagePlaces,
                              @NotNull final PluginDescriptor descriptor,
                              @NotNull final AssignerArtifactDao assignerArtifactDao) {
    super(pagePlaces,
          PlaceId.TEST_DETAILS_BLOCK,
          Constants.BUILD_FEATURE_TYPE,
          descriptor.getPluginResourcesPath("testDetailsExtension.jsp"));
    this.myAssignerArtifactDao = assignerArtifactDao;
  }

  @Override
  public void fillModel(@NotNull Map<String, Object> model, @NotNull HttpServletRequest request) {
    STestRun loadedTestRun = (STestRun) model.get("loadedTestRun");
    if(loadedTestRun == null) return;
    model.put("buildId", loadedTestRun.getBuildId());
    model.put("testId", loadedTestRun.getTestRunId());
    Responsibility responsibility = myAssignerArtifactDao.get(loadedTestRun.getFirstFailed(), loadedTestRun);
    if (responsibility != null) model.put("autoAssignedResponsibility", responsibility);
  }
}