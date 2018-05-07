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

package jetbrains.buildServer.iaa.heuristics;

import com.intellij.openapi.util.Pair;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.iaa.BuildProblemInfo;
import jetbrains.buildServer.iaa.ProblemInfo;
import jetbrains.buildServer.iaa.TestProblemInfo;
import jetbrains.buildServer.iaa.utils.InvestigationsManager;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STest;
import jetbrains.buildServer.serverSide.impl.problems.BuildProblemImpl;
import jetbrains.buildServer.users.User;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;

@Test
public class PreviousResponsibleHeuristicTest extends BaseTestCase {

  private PreviousResponsibleHeuristic myHeuristic;
  private InvestigationsManager myInvestigationsManager;
  private SBuild mySBuild;
  private SProject mySProject;
  private BuildProblemInfo myBuildProblemInfo;
  private TestProblemInfo myTestProblemInfo;
  private BuildProblemImpl myBuildProblem;
  private User myUser;
  private STest mySTest;
  private User myUser2;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myInvestigationsManager = Mockito.mock(InvestigationsManager.class);
    mySBuild = Mockito.mock(SBuild.class);
    mySProject = Mockito.mock(SProject.class);
    myBuildProblemInfo = Mockito.mock(BuildProblemInfo.class);
    myBuildProblem = Mockito.mock(BuildProblemImpl.class);
    myTestProblemInfo = Mockito.mock(TestProblemInfo.class);
    final BuildProblemData buildProblemData = Mockito.mock(BuildProblemData.class);
    myUser = Mockito.mock(User.class);
    myUser2 = Mockito.mock(User.class);
    mySTest = Mockito.mock(STest.class);

    myHeuristic = new PreviousResponsibleHeuristic(myInvestigationsManager);
    when(myBuildProblemInfo.getBuildProblem()).thenReturn(myBuildProblem);
    when(myBuildProblemInfo.getSBuild()).thenReturn(mySBuild);
    when(myBuildProblemInfo.getSProject()).thenReturn(mySProject);
    when(myBuildProblem.getBuildProblemData()).thenReturn(buildProblemData);
    when(buildProblemData.getType()).thenReturn("Type");
    when(mySBuild.getFullName()).thenReturn("Full SBuild Name");
    when(myInvestigationsManager.findPreviousResponsible(mySProject, mySBuild, myBuildProblem)).thenReturn(myUser);

    when(myTestProblemInfo.getSTest()).thenReturn(mySTest);
    when(myTestProblemInfo.getSBuild()).thenReturn(mySBuild);
    when(myTestProblemInfo.getSProject()).thenReturn(mySProject);
    when(mySTest.getTestNameId()).thenReturn(12982318457L);
    when(mySTest.getProjectId()).thenReturn("2134124");
    when(myInvestigationsManager.findPreviousResponsible(myTestProblemInfo)).thenReturn(myUser2);
  }

  public void TestBuildProblemInfo_ResponsibleFound() {
    when(myInvestigationsManager.findPreviousResponsible(mySProject, mySBuild, myBuildProblem)).thenReturn(myUser);

    Pair<User, String> result = myHeuristic.findResponsibleUser(myBuildProblemInfo);
    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result.first).isEqualTo(myUser);
  }

  public void TestBuildProblemInfo_ResponsibleNotFound() {
    when(myInvestigationsManager.findPreviousResponsible(mySProject, mySBuild, myBuildProblem)).thenReturn(null);

    Assertions.assertThat(myHeuristic.findResponsibleUser(myBuildProblemInfo)).isNull();
  }

  public void TestTestProblemInfo_ResponsibleFound() {
    when(myInvestigationsManager.findPreviousResponsible(myTestProblemInfo)).thenReturn(myUser2);

    Pair<User, String> result = myHeuristic.findResponsibleUser(myTestProblemInfo);
    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result.first).isEqualTo(myUser2);
  }

  public void TestTestProblemInfo_ResponsibleNotFound() {
    when(myInvestigationsManager.findPreviousResponsible(myTestProblemInfo)).thenReturn(null);

    Assertions.assertThat(myHeuristic.findResponsibleUser(myTestProblemInfo)).isNull();
  }

  public void TestIncompatibleProblemInfo() {
    ProblemInfo problemInfo = new IncompatibleProblemInfo(mySBuild, mySProject, "Any text");

    Assertions.assertThat(myHeuristic.findResponsibleUser(problemInfo)).isNull();
  }

  class IncompatibleProblemInfo extends ProblemInfo {

    IncompatibleProblemInfo(@NotNull final SBuild sBuild,
                            @NotNull final SProject project,
                            @Nullable final String problemText) {
      super(sBuild, project, problemText);
    }
  }
}