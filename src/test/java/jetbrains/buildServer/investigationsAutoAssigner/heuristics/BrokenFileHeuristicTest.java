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

import com.intellij.openapi.util.Pair;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.processing.HeuristicContext;
import jetbrains.buildServer.investigationsAutoAssigner.processing.ModificationAnalyzerFactory;
import jetbrains.buildServer.investigationsAutoAssigner.utils.ProblemTextExtractor;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.vcs.SVcsModification;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import jetbrains.buildServer.vcs.VcsFileModification;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@Test
public class BrokenFileHeuristicTest extends BaseTestCase {

  private BrokenFileHeuristic myHeuristic;
  private SUser myUser;
  private SUser mySecondUser;
  private BuildPromotionEx myBuildPromotion;
  private ChangeDescriptor myChangeDescriptor;
  private ChangeDescriptor myChangeDescriptor2;
  private HeuristicContext myHeuristicContext;
  private STestRun mySTestRun;
  private ProblemTextExtractor myProblemTextExtractor;
  private ModificationAnalyzerFactory.ModificationAnalyzer myFirstVcsChangeWrapped;
  private ModificationAnalyzerFactory.ModificationAnalyzer myFirstVcsChangeWrapped2;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myProblemTextExtractor = Mockito.mock(ProblemTextExtractor.class);
    ModificationAnalyzerFactory modificationAnalyzerFactory = Mockito.mock(ModificationAnalyzerFactory.class);
    myHeuristic = new BrokenFileHeuristic(myProblemTextExtractor, modificationAnalyzerFactory);
    final SBuild SBuild = Mockito.mock(jetbrains.buildServer.serverSide.SBuild.class);
    final SProject SProject = Mockito.mock(jetbrains.buildServer.serverSide.SProject.class);
    myUser = Mockito.mock(SUser.class);
    when(myUser.getUsername()).thenReturn("myUser1");
    mySecondUser = Mockito.mock(SUser.class);
    when(mySecondUser.getUsername()).thenReturn("myUser2");
    mySTestRun = Mockito.mock(STestRun.class);
    myHeuristicContext = new HeuristicContext(SBuild,
                                              SProject,
                                              Collections.emptyList(),
                                              Collections.singletonList(mySTestRun),
                                              Collections.emptySet());
    myBuildPromotion = Mockito.mock(BuildPromotionEx.class);
    when(SBuild.getBuildPromotion()).thenReturn(myBuildPromotion);
    when(myProblemTextExtractor.getBuildProblemText(any())).thenReturn("I contain ./path1/path1/path1/filename");
    myChangeDescriptor = Mockito.mock(ChangeDescriptor.class);
    final SVcsModification vcsModification = Mockito.mock(SVcsModification.class);
    when(myChangeDescriptor.getRelatedVcsChange()).thenReturn(vcsModification);
    myFirstVcsChangeWrapped = Mockito.mock(ModificationAnalyzerFactory.ModificationAnalyzer.class);
    when(modificationAnalyzerFactory.getInstance(vcsModification)).thenReturn(myFirstVcsChangeWrapped);

    myChangeDescriptor2 = Mockito.mock(ChangeDescriptor.class);
    final SVcsModification vcsModification2 = Mockito.mock(SVcsModification.class);
    when(myChangeDescriptor2.getRelatedVcsChange()).thenReturn(vcsModification2);
    myFirstVcsChangeWrapped2 = Mockito.mock(ModificationAnalyzerFactory.ModificationAnalyzer.class);
    when(modificationAnalyzerFactory.getInstance(vcsModification2)).thenReturn(myFirstVcsChangeWrapped2);

    List<ChangeDescriptor> descriptors = Arrays.asList(myChangeDescriptor, myChangeDescriptor2);
    when(myBuildPromotion.getDetectedChanges(SelectPrevBuildPolicy.SINCE_LAST_BUILD, false))
      .thenReturn(descriptors);

    VcsFileModification mod1 = Mockito.mock(VcsFileModification.class);
    when(mod1.getRelativeFileName()).thenReturn("./path1/path1/path1/filename");
    when(vcsModification.getChanges()).thenReturn(Collections.singletonList(mod1));

    VcsFileModification mod4 = Mockito.mock(VcsFileModification.class);
    when(mod4.getRelativeFileName()).thenReturn("./path4/path4/path4/filename4");
    when(vcsModification2.getChanges()).thenReturn(Collections.singletonList(mod4));

  }

  public void TestNoDetectedChanges() {
    when(myBuildPromotion.getDetectedChanges(SelectPrevBuildPolicy.SINCE_LAST_BUILD, false))
      .thenReturn(Collections.emptyList());

    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);

    Assert.assertTrue(heuristicResult.isEmpty());
  }

  public void TestNoRelatedVcsChange() {
    when(myChangeDescriptor.getRelatedVcsChange()).thenReturn(null);
    when(myChangeDescriptor2.getRelatedVcsChange()).thenReturn(null);

    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);

    Assert.assertTrue(heuristicResult.isEmpty());
  }

  public void TestUnknownVcsUsername() {
    when(myFirstVcsChangeWrapped.findProblematicFile(anyString(), anySet())).thenThrow(IllegalStateException.class);

    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);

    Assert.assertTrue(heuristicResult.isEmpty());
  }

  public void TestCorrectCase() {
    String filePath = "./path1/path1/path1/filename";
    String theProblemText = "I contain " + filePath;
    when(myProblemTextExtractor.getBuildProblemText(any())).thenReturn(theProblemText);

    Pair<User, String> result = Pair.create(myUser, filePath);
    when(myFirstVcsChangeWrapped.findProblematicFile(theProblemText, Collections.emptySet())).thenReturn(result);
    when(myFirstVcsChangeWrapped2.findProblematicFile(theProblemText, Collections.emptySet())).thenReturn(null);

    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);

    Assert.assertFalse(heuristicResult.isEmpty());
    Responsibility responsibility = heuristicResult.getResponsibility(mySTestRun);
    Assert.assertNotNull(responsibility);
    Assert.assertEquals(responsibility.getUser(), myUser);

    when(myFirstVcsChangeWrapped.findProblematicFile(theProblemText, Collections.emptySet())).thenReturn(result);
    when(myFirstVcsChangeWrapped2.findProblematicFile(theProblemText, Collections.emptySet())).thenReturn(result);

    heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);
    Assert.assertFalse(heuristicResult.isEmpty());
    responsibility = heuristicResult.getResponsibility(mySTestRun);
    Assert.assertNotNull(responsibility);
    Assert.assertEquals(responsibility.getUser(), myUser);
  }

  public void TestManyCommitters() {
    String firstFilePath = "./path1/path1/path1/filename";
    String secondFilePath = "./path4/path4/path4/filename";
    String theProblemText = "I contain " + firstFilePath + "and" + secondFilePath;
    when(myProblemTextExtractor.getBuildProblemText(any())).thenReturn(theProblemText);

    Pair<User, String> firstResult = Pair.create(myUser, firstFilePath);
    Pair<User, String> secondResult = Pair.create(mySecondUser, secondFilePath);
    when(myFirstVcsChangeWrapped.findProblematicFile(theProblemText, Collections.emptySet())).thenReturn(firstResult);
    when(myFirstVcsChangeWrapped2.findProblematicFile(theProblemText, Collections.emptySet())).thenReturn(secondResult);

    HeuristicResult result = myHeuristic.findResponsibleUser(myHeuristicContext);
    Assert.assertTrue(result.isEmpty());
  }
}
