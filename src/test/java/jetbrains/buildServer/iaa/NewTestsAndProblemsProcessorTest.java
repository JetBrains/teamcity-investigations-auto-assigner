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
import jetbrains.buildServer.users.User;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Test
public class NewTestsAndProblemsProcessorTest extends BaseTestCase {
  private NewTestsAndProblemsProcessorImpl myProcessor;
  private TestNameResponsibilityFacade myTestNameResponsibilityFacade;
  private BuildProblemResponsibilityFacade myBuildProblemResponsibilityFacade;
  private ResponsibleUserFinder myResponsibleUserFinder;
  private BuildApplicabilityChecker myBuildApplicabilityChecker;
  private SRunningBuild mySRunningBuild;
  private SBuild mySBuild;
  private STestRun mySTestRun;
  private SBuildType mySBuildType;
  private BuildProblemImpl myBuildProblem;
  private TestProblemInfo myTestProblemInfo;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myTestNameResponsibilityFacade = Mockito.mock(TestNameResponsibilityFacade.class);
    myBuildProblemResponsibilityFacade = Mockito.mock(BuildProblemResponsibilityFacade.class);
    myResponsibleUserFinder = Mockito.mock(ResponsibleUserFinder.class);
    myBuildApplicabilityChecker = Mockito.mock(BuildApplicabilityChecker.class);
    myProcessor =
      new NewTestsAndProblemsProcessorImpl(myTestNameResponsibilityFacade, myBuildProblemResponsibilityFacade,
                                           myResponsibleUserFinder,
                                           myBuildApplicabilityChecker);

    mySRunningBuild = Mockito.mock(SRunningBuild.class);
    mySBuild = Mockito.mock(SBuild.class);
    myBuildProblem = Mockito.mock(BuildProblemImpl.class);
    BuildProblemData buildProblemData = Mockito.mock(BuildProblemData.class);
    mySTestRun = Mockito.mock(STestRun.class);
    mySBuildType = Mockito.mock(SBuildType.class);
    myTestProblemInfo = Mockito.mock(TestProblemInfo.class);
    SProject sProject = Mockito.mock(SProject.class);
    final STest STest = Mockito.mock(jetbrains.buildServer.serverSide.STest.class);
    TestName testName = Mockito.mock(TestName.class);
    SUser sUser = Mockito.mock(SUser.class);

    when(mySRunningBuild.getBuildType()).thenReturn(mySBuildType);
    when(mySBuild.getBuildType()).thenReturn(mySBuildType);
    when(mySBuildType.getProject()).thenReturn(sProject);
    when(sProject.getProjectId()).thenReturn("projectId");
    when(mySTestRun.getTest()).thenReturn(STest);
    when(mySTestRun.getFullText()).thenReturn("Full Text Test Run");
    when(STest.getName()).thenReturn(testName);
    when(myBuildApplicabilityChecker.isApplicable(any(), any(), any())).thenReturn(true);
    when(testName.getAsString()).thenReturn("Test Name as String");
    Pair<User, String> anyPair = new Pair<>(sUser, "Failed description");
    //when(myResponsibleUserFinder.findResponsibleUser(any()));
    when(myBuildProblem.getBuildProblemData()).thenReturn(buildProblemData);
    when(buildProblemData.getType()).thenReturn("1234");
  }

  public void Test_OnTestFailed_BuildTypeIsNull() {
    when(mySRunningBuild.getBuildType()).thenReturn(null);

    //myProcessor.processFailedTest(mySRunningBuild, mySTestRun, myTestProblemInfo);

    Mockito.verify(mySTestRun, Mockito.never()).getTest();
  }

  public void Test_OnTestFailed_BuildTypeNotNull() {
    when(mySRunningBuild.getBuildType()).thenReturn(mySBuildType);

   // myProcessor.processFailedTest(mySRunningBuild, mySTestRun, myTestProblemInfo);

    Mockito.verify(mySTestRun, Mockito.atLeastOnce()).getTest();
  }

  public void Test_OnTestFailed_ResponsibleUserNotFound() {
   // when(myResponsibleUserFinder.findResponsibleUser(any())).thenReturn(null);

   // myProcessor.processFailedTest(mySRunningBuild, mySTestRun, myTestProblemInfo);

    Mockito.verify(myTestNameResponsibilityFacade, Mockito.never())
           .setTestNameResponsibility(any(TestName.class), anyString(), any());
  }

  public void Test_OnTestFailed_ResponsibleUserFound() {
    SUser sUser = Mockito.mock(SUser.class);
    Pair<User, String> anyPair = new Pair<>(sUser, "Failed description");
    //when(myResponsibleUserFinder.findResponsibleUser(any())).thenReturn(anyPair);

    //myProcessor.processFailedTest(mySRunningBuild, mySTestRun, myTestProblemInfo);

    Mockito.verify(myTestNameResponsibilityFacade, Mockito.atLeastOnce())
           .setTestNameResponsibility(any(TestName.class), any(), any());
  }

  public void Test_BuildProblemOccurred_BuildTypeIsNull() {
    when(mySBuild.getBuildType()).thenReturn(null);

    myProcessor.onBuildProblemOccurred(mySBuild, myBuildProblem);

    Mockito.verify(myBuildApplicabilityChecker, Mockito.never()).isApplicable(any(), any(), any());
  }

  public void Test_BuildProblemOccurred_BuildTypeNotNull() {
    when(mySBuild.getBuildType()).thenReturn(mySBuildType);

    myProcessor.onBuildProblemOccurred(mySBuild, myBuildProblem);

    Mockito.verify(myBuildApplicabilityChecker, Mockito.atLeastOnce()).isApplicable(any(), any(), any());
  }

  public void Test_BuildProblemOccurred_ApplicabilityFailed() {
    when(myBuildApplicabilityChecker.isApplicable(any(), any(), any())).thenReturn(false);

    myProcessor.onBuildProblemOccurred(mySBuild, myBuildProblem);

    Mockito.verify(myResponsibleUserFinder, Mockito.never()).findResponsibleUser(any());
  }

  public void Test_BuildProblemOccurred_ApplicabilitySucceed() {
    when(myBuildApplicabilityChecker.isApplicable(any(), any(), any())).thenReturn(true);

    myProcessor.onBuildProblemOccurred(mySBuild, myBuildProblem);

    Mockito.verify(myResponsibleUserFinder, Mockito.atLeastOnce()).findResponsibleUser(any());
  }

  public void Test_BuildProblemOccurred_ResponsibleUserNotFound() {
  //  when(myResponsibleUserFinder.findResponsibleUser(any())).thenReturn(null);

    myProcessor.onBuildProblemOccurred(mySBuild, myBuildProblem);

    Mockito.verify(myTestNameResponsibilityFacade, Mockito.never())
           .setTestNameResponsibility(any(TestName.class), anyString(), any());
  }

  public void Test_BuildProblemOccurred_ResponsibleUserFound() {
    SUser sUser = Mockito.mock(SUser.class);
    Pair<User, String> anyPair = new Pair<>(sUser, "Failed description");
  //  when(myResponsibleUserFinder.findResponsibleUser(any())).thenReturn(anyPair);

    myProcessor.onBuildProblemOccurred(mySBuild, myBuildProblem);

    Mockito.verify(myBuildProblemResponsibilityFacade, Mockito.atLeastOnce())
           .setBuildProblemResponsibility(any(BuildProblemInfo.class), any(), any());
  }
}
