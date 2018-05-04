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

import com.intellij.openapi.util.Pair;
import java.util.Arrays;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.iaa.heuristics.Heuristic;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.User;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Test
public class ResponsibleUserFinderTest extends BaseTestCase {

  private ResponsibleUserFinder myUserFinder;
  private Heuristic myHeuristic;
  private Heuristic myHeuristic2;
  private ProblemInfo myProblemInfo;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myHeuristic = Mockito.mock(Heuristic.class);
    myHeuristic2 = Mockito.mock(Heuristic.class);
    final SBuild SBuild = Mockito.mock(jetbrains.buildServer.serverSide.SBuild.class);
    final SProject sProject = Mockito.mock(SProject.class);
    myProblemInfo = new ProblemInfo(SBuild, sProject, "Problem text");
    myUserFinder = new ResponsibleUserFinder(Arrays.asList(myHeuristic, myHeuristic2));
  }

  public void Test_FindResponsibleUser_ResponsibleNotFound() {
    when(myHeuristic.findResponsibleUser(any())).thenReturn(null);
    when(myHeuristic2.findResponsibleUser(any())).thenReturn(null);
    Pair<User, String> responsible = myUserFinder.findResponsibleUser(myProblemInfo);

    Assert.assertNull(responsible);
  }

  public void Test_FindResponsibleUser_CheckSecondIfNotFoundInFirst() {
    when(myHeuristic.findResponsibleUser(any())).thenReturn(null);

    myUserFinder.findResponsibleUser(myProblemInfo);

    Mockito.verify(myHeuristic2, Mockito.atLeastOnce()).findResponsibleUser(any());
  }

  public void Test_FindResponsibleUser_NotCallSecondIfFoundInFirst() {
    SUser sUser = Mockito.mock(SUser.class);
    Pair<User, String> anyPair = new Pair<>(sUser, "Failed description");
    when(myHeuristic.findResponsibleUser(any())).thenReturn(anyPair);

    myUserFinder.findResponsibleUser(myProblemInfo);

    Mockito.verify(myHeuristic2, Mockito.never()).findResponsibleUser(any());
  }

  public void Test_FindResponsibleUser_TakeFirstFound() {
    SUser sUser = Mockito.mock(SUser.class);
    Pair<User, String> anyPair = new Pair<>(sUser, "Description 1");
    Pair<User, String> anyPair2 = new Pair<>(sUser, "Description 2");
    when(myHeuristic.findResponsibleUser(any())).thenReturn(anyPair);
    when(myHeuristic2.findResponsibleUser(any())).thenReturn(anyPair2);

    Pair<User, String> responsible = myUserFinder.findResponsibleUser(myProblemInfo);

    Assert.assertNotNull(responsible);
    Assert.assertEquals(responsible.second, "Description 1");
  }
}
