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
import java.util.Collections;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.responsibility.BuildProblemResponsibilityEntry;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.impl.problems.BuildProblemImpl;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;

@Test
public class BuildApplicabilityCheckerTest extends BaseTestCase {

  private SProject project;
  private BuildProblemResponsibilityEntry responsibilityEntry1;
  private SProject project2;
  private BuildApplicabilityChecker applicabilityChecker;
  private BuildProblemImpl buildProblem;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    buildProblem = Mockito.mock(BuildProblemImpl.class);
    project = Mockito.mock(SProject.class);
    SProject parentProject = Mockito.mock(SProject.class);
    project2 = Mockito.mock(SProject.class);
    responsibilityEntry1 = Mockito.mock(BuildProblemResponsibilityEntry.class);
    BuildProblemResponsibilityEntry responsibilityEntry2 = Mockito.mock(BuildProblemResponsibilityEntry.class);

    when(project.getProjectId()).thenReturn("Project ID");
    when(project2.getProjectId()).thenReturn("Project ID 2");
    when(parentProject.getProjectId()).thenReturn("Parent Project ID");
    when(project.getParentProject()).thenReturn(parentProject);
    when(responsibilityEntry1.getState()).thenReturn(ResponsibilityEntry.State.NONE);
    when(responsibilityEntry2.getState()).thenReturn(ResponsibilityEntry.State.NONE);

    when(buildProblem.isMuted()).thenReturn(false);
    when(buildProblem.isNew()).thenReturn(true);
    when(buildProblem.getAllResponsibilities()).thenReturn(Arrays.asList(responsibilityEntry1, responsibilityEntry2));
    applicabilityChecker = new BuildApplicabilityChecker();
  }

  public void Test_BuildProblemIsMuted() {
    when(buildProblem.isMuted()).thenReturn(true);

    boolean isApplicable = applicabilityChecker.check(project, buildProblem);

    Assert.assertFalse(isApplicable);
  }

  public void Test_BuildProblemIsNotMuted() {
    when(buildProblem.isMuted()).thenReturn(false);

    boolean isApplicable = applicabilityChecker.check(project, buildProblem);

    Assert.assertTrue(isApplicable);
  }

  public void Test_BuildProblemNotNew() {
    when(buildProblem.isNew()).thenReturn(false);

    boolean isApplicable = applicabilityChecker.check(project, buildProblem);

    Assert.assertFalse(isApplicable);
  }

  public void Test_BuildProblemIsNew() {
    when(buildProblem.isNew()).thenReturn(true);

    boolean isApplicable = applicabilityChecker.check(project, buildProblem);

    Assert.assertTrue(isApplicable);
  }

  public void Test_BuildProblemIsUnderInvestigationSameProject() {
    when(responsibilityEntry1.getState()).thenReturn(ResponsibilityEntry.State.TAKEN);
    when(responsibilityEntry1.getProject()).thenReturn(project);

    boolean isApplicable = applicabilityChecker.check(project, buildProblem);

    Assert.assertFalse(isApplicable);
  }

  public void Test_BuildProblemIsUnderInvestigationParentProject() {
    when(responsibilityEntry1.getState()).thenReturn(ResponsibilityEntry.State.TAKEN);
    SProject parentProject = project.getParentProject();
    when(responsibilityEntry1.getProject()).thenReturn(parentProject);

    boolean isApplicable = applicabilityChecker.check(project, buildProblem);

    Assert.assertFalse(isApplicable);
  }

  public void Test_BuildProblemIsUnderInvestigationOtherProject() {
    when(responsibilityEntry1.getState()).thenReturn(ResponsibilityEntry.State.TAKEN);
    when(responsibilityEntry1.getProject()).thenReturn(project2);

    boolean isApplicable = applicabilityChecker.check(project, buildProblem);

    Assert.assertTrue(isApplicable);
  }

  public void Test_BuildProblemNotUnderInvestigation() {
    when(buildProblem.getAllResponsibilities()).thenReturn(Collections.emptyList());

    boolean isApplicable = applicabilityChecker.check(project, buildProblem);

    Assert.assertTrue(isApplicable);
  }
}
