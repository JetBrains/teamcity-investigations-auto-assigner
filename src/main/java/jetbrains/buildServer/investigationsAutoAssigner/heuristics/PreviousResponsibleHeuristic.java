

package jetbrains.buildServer.investigationsAutoAssigner.heuristics;

import com.intellij.openapi.diagnostic.Logger;
import java.util.Map;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.processing.BuildProblemsFilter;
import jetbrains.buildServer.investigationsAutoAssigner.processing.HeuristicContext;
import jetbrains.buildServer.investigationsAutoAssigner.utils.InvestigationsManager;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STest;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.users.User;
import org.jetbrains.annotations.NotNull;

public class PreviousResponsibleHeuristic implements Heuristic {

  private static final Logger LOGGER = Constants.LOGGER;
  private final InvestigationsManager myInvestigationsManager;

  public PreviousResponsibleHeuristic(@NotNull InvestigationsManager investigationsManager) {
    myInvestigationsManager = investigationsManager;
  }

  @NotNull
  @Override
  public String getId() {
    return "PreviousResponsible";
  }

  @NotNull
  public HeuristicResult findResponsibleUser(@NotNull HeuristicContext heuristicContext) {
    HeuristicResult result = new HeuristicResult();
    SBuild sBuild = heuristicContext.getBuild();
    SProject sProject = heuristicContext.getProject();
    Iterable<STestRun> sTestRuns = heuristicContext.getTestRuns();

    Map<Long, User> testId2Responsible = myInvestigationsManager.findInAudit(sTestRuns, sProject);
    for (STestRun sTestRun : heuristicContext.getTestRuns()) {
      STest sTest = sTestRun.getTest();

      User responsibleUser = myInvestigationsManager.findPreviousResponsible(sProject, sBuild, sTest);

      if (responsibleUser == null) {
        responsibleUser = testId2Responsible.get(sTest.getTestNameId());
      }

      if (shouldSkip(responsibleUser, heuristicContext)) {
        continue;
      }

      if (responsibleUser != null) {
        String description = String.format("was previously responsible for the test %s", sTest.getName());

        result.addResponsibility(sTestRun, new Responsibility(responsibleUser, description));
      }
    }

    for (BuildProblem buildProblem : heuristicContext.getBuildProblems()) {
      String buildProblemType = buildProblem.getBuildProblemData().getType();
      if (!BuildProblemsFilter.supportedEverywhereTypes.contains(buildProblemType)) {
        continue;
      }

      User responsibleUser = myInvestigationsManager.findPreviousResponsible(sProject, sBuild, buildProblem);

      if (shouldSkip(responsibleUser, heuristicContext)) {
        continue;
      }

      if (responsibleUser != null) {
        String description = String.format("was previously responsible for the problem %s`", buildProblemType);
        result.addResponsibility(buildProblem, new Responsibility(responsibleUser, description));
      }
    }

    return result;
  }

  private boolean shouldSkip(User responsibleUser, HeuristicContext heuristicContext) {
    if (responsibleUser != null && heuristicContext.getUsersToIgnore().contains(responsibleUser.getUsername())) {
      LOGGER.debug(
        String.format("Build %s: Found PreviousResponsibleHeuristic for user `%s` from black list. Skip.",
                      heuristicContext.getBuild().getBuildId(),
                      responsibleUser.getUsername()));
      return true;
    }

    if (responsibleUser != null && !heuristicContext.getCommitersIds().contains(responsibleUser.getId())) {
      LOGGER.debug(
        String.format("Build %s: Found PreviousResponsibleHeuristic for user `%s` not among commiters. Skip.",
                      heuristicContext.getBuild().getBuildId(),
                      responsibleUser.getUsername()));
      return true;
    }

    return false;
  }
}
