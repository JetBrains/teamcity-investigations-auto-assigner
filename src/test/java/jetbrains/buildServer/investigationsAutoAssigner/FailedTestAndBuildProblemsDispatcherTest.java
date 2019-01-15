/*
 * Copyright 2000-2019 JetBrains s.r.o.
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
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.persistent.StatisticsReporter;
import jetbrains.buildServer.investigationsAutoAssigner.processing.DelayedAssignmentsProcessor;
import jetbrains.buildServer.investigationsAutoAssigner.processing.FailedTestAndBuildProblemsProcessor;
import jetbrains.buildServer.investigationsAutoAssigner.utils.EmailReporter;
import jetbrains.buildServer.parameters.ParametersProvider;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.impl.auth.SecurityContextImpl;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;

@Test
public class FailedTestAndBuildProblemsDispatcherTest {

  private BuildServerListenerEventDispatcher myBsDispatcher;
  private BuildEx myBuild;
  private Branch myBranch;
  private ParametersProvider myParametersProvider;
  private BuildEx mySecondBuild;
  private SRunningBuild myRunningBuild;

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
    when(myRunningBuild.getBuildType()).thenReturn(mock(SBuildType.class));
    when(myRunningBuild.isPersonal()).thenReturn(false);
    when(myRunningBuild.getParametersProvider()).thenReturn(myParametersProvider);

    //configure security context
    final SecurityContextEx securityContextEx = Mockito.mock(SecurityContextImpl.class);
    Mockito.doCallRealMethod().when(securityContextEx).runAsSystem(any(SecurityContextEx.RunAsActionWithResult.class));
    Mockito.doCallRealMethod().when(securityContextEx).runAs(any(), any(SecurityContextEx.RunAsActionWithResult.class));

    //configure event dispatcher
    myBsDispatcher = new BuildServerListenerEventDispatcher(securityContextEx);
    FailedTestAndBuildProblemsProcessor processor = mock(FailedTestAndBuildProblemsProcessor.class);
    DelayedAssignmentsProcessor delayedAssignmentsProcessor = mock(DelayedAssignmentsProcessor.class);

    EmailReporter emailReporter = mock(EmailReporter.class);
    StatisticsReporter sr = mock(StatisticsReporter.class);

    new FailedTestAndBuildProblemsDispatcher(myBsDispatcher, processor, delayedAssignmentsProcessor, emailReporter, sr);
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
    verify(myParametersProvider, times(expectedExecutions)).get(Constants.SHOULD_DELAY_ASSIGNMENTS);
  }

  private void verifyMarkOfPassForBuildFinished(int expectedExecutions) {
    verify(myRunningBuild, times(expectedExecutions)).getBuildId();
  }
}