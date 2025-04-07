

package jetbrains.buildServer.investigationsAutoAssigner;

import java.util.Collections;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.persistent.StatisticsReporter;
import jetbrains.buildServer.investigationsAutoAssigner.processing.DelayedAssignmentsProcessor;
import jetbrains.buildServer.investigationsAutoAssigner.processing.FailedTestAndBuildProblemsProcessor;
import jetbrains.buildServer.investigationsAutoAssigner.utils.AggregationLogger;
import jetbrains.buildServer.parameters.ParametersProvider;
import jetbrains.buildServer.serverSide.Branch;
import jetbrains.buildServer.serverSide.BuildEx;
import jetbrains.buildServer.serverSide.BuildServerListenerEventDispatcher;
import jetbrains.buildServer.serverSide.BuildsManager;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.SecurityContextEx;
import jetbrains.buildServer.serverSide.ServerResponsibility;
import jetbrains.buildServer.serverSide.impl.auth.SecurityContextImpl;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Test
public class FailedTestAndBuildProblemsDispatcherTest {

  private BuildServerListenerEventDispatcher myBsDispatcher;
  private BuildEx myBuild;
  private Branch myBranch;
  private BuildEx mySecondBuild;
  private SRunningBuild myRunningBuild;
  private FailedTestAndBuildProblemsDispatcher myDispatcher;

