

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
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.Dates;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.investigationsAutoAssigner.common.Constants.LOGGER;

public class BuildProblemsAssigner implements BaseAssigner {

  @NotNull private final BuildProblemResponsibilityFacade myBuildProblemResponsibilityFacade;
  private final StatisticsReporter myStatisticsReporter;
  private final WebLinks myWebLinks;
  private final TargetProjectFinder myTargetProjectFinder;
  private final AbstractFavoriteBuildAssigner myFavoriteBuildAssigner;

  public BuildProblemsAssigner(@NotNull final BuildProblemResponsibilityFacade buildProblemResponsibilityFacade,
                               @NotNull final WebLinks webLinks,
                               @NotNull final StatisticsReporter statisticsReporter,
                               @NotNull final TargetProjectFinder targetProjectFinder,
                               @NotNull final AbstractFavoriteBuildAssigner favoriteBuildAssigner) {
    myBuildProblemResponsibilityFacade = buildProblemResponsibilityFacade;
    myStatisticsReporter = statisticsReporter;
    myWebLinks = webLinks;
    myTargetProjectFinder = targetProjectFinder;
    myFavoriteBuildAssigner = favoriteBuildAssigner;
  }

  void assign(final HeuristicResult heuristicsResult,
              final SProject sProject,
              final SBuild sBuild,
              final List<BuildProblem> buildProblems) {
    if (heuristicsResult.isEmpty()) return;

    HashMap<Responsibility, List<BuildProblemInfo>> responsibilityToBuildProblem = new HashMap<>();
    for (BuildProblem buildProblem : buildProblems) {
      Responsibility responsibility = heuristicsResult.getResponsibility(buildProblem);
      responsibilityToBuildProblem.putIfAbsent(responsibility, new ArrayList<>());
      List<BuildProblemInfo> buildProblemList = responsibilityToBuildProblem.get(responsibility);
      buildProblemList.add(buildProblem);
    }

    SProject targetProject =
      myTargetProjectFinder.getPreferredInvestigationProject(sProject, null);
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

        String linkToBuild = myWebLinks.getViewResultsUrl(sBuild);
        myBuildProblemResponsibilityFacade.setBuildProblemResponsibility(
          buildProblemList,
          targetProject.getProjectId(),
          new ResponsibilityEntryEx(
            ResponsibilityEntry.State.TAKEN, responsibility.getUser(), null, Dates.now(),
            responsibility.getAssignDescription(linkToBuild), getRemoveMethod(sBuild.getBuildType()))
        );
        myFavoriteBuildAssigner.markAsFavorite(sBuild, (SUser)responsibility.getUser());

        myStatisticsReporter.reportAssignedInvestigations(buildProblemList.size(), responsibility);
      }
    }
  }
}