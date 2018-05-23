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

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.iaa.common.HeuristicResult;
import jetbrains.buildServer.iaa.common.Responsibility;
import jetbrains.buildServer.iaa.processing.FailedTestAndBuildProblemsProcessor;
import jetbrains.buildServer.iaa.processing.ResponsibleUserFinder;
import jetbrains.buildServer.responsibility.BuildProblemResponsibilityFacade;
import jetbrains.buildServer.responsibility.TestNameResponsibilityFacade;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.impl.problems.BuildProblemImpl;
import jetbrains.buildServer.serverSide.problems.BuildProblemInfo;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.users.SUser;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@Test
public class NewTestsAndFailedTestAndBuildProblemsProcessorTest extends BaseTestCase {
  private FailedTestAndBuildProblemsProcessor myProcessor;
  private TestNameResponsibilityFacade myTestNameResponsibilityFacade;
  private BuildProblemResponsibilityFacade myBuildProblemResponsibilityFacade;
  private ResponsibleUserFinder myResponsibleUserFinder;
  private BuildApplicabilityChecker myBuildApplicabilityChecker;
  private SRunningBuild mySRunningBuild;
  private SBuild mySBuild;
  private STestRun mySTestRun;
  private SBuildType mySBuildType;
  private BuildProblemImpl myBuildProblem;
  private TestApplicabilityChecker myTestApplicabilityChecker;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myTestNameResponsibilityFacade = Mockito.mock(TestNameResponsibilityFacade.class);
    myBuildProblemResponsibilityFacade = Mockito.mock(BuildProblemResponsibilityFacade.class);
    myResponsibleUserFinder = Mockito.mock(ResponsibleUserFinder.class);
    myBuildApplicabilityChecker = Mockito.mock(BuildApplicabilityChecker.class);
    myTestApplicabilityChecker = Mockito.mock(TestApplicabilityChecker.class);
    myProcessor = new FailedTestAndBuildProblemsProcessor(myTestNameResponsibilityFacade,
                                                          myBuildProblemResponsibilityFacade,
                                                          myResponsibleUserFinder,
                                                          myBuildApplicabilityChecker,
                                                          myTestApplicabilityChecker);
    final HeuristicResult heuristicsResult = new HeuristicResult();
    when(myResponsibleUserFinder.findResponsibleUser(any(), anyList(), anyList())).thenReturn(heuristicsResult);
    mySRunningBuild = Mockito.mock(SRunningBuild.class);
    mySBuild = Mockito.mock(SBuild.class);
    myBuildProblem = Mockito.mock(BuildProblemImpl.class);
    BuildProblemData buildProblemData = Mockito.mock(BuildProblemData.class);
    mySTestRun = Mockito.mock(STestRun.class);
    mySBuildType = Mockito.mock(SBuildType.class);
    SProject sProject = Mockito.mock(SProject.class);
    final STest STest = Mockito.mock(jetbrains.buildServer.serverSide.STest.class);
    TestName testName = Mockito.mock(TestName.class);

