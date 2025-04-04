package jetbrains.buildServer.investigationsAutoAssigner.processing;

import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.serverSide.BuildTypeEx;
import jetbrains.buildServer.serverSide.SBuildType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for assigning responsibilities in TeamCity.
 * Provides a method to determine how a responsibility should be removed.
 */
interface BaseAssigner {

  /**
   * Determines the removal method for a responsibility entry based on the given build type.
   *
   * @param buildType The build type associated with the responsibility. Can be {@code null}.
   * @return The appropriate {@link ResponsibilityEntry.RemoveMethod} based on configuration.
   */
  @NotNull
  default ResponsibilityEntry.RemoveMethod getRemoveMethod(@Nullable SBuildType buildType) {
    if (buildType instanceof BuildTypeEx) {
      boolean resolveManually =
        ((BuildTypeEx)buildType).getBooleanInternalParameter(Constants.SHOULD_ASSIGN_RESOLVE_MANUALLY);
      return resolveManually ? ResponsibilityEntry.RemoveMethod.MANUALLY : ResponsibilityEntry.RemoveMethod.WHEN_FIXED;
    }
    return ResponsibilityEntry.RemoveMethod.WHEN_FIXED;
  }
}
