package jetbrains.buildServer.investigationsAutoAssigner.processing;

import java.util.*;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.persistent.StatisticsReporter;
import jetbrains.buildServer.investigationsAutoAssigner.utils.TargetProjectFinder;
import jetbrains.buildServer.responsibility.*;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.util.Dates;
import org.jetbrains.annotations.NotNull;


import static jetbrains.buildServer.investigationsAutoAssigner.common.Constants.LOGGER;

/**
 * Handles assignment of investigations for failed tests to responsible users.
 * Manages the entire process from mapping tests to responsibilities through
 * actual assignment and statistics reporting.
 */
public class FailedTestAssigner implements BaseAssigner {

  private final TestNameResponsibilityFacade testNameResponsibilityFacade;
  private final WebLinks webLinks;
  private final StatisticsReporter statisticsReporter;
  private final TargetProjectFinder targetProjectFinder;

  /**
   * Constructs a FailedTestAssigner with required dependencies.
   *
   * @param testNameResponsibilityFacade Facade for managing test responsibilities
   * @param webLinks                     Service for generating web URLs
   * @param statisticsReporter           Reporter for assignment statistics
   * @param targetProjectFinder          Finder for target investigation projects
   */
  public FailedTestAssigner(@NotNull final TestNameResponsibilityFacade testNameResponsibilityFacade,
                            @NotNull final WebLinks webLinks,
                            @NotNull final StatisticsReporter statisticsReporter,
                            @NotNull final TargetProjectFinder targetProjectFinder) {
    this.testNameResponsibilityFacade = testNameResponsibilityFacade;
    this.webLinks = webLinks;
    this.statisticsReporter = statisticsReporter;
    this.targetProjectFinder = targetProjectFinder;
  }

  /**
   * Assigns investigations for failed tests based on heuristic results.
   *
   * @param heuristicsResult Contains mapping of test failures to responsible users
   * @param project          The project where the build occurred
   * @param build            The build containing the test failures
   * @param testRuns         List of test runs to process
   */
  public void assign(
    @NotNull final HeuristicResult heuristicsResult,
    @NotNull final SProject project,
    @NotNull final SBuild build,
    @NotNull final List<STestRun> testRuns) {

    if (heuristicsResult.isEmpty() || testRuns.isEmpty()) {
      return;
    }

    Map<Responsibility, List<TestName>> responsibilityMap =
      createResponsibilityMap(heuristicsResult, testRuns);
    SProject targetProject = findTargetProject(project);

    // Removed unused heuristicsResult parameter
    processAssignments(build, targetProject, responsibilityMap);
  }

  /**
   * Creates a mapping of responsibilities to their associated test names.
   */
  @NotNull
  private Map<Responsibility, List<TestName>> createResponsibilityMap(@NotNull HeuristicResult heuristicsResult,
                                                                      @NotNull List<STestRun> testRuns) {

    Map<Responsibility, List<TestName>> map = new HashMap<>();

    for (STestRun testRun : testRuns) {
      Responsibility responsibility = heuristicsResult.getResponsibility(testRun);
      if (responsibility != null) {
        map.computeIfAbsent(responsibility, k -> new ArrayList<>()).add(testRun.getTest().getName());
      }
    }
    return map;
  }

  /**
   * Determines the target project for investigations.
   */
  @NotNull
  private SProject findTargetProject(@NotNull SProject sourceProject) {
    SProject preferredProject = this.targetProjectFinder.getPreferredInvestigationProject(sourceProject, null);
    return preferredProject != null ? preferredProject : sourceProject;
  }

  /**
   * Processes all test assignments for the given responsibility map.
   * @param build The build containing test failures
   * @param targetProject The project where investigations will be created
   * @param responsibilityMap Mapping of responsibilities to test names
   */
  private void processAssignments(
    @NotNull SBuild build,
    @NotNull SProject targetProject,
    @NotNull Map<Responsibility, List<TestName>> responsibilityMap) {

    String buildUrl = this.webLinks.getViewResultsUrl(build);

    for (Map.Entry<Responsibility, List<TestName>> entry : responsibilityMap.entrySet()) {
      Responsibility responsibility = entry.getKey();
      List<TestName> testNames = entry.getValue();

      if (testNames.isEmpty()) {
        continue;
      }

      logAssignment(responsibility, targetProject, testNames);
      createInvestigation(responsibility, targetProject, testNames, buildUrl, build);
      reportStatistics(testNames.size(), responsibility);
    }
  }

  /**
   * Logs information about the assignment.
   */
  private void logAssignment(@NotNull Responsibility responsibility,
                             @NotNull SProject project,
                             @NotNull List<TestName> testNames) {

    LOGGER.info(String.format("Automatically assigning investigation(s) to %s in %s # %s because user %s",
                              responsibility.getUser().getUsername(), project.describe(false), testNames,
                              responsibility.getDescription()));
  }

  /**
   * Creates the actual investigation entry.
   */
  private void createInvestigation(@NotNull Responsibility responsibility,
                                   @NotNull SProject project,
                                   @NotNull List<TestName> testNames,
                                   @NotNull String buildUrl,
                                   @NotNull SBuild build) {

    this.testNameResponsibilityFacade.setTestNameResponsibility(testNames, project.getProjectId(),
                                                           new ResponsibilityEntryEx(ResponsibilityEntry.State.TAKEN,
                                                                                     responsibility.getUser(), null,
                                                                                     Dates.now(),
                                                                                     responsibility.getAssignDescription(
                                                                                       buildUrl), getRemoveMethod(
                                                             build.getBuildType())));
  }

  /**
   * Reports statistics about the assignment.
   */
  private void reportStatistics(int count, @NotNull Responsibility responsibility) {
    this.statisticsReporter.reportAssignedInvestigations(count, responsibility);
  }

}