    when(mySRunningBuild.getBuildType()).thenReturn(mySBuildType);
    when(mySBuild.getBuildType()).thenReturn(mySBuildType);
    when(mySBuildType.getProject()).thenReturn(sProject);
    when(sProject.getProjectId()).thenReturn("projectId");
    when(mySTestRun.getTest()).thenReturn(STest);
    when(mySTestRun.getFullText()).thenReturn("Full Text Test Run");
    when(STest.getName()).thenReturn(testName);
    when(myBuildApplicabilityChecker.isApplicable(any(), any(), any())).thenReturn(true);
    when(testName.getAsString()).thenReturn("Test Name as String");
    //when(myResponsibleUserFinder.findResponsibleUser(any()));
    when(myBuildProblem.getBuildProblemData()).thenReturn(buildProblemData);
    when(buildProblemData.getType()).thenReturn("1234");
  }

  public void Test_OnTestFailed_BuildTypeIsNull() {
    when(mySRunningBuild.getBuildType()).thenReturn(null);

    //myProcessor.processFailedTest(mySBuild,
    //                              Collections.singletonList(myBuildProblem),
    //                              Collections.singletonList(mySTestRun));
    Assert.fail();
    Mockito.verify(mySTestRun, Mockito.never()).getTest();
  }

  public void Test_OnTestFailed_BuildTypeNotNull() {
    SUser sUser = Mockito.mock(SUser.class);
    when(mySRunningBuild.getBuildType()).thenReturn(mySBuildType);
    HeuristicResult heuristicResult = new HeuristicResult();
    when(myResponsibleUserFinder.findResponsibleUser(any(), anyList(), anyList())).thenReturn(heuristicResult);
    heuristicResult.addResponsibility(mySTestRun, new Responsibility(sUser, "some text"));
    //
    //myProcessor.processFailedTest(mySBuild,
    //                              Collections.singletonList(myBuildProblem),
    //                              Collections.singletonList(mySTestRun));
    Assert.fail();
    Mockito.verify(mySTestRun, Mockito.atLeastOnce()).getTest();
  }

  public void Test_OnTestFailed_ResponsibleUserNotFound() {
    HeuristicResult emptyHeuristicResult = new HeuristicResult();
    when(myResponsibleUserFinder.findResponsibleUser(any(), anyList(), anyList())).thenReturn(emptyHeuristicResult);

    //myProcessor.processFailedTest(mySBuild,
    //                              Collections.singletonList(myBuildProblem),
    //                              Collections.singletonList(mySTestRun));
    Assert.fail();

    Mockito.verify(myTestNameResponsibilityFacade, Mockito.never())
           .setTestNameResponsibility(any(TestName.class), anyString(), any());
  }

  public void Test_OnTestFailed_ResponsibleUserFound() {
    SUser sUser = Mockito.mock(SUser.class);
    HeuristicResult heuristicResult = new HeuristicResult();
    heuristicResult.addResponsibility(mySTestRun, new Responsibility(sUser, "Failed description"));
    when(myResponsibleUserFinder.findResponsibleUser(any(), anyList(), anyList())).thenReturn(heuristicResult);

    //myProcessor.processFailedTest(mySBuild,
    //                              Collections.singletonList(myBuildProblem),
    //                              Collections.singletonList(mySTestRun));
    Assert.fail();

    Mockito.verify(myTestNameResponsibilityFacade, Mockito.atLeastOnce())
           .setTestNameResponsibility(any(TestName.class), any(), any());
  }

  public void Test_BuildProblemOccurred_BuildTypeIsNull() {
    when(mySBuild.getBuildType()).thenReturn(null);

    //myProcessor.onBuildProblemOccurred(mySBuild, myBuildProblem);
    Assert.fail();

    Mockito.verify(myBuildApplicabilityChecker, Mockito.never()).isApplicable(any(), any(), any());
  }

  public void Test_BuildProblemOccurred_BuildTypeNotNull() {
    when(mySBuild.getBuildType()).thenReturn(mySBuildType);

    //myProcessor.onBuildProblemOccurred(mySBuild, myBuildProblem);
    Assert.fail();

    Mockito.verify(myBuildApplicabilityChecker, Mockito.atLeastOnce()).isApplicable(any(), any(), any());
  }

  public void Test_BuildProblemOccurred_ApplicabilityFailed() {
    when(myBuildApplicabilityChecker.isApplicable(any(), any(), any())).thenReturn(false);

    //myProcessor.onBuildProblemOccurred(mySBuild, myBuildProblem);
    Assert.fail();

    Mockito.verify(myResponsibleUserFinder, Mockito.never()).findResponsibleUser(any(), anyList(), anyList());
  }

  public void Test_BuildProblemOccurred_ApplicabilitySucceed() {
    when(myBuildApplicabilityChecker.isApplicable(any(), any(), any())).thenReturn(true);

    //myProcessor.onBuildProblemOccurred(mySBuild, myBuildProblem);
    Assert.fail();

    Mockito.verify(myResponsibleUserFinder, Mockito.atLeastOnce()).findResponsibleUser(any(), anyList(), anyList());
  }

  public void Test_BuildProblemOccurred_ResponsibleUserNotFound() {
    HeuristicResult emptyHeuristicResult = new HeuristicResult();
    when(myResponsibleUserFinder.findResponsibleUser(any(), anyList(), anyList())).thenReturn(emptyHeuristicResult);

    //myProcessor.onBuildProblemOccurred(mySBuild, myBuildProblem);
    Assert.fail();

    Mockito.verify(myTestNameResponsibilityFacade, Mockito.never())
           .setTestNameResponsibility(any(TestName.class), anyString(), any());
  }

  public void Test_BuildProblemOccurred_ResponsibleUserFound() {
    SUser sUser = Mockito.mock(SUser.class);
    HeuristicResult heuristicResult = new HeuristicResult();
    heuristicResult.addResponsibility(myBuildProblem, new Responsibility(sUser, "Failed description"));
    when(myResponsibleUserFinder.findResponsibleUser(any(), anyList(), anyList())).thenReturn(heuristicResult);

    //myProcessor.onBuildProblemOccurred(mySBuild, myBuildProblem);
    Assert.fail();

    Mockito.verify(myBuildProblemResponsibilityFacade, Mockito.atLeastOnce())
           .setBuildProblemResponsibility(any(BuildProblemInfo.class), any(), any());
  }
}
