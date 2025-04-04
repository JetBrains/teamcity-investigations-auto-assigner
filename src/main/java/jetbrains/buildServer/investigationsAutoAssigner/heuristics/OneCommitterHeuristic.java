package jetbrains.buildServer.investigationsAutoAssigner.heuristics;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicNotApplicableException;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.processing.BuildProblemsFilter;
import jetbrains.buildServer.investigationsAutoAssigner.processing.HeuristicContext;
import jetbrains.buildServer.investigationsAutoAssigner.processing.ModificationAnalyzerFactory;
import jetbrains.buildServer.log.LogUtil;
import jetbrains.buildServer.serverSide.BuildStatisticsOptions;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.vcs.SVcsModification;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Heuristic that identifies the user responsible for a build based on the assumption that only one committer
 * is responsible for the changes in the build. The heuristic checks the commit history of the build to find the
 * user who committed the changes and assigns them as the responsible user. The heuristic also ensures that the
 * previous build did not have compilation errors before assigning responsibility.
 *
 * <p>This heuristic can be ignored if multiple committers are identified, or if the build's previous build
 * contained compilation errors.</p>
 *
 * <p>The responsibility is assigned to the user for both test runs and build problems, but only those that
 * are supported by the heuristic (i.e., build problems that are supported everywhere).</p>
 *
 * <p>Note: This heuristic requires a {@link ModificationAnalyzerFactory} to analyze the commits.</p>
 *
 * @see Heuristic
 * @see Responsibility
 * @see HeuristicResult
 */
public class OneCommitterHeuristic implements Heuristic {
  private static final Logger LOGGER = Constants.LOGGER;
  private final ModificationAnalyzerFactory modificationAnalyzerFactory;

  /**
   * Constructs a new OneCommitterHeuristic.
   *
   * @param modificationAnalyzerFactory The factory used to analyze modifications in the commits.
   */
  public OneCommitterHeuristic(@NotNull ModificationAnalyzerFactory modificationAnalyzerFactory) {
    this.modificationAnalyzerFactory = modificationAnalyzerFactory;
  }

  /**
   * Finds the user responsible for the current build by analyzing its commit history. The heuristic
   * checks if there is a single committer and whether the previous build had any compilation errors.
   *
   * @param heuristicContext The context in which the heuristic is applied, including the current build.
   * @return A HeuristicResult containing the identified responsibilities, or an empty result if no user is found.
   */
  @NotNull
  @Override
  public HeuristicResult findResponsibleUser(@NotNull HeuristicContext heuristicContext) {
    HeuristicResult heuristicResult = new HeuristicResult();
    SBuild build = heuristicContext.getBuild();

    // Get the responsible user
    User responsible = getResponsibleUser(build, heuristicContext);
    if (responsible == null) return heuristicResult;

    // Check for compilation errors and return if found
    if (isCompilationErrorFixed(build)) {
      LOGGER.debug("Heuristic \"OneCommitter\" found " + responsible.getDescriptiveName() + " as responsible, but " +
                   "results are ignored because the previous build contained compilation errors. Build: " +
                   LogUtil.describe(build));

      return heuristicResult;
    }

    // Add responsibility to the test runs and build problems
    Responsibility responsibility = new Responsibility(responsible, "was the only committer to the build");
    addResponsibilitiesToTestRuns(heuristicContext, heuristicResult, responsibility);
    addResponsibilitiesToBuildProblems(heuristicContext, heuristicResult, responsibility);

    return heuristicResult;
  }

  /**
   * Retrieves the user responsible for the commit changes in the build.
   *
   * @param build            The current build whose commit history is being analyzed.
   * @param heuristicContext The context for the heuristic, including the users to ignore.
   * @return The user responsible for the commits, or null if no single committer is found.
   */
  private User getResponsibleUser(SBuild build, HeuristicContext heuristicContext) {
    User responsibleUser = null;
    final SelectPrevBuildPolicy selectPrevBuildPolicy = SelectPrevBuildPolicy.SINCE_LAST_BUILD;
    for (SVcsModification vcsChange : build.getChanges(selectPrevBuildPolicy, true)) {
      try {
        ModificationAnalyzerFactory.ModificationAnalyzer vcsChangeWrapped =
          this.modificationAnalyzerFactory.getInstance(vcsChange);
        User probableResponsible = vcsChangeWrapped.getOnlyCommitter(heuristicContext.getUsersToIgnore());
        if (probableResponsible == null) continue;
        ensureSameUsers(responsibleUser, probableResponsible);
        responsibleUser = probableResponsible;
      } catch (HeuristicNotApplicableException ex) {
        LOGGER.debug(
          "Heuristic \"OneCommitter\" is ignored as " + ex.getMessage() + ". Build: " + LogUtil.describe(build));
        return null;
      }
    }
    return responsibleUser;
  }

  /**
   * Adds the responsibility for the test runs in the heuristic context.
   *
   * @param heuristicContext The context containing the test runs.
   * @param result           The result to which responsibilities will be added.
   * @param responsibility   The responsibility to assign.
   */
  private void addResponsibilitiesToTestRuns(HeuristicContext heuristicContext,
                                             HeuristicResult result,
                                             Responsibility responsibility) {
    heuristicContext.getTestRuns().forEach(sTestRun -> result.addResponsibility(sTestRun, responsibility));
  }

  /**
   * Adds the responsibility for the build problems in the heuristic context.
   *
   * @param heuristicContext The context containing the build problems.
   * @param result           The result to which responsibilities will be added.
   * @param responsibility   The responsibility to assign.
   */
  private void addResponsibilitiesToBuildProblems(HeuristicContext heuristicContext,
                                                  HeuristicResult result,
                                                  Responsibility responsibility) {
    heuristicContext.getBuildProblems().stream().filter(
                      problem -> BuildProblemsFilter.supportedEverywhereTypes.contains(problem.getBuildProblemData().getType()))
                    .forEach(buildProblem -> result.addResponsibility(buildProblem, responsibility));
  }

  /**
   * Checks if the previous build had compilation errors and if they have been fixed in the current build.
   *
   * @param build The current build.
   * @return True if the previous build had compilation errors, and they are fixed in the current build, false otherwise.
   */
  private boolean isCompilationErrorFixed(final SBuild build) {
    SBuild previousFinished = build.getPreviousFinished();
    return !containsCompilationErrors(build) && previousFinished != null && containsCompilationErrors(previousFinished);
  }

  /**
   * Checks if the build contains any compilation errors.
   *
   * @param build The build to check.
   * @return True if the build contains compilation errors, false otherwise.
   */
  private boolean containsCompilationErrors(@NotNull SBuild build) {
    BuildStatisticsOptions opts = new BuildStatisticsOptions(BuildStatisticsOptions.COMPILATION_ERRORS, 0);
    return build.getBuildStatistics(opts).getCompilationErrorsCount() > 0;
  }

  /**
   * Ensures that both users are the same, otherwise throws a HeuristicNotApplicableException.
   *
   * @param first  The first user to compare.
   * @param second The second user to compare.
   */
  private void ensureSameUsers(@Nullable User first, @Nullable User second) {
    if (first != null && second != null && !first.equals(second)) {
      throw new HeuristicNotApplicableException("there are more than one TeamCity user");
    }
  }

  /**
   * Returns the unique identifier for this heuristic.
   *
   * @return The ID of the heuristic.
   */
  @Override
  @NotNull
  public String getId() {
    return "OneCommitter";
  }
}
