

// DefaultUserHeuristic.java
package jetbrains.buildServer.investigationsAutoAssigner.heuristics;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.common.DefaultUserResponsibility;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.processing.HeuristicContext;
import jetbrains.buildServer.investigationsAutoAssigner.utils.CustomParameters;
import jetbrains.buildServer.log.LogUtil;
import jetbrains.buildServer.serverSide.BuildTypeEx;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.users.UserModelEx;
import jetbrains.buildServer.users.impl.UserEx;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.investigationsAutoAssigner.processing.BuildProblemsFilter.snapshotDependencyErrorTypes;

public class DefaultUserHeuristic implements Heuristic {

  private static final Logger LOGGER = Constants.LOGGER;
  @NotNull private final UserModelEx userModel;

  public DefaultUserHeuristic(@NotNull final UserModelEx userModel) {
    this.userModel = userModel;
  }

  @Override
  @NotNull
  public String getId() {
    return "DefaultUser";
  }

  @NotNull
  @Override
  public HeuristicResult findResponsibleUser(@NotNull HeuristicContext context) {
    HeuristicResult result = new HeuristicResult();
    SBuild build = context.getBuild();
    String username = CustomParameters.getDefaultResponsible(build);

    if (StringUtil.isEmpty(username)) return result;

    UserEx user = userModel.findUserAccount(null, username);
    if (user == null) {
      logUserNotFound(build, username);
      return result;
    }

    Responsibility responsibility = new DefaultUserResponsibility(user);
    boolean includeSnapshotErrors = shouldIncludeSnapshotErrors(build);

    context.getBuildProblems().stream()
           .filter(problem -> includeSnapshotErrors ||
                              !snapshotDependencyErrorTypes.contains(problem.getBuildProblemData().getType()))
           .forEach(problem -> result.addResponsibility(problem, responsibility));

    context.getTestRuns().forEach(test -> result.addResponsibility(test, responsibility));
    return result;
  }

  private boolean shouldIncludeSnapshotErrors(SBuild build) {
    if (build.isCompositeBuild()) return true;

    SBuildType type = build.getBuildType();
    return !(type instanceof BuildTypeEx
             ? ((BuildTypeEx)type).getBooleanInternalParameterOrTrue(
      Constants.IGNORE_SNAPSHOT_DEPENDENCY_ERRORS_IN_DEFAULT_HEURISTIC)
             : TeamCityProperties.getBooleanOrTrue(Constants.IGNORE_SNAPSHOT_DEPENDENCY_ERRORS_IN_DEFAULT_HEURISTIC));
  }

  private void logUserNotFound(SBuild build, String username) {
    LOGGER.warn(String.format(
      "Ignoring heuristic \"DefaultUser\": no user \"%s\" in build settings. Build: %s, Configuration: %s",
      username,
      LogUtil.describe(build),
      LogUtil.describe(build.getBuildType())));
  }
}
