package jetbrains.buildServer.investigationsAutoAssigner.utils;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.investigationsAutoAssigner.common.FailedBuildInfo;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.users.User;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


@Test
public class AggregationLoggerTest extends BaseTestCase {

  private AggregationLogger aggregationLogger;
  private WebLinks webLinksMock;
  private CustomParameters customParametersMock;
  private SBuild sBuildMock;
  private FailedBuildInfo failedBuildInfoMock;
  private HeuristicResult heuristicResultMock;
  private STestRun testRunMock;
  private Responsibility responsibilityMock;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    webLinksMock = Mockito.mock(WebLinks.class);
    customParametersMock = Mockito.mock(CustomParameters.class);
    aggregationLogger = new AggregationLogger(webLinksMock, customParametersMock);

    sBuildMock = Mockito.mock(SBuild.class);
    failedBuildInfoMock = Mockito.mock(FailedBuildInfo.class);
    heuristicResultMock = Mockito.mock(HeuristicResult.class);

    testRunMock = Mockito.mock(STestRun.class);
    responsibilityMock = Mockito.mock(Responsibility.class);

  }

  public void testLogResultsShouldNotLogWhenConditionsNotMet() {
    Mockito.when(failedBuildInfoMock.getBuild()).thenReturn(sBuildMock);
    Mockito.when(failedBuildInfoMock.getHeuristicsResult()).thenReturn(heuristicResultMock);
    Mockito.when(customParametersMock.isBuildFeatureEnabled(sBuildMock)).thenReturn(false);
    Mockito.when(failedBuildInfoMock.shouldDelayAssignments()).thenReturn(false);

    aggregationLogger.logResults(failedBuildInfoMock);

    Mockito.verify(webLinksMock, Mockito.never()).getViewResultsUrl(sBuildMock);
  }

  public void testLogResultsShouldNotLogWhenAssignmentsDelayed() {
    Mockito.when(failedBuildInfoMock.getBuild()).thenReturn(sBuildMock);
    Mockito.when(failedBuildInfoMock.getHeuristicsResult()).thenReturn(heuristicResultMock);
    Mockito.when(customParametersMock.isBuildFeatureEnabled(sBuildMock)).thenReturn(true);
    Mockito.when(failedBuildInfoMock.shouldDelayAssignments())
           .thenReturn(true);  // Delaying assignments should prevent logging

    aggregationLogger.logResults(failedBuildInfoMock);

    Mockito.verify(webLinksMock, Mockito.never()).getViewResultsUrl(sBuildMock);
  }

  public void testFormatTestEntry() throws Exception {
    Mockito.when(webLinksMock.getViewResultsUrl(sBuildMock)).thenReturn("https://example.com");

    Mockito.when(responsibilityMock.getDescription()).thenReturn("Test failed due to XYZ");

    User userMock = Mockito.mock(User.class);
    Mockito.when(responsibilityMock.getUser()).thenReturn(userMock);
    Mockito.when(userMock.getDescriptiveName()).thenReturn("John Doe");

    STest testMock = Mockito.mock(STest.class);
    Mockito.when(testRunMock.getTest()).thenReturn(testMock);
    Mockito.when(testMock.getTestNameId()).thenReturn(Long.valueOf("0")); // Mocking the testNameId

    String expected =
      "* Test entry (url: https://example.com#testNameId0) for John Doe. The user Test failed due to XYZ.\n";

    Method formatTestEntryMethod =
      AggregationLogger.class.getDeclaredMethod("formatTestEntry", STestRun.class, Responsibility.class, SBuild.class);
    formatTestEntryMethod.setAccessible(true); // Make private method accessible
    String result =
      (String)formatTestEntryMethod.invoke(aggregationLogger, testRunMock, responsibilityMock, sBuildMock);

    assertEquals(result, expected);
  }


  public void testFormatBuildProblemEntry() throws Exception {
    User userMock = Mockito.mock(User.class);
    Mockito.when(responsibilityMock.getUser()).thenReturn(userMock);
    Mockito.when(userMock.getDescriptiveName()).thenReturn("Jane Doe");
    Mockito.when(responsibilityMock.getDescription()).thenReturn("Build failed due to ABC");

    String expected = "* Build problem entry for Jane Doe. The user Build failed due to ABC.\n";

    Method formatBuildProblemEntryMethod =
      AggregationLogger.class.getDeclaredMethod("formatBuildProblemEntry", Responsibility.class);
    formatBuildProblemEntryMethod.setAccessible(true); // Make private method accessible
    String result = (String)formatBuildProblemEntryMethod.invoke(aggregationLogger, responsibilityMock);

    assertEquals(result, expected);
  }


  public void testGenerateForBuildProblems_WithBuildProblems_WithResponsibilities() throws Exception {
    BuildEx buildExMock = Mockito.mock(BuildEx.class); // Mocking BuildEx instead of SBuild
    BuildProblem buildProblemMock = Mockito.mock(BuildProblem.class);
    List<BuildProblem> buildProblems = Collections.singletonList(buildProblemMock);
    Mockito.when(buildExMock.getBuildProblems()).thenReturn(buildProblems);

    HeuristicResult heuristicsResultMock = Mockito.mock(HeuristicResult.class);
    Responsibility responsibilityMock = Mockito.mock(Responsibility.class);
    User userMock = Mockito.mock(User.class); // Mocking User object
    Mockito.when(responsibilityMock.getUser()).thenReturn(userMock);
    Mockito.when(userMock.getDescriptiveName()).thenReturn("John Doe"); // Mocking the username
    Mockito.when(heuristicsResultMock.getResponsibility(buildProblemMock)).thenReturn(responsibilityMock);

    Method formatBuildProblemEntryMethod =
      AggregationLogger.class.getDeclaredMethod("formatBuildProblemEntry", Responsibility.class);
    formatBuildProblemEntryMethod.setAccessible(true); // Make private method accessible
    String formattedEntry = "Formatted Build Problem Entry";
    Mockito.when(formatBuildProblemEntryMethod.invoke(aggregationLogger, responsibilityMock))
           .thenReturn(formattedEntry);

    Method generateForBuildProblemsMethod =
      AggregationLogger.class.getDeclaredMethod("generateForBuildProblems", SBuild.class, HeuristicResult.class);
    generateForBuildProblemsMethod.setAccessible(true); // Make private method accessible
    String result = (String)generateForBuildProblemsMethod.invoke(aggregationLogger, buildExMock, heuristicsResultMock);

    assertTrue(result.contains(formattedEntry));
  }


  public void testGetTitle_NewDelayedAssignment() throws Exception {
    FailedBuildInfo failedBuildInfoMock = Mockito.mock(FailedBuildInfo.class);
    SBuild sBuildMock = Mockito.mock(SBuild.class);
    Mockito.when(failedBuildInfoMock.getBuild()).thenReturn(sBuildMock);
    Mockito.when(failedBuildInfoMock.shouldDelayAssignments()).thenReturn(true);

    Method getTitleMethod = AggregationLogger.class.getDeclaredMethod("getTitle", FailedBuildInfo.class);
    getTitleMethod.setAccessible(true); // Make private method accessible
    String result = (String)getTitleMethod.invoke(aggregationLogger, failedBuildInfoMock);

    assertEquals(result, "New delayed assignment");
  }


}