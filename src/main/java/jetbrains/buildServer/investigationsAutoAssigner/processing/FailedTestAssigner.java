

package jetbrains.buildServer.investigationsAutoAssigner.processing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.persistent.StatisticsReporter;
import jetbrains.buildServer.investigationsAutoAssigner.utils.TargetProjectFinder;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.responsibility.ResponsibilityEntryEx;
import jetbrains.buildServer.responsibility.TestNameResponsibilityFacade;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.WebLinks;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.Dates;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.investigationsAutoAssigner.common.Constants.LOGGER;

public class FailedTestAssigner implements BaseAssigner {

  @NotNull private final TestNameResponsibilityFacade myTestNameResponsibilityFacade;
  private final WebLinks myWebLinks;
  private final StatisticsReporter myStatisticsReporter;
  private final TargetProjectFinder myTargetProjectFinder;
  private final AbstractFavoriteBuildAssigner myFavoriteBuildAssigner;

  public FailedTestAssigner(@NotNull final TestNameResponsibilityFacade testNameResponsibilityFacade,
                            @NotNull final WebLinks webLinks,
                            @NotNull final StatisticsReporter statisticsReporter,
                            @NotNull final TargetProjectFinder targetProjectFinder,
                            @NotNull final AbstractFavoriteBuildAssigner favoriteBuildAssigner) {
    myTestNameResponsibilityFacade = testNameResponsibilityFacade;
    myWebLinks = webLinks;
    myStatisticsReporter = statisticsReporter;
    myTargetProjectFinder = targetProjectFinder;
    myFavoriteBuildAssigner = favoriteBuildAssigner;
  }

  void assign(final HeuristicResult heuristicsResult,
              final SProject sProject,
              final SBuild sBuild,
              final List<STestRun> sTestRuns) {
    if (heuristicsResult.isEmpty()) return;

    HashMap<Responsibility, List<TestName>> responsibilityToTestNames = new HashMap<>();
    for (STestRun sTestRun : sTestRuns) {
      Responsibility responsibility = heuristicsResult.getResponsibility(sTestRun);
      responsibilityToTestNames.computeIfAbsent(responsibility, devNull -> new ArrayList<>());
      List<TestName> testNameList = responsibilityToTestNames.get(responsibility);
      testNameList.add(sTestRun.getTest().getName());
    }

    SProject targetProject =
      myTargetProjectFinder.getPreferredInvestigationProject(sProject, null);
    if (targetProject == null) {
      targetProject = sProject;
    }


    Set<Responsibility> uniqueResponsibilities = responsibilityToTestNames.keySet();
    for (Responsibility responsibility : uniqueResponsibilities) {
      if (responsibility != null) {
        List<TestName> testNameList = responsibilityToTestNames.get(responsibility);
        LOGGER.info(String.format("Automatically assigning investigation(s) to %s in %s # %s because user %s",
                                            responsibility.getUser().getUsername(),
                                            targetProject.describe(false),
                                            testNameList,
                                            responsibility.getDescription()));

        String linkToBuild = myWebLinks.getViewResultsUrl(sBuild);
        myTestNameResponsibilityFacade.setTestNameResponsibility(
          testNameList, targetProject.getProjectId(),
          new ResponsibilityEntryEx(
            ResponsibilityEntry.State.TAKEN, responsibility.getUser(), null, Dates.now(),
            responsibility.getAssignDescription(linkToBuild), getRemoveMethod(sBuild.getBuildType()))
        );
        myFavoriteBuildAssigner.markAsFavorite(sBuild, (SUser)responsibility.getUser());

        myStatisticsReporter.reportAssignedInvestigations(testNameList.size(), responsibility);
      }
    }
  }
}