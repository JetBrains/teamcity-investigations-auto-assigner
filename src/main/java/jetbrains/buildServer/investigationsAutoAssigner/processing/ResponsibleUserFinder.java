

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

public class ResponsibleUserFinder {
  private final List<Heuristic> myOrderedHeuristics;
  private final CustomParameters myCustomParameters;

  public ResponsibleUserFinder(@NotNull final List<Heuristic> orderedHeuristics,
                               @NotNull final CustomParameters customParameters) {
    myOrderedHeuristics = orderedHeuristics;
    myCustomParameters = customParameters;
  }

  HeuristicResult findResponsibleUser(SBuild sBuild,
                                      SProject sProject,
                                      List<BuildProblem> buildProblems,
                                      List<STestRun> testRuns) {

    if (buildProblems.isEmpty() && testRuns.isEmpty()) {
      return new HeuristicResult();
    }

    HeuristicResult result = new HeuristicResult();
    Set<String> usernamesBlackList = CustomParameters.getUsersToIgnore(sBuild);
    for (Heuristic heuristic : myOrderedHeuristics) {
      if (myCustomParameters.isHeuristicsDisabled(heuristic.getId())) {
        continue;
      }

      HeuristicContext heuristicContext =
        new HeuristicContext(sBuild, sProject, buildProblems, testRuns, usernamesBlackList);
      HeuristicResult heuristicResult = heuristic.findResponsibleUser(heuristicContext);

      buildProblems = heuristicContext.getBuildProblems()
                                      .stream()
                                      .filter(buildProblem -> heuristicResult.getResponsibility(buildProblem) == null)
                                      .collect(Collectors.toList());

      testRuns = heuristicContext.getTestRuns()
                                 .stream()
                                 .filter(sTestRun -> heuristicResult.getResponsibility(sTestRun) == null)
                                 .collect(Collectors.toList());

      result.merge(heuristicResult);

      if (buildProblems.isEmpty() && testRuns.isEmpty()) {
        break;
      }
    }

    return result;
  }
}