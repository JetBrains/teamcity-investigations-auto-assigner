

package jetbrains.buildServer.investigationsAutoAssigner.processing;

import java.util.Collections;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.BuildProblemTypes;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.common.FailedBuildInfo;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.persistent.AssignerArtifactDao;
import jetbrains.buildServer.investigationsAutoAssigner.utils.CustomParameters;
import jetbrains.buildServer.parameters.ParametersProvider;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.impl.problems.BuildProblemImpl;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.users.SUser;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@Test
public class FailedTestAndBuildProblemsProcessorTest extends BaseTestCase {
  private FailedTestAndBuildProblemsProcessor myProcessor;
  private ResponsibleUserFinder myResponsibleUserFinder;
  private BuildEx mySBuild;
  private SBuildType mySBuildType;
  private AssignerArtifactDao myAssignerArtifactDao;
  private FailedBuildInfo myFailedBuildInfo;
  private HeuristicResult myNotEmptyHeuristicResult;
  private FailedTestAssigner myFailedTestAssigner;
  private SUser mySUser;
  private BuildProblemsAssigner myBuildProblemsAssigner;
  private BuildProblemsFilter myBuildProblemsFilter;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myResponsibleUserFinder = Mockito.mock(ResponsibleUserFinder.class);
    final FailedTestFilter failedTestFilter = Mockito.mock(FailedTestFilter.class);
    myFailedTestAssigner = Mockito.mock(FailedTestAssigner.class);
    myBuildProblemsFilter = Mockito.mock(BuildProblemsFilter.class);
    myBuildProblemsAssigner = Mockito.mock(BuildProblemsAssigner.class);
    myAssignerArtifactDao = Mockito.mock(AssignerArtifactDao.class);
    myProcessor = new FailedTestAndBuildProblemsProcessor(myResponsibleUserFinder,
                                                          failedTestFilter,
                                                          myFailedTestAssigner,
                                                          myBuildProblemsFilter,
                                                          myBuildProblemsAssigner,
                                                          myAssignerArtifactDao,
                                                          new CustomParameters());

    //configure tests
    TestName testNameMock = Mockito.mock(TestName.class);
    when(testNameMock.getAsString()).thenReturn("Test Name as String");

    final STest sTestMock = Mockito.mock(jetbrains.buildServer.serverSide.STest.class);
    when(sTestMock.getName()).thenReturn(testNameMock);

    final STestRun sTestRun = Mockito.mock(jetbrains.buildServer.serverSide.STestRun.class);
    when(sTestRun.getTest()).thenReturn(sTestMock);
    when(sTestRun.getFullText()).thenReturn("Full Text Test Run");

    //configure build stats
    BuildStatistics buildStatistics = Mockito.mock(BuildStatistics.class);
    when(buildStatistics.getFailedTests()).thenReturn(Collections.singletonList(sTestRun));

    //configure project
    SProject sProject = Mockito.mock(SProject.class);
    when(sProject.getProjectId()).thenReturn("projectId");

    //configure build type
    mySBuildType = Mockito.mock(SBuildType.class);
    when(mySBuildType.getProject()).thenReturn(sProject);

    //configure parameters provider
    ParametersProvider myParametersProvider = Mockito.mock(ParametersProvider.class);

    //configure build
    mySBuild = Mockito.mock(BuildEx.class);
    when(mySBuild.getBuildType()).thenReturn(mySBuildType);
    when(mySBuild.getBuildStatistics(any())).thenReturn(buildStatistics);
    when(mySBuild.getParametersProvider()).thenReturn(myParametersProvider);
    myFailedBuildInfo = new FailedBuildInfo(mySBuild);

    //configure heuristic results
    myNotEmptyHeuristicResult = new HeuristicResult();
    mySUser = Mockito.mock(SUser.class);
    myNotEmptyHeuristicResult.addResponsibility(sTestRun, new Responsibility(mySUser, "Failed description"));

