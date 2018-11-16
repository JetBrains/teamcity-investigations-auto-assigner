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

package jetbrains.buildServer.investigationsAutoAssigner.processing;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.investigationsAutoAssigner.common.FailedBuildInfo;
import jetbrains.buildServer.investigationsAutoAssigner.utils.BuildProblemUtils;
import jetbrains.buildServer.investigationsAutoAssigner.utils.InvestigationsManager;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.responsibility.BuildProblemResponsibilityEntry;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.serverSide.BuildPromotion;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.impl.problems.BuildProblemImpl;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;

@Test
public class BuildProblemsFilterTest extends BaseTestCase {

  private SProject mySProject;
  private BuildProblemResponsibilityEntry myResponsibilityEntry;
  private BuildProblemsFilter myBuildProblemsFilter;
  private BuildProblemImpl myBuildProblem;
  private InvestigationsManager myInvestigationsManager;
  private SBuild mySBuild;
  private BuildProblemData myBuildProblemData;
  private FailedBuildInfo myFailedBuildInfo;
  private List<BuildProblem> myBuildProblemWrapper;
  private BuildProblemUtils myBuildProblemUtils;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myBuildProblem = Mockito.mock(BuildProblemImpl.class);
    mySProject = Mockito.mock(SProject.class);
    mySBuild = Mockito.mock(SBuild.class);
    BuildPromotion buildPromotion = Mockito.mock(BuildPromotion.class);
    SProject parentProject = Mockito.mock(SProject.class);
    final SProject project2 = Mockito.mock(SProject.class);
    myResponsibilityEntry = Mockito.mock(BuildProblemResponsibilityEntry.class);
    myBuildProblemData = Mockito.mock(BuildProblemData.class);
    BuildProblemResponsibilityEntry responsibilityEntry2 = Mockito.mock(BuildProblemResponsibilityEntry.class);
    myInvestigationsManager = Mockito.mock(InvestigationsManager.class);
    myBuildProblemUtils = Mockito.mock(BuildProblemUtils.class);

    when(mySBuild.getBuildPromotion()).thenReturn(buildPromotion);
    when(mySProject.getProjectId()).thenReturn("Project ID");
    when(project2.getProjectId()).thenReturn("Project ID 2");
    when(parentProject.getProjectId()).thenReturn("Parent Project ID");
    when(mySProject.getParentProject()).thenReturn(parentProject);
    when(myResponsibilityEntry.getState()).thenReturn(ResponsibilityEntry.State.NONE);
    when(responsibilityEntry2.getState()).thenReturn(ResponsibilityEntry.State.NONE);
    when(myBuildProblem.getBuildProblemData()).thenReturn(myBuildProblemData);
    when(myBuildProblemData.getType()).thenReturn(Constants.TC_COMPILATION_ERROR_TYPE);
    when(myBuildProblem.isMuted()).thenReturn(false);
    when(myBuildProblemUtils.isNew(myBuildProblem)).thenReturn(true);
    when(myBuildProblem.getAllResponsibilities())
      .thenReturn(Arrays.asList(myResponsibilityEntry, responsibilityEntry2));
    when(myInvestigationsManager.checkUnderInvestigation(mySProject, mySBuild, myBuildProblem)).thenReturn(false);
    when(myInvestigationsManager.checkUnderInvestigation(project2, mySBuild, myBuildProblem)).thenReturn(false);
    myBuildProblemsFilter = new BuildProblemsFilter(myInvestigationsManager, myBuildProblemUtils);

    myBuildProblemWrapper = Collections.singletonList(myBuildProblem);
    myFailedBuildInfo = new FailedBuildInfo(mySBuild);
  }

  public void Test_BuildProblemIsMuted() {
    when(myBuildProblem.isMuted()).thenReturn(true);

    List<BuildProblem> applicableBuildProblems =
      myBuildProblemsFilter.apply(myFailedBuildInfo, mySProject, myBuildProblemWrapper);

    Assert.assertEquals(applicableBuildProblems.size(), 0);
  }

  public void Test_BuildProblemIsNotMuted() {
    when(myBuildProblem.isMuted()).thenReturn(false);

    List<BuildProblem> applicableBuildProblems =
      myBuildProblemsFilter.apply(myFailedBuildInfo, mySProject, myBuildProblemWrapper);

    Assert.assertEquals(applicableBuildProblems.size(), 1);
  }

  public void Test_BuildProblemNotNew() {
    when(myBuildProblemUtils.isNew(myBuildProblem)).thenReturn(false);

    List<BuildProblem> applicableBuildProblems =
      myBuildProblemsFilter.apply(myFailedBuildInfo, mySProject, myBuildProblemWrapper);

    Assert.assertEquals(applicableBuildProblems.size(), 0);
  }

  public void Test_BuildProblemIsNew() {
    when(myBuildProblemUtils.isNew(myBuildProblem)).thenReturn(true);

    List<BuildProblem> applicableBuildProblems =
      myBuildProblemsFilter.apply(myFailedBuildInfo, mySProject, myBuildProblemWrapper);

    Assert.assertEquals(applicableBuildProblems.size(), 1);
  }

  public void Test_BuildProblemIsUnderInvestigation() {
    when(myInvestigationsManager.checkUnderInvestigation(mySProject, mySBuild, myBuildProblem)).thenReturn(true);
    when(myResponsibilityEntry.getProject()).thenReturn(mySProject);

    List<BuildProblem> applicableBuildProblems =
      myBuildProblemsFilter.apply(myFailedBuildInfo, mySProject, myBuildProblemWrapper);

    Assert.assertEquals(applicableBuildProblems.size(), 0);
  }

  public void Test_BuildProblemNotUnderInvestigation() {
    when(myInvestigationsManager.checkUnderInvestigation(mySProject, mySBuild, myBuildProblem)).thenReturn(false);
    when(myResponsibilityEntry.getProject()).thenReturn(mySProject);

    List<BuildProblem> applicableBuildProblems =
      myBuildProblemsFilter.apply(myFailedBuildInfo, mySProject, myBuildProblemWrapper);

    Assert.assertEquals(applicableBuildProblems.size(), 1);
  }

  public void Test_BuildProblemHasIncompatibleType() {
    when(myBuildProblemData.getType()).thenReturn("Incompatible Type");

    List<BuildProblem> applicableBuildProblems =
      myBuildProblemsFilter.apply(myFailedBuildInfo, mySProject, myBuildProblemWrapper);

    Assert.assertEquals(applicableBuildProblems.size(), 0);
  }

  public void Test_BuildProblemHasCompatibleType() {
    when(myBuildProblemData.getType()).thenReturn(Constants.TC_COMPILATION_ERROR_TYPE);

    List<BuildProblem> applicableBuildProblems =
      myBuildProblemsFilter.apply(myFailedBuildInfo, mySProject, myBuildProblemWrapper);

    Assert.assertEquals(applicableBuildProblems.size(), 1);
  }
}
