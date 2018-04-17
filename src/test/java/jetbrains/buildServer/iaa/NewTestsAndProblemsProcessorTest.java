/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

package jetbrains.buildServer.iaa;

import com.intellij.openapi.util.Pair;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.responsibility.BuildProblemResponsibilityFacade;
import jetbrains.buildServer.responsibility.TestNameResponsibilityFacade;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.impl.problems.BuildProblemImpl;
import jetbrains.buildServer.serverSide.problems.BuildProblemInfo;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.users.SUser;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Test
public class NewTestsAndProblemsProcessorTest extends BaseTestCase {
  private NewTestsAndProblemsProcessorImpl processor;
  private TestNameResponsibilityFacade testNameResponsibilityFacade;
  private BuildProblemResponsibilityFacade buildProblemResponsibilityFacade;
  private ResponsibleUserFinder responsibleUserFinder;
  private TestApplicabilityChecker testApplicabilityChecker;
  private BuildApplicabilityChecker buildApplicabilityChecker;
  private SRunningBuild sRunningBuild;
  private SBuild sBuild;
  private STestRun sTestRun;
  private SBuildType sBuildType;
  private STest sTest;
  private BuildProblemImpl buildProblem;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    testNameResponsibilityFacade = Mockito.mock(TestNameResponsibilityFacade.class);
    buildProblemResponsibilityFacade = Mockito.mock(BuildProblemResponsibilityFacade.class);
    responsibleUserFinder = Mockito.mock(ResponsibleUserFinder.class);
    testApplicabilityChecker = Mockito.mock(TestApplicabilityChecker.class);
    buildApplicabilityChecker = Mockito.mock(BuildApplicabilityChecker.class);
    processor = new NewTestsAndProblemsProcessorImpl(testNameResponsibilityFacade, buildProblemResponsibilityFacade,
                                                     responsibleUserFinder, testApplicabilityChecker,
                                                     buildApplicabilityChecker);

    sRunningBuild = Mockito.mock(SRunningBuild.class);
    sBuild = Mockito.mock(SBuild.class);
    buildProblem = Mockito.mock(BuildProblemImpl.class);
    BuildProblemData buildProblemData = Mockito.mock(BuildProblemData.class);
    sTestRun = Mockito.mock(STestRun.class);
    sBuildType = Mockito.mock(SBuildType.class);
    SProject sProject = Mockito.mock(SProject.class);
    sTest = Mockito.mock(STest.class);
    TestName testName = Mockito.mock(TestName.class);
    SUser sUser = Mockito.mock(SUser.class);

