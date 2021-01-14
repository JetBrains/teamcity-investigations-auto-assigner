/*
 * Copyright 2000-2021 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.investigationsAutoAssigner;

import java.util.Collections;
import jetbrains.buildServer.investigationsAutoAssigner.persistent.StatisticsReporter;
import jetbrains.buildServer.investigationsAutoAssigner.processing.DelayedAssignmentsProcessor;
import jetbrains.buildServer.investigationsAutoAssigner.processing.FailedTestAndBuildProblemsProcessor;
import jetbrains.buildServer.investigationsAutoAssigner.utils.CustomParameters;
import jetbrains.buildServer.investigationsAutoAssigner.utils.AggregationLogger;
import jetbrains.buildServer.parameters.ParametersProvider;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.impl.auth.SecurityContextImpl;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

@Test
public class FailedTestAndBuildProblemsDispatcherTest {

  private BuildServerListenerEventDispatcher myBsDispatcher;
  private BuildEx myBuild;
  private Branch myBranch;
  private ParametersProvider myParametersProvider;
  private BuildEx mySecondBuild;
  private SRunningBuild myRunningBuild;
  private CustomParameters myCustomParameters;
  private DelayedAssignmentsProcessor myDelayedAssignmentsProcessor;
  private SBuildType mySBuildType;

  @BeforeMethod
  public void setUp() throws Throwable {

    myParametersProvider = mock(ParametersProvider.class);
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
    mySBuildType = mock(SBuildType.class);
    when(mySBuildType.getInternalId()).thenReturn("INTERNAL_iD");
    when(myRunningBuild.getBuildType()).thenReturn(mySBuildType);
    when(myRunningBuild.isPersonal()).thenReturn(false);
    when(myRunningBuild.getParametersProvider()).thenReturn(myParametersProvider);

    //configure security context
    final SecurityContextEx securityContextEx = Mockito.mock(SecurityContextImpl.class);
    Mockito.doCallRealMethod().when(securityContextEx).runAsSystem(any(SecurityContextEx.RunAsActionWithResult.class));
    Mockito.doCallRealMethod().when(securityContextEx).runAs(any(), any(SecurityContextEx.RunAsActionWithResult.class));
    Mockito.doCallRealMethod().when(securityContextEx).runAsSystemUnchecked(any(SecurityContextEx.RunAsActionWithResult.class));
    Mockito.doCallRealMethod().when(securityContextEx).runAsUnchecked(any(), any(SecurityContextEx.RunAsActionWithResult.class));

    //configure event dispatcher
    myBsDispatcher = new BuildServerListenerEventDispatcher(securityContextEx);
    FailedTestAndBuildProblemsProcessor processor = mock(FailedTestAndBuildProblemsProcessor.class);
    myDelayedAssignmentsProcessor = mock(DelayedAssignmentsProcessor.class);

    AggregationLogger aggregationLogger = mock(AggregationLogger.class);
    myCustomParameters = mock(CustomParameters.class);
    when(myCustomParameters.shouldDelayAssignments(any())).thenReturn(false);
    when(myCustomParameters.isBuildFeatureEnabled(any())).thenReturn(true);
    StatisticsReporter statisticsReporter = mock(StatisticsReporter.class);


    new FailedTestAndBuildProblemsDispatcher(myBsDispatcher,
                                             processor,
                                             myDelayedAssignmentsProcessor,
                                             aggregationLogger,
                                             statisticsReporter,
                                             myCustomParameters);

  }

  public void Test_BuildProblemsChanged_PersonalBuildFiltered() {
    when(myBuild.isPersonal()).thenReturn(true);

    myBsDispatcher.getMulticaster().buildProblemsChanged(myBuild, Collections.emptyList(), Collections.emptyList());
    verifyMarkOfPassBuildProblemsChanged(0);
  }

  public void Test_BuildProblemsChanged_FeatureBranchIgnored() {
    when(myBranch.isDefaultBranch()).thenReturn(false);

    myBsDispatcher.getMulticaster().buildProblemsChanged(myBuild, Collections.emptyList(), Collections.emptyList());
    verifyMarkOfPassBuildProblemsChanged(0);
  }

  public void Test_BuildProblemsChanged_NormalBuildAdded() {
    when(myBranch.isDefaultBranch()).thenReturn(true);
    when(myBuild.isPersonal()).thenReturn(false);

    myBsDispatcher.getMulticaster().buildProblemsChanged(myBuild, Collections.emptyList(), Collections.emptyList());
    verifyMarkOfPassBuildProblemsChanged(1);
  }

  public void Test_BuildProblemsChanged_BuildAddsOnlyOnce() {
    myBsDispatcher.getMulticaster().buildProblemsChanged(myBuild, Collections.emptyList(), Collections.emptyList());
    myBsDispatcher.getMulticaster().buildProblemsChanged(myBuild, Collections.emptyList(), Collections.emptyList());
    verifyMarkOfPassBuildProblemsChanged(1);
  }

  public void Test_BuildProblemsChanged_TwoBuilds() {
    myBsDispatcher.getMulticaster().buildProblemsChanged(myBuild, Collections.emptyList(), Collections.emptyList());
    myBsDispatcher.getMulticaster()
                  .buildProblemsChanged(mySecondBuild, Collections.emptyList(), Collections.emptyList());
    verifyMarkOfPassBuildProblemsChanged(2);
  }

  public void Test_BuildFinished_PersonalBuildIgnored() {
    when(myRunningBuild.isPersonal()).thenReturn(true);
    myBsDispatcher.getMulticaster().buildFinished(myRunningBuild);
    verifyMarkOfPassForBuildFinished(0);
  }

  public void Test_BuildFinished_FeatureBranchIgnored() {
    when(myBranch.isDefaultBranch()).thenReturn(false);
    myBsDispatcher.getMulticaster().buildFinished(myRunningBuild);
    verifyMarkOfPassForBuildFinished(0);
  }

  public void Test_BuildFinished_NormalCase() {
    when(myRunningBuild.isPersonal()).thenReturn(false);
    when(myBranch.isDefaultBranch()).thenReturn(true);

    myBsDispatcher.getMulticaster().buildFinished(myRunningBuild);
    verifyMarkOfPassForBuildFinished(1);
  }

  private void verifyMarkOfPassBuildProblemsChanged(int expectedExecutions) {
    await().atMost(1, SECONDS)
           .pollDelay(50, MILLISECONDS)
           .pollInterval(50, MILLISECONDS)
           .until(() -> verifyMarkOfPassBuildProblemsChangedSilent(expectedExecutions));
    verify(myCustomParameters, times(expectedExecutions)).shouldDelayAssignments(any());
  }

  private boolean verifyMarkOfPassBuildProblemsChangedSilent(int expectedExecutions) {
    try {
      verify(myCustomParameters, times(expectedExecutions)).shouldDelayAssignments(any());
      return true;
    } catch (AssertionError err) {
      return false;
    }
  }

  private void verifyMarkOfPassForBuildFinished(int expectedExecutions) {
    await().atMost(1, SECONDS)
           .pollDelay(50, MILLISECONDS)
           .pollInterval(50, MILLISECONDS)
           .until(() -> verifyMarkOfPassForBuildFinishedSilent(expectedExecutions));
    verify(mySBuildType, times(expectedExecutions)).getInternalId();
  }

  private boolean verifyMarkOfPassForBuildFinishedSilent(int expectedExecutions) {
    try {
      verify(mySBuildType, times(expectedExecutions)).getInternalId();
      return true;
    } catch (AssertionError err) {
      return false;
    }
  }
}