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

/**
 * Heuristic for assigning responsibility to a default user based on configuration.
 * <p>
 * The heuristic assigns responsibility for build problems and test runs to a user
 * specified in the Investigations Auto-Assigner settings. If the user is not found, a warning is logged.
 * It also allows for controlling the application of snapshot dependency errors.
 * </p>
 *
 * @see DefaultUserResponsibility
 */
public class DefaultUserHeuristic implements Heuristic {

  private static final Logger LOGGER = Constants.LOGGER;

  @NotNull private final UserModelEx userModel;

  /**
   * Constructor for the DefaultUserHeuristic.
   *
   * @param userModel the user model used to find the specified user
   */
  public DefaultUserHeuristic(@NotNull final UserModelEx userModel) {
    this.userModel = userModel;
  }

  /**
   * Gets the ID of the heuristic.
   *
   * @return the ID of the heuristic
   */
  @Override
  @NotNull
  public String getId() {
    return "DefaultUser";
  }

  /**
   * Finds the responsible user for the build based on the default user specified
   * in the Investigations Auto-Assigner settings.
   *
   * @param heuristicContext the context of the heuristic
   * @return the result containing the assigned responsibility
   */
  @NotNull
  @Override
  public HeuristicResult findResponsibleUser(@NotNull HeuristicContext heuristicContext) {
    HeuristicResult heuristicResult = new HeuristicResult();

    SBuild build = heuristicContext.getBuild();
    String defaultResponsibleUser = CustomParameters.getDefaultResponsible(build);
    if (StringUtil.isEmpty(defaultResponsibleUser)) return heuristicResult;

    // Find the user by username
    UserEx responsibleUser = userModel.findUserAccount(null, defaultResponsibleUser);
    if (responsibleUser == null) {
      LOGGER.warn("Ignoring heuristic \"DefaultUser\" as there is no TeamCity user with the username \"" +
                  defaultResponsibleUser + "\" specified in the Investigations Auto-Assigner settings in build " +
                  LogUtil.describe(build) + ". Affected build configuration: " +
                  LogUtil.describe(build.getBuildType()) + ". Please ensure the username is correct in the settings.");
      return heuristicResult;
    }

    boolean applyForSnapshotDependencyErrors = shouldApplyForSnapshotDependencyErrors(build);
    Responsibility responsibility = new DefaultUserResponsibility(responsibleUser);

    // Assign responsibilities to build problems and test runs
    addResponsibilitiesToBuildProblemsAndTests(heuristicContext, applyForSnapshotDependencyErrors, responsibility,
                                               heuristicResult);

    return heuristicResult;
  }

  /**
   * Adds the responsibility for build problems and test runs.
   *
   * @param heuristicContext                 the context of the heuristic
   * @param applyForSnapshotDependencyErrors whether to apply for snapshot dependency errors
   * @param responsibility                   the responsibility object for the user
   * @param heuristicResult                  the result to add responsibilities to
   */
  private void addResponsibilitiesToBuildProblemsAndTests(@NotNull HeuristicContext heuristicContext,
                                                          boolean applyForSnapshotDependencyErrors,
                                                          @NotNull Responsibility responsibility,
                                                          @NotNull HeuristicResult heuristicResult) {
    heuristicContext.getBuildProblems().stream().filter(buildProblem -> applyForSnapshotDependencyErrors ||
                                                                        !snapshotDependencyErrorTypes.contains(
                                                                          buildProblem.getBuildProblemData().getType()))
                    .forEach(buildProblem -> heuristicResult.addResponsibility(buildProblem, responsibility));
    heuristicContext.getTestRuns().forEach(testRun -> heuristicResult.addResponsibility(testRun, responsibility));
  }

  /**
   * Determines whether to apply responsibility for snapshot dependency errors.
   *
   * @param build the build for which to check the snapshot dependency errors
   * @return true if snapshot dependency errors should be considered, false otherwise
   */
  private boolean shouldApplyForSnapshotDependencyErrors(SBuild build) {
    if (build.isCompositeBuild()) {
      return true;
    }
    SBuildType buildType = build.getBuildType();
    boolean ignoreSnapshotDependencyErrors =
      buildType instanceof BuildTypeEx
      ? ((BuildTypeEx)buildType).getBooleanInternalParameterOrTrue(
        Constants.IGNORE_SNAPSHOT_DEPENDENCY_ERRORS_IN_DEFAULT_HEURISTIC)
      : TeamCityProperties.getBooleanOrTrue(Constants.IGNORE_SNAPSHOT_DEPENDENCY_ERRORS_IN_DEFAULT_HEURISTIC);
    return !ignoreSnapshotDependencyErrors;
  }
}
