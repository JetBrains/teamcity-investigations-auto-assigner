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
import jetbrains.buildServer.iaa.utils.InvestigationsManager;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.iaa.common.Constants;
import jetbrains.buildServer.responsibility.BuildProblemResponsibilityEntry;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.impl.problems.BuildProblemImpl;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;

@Test
public class BuildApplicabilityCheckerTest extends BaseTestCase {

  private SProject mySProject;
  private BuildProblemResponsibilityEntry myResponsibilityEntry;
  private BuildApplicabilityChecker myApplicabilityChecker;
  private BuildProblemImpl myBuildProblem;
  private InvestigationsManager myInvestigationsManager;
  private SBuild mySBuild;
  private BuildProblemData myBuildProblemData;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myBuildProblem = Mockito.mock(BuildProblemImpl.class);
    mySProject = Mockito.mock(SProject.class);
    mySBuild = Mockito.mock(SBuild.class);
    SProject parentProject = Mockito.mock(SProject.class);
    final SProject project2 = Mockito.mock(SProject.class);
    myResponsibilityEntry = Mockito.mock(BuildProblemResponsibilityEntry.class);
    myBuildProblemData = Mockito.mock(BuildProblemData.class);
    BuildProblemResponsibilityEntry responsibilityEntry2 = Mockito.mock(BuildProblemResponsibilityEntry.class);
    myInvestigationsManager = Mockito.mock(InvestigationsManager.class);

    when(mySProject.getProjectId()).thenReturn("Project ID");
    when(project2.getProjectId()).thenReturn("Project ID 2");
    when(parentProject.getProjectId()).thenReturn("Parent Project ID");
    when(mySProject.getParentProject()).thenReturn(parentProject);
    when(myResponsibilityEntry.getState()).thenReturn(ResponsibilityEntry.State.NONE);
    when(responsibilityEntry2.getState()).thenReturn(ResponsibilityEntry.State.NONE);
    when(myBuildProblem.getBuildProblemData()).thenReturn(myBuildProblemData);
    when(myBuildProblemData.getType()).thenReturn(Constants.TC_COMPILATION_ERROR_TYPE);
    when(myBuildProblem.isMuted()).thenReturn(false);
    when(myBuildProblem.isNew()).thenReturn(true);
    when(myBuildProblem.getAllResponsibilities())
      .thenReturn(Arrays.asList(myResponsibilityEntry, responsibilityEntry2));
    when(myInvestigationsManager.checkUnderInvestigation(mySProject, mySBuild, myBuildProblem)).thenReturn(false);
    when(myInvestigationsManager.checkUnderInvestigation(project2, mySBuild, myBuildProblem)).thenReturn(false);
    myApplicabilityChecker = new BuildApplicabilityChecker(myInvestigationsManager);
  }

  public void Test_BuildProblemIsMuted() {
    when(myBuildProblem.isMuted()).thenReturn(true);

    boolean isApplicable = myApplicabilityChecker.isApplicable(mySProject, mySBuild, myBuildProblem);

    Assert.assertFalse(isApplicable);
  }

  public void Test_BuildProblemIsNotMuted() {
    when(myBuildProblem.isMuted()).thenReturn(false);

    boolean isApplicable = myApplicabilityChecker.isApplicable(mySProject, mySBuild, myBuildProblem);

    Assert.assertTrue(isApplicable);
  }

  public void Test_BuildProblemNotNew() {
    when(myBuildProblem.isNew()).thenReturn(false);

    boolean isApplicable = myApplicabilityChecker.isApplicable(mySProject, mySBuild, myBuildProblem);

    Assert.assertFalse(isApplicable);
  }

  public void Test_BuildProblemIsNew() {
    when(myBuildProblem.isNew()).thenReturn(true);

    boolean isApplicable = myApplicabilityChecker.isApplicable(mySProject, mySBuild, myBuildProblem);

    Assert.assertTrue(isApplicable);
  }

  public void Test_BuildProblemIsUnderInvestigation() {
    when(myInvestigationsManager.checkUnderInvestigation(mySProject, mySBuild, myBuildProblem)).thenReturn(true);
    when(myResponsibilityEntry.getProject()).thenReturn(mySProject);

    boolean isApplicable = myApplicabilityChecker.isApplicable(mySProject, mySBuild, myBuildProblem);

    Assert.assertFalse(isApplicable);
  }

  public void Test_BuildProblemNotUnderInvestigation() {
    when(myInvestigationsManager.checkUnderInvestigation(mySProject, mySBuild, myBuildProblem)).thenReturn(false);
    when(myResponsibilityEntry.getProject()).thenReturn(mySProject);

    boolean isApplicable = myApplicabilityChecker.isApplicable(mySProject, mySBuild, myBuildProblem);

    Assert.assertTrue(isApplicable);
  }

  public void Test_BuildProblemHasIncompatibleType() {
    when(myBuildProblemData.getType()).thenReturn("Incompatible Type");

    boolean isApplicable = myApplicabilityChecker.isApplicable(mySProject, mySBuild, myBuildProblem);

    Assert.assertFalse(isApplicable);
  }

  public void Test_BuildProblemHasCompatibleType() {
    when(myBuildProblemData.getType()).thenReturn(Constants.TC_COMPILATION_ERROR_TYPE);

    boolean isApplicable = myApplicabilityChecker.isApplicable(mySProject, mySBuild, myBuildProblem);

    Assert.assertTrue(isApplicable);
  }
}