  @BeforeMethod
  public void setUp() throws Throwable {

    ParametersProvider parametersProvider = mock(ParametersProvider.class);
    myBranch = mock(Branch.class);
    when(myBranch.isDefaultBranch()).thenReturn(true);

    //configure build
    myBuild = mock(BuildEx.class);
    when(myBuild.getBuildId()).thenReturn(239L);
    when(myBuild.getBranch()).thenReturn(myBranch);
    when(myBuild.getBuildType()).thenReturn(mock(SBuildType.class));
    when(myBuild.isPersonal()).thenReturn(false);
    when(myBuild.getParametersProvider()).thenReturn(parametersProvider);

    //configure second build
    mySecondBuild = mock(BuildEx.class);
    when(mySecondBuild.getBuildId()).thenReturn(238L);
    when(mySecondBuild.getBranch()).thenReturn(myBranch);
    when(mySecondBuild.getBuildType()).thenReturn(mock(SBuildType.class));
    when(mySecondBuild.isPersonal()).thenReturn(false);
    when(mySecondBuild.getParametersProvider()).thenReturn(parametersProvider);

    //configure running build
    myRunningBuild = mock(SRunningBuild.class);
    when(myRunningBuild.getBuildId()).thenReturn(239L);
    when(myRunningBuild.getBranch()).thenReturn(myBranch);
    SBuildType SBuildType = mock(jetbrains.buildServer.serverSide.SBuildType.class);
    when(SBuildType.getInternalId()).thenReturn("INTERNAL_iD");
    when(myRunningBuild.getBuildType()).thenReturn(SBuildType);
    when(myRunningBuild.isPersonal()).thenReturn(false);
    when(myRunningBuild.getParametersProvider()).thenReturn(parametersProvider);

    //configure security context
    final SecurityContextEx securityContextEx = Mockito.mock(SecurityContextImpl.class);
    Mockito.doCallRealMethod().when(securityContextEx).runAsSystem(any(SecurityContextEx.RunAsActionWithResult.class));
    Mockito.doCallRealMethod().when(securityContextEx).runAs(any(), any(SecurityContextEx.RunAsActionWithResult.class));
    Mockito.doCallRealMethod().when(securityContextEx).runAsSystemUnchecked(any(SecurityContextEx.RunAsActionWithResult.class));
    Mockito.doCallRealMethod().when(securityContextEx).runAsUnchecked(any(), any(SecurityContextEx.RunAsActionWithResult.class));

    //configure event dispatcher
    myBsDispatcher = new BuildServerListenerEventDispatcher(securityContextEx);
    FailedTestAndBuildProblemsProcessor processor = mock(FailedTestAndBuildProblemsProcessor.class);
    DelayedAssignmentsProcessor delayedAssignmentsProcessor = mock(DelayedAssignmentsProcessor.class);

    AggregationLogger aggregationLogger = mock(AggregationLogger.class);
    StatisticsReporter statisticsReporter = mock(StatisticsReporter.class);

    ServerResponsibility serverResponsibility = mock(ServerResponsibility.class);
    when(serverResponsibility.canSendNotifications()).thenReturn(true);

    BuildsManager buildsManager = mock(BuildsManager.class);
    when(buildsManager.findBuildInstanceById(239L)).thenReturn(myRunningBuild);
    when(buildsManager.findBuildInstanceById(238L)).thenReturn(mySecondBuild);

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
                                               delayedAssignmentsProcessor,
                                               aggregationLogger,
                                               statisticsReporter,
                                               buildsManager,
                                               serverResponsibility);

  }

  public void Test_BuildProblemsChanged_PersonalBuildFiltered() {
    when(myBuild.isPersonal()).thenReturn(true);

    myBsDispatcher.getMulticaster().buildProblemsChanged(myBuild, Collections.emptyList(), Collections.emptyList());
    assertTrue(myDispatcher.getRememberedFailedBuilds().isEmpty());
  }

  public void Test_BuildProblemsChanged_FeatureBranchIgnored() {
    when(myBranch.isDefaultBranch()).thenReturn(false);

    myBsDispatcher.getMulticaster().buildProblemsChanged(myBuild, Collections.emptyList(), Collections.emptyList());
    assertTrue(myDispatcher.getRememberedFailedBuilds().isEmpty());
  }

  public void Test_BuildProblemsChanged_NormalBuildAdded() {
    when(myBranch.isDefaultBranch()).thenReturn(true);
    when(myBuild.isPersonal()).thenReturn(false);

    myBsDispatcher.getMulticaster().buildProblemsChanged(myBuild, Collections.emptyList(), Collections.emptyList());
    assertFalse(myDispatcher.getRememberedFailedBuilds().isEmpty());
  }

  public void Test_BuildProblemsChanged_BuildAddsOnlyOnce() {
    myBsDispatcher.getMulticaster().buildProblemsChanged(myBuild, Collections.emptyList(), Collections.emptyList());
    myBsDispatcher.getMulticaster().buildProblemsChanged(myBuild, Collections.emptyList(), Collections.emptyList());
    assertFalse(myDispatcher.getRememberedFailedBuilds().isEmpty());
  }

  public void Test_BuildProblemsChanged_TwoBuilds() {
    myBsDispatcher.getMulticaster().buildProblemsChanged(myBuild, Collections.emptyList(), Collections.emptyList());
    myBsDispatcher.getMulticaster()
                  .buildProblemsChanged(mySecondBuild, Collections.emptyList(), Collections.emptyList());
    assertEquals(2, myDispatcher.getRememberedFailedBuilds().size());
  }

  public void Test_BuildFinished_PersonalBuildIgnored() {
    when(myRunningBuild.isPersonal()).thenReturn(true);
    myBsDispatcher.getMulticaster().buildFinished(myRunningBuild);
    assertTrue(myDispatcher.getRememberedFailedBuilds().isEmpty());
  }

  public void Test_BuildFinished_FeatureBranchIgnored() {
    when(myBranch.isDefaultBranch()).thenReturn(false);
    myBsDispatcher.getMulticaster().buildFinished(myRunningBuild);
    assertTrue(myDispatcher.getRememberedFailedBuilds().isEmpty());
  }

  public void Test_BuildFinished_NormalCase() {
    when(myRunningBuild.isPersonal()).thenReturn(false);
    when(myBranch.isDefaultBranch()).thenReturn(true);

    myBsDispatcher.getMulticaster().buildProblemsChanged(myRunningBuild, Collections.emptyList(), Collections.emptyList());
    assertFalse(myDispatcher.getRememberedFailedBuilds().isEmpty());
    myBsDispatcher.getMulticaster().buildFinished(myRunningBuild);
    assertTrue(myDispatcher.getRememberedFailedBuilds().isEmpty());
  }

}