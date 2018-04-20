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

import java.util.Arrays;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.iaa.utils.FlakyTestDetector;
import jetbrains.buildServer.iaa.utils.InvestigationsManager;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.responsibility.TestNameResponsibilityEntry;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STest;
import jetbrains.buildServer.serverSide.STestRun;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@Test
public class TestApplicabilityCheckerTest extends BaseTestCase {

  private TestApplicabilityChecker applicabilityChecker;
  private SProject project;
  private STestRun sTestRun ;
  private TestNameResponsibilityEntry responsibilityEntry1;
  private FlakyTestDetector flakyTestDetector;
  private InvestigationsManager investigationsManager;
  private STest sTest;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    flakyTestDetector = Mockito.mock(FlakyTestDetector.class);
    project = Mockito.mock(SProject.class);
    SProject parentProject = Mockito.mock(SProject.class);
    final SProject project2 = Mockito.mock(SProject.class);
    sTestRun = Mockito.mock(STestRun.class);
    responsibilityEntry1 = Mockito.mock(TestNameResponsibilityEntry.class);
    investigationsManager = Mockito.mock(InvestigationsManager.class);
    final TestNameResponsibilityEntry responsibilityEntry2 = Mockito.mock(TestNameResponsibilityEntry.class);
    sTest = Mockito.mock(STest.class);
    when(project.getProjectId()).thenReturn("Project ID");
    when(project2.getProjectId()).thenReturn("Project ID 2");
    when(parentProject.getProjectId()).thenReturn("Parent Project ID");
    when(project.getParentProject()).thenReturn(parentProject);
    when(sTestRun.isMuted()).thenReturn(false);
    when(sTestRun.isFixed()).thenReturn(false);
    when(sTestRun.isNewFailure()).thenReturn(true);
    when(sTestRun.getTest()).thenReturn(sTest);
    when(sTest.getAllResponsibilities()).thenReturn(Arrays.asList(responsibilityEntry1, responsibilityEntry2));
    when(flakyTestDetector.isFlaky(anyLong())).thenReturn(false);
    when(responsibilityEntry1.getState()).thenReturn(ResponsibilityEntry.State.NONE);
    when(responsibilityEntry2.getState()).thenReturn(ResponsibilityEntry.State.NONE);
    when(investigationsManager.checkUnderInvestigation(project, sTest)).thenReturn(false);
    applicabilityChecker = new TestApplicabilityChecker(flakyTestDetector, investigationsManager);
  }

  public void Test_TestRunIsMuted() {
    when(sTestRun.isMuted()).thenReturn(true);

    boolean isApplicable = applicabilityChecker.check(project, sTestRun);

    Assert.assertFalse(isApplicable);
  }

  public void Test_TestRunIsNotMuted() {
    when(sTestRun.isMuted()).thenReturn(false);

    boolean isApplicable = applicabilityChecker.check(project, sTestRun);

    Assert.assertTrue(isApplicable);
  }

  public void Test_TestRunIsFixed() {
    when(sTestRun.isFixed()).thenReturn(true);

    boolean isApplicable = applicabilityChecker.check(project, sTestRun);

    Assert.assertFalse(isApplicable);
  }

  public void Test_TestRunIsNotFixed() {
    when(sTestRun.isFixed()).thenReturn(false);

    boolean isApplicable = applicabilityChecker.check(project, sTestRun);

    Assert.assertTrue(isApplicable);
  }

  public void Test_TestRunNotNewFailure() {
    when(sTestRun.isNewFailure()).thenReturn(false);

    boolean isApplicable = applicabilityChecker.check(project, sTestRun);

    Assert.assertFalse(isApplicable);
  }

  public void Test_TestRunIsNewFailure() {
    when(sTestRun.isNewFailure()).thenReturn(true);

    boolean isApplicable = applicabilityChecker.check(project, sTestRun);

    Assert.assertTrue(isApplicable);
  }

  public void Test_BuildProblemIsUnderInvestigation() {
    when(investigationsManager.checkUnderInvestigation(project, sTest)).thenReturn(true);
    when(responsibilityEntry1.getProject()).thenReturn(project);

    boolean isApplicable = applicabilityChecker.check(project, sTestRun);

    Assert.assertFalse(isApplicable);
  }

  public void Test_BuildProblemNotUnderInvestigation() {
    when(investigationsManager.checkUnderInvestigation(project, sTest)).thenReturn(false);
    when(responsibilityEntry1.getProject()).thenReturn(project);

    boolean isApplicable = applicabilityChecker.check(project, sTestRun);

    Assert.assertTrue(isApplicable);
  }

  public void Test_TestIsFlaky() {
    when(flakyTestDetector.isFlaky(anyLong())).thenReturn(true);

    boolean isApplicable = applicabilityChecker.check(project, sTestRun);

    Assert.assertFalse(isApplicable);
  }

  public void Test_TestNotFlaky() {
    when(flakyTestDetector.isFlaky(anyLong())).thenReturn(false);

    boolean isApplicable = applicabilityChecker.check(project, sTestRun);

    Assert.assertTrue(isApplicable);
  }
}
