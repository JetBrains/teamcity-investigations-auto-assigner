package jetbrains.buildServer.investigationsAutoAssigner.representation;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.web.openapi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the test details extension displayed in the test details block.
 * This class is responsible for populating the model with the necessary test-related information.
 */
public class TestDetailsExtension extends SimplePageExtension {

  /**
   * Constructs the TestDetailsExtension with the specified page places and plugin descriptor.
   *
   * @param pagePlaces the page places to associate with this extension
   * @param descriptor the plugin descriptor containing resource paths for the extension
   */
  public TestDetailsExtension(@NotNull final PagePlaces pagePlaces,
                              @NotNull final PluginDescriptor descriptor) {
    super(pagePlaces,
          PlaceId.TEST_DETAILS_BLOCK,
          Constants.BUILD_FEATURE_TYPE,
          descriptor.getPluginResourcesPath("testDetailsExtension.jsp"));
  }

  /**
   * Fills the model with test-related information.
   *
   * @param model the model to populate
   * @param request the HTTP request from which the test details are extracted
   */
  @Override
  public void fillModel(@NotNull Map<String, Object> model, @NotNull HttpServletRequest request) {
    STestRun testRun = getLoadedTestRun(model);
    if (testRun != null) {
      populateModelWithTestDetails(model, testRun);
    }
  }

  /**
   * Retrieves the loaded test run from the model.
   *
   * @param model the model containing the test details
   * @return the loaded test run, or null if not present in the model
   */
  @Nullable
  private STestRun getLoadedTestRun(@NotNull Map<String, Object> model) {
    return (STestRun) model.get("loadedTestRun");
  }

  /**
   * Populates the model with the test's build ID and test run ID.
   *
   * @param model the model to populate
   * @param testRun the test run containing the necessary details
   */
  private void populateModelWithTestDetails(@NotNull Map<String, Object> model, @NotNull STestRun testRun) {
    model.put("buildId", testRun.getBuildId());
    model.put("testId", testRun.getTestRunId());
  }
}
