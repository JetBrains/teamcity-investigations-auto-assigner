package jetbrains.buildServer.investigationsAutoAssigner.processing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.persistent.StatisticsReporter;
import jetbrains.buildServer.investigationsAutoAssigner.utils.TargetProjectFinder;
import jetbrains.buildServer.responsibility.BuildProblemResponsibilityFacade;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.responsibility.ResponsibilityEntryEx;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.WebLinks;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.serverSide.problems.BuildProblemInfo;
import jetbrains.buildServer.util.Dates;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.investigationsAutoAssigner.common.Constants.LOGGER;

/**
 * Handles automatic assignment of build problem investigations based on heuristic analysis.
 * <p>
 * This class assigns responsibility for build problems to users identified by heuristics.
 * It uses {@link HeuristicResult} to determine responsible users and delegates responsibility
 * assignment to {@link BuildProblemResponsibilityFacade}. Assigned responsibilities are reported
 * via {@link StatisticsReporter}.
 * </p>
 *
 * @see BaseAssigner
 * @see HeuristicResult
 * @see BuildProblemResponsibilityFacade
 */
public class BuildProblemsAssigner implements BaseAssigner {

  @NotNull private final BuildProblemResponsibilityFacade buildProblemResponsibilityFacade;
  private final StatisticsReporter statisticsReporter;
  private final WebLinks webLinks;
  private final TargetProjectFinder targetProjectFinder;

  /**
   * Constructs a {@code BuildProblemsAssigner} instance.
   *
   * @param buildProblemResponsibilityFacade the facade for assigning responsibilities to build problems
   * @param webLinks                         the utility for generating links to builds
   * @param statisticsReporter               the reporter for logging assigned investigations
   * @param targetProjectFinder              the utility to determine the appropriate project for assignment
   */
  public BuildProblemsAssigner(@NotNull final BuildProblemResponsibilityFacade buildProblemResponsibilityFacade,
                               @NotNull final WebLinks webLinks,
                               @NotNull final StatisticsReporter statisticsReporter,
                               @NotNull final TargetProjectFinder targetProjectFinder) {
    this.buildProblemResponsibilityFacade = buildProblemResponsibilityFacade;
    this.statisticsReporter = statisticsReporter;
    this.webLinks = webLinks;
    this.targetProjectFinder = targetProjectFinder;
  }

  /**
   * Assigns responsibility for build problems based on heuristic analysis.
   * <p>
   * This method processes a set of build problems and assigns responsibility to users based
   * on the results of the provided heuristic analysis. Responsibilities are assigned at the
   * project level, and the results are logged.
   * </p>
   *
   * @param heuristicsResult the result of the heuristic analysis that determines responsible users
   * @param sProject         the project where the build problems occurred
   * @param sBuild           the specific build in which the problems were found
   * @param buildProblems    the list of build problems to be assigned
   */
  void assign(final HeuristicResult heuristicsResult,
              final SProject sProject,
              final SBuild sBuild,
              final List<BuildProblem> buildProblems) {
    if (heuristicsResult.isEmpty()) return;

    HashMap<Responsibility, List<BuildProblemInfo>> responsibilityToBuildProblem = new HashMap<>();
    for (BuildProblem buildProblem : buildProblems) {
      Responsibility responsibility = heuristicsResult.getResponsibility(buildProblem);
      responsibilityToBuildProblem
        .computeIfAbsent(responsibility, k -> new ArrayList<>())
        .add(buildProblem);
    }

    SProject targetProject = this.targetProjectFinder.getPreferredInvestigationProject(sProject, null);
    if (targetProject == null) {
      targetProject = sProject;
    }

    Set<Responsibility> uniqueResponsibilities = responsibilityToBuildProblem.keySet();
    for (Responsibility responsibility : uniqueResponsibilities) {
      if (responsibility != null) {
        LOGGER.info(String.format("Automatically assigning investigation(s) to %s in %s because user %s",
                                  responsibility.getUser().getUsername(),
                                  targetProject.describe(false),
                                  responsibility.getDescription()));
        List<BuildProblemInfo> buildProblemList = responsibilityToBuildProblem.get(responsibility);

        String linkToBuild = this.webLinks.getViewResultsUrl(sBuild);
        this.buildProblemResponsibilityFacade.setBuildProblemResponsibility(
          buildProblemList,
          targetProject.getProjectId(),
          new ResponsibilityEntryEx(
            ResponsibilityEntry.State.TAKEN, responsibility.getUser(), null, Dates.now(),
            responsibility.getAssignDescription(linkToBuild), getRemoveMethod(sBuild.getBuildType()))
        );

        this.statisticsReporter.reportAssignedInvestigations(buildProblemList.size(), responsibility);
      }
    }
  }
}
