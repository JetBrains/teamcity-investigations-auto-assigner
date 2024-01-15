

package jetbrains.buildServer.investigationsAutoAssigner;

import java.util.Arrays;
import java.util.Map;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.representation.AutoAssignerBuildFeatureController;
import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AutoAssignerBuildFeature extends BuildFeature {
  private final String myEditUrl;

  public AutoAssignerBuildFeature(@NotNull final PluginDescriptor descriptor) {
    myEditUrl = descriptor.getPluginResourcesPath(AutoAssignerBuildFeatureController.CONTROLLER_URL);
  }

  @NotNull
  @Override
  public String getType() {
    return Constants.BUILD_FEATURE_TYPE;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return Constants.BUILD_FEATURE_DISPLAY_NAME;
  }

  @Nullable
  @Override
  public String getEditParametersUrl() {
    return myEditUrl;
  }

  @NotNull
  @Override
  public String describeParameters(@NotNull Map<String, String> params) {
    final String userName = params.get(Constants.DEFAULT_RESPONSIBLE);
    final String usersToIgnore = params.get(Constants.USERS_TO_IGNORE);
    final String shouldDelayAssignments = params.get(Constants.ASSIGN_ON_SECOND_FAILURE);

    StringBuilder sb = new StringBuilder();
    if (StringUtil.isTrue(shouldDelayAssignments)) {
      sb.append("On second failure strategy").append("\n");
    }
    if (StringUtil.isNotEmpty(userName)) {
      sb.append("Default assignee: ").append(userName).append("\n");
    }
    if (StringUtil.isNotEmpty(usersToIgnore)) {
      String usersToIgnoreOneLine = StringUtil.join(", ", Arrays.asList(usersToIgnore.split("\n")));
      sb.append("Users to ignore: ").append(usersToIgnoreOneLine);
    }
    return sb.toString().trim();
  }

  @Override
  public boolean isMultipleFeaturesPerBuildTypeAllowed() {
    return false;
  }

  @Override
  public boolean isRequiresAgent() { return false; }
}