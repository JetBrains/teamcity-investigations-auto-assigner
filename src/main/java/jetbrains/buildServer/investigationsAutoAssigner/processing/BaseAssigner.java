

package jetbrains.buildServer.investigationsAutoAssigner.processing;

import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.serverSide.BuildTypeEx;
import jetbrains.buildServer.serverSide.SBuildType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

interface BaseAssigner {

  @NotNull
  default ResponsibilityEntry.RemoveMethod getRemoveMethod(@Nullable SBuildType buildType) {
    if (buildType == null) {
      return ResponsibilityEntry.RemoveMethod.WHEN_FIXED;
    }
    return ((BuildTypeEx)buildType).getBooleanInternalParameter(Constants.SHOULD_ASSIGN_RESOLVE_MANUALLY) ?
           ResponsibilityEntry.RemoveMethod.MANUALLY :
           ResponsibilityEntry.RemoveMethod.WHEN_FIXED;
  }
}