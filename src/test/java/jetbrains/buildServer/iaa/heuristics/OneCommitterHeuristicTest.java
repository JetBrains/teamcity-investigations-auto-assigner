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

  private OneCommitterHeuristic myHeuristic;
  private UserSet myUserSetMock;
  private User myFirstUser;
  private SUser mySecondUser;
  private ProblemInfo myProblemInfo;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myHeuristic = new OneCommitterHeuristic();
    final SBuild sBuildMock = Mockito.mock(SBuild.class);
    final SProject sProjectMock = Mockito.mock(SProject.class);
    myUserSetMock = Mockito.mock(UserSet.class);
    myFirstUser = Mockito.mock(User.class);
    mySecondUser = Mockito.mock(SUser.class);
    when(sBuildMock.getCommitters(SelectPrevBuildPolicy.SINCE_LAST_BUILD)).thenReturn(myUserSetMock);
    myProblemInfo = new ProblemInfo(sBuildMock, sProjectMock, "problem text");
  }

  public void TestWithOneResponsible() {
    //when(myUserSetMock.getUsers()).thenReturn(new HashSet<>(Collections.singletonList(myFirstUser)));
    //Pair<User, String> responsible = myHeuristic.findResponsibleUser(myProblemInfo);
    //Assert.assertNotNull(responsible);
    //Assert.assertEquals(responsible.first, myFirstUser);
    Assert.fail();
  }

  public void TestWithoutResponsible() {
    //when(myUserSetMock.getUsers()).thenReturn(new HashSet<User>());
    //Pair<User, String> responsible = myHeuristic.findResponsibleUser(myProblemInfo);
    //Assert.assertNull(responsible);
    Assert.fail();
  }

  public void TestWithManyResponsible() {
    //when(myUserSetMock.getUsers()).thenReturn(new HashSet<>(Arrays.asList(myFirstUser, mySecondUser)));
    //Pair<User, String> responsible = myHeuristic.findResponsibleUser(myProblemInfo);
    Assert.fail();
    //Assert.assertNull(responsible);
  }
}
