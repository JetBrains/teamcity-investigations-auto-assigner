package jetbrains.buildServer.investigationsAutoAssigner.processing;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.heuristics.Heuristic;
import jetbrains.buildServer.investigationsAutoAssigner.utils.CustomParameters;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import org.jetbrains.annotations.NotNull;

/**
 * Responsible for finding the user responsible for build problems and test failures based on heuristics.
 * The process iterates through a list of heuristics to find the responsible user for each problem.
 */
public class ResponsibleUserFinder {

  private final List<Heuristic> orderedHeuristics;
  private final CustomParameters customParameters;

  /**
   * Constructor for ResponsibleUserFinder.
   *
   * @param orderedHeuristics list of heuristics to be applied in order.
   * @param customParameters custom parameters containing configuration for the heuristics.
   */
  public ResponsibleUserFinder(@NotNull final List<Heuristic> orderedHeuristics,
                               @NotNull final CustomParameters customParameters) {
    this.orderedHeuristics = orderedHeuristics;
    this.customParameters = customParameters;
  }

  /**
   * Finds the responsible user for build problems and test failures by applying heuristics.
   * Iterates over each heuristic and checks for responsibility.
   *
   * @param sBuild the build being analyzed.
   * @param sProject the project associated with the build.
   * @param buildProblems list of build problems to be analyzed.
   * @param testRuns list of test runs to be analyzed.
   * @return HeuristicResult containing the user responsible for the problems or an empty result if no responsibility is found.
   */
  public HeuristicResult findResponsibleUser(SBuild sBuild,
                                             SProject sProject,
                                             List<BuildProblem> buildProblems,
                                             List<STestRun> testRuns) {

    if (buildProblems.isEmpty() && testRuns.isEmpty()) {
      return new HeuristicResult(); // Return empty result if there are no problems or test runs.
    }

    HeuristicResult result = new HeuristicResult();
    Set<String> usernamesBlackList = CustomParameters.getUsersToIgnore(sBuild);

    for (Heuristic heuristic : this.orderedHeuristics) {
      if (isHeuristicDisabled(heuristic)) {
        continue; // Skip disabled heuristics.
      }

      HeuristicContext heuristicContext = createHeuristicContext(sBuild, sProject, buildProblems, testRuns, usernamesBlackList);
      HeuristicResult heuristicResult = applyHeuristic(heuristic, heuristicContext);

      // Filter out problems and test runs with identified responsibility.
      buildProblems = filterUnresolvedProblems(heuristicResult, heuristicContext.getBuildProblems());
      testRuns = filterUnresolvedTestRuns(heuristicResult, heuristicContext.getTestRuns());

      result.merge(heuristicResult);

      // Stop processing if no unresolved issues remain.
      if (buildProblems.isEmpty() && testRuns.isEmpty()) {
        break;
      }
    }

    return result;
  }

  /**
   * Checks if a heuristic is disabled.
   *
   * @param heuristic the heuristic to check.
   * @return true if the heuristic is disabled, false otherwise.
   */
  private boolean isHeuristicDisabled(Heuristic heuristic) {
    return this.customParameters.isHeuristicsDisabled(heuristic.getId());
  }

  /**
   * Creates a context object for the heuristic to use during processing.
   *
   * @param sBuild the build being analyzed.
   * @param sProject the project associated with the build.
   * @param buildProblems the list of build problems.
   * @param testRuns the list of test runs.
   * @param usernamesBlackList the list of users to ignore.
   * @return a new HeuristicContext instance.
   */
  private HeuristicContext createHeuristicContext(SBuild sBuild, SProject sProject,
                                                  List<BuildProblem> buildProblems,
                                                  List<STestRun> testRuns,
                                                  Set<String> usernamesBlackList) {
    return new HeuristicContext(sBuild, sProject, buildProblems, testRuns, usernamesBlackList);
  }

  /**
   * Applies the heuristic to the provided context and returns the result.
   *
   * @param heuristic the heuristic to apply.
   * @param heuristicContext the context to apply the heuristic to.
   * @return the result of applying the heuristic.
   */
  private HeuristicResult applyHeuristic(Heuristic heuristic, HeuristicContext heuristicContext) {
    return heuristic.findResponsibleUser(heuristicContext);
  }

  /**
   * Filters the list of build problems to only include unresolved ones.
   *
   * @param heuristicResult the result from applying the heuristic.
   * @param buildProblems the list of build problems.
   * @return a filtered list of unresolved build problems.
   */
  private List<BuildProblem> filterUnresolvedProblems(HeuristicResult heuristicResult, List<BuildProblem> buildProblems) {
    return buildProblems.stream()
                        .filter(buildProblem -> heuristicResult.getResponsibility(buildProblem) == null)
                        .collect(Collectors.toList());
  }

  /**
   * Filters the list of test runs to only include unresolved ones.
   *
   * @param heuristicResult the result from applying the heuristic.
   * @param testRuns the list of test runs.
   * @return a filtered list of unresolved test runs.
   */
  private List<STestRun> filterUnresolvedTestRuns(HeuristicResult heuristicResult, List<STestRun> testRuns) {
    return testRuns.stream()
                   .filter(sTestRun -> heuristicResult.getResponsibility(sTestRun) == null)
                   .collect(Collectors.toList());
  }
}
