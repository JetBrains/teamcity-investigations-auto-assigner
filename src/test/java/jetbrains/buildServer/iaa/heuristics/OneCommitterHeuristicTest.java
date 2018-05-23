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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.iaa.processing.HeuristicContext;
import jetbrains.buildServer.iaa.common.HeuristicResult;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.STestRun;
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
  private STestRun mySTestRun;
  private HeuristicContext myHeuristicContext;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myHeuristic = new OneCommitterHeuristic();
    final SBuild sBuild = Mockito.mock(SBuild.class);
    myUserSetMock = Mockito.mock(UserSet.class);
    myFirstUser = Mockito.mock(User.class);
    mySecondUser = Mockito.mock(SUser.class);
    when(sBuild.getCommitters(SelectPrevBuildPolicy.SINCE_LAST_BUILD)).thenReturn(myUserSetMock);
    mySTestRun = Mockito.mock(STestRun.class);
    myHeuristicContext =
      new HeuristicContext(sBuild, Collections.emptyList(), Collections.singletonList(mySTestRun));
  }

  public void TestWithOneResponsible() {
    when(myUserSetMock.getUsers()).thenReturn(new HashSet<>(Collections.singletonList(myFirstUser)));
    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);

    Assert.assertFalse(heuristicResult.isEmpty());
    Assert.assertNotNull(heuristicResult.getResponsibility(mySTestRun));
    Assert.assertEquals(heuristicResult.getResponsibility(mySTestRun).getUser(), myFirstUser);
  }

  public void TestWithoutResponsible() {
    when(myUserSetMock.getUsers()).thenReturn(new HashSet<User>());

    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);

    Assert.assertTrue(heuristicResult.isEmpty());
  }

  public void TestWithManyResponsible() {
    when(myUserSetMock.getUsers()).thenReturn(new HashSet<>(Arrays.asList(myFirstUser, mySecondUser)));

    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);

    Assert.assertTrue(heuristicResult.isEmpty());
  }
}
