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

package jetbrains.buildServer.iaa.processing;

import java.util.Arrays;
import java.util.Collections;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.iaa.common.HeuristicResult;
import jetbrains.buildServer.iaa.common.Responsibility;
import jetbrains.buildServer.responsibility.TestNameResponsibilityFacade;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STest;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.users.impl.UserImpl;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@Test
public class FailedTestAssignerTest extends BaseTestCase {

  private TestNameResponsibilityFacade myTestNameResponsibilityFacade;
  private STestRun mySTestRun1;
  private HeuristicResult myHeuristicResult;
  private User myUser1;
  private SProject mySProject;
  private STestRun mySTestRun2;
  private User myUser2;
  private FailedTestAssigner myTestedFailedTestAssigner;

  @BeforeMethod
  @Override
  protected void setUp() {
    myTestNameResponsibilityFacade = Mockito.mock(TestNameResponsibilityFacade.class);
    mySTestRun1 = Mockito.mock(STestRun.class);
    STest sTest1 = Mockito.mock(STest.class);
    TestName testName1 = Mockito.mock(TestName.class);
    when(mySTestRun1.getTest()).thenReturn(sTest1);
    when(mySTestRun1.getTestRunId()).thenReturn(0);
    when(sTest1.getName()).thenReturn(testName1);

    mySTestRun2 = Mockito.mock(STestRun.class);
    STest sTest2 = Mockito.mock(STest.class);
    TestName testName2 = Mockito.mock(TestName.class);
    when(mySTestRun2.getTest()).thenReturn(sTest2);
    when(mySTestRun1.getTestRunId()).thenReturn(1);
    when(sTest2.getName()).thenReturn(testName2);

    mySProject = Mockito.mock(SProject.class);
    myUser1 = Mockito.mock(UserImpl.class);
    when(myUser1.getUsername()).thenReturn("user1");
    when(myUser1.getId()).thenReturn(1L);
    myUser2 = Mockito.mock(UserImpl.class);
    when(myUser2.getUsername()).thenReturn("user2");
    when(myUser2.getId()).thenReturn(2L);
    myHeuristicResult = new HeuristicResult();

    myTestedFailedTestAssigner = new FailedTestAssigner(myTestNameResponsibilityFacade);
  }

  public void Test_NoTestRuns() {
    myTestedFailedTestAssigner.assign(myHeuristicResult, mySProject, Collections.emptyList(), false);

    Mockito.verify(myTestNameResponsibilityFacade, Mockito.never()).setTestNameResponsibility(anyList(), any(), any());
  }

  public void Test_NoResponsibilitiesFound() {
    myTestedFailedTestAssigner.assign(myHeuristicResult, mySProject, Collections.singletonList(mySTestRun1), false);

    Mockito.verify(myTestNameResponsibilityFacade, Mockito.never()).setTestNameResponsibility(anyList(), any(), any());
  }

  public void Test_OneResponsibilityFound() {
    Responsibility putResponsibility = new Responsibility(myUser1, "any description");
    myHeuristicResult.addResponsibility(mySTestRun1, putResponsibility);

    myTestedFailedTestAssigner.assign(myHeuristicResult, mySProject, Collections.singletonList(mySTestRun1), false);

    Mockito.verify(myTestNameResponsibilityFacade, Mockito.only()).setTestNameResponsibility(anyList(), any(), any());
  }

  public void Test_OneResponsibilityFoundSilentModeOn() {
    Responsibility putResponsibility = new Responsibility(myUser1, "any description");
    myHeuristicResult.addResponsibility(mySTestRun1, putResponsibility);

    myTestedFailedTestAssigner.assign(myHeuristicResult, mySProject, Collections.singletonList(mySTestRun1), true);

    Mockito.verify(myTestNameResponsibilityFacade, Mockito.never()).setTestNameResponsibility(anyList(), any(), any());
  }


  public void Test_TwoSameResponsibilitiesFound() {
    Responsibility putResponsibility = new Responsibility(myUser1, "any description");
    Responsibility putResponsibility2 = new Responsibility(myUser1, "any description");
    myHeuristicResult.addResponsibility(mySTestRun1, putResponsibility);
    myHeuristicResult.addResponsibility(mySTestRun2, putResponsibility2);

    myTestedFailedTestAssigner.assign(myHeuristicResult, mySProject, Arrays.asList(mySTestRun1, mySTestRun2), false);

    Mockito.verify(myTestNameResponsibilityFacade, Mockito.only()).setTestNameResponsibility(anyList(), any(), any());
  }

  public void Test_TwoDifferentResponsibilitiesFound() {
    myHeuristicResult.addResponsibility(mySTestRun1, new Responsibility(myUser1, "any description"));
    myHeuristicResult.addResponsibility(mySTestRun2, new Responsibility(myUser2, "any description"));
    myTestedFailedTestAssigner.assign(myHeuristicResult, mySProject, Arrays.asList(mySTestRun1, mySTestRun2), false);
    Mockito.verify(myTestNameResponsibilityFacade, Mockito.times(2)).setTestNameResponsibility(anyList(), any(), any());

    myHeuristicResult = new HeuristicResult();
    Mockito.clearInvocations(myTestNameResponsibilityFacade);
    myHeuristicResult.addResponsibility(mySTestRun1, new Responsibility(myUser1, "any description"));
    myHeuristicResult.addResponsibility(mySTestRun2, new Responsibility(myUser1, "any description 2"));
    myTestedFailedTestAssigner.assign(myHeuristicResult, mySProject, Arrays.asList(mySTestRun1, mySTestRun2), false);
    Mockito.verify(myTestNameResponsibilityFacade, Mockito.times(2)).setTestNameResponsibility(anyList(), any(), any());

    myHeuristicResult = new HeuristicResult();
    Mockito.clearInvocations(myTestNameResponsibilityFacade);
    myHeuristicResult.addResponsibility(mySTestRun1, new Responsibility(myUser1, "any description"));
    myHeuristicResult.addResponsibility(mySTestRun2, new Responsibility(myUser2, "any description 2"));
    myTestedFailedTestAssigner.assign(myHeuristicResult, mySProject, Arrays.asList(mySTestRun1, mySTestRun2), false);
    Mockito.verify(myTestNameResponsibilityFacade, Mockito.times(2)).setTestNameResponsibility(anyList(), any(), any());
  }
}
