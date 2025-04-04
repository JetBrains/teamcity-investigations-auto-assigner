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

/**
 * This class represents the build feature for the Auto Assigner.
 * It provides configuration and management of the Auto Assigner build feature within TeamCity.
 */
public class AutoAssignerBuildFeature extends BuildFeature {

  private final String myEditUrl;

  /**
   * Constructs an AutoAssignerBuildFeature with the specified plugin descriptor.
   * The URL for editing parameters is obtained from the plugin's resources.
   *
   * @param descriptor the plugin descriptor used to get the edit URL
   */
  public AutoAssignerBuildFeature(@NotNull final PluginDescriptor descriptor) {
    myEditUrl = descriptor.getPluginResourcesPath(AutoAssignerBuildFeatureController.CONTROLLER_URL);
  }

  /**
   * Returns the type of the build feature.
   *
   * @return the feature type string
   */
  @NotNull
  @Override
  public String getType() {
    return Constants.BUILD_FEATURE_TYPE;
  }

  /**
   * Returns the display name for the build feature.
   *
   * @return the display name of the build feature
   */
  @NotNull
  @Override
  public String getDisplayName() {
    return Constants.BUILD_FEATURE_DISPLAY_NAME;
  }

  /**
   * Returns the URL for editing the parameters of the build feature.
   *
   * @return the URL for editing parameters, or null if not available
   */
  @Nullable
  @Override
  public String getEditParametersUrl() {
    return myEditUrl;
  }

  /**
   * Describes the parameters for the build feature based on the given map of parameters.
   * This includes details such as the default assignee, users to ignore, and assignment delay strategy.
   *
   * @param params a map of parameters for the build feature
   * @return a description of the parameters as a formatted string
   */
  @NotNull
  @Override
  public String describeParameters(@NotNull Map<String, String> params) {
    final String userName = params.get(Constants.DEFAULT_RESPONSIBLE);
    final String usersToIgnore = params.get(Constants.USERS_TO_IGNORE);
    final String shouldDelayAssignments = params.get(Constants.ASSIGN_ON_SECOND_FAILURE);

    StringBuilder sb = new StringBuilder();

    // Append strategy if assignment delay is enabled
    if (StringUtil.isTrue(shouldDelayAssignments)) {
      sb.append("On second failure strategy").append("\n");
    }

    // Append default assignee if specified
    if (StringUtil.isNotEmpty(userName)) {
      sb.append("Default assignee: ").append(userName).append("\n");
    }

    // Append ignored users if specified
    if (StringUtil.isNotEmpty(usersToIgnore)) {
      String usersToIgnoreOneLine = StringUtil.join(", ", Arrays.asList(usersToIgnore.split("\n")));
      sb.append("Users to ignore: ").append(usersToIgnoreOneLine);
    }

    return sb.toString().trim();
  }

  /**
   * Specifies whether multiple features of this type are allowed per build configuration.
   *
   * @return false, as multiple features of this type are not allowed
   */
  @Override
  public boolean isMultipleFeaturesPerBuildTypeAllowed() {
    return false;
  }

  /**
   * Specifies whether this build feature requires an agent to function.
   *
   * @return false, as the feature does not require an agent
   */
  @Override
  public boolean isRequiresAgent() {
    return false;
  }
}
