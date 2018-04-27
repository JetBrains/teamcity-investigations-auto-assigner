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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.iaa.ProblemInfo;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.users.UserSet;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;

@Test
public class OneCommitterHeuristicTest extends BaseTestCase {

  private OneCommitterHeuristic heuristic;
  private UserSet userSetMock;
  private User firstUser;
  private SUser secondUser;
  private ProblemInfo problemInfo;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    heuristic = new OneCommitterHeuristic();
    final SBuild sBuildMock = Mockito.mock(SBuild.class);
    final SProject sProjectMock = Mockito.mock(SProject.class);
    userSetMock = Mockito.mock(UserSet.class);
    firstUser = Mockito.mock(User.class);
    secondUser = Mockito.mock(SUser.class);
    when(sBuildMock.getCommitters(SelectPrevBuildPolicy.SINCE_LAST_BUILD)).thenReturn(userSetMock);
    problemInfo = new ProblemInfo(sBuildMock, sProjectMock, "problem text");
  }

  public void TestWithOneResponsible() {
    when(userSetMock.getUsers()).thenReturn(new HashSet<>(Collections.singletonList(firstUser)));
    Pair<User, String> responsible = heuristic.findResponsibleUser(problemInfo);
    Assert.assertNotNull(responsible);
    Assert.assertEquals(responsible.first, firstUser);
  }

  public void TestWithoutResponsible() {
    when(userSetMock.getUsers()).thenReturn(new HashSet<User>());
    Pair<User, String> responsible = heuristic.findResponsibleUser(problemInfo);
    Assert.assertNull(responsible);
  }

  public void TestWithManyResponsible() {
    when(userSetMock.getUsers()).thenReturn(new HashSet<>(Arrays.asList(firstUser, secondUser)));
    Pair<User, String> responsible = heuristic.findResponsibleUser(problemInfo);
    Assert.assertNull(responsible);
  }
}