    when(sRunningBuild.getBuildType()).thenReturn(sBuildType);
    when(sBuild.getBuildType()).thenReturn(sBuildType);
    when(sBuildType.getProject()).thenReturn(sProject);
    when(sProject.getProjectId()).thenReturn("projectId");
    when(sTestRun.getTest()).thenReturn(sTest);
    when(sTestRun.getFullText()).thenReturn("Full Text Test Run");
    when(sTest.getName()).thenReturn(testName);
    when(testApplicabilityChecker.check(any(), any())).thenReturn(true);
    when(buildApplicabilityChecker.check(any(), any())).thenReturn(true);
    when(testName.getAsString()).thenReturn("Test Name as String");
    Pair<SUser, String> anyPair = new Pair<>(sUser, "Failed description");
    when(responsibleUserFinder.findResponsibleUser(any(), any())).thenReturn(anyPair);
    when(buildProblem.getBuildProblemData()).thenReturn(buildProblemData);
    when(buildProblemData.getType()).thenReturn("1234");
  }

  public void Test_OnTestFailed_BuildTypeIsNull() {
    when(sRunningBuild.getBuildType()).thenReturn(null);

    processor.onTestFailed(sRunningBuild, sTestRun);

    Mockito.verify(sTestRun, Mockito.never()).getTest();
  }

  public void Test_OnTestFailed_BuildTypeNotNull() {
    when(sRunningBuild.getBuildType()).thenReturn(sBuildType);

    processor.onTestFailed(sRunningBuild, sTestRun);

    Mockito.verify(sTestRun, Mockito.atLeastOnce()).getTest();
  }

  public void Tes_OnTestFailed_ApplicabilityFailed() {
    when(testApplicabilityChecker.check(any(), any())).thenReturn(false);

    processor.onTestFailed(sRunningBuild, sTestRun);

    Mockito.verify(sTest, Mockito.never()).getName();
  }

  public void Test_OnTestFailed_ApplicabilitySucceed() {
    when(testApplicabilityChecker.check(any(), any())).thenReturn(true);

    processor.onTestFailed(sRunningBuild, sTestRun);

    Mockito.verify(sTest, Mockito.atLeastOnce()).getName();
  }

  public void Test_OnTestFailed_ResponsibleUserNotFound() {
    when(responsibleUserFinder.findResponsibleUser(any(), any())).thenReturn(null);

    processor.onTestFailed(sRunningBuild, sTestRun);

    Mockito.verify(testNameResponsibilityFacade, Mockito.never())
           .setTestNameResponsibility(any(TestName.class), anyString(), any());
  }

  public void Test_OnTestFailed_ResponsibleUserFound() {
    SUser sUser = Mockito.mock(SUser.class);
    Pair<SUser, String> anyPair = new Pair<>(sUser, "Failed description");
    when(responsibleUserFinder.findResponsibleUser(any(), any())).thenReturn(anyPair);

    processor.onTestFailed(sRunningBuild, sTestRun);

    Mockito.verify(testNameResponsibilityFacade, Mockito.atLeastOnce())
           .setTestNameResponsibility(any(TestName.class), any(), any());
  }

  public void Test_BuildProblemOccurred_BuildTypeIsNull() {
    when(sBuild.getBuildType()).thenReturn(null);

    processor.onBuildProblemOccurred(sBuild, buildProblem);

    Mockito.verify(buildApplicabilityChecker, Mockito.never()).check(any(), any());
  }

  public void Test_BuildProblemOccurred_BuildTypeNotNull() {
    when(sBuild.getBuildType()).thenReturn(sBuildType);

    processor.onBuildProblemOccurred(sBuild, buildProblem);

    Mockito.verify(buildApplicabilityChecker, Mockito.atLeastOnce()).check(any(), any());
  }

  public void Test_BuildProblemOccurred_ApplicabilityFailed() {
    when(buildApplicabilityChecker.check(any(), any())).thenReturn(false);

    processor.onBuildProblemOccurred(sBuild, buildProblem);

    Mockito.verify(responsibleUserFinder, Mockito.never()).findResponsibleUser(any(), any());
  }

  public void Test_BuildProblemOccurred_ApplicabilitySucceed() {
    when(buildApplicabilityChecker.check(any(), any())).thenReturn(true);

    processor.onBuildProblemOccurred(sBuild, buildProblem);

    Mockito.verify(responsibleUserFinder, Mockito.atLeastOnce()).findResponsibleUser(any(), any());
  }

  public void Test_BuildProblemOccurred_ResponsibleUserNotFound() {
    when(responsibleUserFinder.findResponsibleUser(any(), any())).thenReturn(null);

    processor.onBuildProblemOccurred(sBuild, buildProblem);

    Mockito.verify(testNameResponsibilityFacade, Mockito.never())
           .setTestNameResponsibility(any(TestName.class), anyString(), any());
  }

  public void Test_BuildProblemOccurred_ResponsibleUserFound() {
    SUser sUser = Mockito.mock(SUser.class);
    Pair<SUser, String> anyPair = new Pair<>(sUser, "Failed description");
    when(responsibleUserFinder.findResponsibleUser(any(), any())).thenReturn(anyPair);

    processor.onBuildProblemOccurred(sBuild, buildProblem);

    Mockito.verify(buildProblemResponsibilityFacade, Mockito.atLeastOnce())
           .setBuildProblemResponsibility(any(BuildProblemInfo.class), any(), any());
  }
}
