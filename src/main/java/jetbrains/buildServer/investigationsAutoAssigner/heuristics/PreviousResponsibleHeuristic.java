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

/**
 * Heuristic that assigns responsibility for test failures based on previous user involvement with
 * the test or build problem. The heuristic uses historical data to identify the user who was
 * previously responsible for a failing test or build problem.
 */
public class PreviousResponsibleHeuristic implements Heuristic {

  private static final Logger LOGGER = Constants.LOGGER;
  private final InvestigationsManager investigationsManager;

  /**
   * Constructs a new PreviousResponsibleHeuristic.
   *
   * @param investigationsManager The manager for handling investigation data.
   */
  public PreviousResponsibleHeuristic(@NotNull InvestigationsManager investigationsManager) {
    this.investigationsManager = investigationsManager;
  }

  /**
   * Finds the responsible user for the given build and its associated test runs and build problems.
   * This is determined by looking at previous responsibilities and matching them to the current context.
   *
   * @param heuristicContext The context for the heuristic, including the build, test runs, and build problems.
   * @return A HeuristicResult containing the identified responsibilities.
   */
  @NotNull
  public HeuristicResult findResponsibleUser(@NotNull HeuristicContext heuristicContext) {
    HeuristicResult heuristicResult = new HeuristicResult();
    SBuild sBuild = heuristicContext.getBuild();
    SProject sProject = heuristicContext.getProject();
    Iterable<STestRun> sTestRuns = heuristicContext.getTestRuns();

    // Process previous responsibilities from audit data
    HashMap<Long, User> testId2Responsible = this.investigationsManager.findInAudit(sTestRuns, sProject);

    // Process Test Runs
    for (STestRun sTestRun : sTestRuns) {
      STest sTest = sTestRun.getTest();
      User responsibleUser = this.investigationsManager.findPreviousResponsible(sProject, sBuild, sTest);
      responsibleUser = (responsibleUser == null) ? testId2Responsible.get(sTest.getTestNameId()) : responsibleUser;

      if (responsibleUser != null && !shouldSkip(responsibleUser, heuristicContext)) {
        heuristicResult.addResponsibility(sTestRun, new Responsibility(responsibleUser, String.format(
          "was previously responsible for the test %s", sTest.getName())));
      }
    }

    // Process Build Problems
    for (BuildProblem buildProblem : heuristicContext.getBuildProblems()) {
      String buildProblemType = buildProblem.getBuildProblemData().getType();
      if (!BuildProblemsFilter.supportedEverywhereTypes.contains(buildProblemType)) {
        continue;
      }

      User responsibleUser = this.investigationsManager.findPreviousResponsible(sProject, sBuild, buildProblem);
      if (responsibleUser != null && !shouldSkip(responsibleUser, heuristicContext)) {
        heuristicResult.addResponsibility(buildProblem, new Responsibility(responsibleUser, String.format(
          "was previously responsible for the problem %s", buildProblemType)));
      }
    }

    return heuristicResult;
  }

  /**
   * Determines whether a responsible user should be skipped based on the current context.
   *
   * @param responsibleUser  The user who is responsible for a test or build problem.
   * @param heuristicContext The context of the heuristic, including the users to ignore and the committers.
   * @return True if the user should be skipped, false otherwise.
   */
  private boolean shouldSkip(User responsibleUser, HeuristicContext heuristicContext) {
    if (responsibleUser == null) return false; // No user to skip

    long buildId = heuristicContext.getBuild().getBuildId();
    String username = responsibleUser.getUsername();

    if (responsibleUser != null && heuristicContext.getUsersToIgnore().contains(username)) {
      LOGGER.debug(
        String.format("Build %s: Found PreviousResponsibleHeuristic for user `%s` from black list. Skip.", buildId,
                      username));
      return true;
    }

    if (responsibleUser != null && !heuristicContext.getCommittersIds().contains(responsibleUser.getId())) {
      LOGGER.debug(
        String.format("Build %s: Found PreviousResponsibleHeuristic for user `%s` not among committers. Skip.", buildId,
                      username));
      return true;
    }

    return false;
  }

  /**
   * Returns the unique identifier for this heuristic.
   *
   * @return The ID of the heuristic.
   */
  @NotNull
  @Override
  public String getId() {
    return "PreviousResponsible";
  }
}
