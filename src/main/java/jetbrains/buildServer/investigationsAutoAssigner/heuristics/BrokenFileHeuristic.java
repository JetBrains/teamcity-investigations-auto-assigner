

package jetbrains.buildServer.investigationsAutoAssigner.heuristics;

package jetbrains.buildServer.investigationsAutoAssigner.heuristics;

import com.intellij.openapi.diagnostic.Logger;
import java.util.HashMap;
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
  private final InvestigationsManager investigationsManager;

  public PreviousResponsibleHeuristic(@NotNull InvestigationsManager investigationsManager) {
    this.investigationsManager = investigationsManager;
  }

  @NotNull
  @Override
  public String getId() {
    return "PreviousResponsible";
  }

  @NotNull
  @Override
  public HeuristicResult findResponsibleUser(@NotNull HeuristicContext context) {
    HeuristicResult result = new HeuristicResult();
    SBuild build = context.getBuild();
    SProject project = context.getProject();

    HashMap<Long, User> testIdToResponsible = investigationsManager.findInAudit(context.getTestRuns(), project);
    processTestRuns(context, project, build, testIdToResponsible, result);
    processBuildProblems(context, project, build, result);

    return result;
  }

  private void processTestRuns(HeuristicContext context, SProject project, SBuild build,
                               HashMap<Long, User> auditMap, HeuristicResult result) {
    for (STestRun testRun : context.getTestRuns()) {
      STest test = testRun.getTest();

      User user = investigationsManager.findPreviousResponsible(project, build, test);
      if (user == null) {
        user = auditMap.get(test.getTestNameId());
      }

      if (!isValidUser(user, context)) continue;

      String description = String.format("was previously responsible for the test %s", test.getName());
      result.addResponsibility(testRun, new Responsibility(user, description));
    }
  }

  private void processBuildProblems(HeuristicContext context, SProject project, SBuild build, HeuristicResult result) {
    for (BuildProblem problem : context.getBuildProblems()) {
      String type = problem.getBuildProblemData().getType();
      if (!BuildProblemsFilter.supportedEverywhereTypes.contains(type)) continue;

      User user = investigationsManager.findPreviousResponsible(project, build, problem);
      if (!isValidUser(user, context)) continue;

      String description = String.format("was previously responsible for the problem %s", type);
      result.addResponsibility(problem, new Responsibility(user, description));
    }
  }

  private boolean isValidUser(User user, HeuristicContext context) {
    if (user == null) return false;
    String username = user.getUsername();
    long userId = user.getId();

    if (context.getUsersToIgnore().contains(username)) {
      LOGGER.debug(String.format("Build %s: PreviousResponsibleHeuristic skipped user '%s' (ignored).",
                                 context.getBuild().getBuildId(), username));
      return false;
    }
    if (!context.getCommitersIds().contains(userId)) {
      LOGGER.debug(String.format("Build %s: PreviousResponsibleHeuristic skipped user '%s' (not a committer).",
                                 context.getBuild().getBuildId(), username));
      return false;
    }
    return true;
  }
}
