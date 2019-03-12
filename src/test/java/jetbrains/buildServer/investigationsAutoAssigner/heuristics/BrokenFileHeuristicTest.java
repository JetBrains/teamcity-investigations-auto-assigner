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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.processing.HeuristicContext;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.utils.ProblemTextExtractor;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.vcs.SVcsModification;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import jetbrains.buildServer.vcs.VcsFileModification;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Test
public class BrokenFileHeuristicTest extends BaseTestCase {

  private BrokenFileHeuristic myHeuristic;
  private SUser myUser;
  private SUser mySecondUser;
  private BuildPromotionEx myBuildPromotion;
  private ChangeDescriptor myChangeDescriptor;
  private ChangeDescriptor myChangeDescriptor2;
  private SVcsModification myVcsModification;
  private SVcsModification myVcsModification2;
  private HeuristicContext myHeuristicContext;
  private STestRun mySTestRun;
  private ProblemTextExtractor myProblemTextExtractor;
  private SBuild mySBuild;
  private SProject mySProject;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myProblemTextExtractor = Mockito.mock(ProblemTextExtractor.class);
    myHeuristic = new BrokenFileHeuristic(myProblemTextExtractor, new VcsChangeWrapperFactory());
    mySBuild = Mockito.mock(jetbrains.buildServer.serverSide.SBuild.class);
    mySProject = Mockito.mock(jetbrains.buildServer.serverSide.SProject.class);
    myUser = Mockito.mock(SUser.class);
    when(myUser.getUsername()).thenReturn("myUser1");
    mySecondUser = Mockito.mock(SUser.class);
    when(mySecondUser.getUsername()).thenReturn("myUser2");
    mySTestRun = Mockito.mock(STestRun.class);
    myHeuristicContext = new HeuristicContext(mySBuild,
                                              mySProject,
                                              Collections.emptyList(),
                                              Collections.singletonList(mySTestRun),
                                              Collections.emptyList());
    myBuildPromotion = Mockito.mock(BuildPromotionEx.class);
    when(mySBuild.getBuildPromotion()).thenReturn(myBuildPromotion);
    when(myProblemTextExtractor.getBuildProblemText(any())).thenReturn("I contain ./path1/path1/path1/filename");
    myChangeDescriptor = Mockito.mock(ChangeDescriptor.class);
    myVcsModification = Mockito.mock(SVcsModification.class);
    when(myChangeDescriptor.getRelatedVcsChange()).thenReturn(myVcsModification);

    myChangeDescriptor2 = Mockito.mock(ChangeDescriptor.class);
    myVcsModification2 = Mockito.mock(SVcsModification.class);
    when(myChangeDescriptor2.getRelatedVcsChange()).thenReturn(myVcsModification2);

    List<ChangeDescriptor> descriptors = Arrays.asList(myChangeDescriptor, myChangeDescriptor2);
    when(myBuildPromotion.getDetectedChanges(SelectPrevBuildPolicy.SINCE_LAST_BUILD, false))
      .thenReturn(descriptors);

    VcsFileModification mod1 = Mockito.mock(VcsFileModification.class);
    VcsFileModification mod2 = Mockito.mock(VcsFileModification.class);
    VcsFileModification mod3 = Mockito.mock(VcsFileModification.class);
    when(mod1.getRelativeFileName()).thenReturn("./path1/path1/path1/filename");
    when(mod2.getRelativeFileName()).thenReturn("./path2/path2/path2/filename2");
    when(mod3.getRelativeFileName()).thenReturn("./path3/path3/path3/filename3");
    when(myVcsModification.getChanges()).thenReturn(Arrays.asList(mod1, mod2, mod3));

    VcsFileModification mod4 = Mockito.mock(VcsFileModification.class);
    VcsFileModification mod5 = Mockito.mock(VcsFileModification.class);
    VcsFileModification mod6 = Mockito.mock(VcsFileModification.class);
    when(mod4.getRelativeFileName()).thenReturn("./path4/path4/path4/filename4");
    when(mod5.getRelativeFileName()).thenReturn("./path5/path5/path5/filename5");
    when(mod6.getRelativeFileName()).thenReturn("./path6/path6/path6/filename6");
    when(myVcsModification2.getChanges()).thenReturn(Arrays.asList(mod4, mod5, mod6));
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

  public void TestWithDetectedBrokenFileWithoutCommitters() {
    when(myVcsModification.getCommitters()).thenReturn(Collections.emptyList());

    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);

