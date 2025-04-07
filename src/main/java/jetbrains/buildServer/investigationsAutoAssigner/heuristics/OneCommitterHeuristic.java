

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

public class OneCommitterHeuristic implements Heuristic {
  private static final Logger LOGGER = Constants.LOGGER;
  private final ModificationAnalyzerFactory modificationAnalyzerFactory;

  public OneCommitterHeuristic(@NotNull ModificationAnalyzerFactory modificationAnalyzerFactory) {
    this.modificationAnalyzerFactory = modificationAnalyzerFactory;
  }

  @Override
  @NotNull
  public String getId() {
    return "OneCommitter";
  }

  @NotNull
  @Override
  public HeuristicResult findResponsibleUser(@NotNull HeuristicContext context) {
    HeuristicResult result = new HeuristicResult();
    SBuild build = context.getBuild();

    User responsible = getOnlyCommitter(build, context);
    if (responsible == null || isCompilationErrorFixed(build)) return result;

    Responsibility responsibility = new Responsibility(responsible, "was the only committer to the build");

    context.getTestRuns().forEach(testRun -> result.addResponsibility(testRun, responsibility));

    context.getBuildProblems().stream()
           .filter(
             problem -> BuildProblemsFilter.supportedEverywhereTypes.contains(problem.getBuildProblemData().getType()))
           .forEach(problem -> result.addResponsibility(problem, responsibility));

    return result;
  }

  @Nullable
  private User getOnlyCommitter(SBuild build, HeuristicContext context) {
    User responsible = null;
    for (SVcsModification change : build.getChanges(SelectPrevBuildPolicy.SINCE_LAST_BUILD, true)) {
      try {
        User probable = modificationAnalyzerFactory.getInstance(change).getOnlyCommitter(context.getUsersToIgnore());
        if (probable == null) continue;
        ensureSameUsers(responsible, probable);
        responsible = probable;
      } catch (HeuristicNotApplicableException ex) {
        LOGGER.debug("Heuristic \"OneCommitter\" is ignored as "
                     + ex.getMessage() + ". Build: "
                     + LogUtil.describe(build));
        return null;
      }
    }
    return responsible;
  }

  private boolean isCompilationErrorFixed(@NotNull SBuild build) {
    SBuild previous = build.getPreviousFinished();
    return previous != null && !hasCompilationErrors(build) && hasCompilationErrors(previous);
  }

  private boolean hasCompilationErrors(@NotNull SBuild build) {
    return build.getBuildStatistics(new BuildStatisticsOptions(BuildStatisticsOptions.COMPILATION_ERRORS, 0))
                .getCompilationErrorsCount() > 0;
  }

  private void ensureSameUsers(@Nullable User existing, @Nullable User next) {
    if (existing != null && next != null && !existing.equals(next)) {
      throw new HeuristicNotApplicableException("there are more than one TeamCity user");
    }
  }
}