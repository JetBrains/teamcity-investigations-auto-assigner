

package jetbrains.buildServer.investigationsAutoAssigner;

import java.util.Collections;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.persistent.StatisticsReporter;
import jetbrains.buildServer.investigationsAutoAssigner.processing.DelayedAssignmentsProcessor;
import jetbrains.buildServer.investigationsAutoAssigner.processing.FailedTestAndBuildProblemsProcessor;
import jetbrains.buildServer.investigationsAutoAssigner.utils.CustomParameters;
import jetbrains.buildServer.investigationsAutoAssigner.utils.AggregationLogger;
import jetbrains.buildServer.parameters.ParametersProvider;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.impl.auth.SecurityContextImpl;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

@Test
public class FailedTestAndBuildProblemsDispatcherTest {

  private BuildServerListenerEventDispatcher myBsDispatcher;
  private BuildEx myBuild;
  private BuildEx mySecondBuild;
  private Branch myBranch;
  private SRunningBuild myRunningBuild;
  private FailedTestAndBuildProblemsDispatcher myDispatcher;


  @BeforeMethod
  public void setUp() throws Throwable {

    ParametersProvider myParametersProvider = mock(ParametersProvider.class);
    myBranch = mock(Branch.class);
    when(myBranch.isDefaultBranch()).thenReturn(true);

    //configure build
    myBuild = mock(BuildEx.class);
    when(myBuild.getBuildId()).thenReturn(239L);
    when(myBuild.getBranch()).thenReturn(myBranch);
    when(myBuild.getBuildType()).thenReturn(mock(SBuildType.class));
    when(myBuild.isPersonal()).thenReturn(false);
    when(myBuild.getParametersProvider()).thenReturn(myParametersProvider);

    //configure second build
    mySecondBuild = mock(BuildEx.class);
    when(mySecondBuild.getBuildId()).thenReturn(238L);
    when(mySecondBuild.getBranch()).thenReturn(myBranch);
    when(mySecondBuild.getBuildType()).thenReturn(mock(SBuildType.class));
    when(mySecondBuild.isPersonal()).thenReturn(false);
    when(mySecondBuild.getParametersProvider()).thenReturn(myParametersProvider);

    //configure running build
    myRunningBuild = mock(SRunningBuild.class);
    when(myRunningBuild.getBuildId()).thenReturn(239L);
    when(myRunningBuild.getBranch()).thenReturn(myBranch);
    SBuildType mySBuildType = mock(SBuildType.class);
    when(mySBuildType.getInternalId()).thenReturn("INTERNAL_iD");
    when(myRunningBuild.getBuildType()).thenReturn(mySBuildType);
    when(myRunningBuild.isPersonal()).thenReturn(false);
    when(myRunningBuild.getParametersProvider()).thenReturn(myParametersProvider);

    //configure security context
    final SecurityContextEx securityContextEx = Mockito.mock(SecurityContextImpl.class);
    Mockito.doCallRealMethod().when(securityContextEx).runAsSystem(ArgumentMatchers.<SecurityContextEx.RunAsActionWithResult<Object>>any());
    Mockito.doCallRealMethod().when(securityContextEx).runAs(any(), ArgumentMatchers.<SecurityContextEx.RunAsActionWithResult<Object>>any());
    Mockito.doCallRealMethod().when(securityContextEx).runAsSystemUnchecked(ArgumentMatchers.<SecurityContextEx.RunAsActionWithResult<Object>>any());
    Mockito.doCallRealMethod().when(securityContextEx).runAsUnchecked(any(), ArgumentMatchers.<SecurityContextEx.RunAsActionWithResult<Object>>any());

    //configure event dispatcher
    myBsDispatcher = new BuildServerListenerEventDispatcher(securityContextEx);
    FailedTestAndBuildProblemsProcessor processor = mock(FailedTestAndBuildProblemsProcessor.class);
    DelayedAssignmentsProcessor myDelayedAssignmentsProcessor = mock(DelayedAssignmentsProcessor.class);

    AggregationLogger aggregationLogger = mock(AggregationLogger.class);
    CustomParameters myCustomParameters = mock(CustomParameters.class);
    when(myCustomParameters.isBuildFeatureEnabled(any())).thenReturn(true);
    StatisticsReporter statisticsReporter = mock(StatisticsReporter.class);

    ServerResponsibility serverResponsibility = mock(ServerResponsibility.class);
    when(serverResponsibility.canSendNotifications()).thenReturn(true);

    BuildsManager myBuildsManager = mock(BuildsManager.class);
    when(myBuildsManager.findBuildInstanceById(239L)).thenReturn(myRunningBuild);
    when(myBuildsManager.findBuildInstanceById(238L)).thenReturn(mySecondBuild);

    SBuildFeatureDescriptor sBuildFeatureDescriptor = Mockito.mock(SBuildFeatureDescriptor.class);
    when(sBuildFeatureDescriptor.getParameters()).thenReturn(Collections.singletonMap(Constants.ASSIGN_ON_SECOND_FAILURE, "false"));
    when(myBuild.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE))
      .thenReturn(Collections.singletonList(sBuildFeatureDescriptor));
    when(myRunningBuild.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE))
      .thenReturn(Collections.singletonList(sBuildFeatureDescriptor));
    when(mySecondBuild.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE))
      .thenReturn(Collections.singletonList(sBuildFeatureDescriptor));

    myDispatcher =
      new FailedTestAndBuildProblemsDispatcher(myBsDispatcher,
                                               processor,
                                               myDelayedAssignmentsProcessor,
                                               aggregationLogger,
                                               statisticsReporter,
                                               myCustomParameters,
                                               myBuildsManager,
                                               serverResponsibility);

  }

  public void test_BuildProblemsChanged_PersonalBuildFiltered() {
    when(myBuild.isPersonal()).thenReturn(true);

    myBsDispatcher.getMulticaster().buildProblemsChanged(myBuild, Collections.emptyList(), Collections.emptyList());
    assertTrue(myDispatcher.getRememberedFailedBuilds().isEmpty());
  }

  public void test_BuildProblemsChanged_FeatureBranchIgnored() {
    when(myBranch.isDefaultBranch()).thenReturn(false);

    myBsDispatcher.getMulticaster().buildProblemsChanged(myBuild, Collections.emptyList(), Collections.emptyList());
    assertTrue(myDispatcher.getRememberedFailedBuilds().isEmpty());
  }

  public void test_BuildProblemsChanged_NormalBuildAdded() {
    when(myBranch.isDefaultBranch()).thenReturn(true);
    when(myBuild.isPersonal()).thenReturn(false);

    myBsDispatcher.getMulticaster().buildProblemsChanged(myBuild, Collections.emptyList(), Collections.emptyList());
    assertFalse(myDispatcher.getRememberedFailedBuilds().isEmpty());
  }

  public void test_BuildProblemsChanged_BuildAddsOnlyOnce() {
    myBsDispatcher.getMulticaster().buildProblemsChanged(myBuild, Collections.emptyList(), Collections.emptyList());
    myBsDispatcher.getMulticaster().buildProblemsChanged(myBuild, Collections.emptyList(), Collections.emptyList());
    assertFalse(myDispatcher.getRememberedFailedBuilds().isEmpty());
  }

  public void test_BuildProblemsChanged_TwoBuilds() {
    myBsDispatcher.getMulticaster().buildProblemsChanged(myBuild, Collections.emptyList(), Collections.emptyList());
    myBsDispatcher.getMulticaster()
                  .buildProblemsChanged(mySecondBuild, Collections.emptyList(), Collections.emptyList());
    assertEquals(myDispatcher.getRememberedFailedBuilds().size(), 2);
  }

  public void test_BuildFinished_PersonalBuildIgnored() {
    when(myRunningBuild.isPersonal()).thenReturn(true);
    myBsDispatcher.getMulticaster().buildFinished(myRunningBuild);
    assertTrue(myDispatcher.getRememberedFailedBuilds().isEmpty());
  }

  public void test_BuildFinished_FeatureBranchIgnored() {
    when(myBranch.isDefaultBranch()).thenReturn(false);
    myBsDispatcher.getMulticaster().buildFinished(myRunningBuild);
    assertTrue(myDispatcher.getRememberedFailedBuilds().isEmpty());
  }

  public void test_BuildFinished_NormalCase() {
    when(myRunningBuild.isPersonal()).thenReturn(false);
    when(myBranch.isDefaultBranch()).thenReturn(true);

    myBsDispatcher.getMulticaster().buildProblemsChanged(myRunningBuild, Collections.emptyList(), Collections.emptyList());
    assertFalse(myDispatcher.getRememberedFailedBuilds().isEmpty());
    myBsDispatcher.getMulticaster().buildFinished(myRunningBuild);
    assertTrue(myDispatcher.getRememberedFailedBuilds().isEmpty());
  }

}