    Assert.assertTrue(heuristicResult.isEmpty());
  }

  public void TestCorrectCase() {
    when(myProblemTextExtractor.getBuildProblemText(any())).thenReturn("I contain ./path1/path1/path1/filename");

    when(myVcsModification.getCommitters()).thenReturn(Collections.singletonList(myUser));
    when(myVcsModification2.getCommitters()).thenReturn(Collections.emptyList());

    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);

    Assert.assertFalse(heuristicResult.isEmpty());
    Responsibility responsibility = heuristicResult.getResponsibility(mySTestRun);
    assert  responsibility != null;
    Assert.assertEquals(responsibility.getUser(), myUser);

    when(myProblemTextExtractor.getBuildProblemText(any())).thenReturn("I contain ./path1/path1/path1/filename and " +
                                                                       "./path4/path4/path4/filename4");

    when(myVcsModification.getCommitters()).thenReturn(Collections.singletonList(myUser));
    when(myVcsModification2.getCommitters()).thenReturn(Collections.singletonList(myUser));
    heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);
    Assert.assertFalse(heuristicResult.isEmpty());
    responsibility = heuristicResult.getResponsibility(mySTestRun);
    assert responsibility != null;
    Assert.assertEquals(responsibility.getUser(), myUser);
  }

  public void TestGitIgnoreCase() {
    when(myProblemTextExtractor.getBuildProblemText(any())).thenReturn("I contain ./no/file/here");
    VcsFileModification mod = Mockito.mock(VcsFileModification.class);
    when(myVcsModification.getChanges()).thenReturn(Collections.singletonList(mod));
    when(mod.getRelativeFileName()).thenReturn(".gitignore");

    when(myVcsModification.getCommitters()).thenReturn(Collections.singletonList(myUser));
    when(myVcsModification2.getCommitters()).thenReturn(Collections.singletonList(mySecondUser));

    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);

    Assert.assertTrue(heuristicResult.isEmpty());
  }

  public void TestManyCommitters() {
    when(myProblemTextExtractor.getBuildProblemText(any())).thenReturn("I contain ./path1/path1/path1/filename" +
                                                                       "and ./path4/path4/path4/filename4");
    when(myVcsModification.getCommitters()).thenReturn(Collections.singletonList(myUser));
    when(myVcsModification2.getCommitters()).thenReturn(Collections.singletonList(mySecondUser));
    HeuristicResult result = myHeuristic.findResponsibleUser(myHeuristicContext);
    Assert.assertTrue(result.isEmpty());

    when(myVcsModification.getCommitters()).thenReturn(Arrays.asList(myUser, mySecondUser));
    result = myHeuristic.findResponsibleUser(myHeuristicContext);
    Assert.assertTrue(result.isEmpty());
  }

  public void TestWhiteList() {
    when(myProblemTextExtractor.getBuildProblemText(any())).thenReturn("I contain ./path1/path1/path1/filename" +
                                                                       "and ./path4/path4/path4/filename4");
    when(myVcsModification.getCommitters()).thenReturn(Collections.singletonList(myUser));
    when(myVcsModification2.getCommitters()).thenReturn(Collections.singletonList(mySecondUser));
    HeuristicContext heuristicContexts = new HeuristicContext(mySBuild,
                                                              mySProject,
                                                              Collections.emptyList(),
                                                              Collections.singletonList(mySTestRun),
                                                              Collections.singletonList(myUser.getUsername()));
    HeuristicResult result = myHeuristic.findResponsibleUser(heuristicContexts);
    Assert.assertFalse(result.isEmpty());

    when(myVcsModification.getCommitters()).thenReturn(Arrays.asList(myUser, mySecondUser));
    result = myHeuristic.findResponsibleUser(heuristicContexts);
    Assert.assertFalse(result.isEmpty());
  }

  public void TestSmallFilePaths() {
    when(myProblemTextExtractor.getBuildProblemText(any())).thenReturn("I contain ./any/other/build/text");
    VcsFileModification mod = Mockito.mock(VcsFileModification.class);
    when(myVcsModification.getChanges()).thenReturn(Collections.singletonList(mod));
    when(mod.getRelativeFileName()).thenReturn("build.gradle");

    when(myVcsModification.getCommitters()).thenReturn(Collections.singletonList(myUser));
    when(myVcsModification2.getCommitters()).thenReturn(Collections.singletonList(mySecondUser));

    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);

    Assert.assertTrue(heuristicResult.isEmpty());
  }

  public void TestSmallFilePaths2() {
    when(myProblemTextExtractor.getBuildProblemText(any())).thenReturn("I contain ./any/hmbrm/build.gradle");
    VcsFileModification mod = Mockito.mock(VcsFileModification.class);
    when(myVcsModification.getChanges()).thenReturn(Collections.singletonList(mod));
    when(mod.getRelativeFileName()).thenReturn("build.gradle");

    when(myVcsModification.getCommitters()).thenReturn(Collections.singletonList(myUser));
    when(myVcsModification2.getCommitters()).thenReturn(Collections.singletonList(mySecondUser));

    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);

    Assert.assertFalse(heuristicResult.isEmpty());
  }
}
