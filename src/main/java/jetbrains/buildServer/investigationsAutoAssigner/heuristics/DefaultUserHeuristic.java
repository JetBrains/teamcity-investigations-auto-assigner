

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

  @NotNull private final UserModelEx myUserModel;

  public DefaultUserHeuristic(@NotNull final UserModelEx userModel) {
    myUserModel = userModel;
  }

  @Override
  @NotNull
  public String getId() {
    return "DefaultUser";
  }

  @NotNull
  @Override
  public HeuristicResult findResponsibleUser(@NotNull HeuristicContext heuristicContext) {
    HeuristicResult result = new HeuristicResult();

    SBuild build = heuristicContext.getBuild();
    String defaultResponsible = CustomParameters.getDefaultResponsible(build);
    if (StringUtil.isEmpty(defaultResponsible)) return result;

    UserEx responsibleUser = myUserModel.findUserAccount(null, defaultResponsible);
    if (responsibleUser == null) {
      LOGGER.warn("Ignoring heuristic \"DefaultUser\" as there is no TeamCity user with the username \"" +
                  defaultResponsible + "\" specified in the Investigations Auto-Assigner settings in the build: " +
                  LogUtil.describe(build) + "Affected build configuration: " +
                  LogUtil.describe(build.getBuildType()));
      return result;
    }

    boolean applyForSnapshotDependencyErrors = shouldApplyForSnapshotDependencyErrors(build);
    Responsibility responsibility = new DefaultUserResponsibility(responsibleUser);
    heuristicContext.getBuildProblems()
                    .stream()
                    .filter(buildProblem -> applyForSnapshotDependencyErrors || !snapshotDependencyErrorTypes.contains(buildProblem.getBuildProblemData().getType()))
                    .forEach(buildProblem -> result.addResponsibility(buildProblem, responsibility));
    heuristicContext.getTestRuns().forEach(testRun -> result.addResponsibility(testRun, responsibility));

    return result;
  }

  private boolean shouldApplyForSnapshotDependencyErrors(SBuild build) {
    if (build.isCompositeBuild()) {
      return true;
    }
    SBuildType buildType = build.getBuildType();
    boolean ignoreSnapshotDependencyErrors =
      buildType instanceof BuildTypeEx
      ? ((BuildTypeEx)buildType).getBooleanInternalParameterOrTrue(Constants.IGNORE_SNAPSHOT_DEPENDENCY_ERRORS_IN_DEFAULT_HEURISTIC)
      : TeamCityProperties.getBooleanOrTrue(Constants.IGNORE_SNAPSHOT_DEPENDENCY_ERRORS_IN_DEFAULT_HEURISTIC);
    return !ignoreSnapshotDependencyErrors;
  }
}