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

  private PreviousResponsibleHeuristic heuristic;
  private InvestigationsManager investigationsManager;
  private SBuild sBuild;
  private SProject sProject;
  private BuildProblemInfo buildProblemInfo;
  private TestProblemInfo testProblemInfo;
  private BuildProblemImpl buildProblem;
  private User user1;
  private STest sTest;
  private User user2;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    investigationsManager = Mockito.mock(InvestigationsManager.class);
    sBuild = Mockito.mock(SBuild.class);
    sProject = Mockito.mock(SProject.class);
    buildProblemInfo = Mockito.mock(BuildProblemInfo.class);
    buildProblem = Mockito.mock(BuildProblemImpl.class);
    testProblemInfo = Mockito.mock(TestProblemInfo.class);
    final BuildProblemData buildProblemData = Mockito.mock(BuildProblemData.class);
    user1 = Mockito.mock(User.class);
    user2 = Mockito.mock(User.class);
    sTest = Mockito.mock(STest.class);

    heuristic = new PreviousResponsibleHeuristic(investigationsManager);
    when(buildProblemInfo.getBuildProblem()).thenReturn(buildProblem);
    when(buildProblemInfo.getSBuild()).thenReturn(sBuild);
    when(buildProblemInfo.getSProject()).thenReturn(sProject);
    when(buildProblem.getBuildProblemData()).thenReturn(buildProblemData);
    when(buildProblemData.getType()).thenReturn("Type");
    when(sBuild.getFullName()).thenReturn("Full SBuild Name");
    when(investigationsManager.findPreviousResponsible(sProject, sBuild, buildProblem)).thenReturn(user1);

    when(testProblemInfo.getSTest()).thenReturn(sTest);
    when(testProblemInfo.getSBuild()).thenReturn(sBuild);
    when(testProblemInfo.getSProject()).thenReturn(sProject);
    when(sTest.getTestNameId()).thenReturn(12982318457L);
    when(sTest.getProjectId()).thenReturn("2134124");
    when(investigationsManager.findPreviousResponsible(sProject, sBuild, sTest)).thenReturn(user2);
  }

  public void TestBuildProblemInfo_ResponsibleFound() {
    when(investigationsManager.findPreviousResponsible(sProject, sBuild, buildProblem)).thenReturn(user1);

    Pair<User, String> result = heuristic.findResponsibleUser(buildProblemInfo);
    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result.first).isEqualTo(user1);
  }

  public void TestBuildProblemInfo_ResponsibleNotFound() {
    when(investigationsManager.findPreviousResponsible(sProject, sBuild, buildProblem)).thenReturn(null);

    Assertions.assertThat(heuristic.findResponsibleUser(buildProblemInfo)).isNull();
  }

  public void TestTestProblemInfo_ResponsibleFound() {
    when(investigationsManager.findPreviousResponsible(sProject, sBuild, sTest)).thenReturn(user2);

    Pair<User, String> result = heuristic.findResponsibleUser(testProblemInfo);
    Assertions.assertThat(result).isNotNull();
    Assertions.assertThat(result.first).isEqualTo(user2);
  }

  public void TestTestProblemInfo_ResponsibleNotFound() {
    when(investigationsManager.findPreviousResponsible(sProject, sBuild, sTest)).thenReturn(null);

    Assertions.assertThat(heuristic.findResponsibleUser(testProblemInfo)).isNull();
  }

  public void TestIncompatibleProblemInfo() {
    ProblemInfo problemInfo = new IncompatibleProblemInfo(sBuild, sProject, "Any text");

    Assertions.assertThat(heuristic.findResponsibleUser(problemInfo)).isNull();
  }

  class IncompatibleProblemInfo extends ProblemInfo {

     IncompatibleProblemInfo(@NotNull final SBuild sBuild,
                                   @NotNull final SProject project,
                                   @Nullable final String problemText) {
      super(sBuild, project, problemText);
    }
  }
}