    //configure finder
    when(myResponsibleUserFinder.findResponsibleUser(any(), any(), anyList(), anyList())).thenReturn(myNotEmptyHeuristicResult);
  }

  public void TestBuildTypeIsNull() {
    when(mySBuild.getBuildType()).thenReturn(null);

    myProcessor.processBuild(myFailedBuildInfo);

    Mockito.verify(mySBuild, Mockito.never()).getBuildProblems();
  }

  public void TestBuildTypeNotNull() {
    when(mySBuild.getBuildType()).thenReturn(mySBuildType);

    myProcessor.processBuild(myFailedBuildInfo);

    Mockito.verify(mySBuild, Mockito.atLeastOnce()).getBuildProblems();
  }

  public void TestAssignerHasRightHeuristicsResult() {
    when(myResponsibleUserFinder.findResponsibleUser(any(), any(), anyList(), anyList()))
      .thenReturn(myNotEmptyHeuristicResult);

    Mockito.doAnswer((Answer<Void>)invocation -> {
      final Object[] args = invocation.getArguments();
      assertEquals(3, args.length);
      assertEquals(args[2], myNotEmptyHeuristicResult);
      return null;
    }).when(myAssignerArtifactDao).appendHeuristicsResult(any(), any(), any());

    myProcessor.processBuild(myFailedBuildInfo);

    Mockito.verify(myAssignerArtifactDao, Mockito.atLeastOnce()).appendHeuristicsResult(any(), any(), any());
  }

  public void TestBuildFeatureNotConfigured() {
    when(mySBuild.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE)).thenReturn(Collections.emptyList());

    myProcessor.processBuild(myFailedBuildInfo);

    Mockito.verify(myFailedTestAssigner, Mockito.never()).assign(any(), any(), any(), anyList());
  }

  public void TestDelayedAssignment() {
    configureBuildFeature(mySBuild, true);
    FailedBuildInfo failedBuildInfo = new FailedBuildInfo(mySBuild);

    myProcessor.processBuild(failedBuildInfo);

    Mockito.verify(myFailedTestAssigner, Mockito.never()).assign(any(), any(), any(), anyList());
  }

  public void TestDelayedAssignmentExitCodeProblem() {
    BuildProblemData buildProblemData = Mockito.mock(BuildProblemData.class);
    when(buildProblemData.getType()).thenReturn(BuildProblemTypes.TC_EXIT_CODE_TYPE);
    BuildProblemImpl exitCodeBuildProblem = Mockito.mock(BuildProblemImpl.class);
    when(exitCodeBuildProblem.getBuildProblemData()).thenReturn(buildProblemData);

    when(mySBuild.getBuildProblems()).thenReturn(Collections.singletonList(exitCodeBuildProblem));
    when(myBuildProblemsFilter.apply(any(), any(), any())).thenReturn(Collections.singletonList(exitCodeBuildProblem));
    when(myBuildProblemsFilter.getStillApplicable(any(), any(), any())).thenReturn(Collections.singletonList(exitCodeBuildProblem));

    configureBuildFeature(mySBuild, true);
    FailedBuildInfo failedBuildInfo = new FailedBuildInfo(mySBuild);
    myNotEmptyHeuristicResult.addResponsibility(exitCodeBuildProblem, new Responsibility(mySUser, "Failed description"));
    myProcessor.processBuild(failedBuildInfo);

    Mockito.verify(myBuildProblemsAssigner, Mockito.never()).assign(any(), any(), any(), anyList());
  }

  public void TestDelayedAssignmentCompileProblem() {
    BuildProblemData buildProblemData = Mockito.mock(BuildProblemData.class);
    when(buildProblemData.getType()).thenReturn(BuildProblemTypes.TC_COMPILATION_ERROR_TYPE);
    BuildProblemImpl compilationBuildProblem = Mockito.mock(BuildProblemImpl.class);
    when(compilationBuildProblem.getBuildProblemData()).thenReturn(buildProblemData);

    when(mySBuild.getBuildProblems()).thenReturn(Collections.singletonList(compilationBuildProblem));
    when(myBuildProblemsFilter.apply(any(), any(), any())).thenReturn(Collections.singletonList(compilationBuildProblem));
    when(myBuildProblemsFilter.getStillApplicable(any(), any(), any())).thenReturn(Collections.singletonList(compilationBuildProblem));

    configureBuildFeature(mySBuild, true);
    FailedBuildInfo failedBuildInfo = new FailedBuildInfo(mySBuild);
    myNotEmptyHeuristicResult.addResponsibility(compilationBuildProblem, new Responsibility(mySUser, "Failed description"));
    myProcessor.processBuild(failedBuildInfo);

    Mockito.verify(myBuildProblemsAssigner, Mockito.atLeastOnce()).assign(any(), any(), any(), anyList());
  }

  public void TestRegularAssignment() {
    configureBuildFeature(mySBuild, false);

    myProcessor.processBuild(myFailedBuildInfo);

    Mockito.verify(myFailedTestAssigner, Mockito.atLeastOnce()).assign(any(), any(), any(), anyList());
  }

  public void TestDefaultAssignmentIsRegular() {
    configureBuildFeature(mySBuild, false);

    myProcessor.processBuild(myFailedBuildInfo);

    Mockito.verify(myFailedTestAssigner, Mockito.atLeastOnce()).assign(any(), any(), any(), anyList());
  }

  private void configureBuildFeature(@NotNull SBuild sBuild, boolean delayedAssignment) {
    SBuildFeatureDescriptor sBuildFeatureDescriptor = Mockito.mock(SBuildFeatureDescriptor.class);
    when(sBuildFeatureDescriptor.getParameters()).thenReturn(Collections.singletonMap(Constants.ASSIGN_ON_SECOND_FAILURE, String.valueOf(delayedAssignment)));
    when(sBuild.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE))
      .thenReturn(Collections.singletonList(sBuildFeatureDescriptor));
  }
}