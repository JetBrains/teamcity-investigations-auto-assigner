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

package jetbrains.buildServer.investigationsAutoAssigner.heuristics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicNotApplicableException;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.processing.HeuristicContext;
import jetbrains.buildServer.investigationsAutoAssigner.processing.ModificationAnalyzerFactory;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.users.impl.UserEx;
import jetbrains.buildServer.vcs.SVcsModification;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

@Test
public class OneCommitterHeuristicTest extends BaseTestCase {

  private OneCommitterHeuristic myHeuristic;
  private UserEx myFirstUser;
  private UserEx mySecondUser;
  private STestRun mySTestRun;
  private HeuristicContext myHeuristicContext;
  private SBuild mySBuild;
  private List<SVcsModification> myChanges;
  private SVcsModification myMod1;
  private SVcsModification myMod2;
  private ModificationAnalyzerFactory.ModificationAnalyzer myFirstModificationAnalyzer;
  private ModificationAnalyzerFactory.ModificationAnalyzer mySecondModificationAnalyzer;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    ModificationAnalyzerFactory modificationAnalyzerFactory = Mockito.mock(ModificationAnalyzerFactory.class);
    myFirstModificationAnalyzer = Mockito.mock(ModificationAnalyzerFactory.ModificationAnalyzer.class);
    mySecondModificationAnalyzer = Mockito.mock(ModificationAnalyzerFactory.ModificationAnalyzer.class);
    myHeuristic = new OneCommitterHeuristic(modificationAnalyzerFactory);
    mySBuild = Mockito.mock(SBuild.class);

    String firstUserUsername = "myFirstUser";
    myFirstUser = Mockito.mock(UserEx.class);
    myMod1 = Mockito.mock(SVcsModification.class);

    when(myFirstUser.getUsername()).thenReturn(firstUserUsername);
    when(modificationAnalyzerFactory.getInstance(myMod1)).thenReturn(myFirstModificationAnalyzer);

    String secondUserUsername = "mySecondUser";
    mySecondUser = Mockito.mock(UserEx.class);
    myMod2 = Mockito.mock(SVcsModification.class);
    when(mySecondUser.getUsername()).thenReturn(secondUserUsername);
    when(modificationAnalyzerFactory.getInstance(myMod2)).thenReturn(mySecondModificationAnalyzer);

    myChanges = new ArrayList<>();
    when(mySBuild.getChanges(SelectPrevBuildPolicy.SINCE_LAST_BUILD, true))
      .thenReturn(myChanges);
    mySTestRun = Mockito.mock(STestRun.class);
    myHeuristicContext = new HeuristicContext(mySBuild,
                                              Mockito.mock(SProject.class),
                                              Collections.emptyList(),
                                              Collections.singletonList(mySTestRun),
                                              Collections.emptySet());
  }

  public void TestWithOneResponsible() {
    myChanges.add(myMod1);
    when(myFirstModificationAnalyzer.getOnlyCommitter(anySet())).thenReturn(myFirstUser);

    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);
    Responsibility responsibility = heuristicResult.getResponsibility(mySTestRun);

    Assert.assertFalse(heuristicResult.isEmpty());
    Assert.assertNotNull(responsibility);
    Assert.assertEquals(responsibility.getUser(), myFirstUser);
  }

  public void TestWithoutResponsible() {
    assert myChanges.isEmpty();

    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);

    Assert.assertTrue(heuristicResult.isEmpty());
  }

  public void TestWithManyResponsible() {
    myChanges.add(myMod1);
    myChanges.add(myMod2);
    when(myFirstModificationAnalyzer.getOnlyCommitter(anySet())).thenReturn(myFirstUser);
    when(mySecondModificationAnalyzer.getOnlyCommitter(anySet())).thenReturn(mySecondUser);

    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);

    Assert.assertTrue(heuristicResult.isEmpty());
  }

  public void TestUsersToIgnore() {
    myChanges.add(myMod1);
    myChanges.add(myMod2);
    when(myFirstModificationAnalyzer.getOnlyCommitter(anySet())).thenReturn(null);
    when(mySecondModificationAnalyzer.getOnlyCommitter(anySet())).thenReturn(mySecondUser);

    HeuristicContext hc = new HeuristicContext(mySBuild,
                                               Mockito.mock(SProject.class),
                                               Collections.emptyList(),
                                               Collections.singletonList(mySTestRun),
                                               Collections.singleton(myFirstUser.getUsername()));
    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(hc);
    Responsibility responsibility = heuristicResult.getResponsibility(mySTestRun);

    Assert.assertFalse(heuristicResult.isEmpty());
    Assert.assertNotNull(responsibility);
    Assert.assertEquals(responsibility.getUser(), mySecondUser);
  }

  public void TestUnknownUser() {
    myChanges.add(myMod1);
    myChanges.add(myMod2);
    when(myFirstModificationAnalyzer.getOnlyCommitter(anySet())).thenReturn(myFirstUser);
    when(mySecondModificationAnalyzer.getOnlyCommitter(anySet())).thenThrow(HeuristicNotApplicableException.class);

    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);

    Assert.assertTrue(heuristicResult.isEmpty());
  }
}
