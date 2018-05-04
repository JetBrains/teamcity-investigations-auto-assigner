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
import java.util.List;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.iaa.ProblemInfo;
import jetbrains.buildServer.serverSide.BuildPromotionEx;
import jetbrains.buildServer.serverSide.ChangeDescriptor;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.vcs.SVcsModification;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import jetbrains.buildServer.vcs.VcsFileModification;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;

@Test
public class BrokenFileHeuristicTest extends BaseTestCase {

  private BrokenFileHeuristic myHeuristic;
  private SBuild mySBuildMock;
  private SUser myUser;
  private SUser mySecondUser;
  private ProblemInfo myProblemInfoWithMock;
  private BuildPromotionEx myBuildPromotion;
  private ChangeDescriptor myChangeDescriptor;
  private ChangeDescriptor myChangeDescriptor2;
  private SVcsModification myVcsModification;
  private SVcsModification sVcsModification2;
  private SProject mySProjectMock;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myHeuristic = new BrokenFileHeuristic();
    mySBuildMock = Mockito.mock(SBuild.class);
    mySProjectMock = Mockito.mock(SProject.class);
    myUser = Mockito.mock(SUser.class);
    mySecondUser = Mockito.mock(SUser.class);
    myProblemInfoWithMock = new ProblemInfo(mySBuildMock, mySProjectMock, "problem text");

    myBuildPromotion = Mockito.mock(BuildPromotionEx.class);
    when(mySBuildMock.getBuildPromotion()).thenReturn(myBuildPromotion);

    myChangeDescriptor = Mockito.mock(ChangeDescriptor.class);
    myVcsModification = Mockito.mock(SVcsModification.class);
    when(myChangeDescriptor.getRelatedVcsChange()).thenReturn(myVcsModification);

    myChangeDescriptor2 = Mockito.mock(ChangeDescriptor.class);
    sVcsModification2 = Mockito.mock(SVcsModification.class);
    when(myChangeDescriptor2.getRelatedVcsChange()).thenReturn(sVcsModification2);

    List<ChangeDescriptor> descriptors = Arrays.asList(myChangeDescriptor, myChangeDescriptor2);
    when(myBuildPromotion.getDetectedChanges(SelectPrevBuildPolicy.SINCE_LAST_BUILD, true)).thenReturn(descriptors);

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
    when(sVcsModification2.getChanges()).thenReturn(Arrays.asList(mod4, mod5, mod6));
  }

  public void TestNoDetectedChanges() {
    when(myBuildPromotion.getDetectedChanges(SelectPrevBuildPolicy.SINCE_LAST_BUILD, true))
      .thenReturn(Collections.emptyList());
    Assert.assertNull(myHeuristic.findResponsibleUser(myProblemInfoWithMock));
  }

  public void TestNoRelatedVcsChange() {
    when(myChangeDescriptor.getRelatedVcsChange()).thenReturn(null);
    when(myChangeDescriptor2.getRelatedVcsChange()).thenReturn(null);
    Assert.assertNull(myHeuristic.findResponsibleUser(myProblemInfoWithMock));
  }

  public void TestWithDetectedBrokenFileWithoutCommitters() {
    when(myVcsModification.getCommitters()).thenReturn(Collections.emptyList());
    Pair<User, String> responsible = myHeuristic.findResponsibleUser(myProblemInfoWithMock);
    Assert.assertNull(responsible);
  }

  public void TestCorrectCase() {
    ProblemInfo problemInfoWithMock = new ProblemInfo(mySBuildMock, mySProjectMock,
                                                      "I contain ./path1/path1/path1/filename");

    when(myVcsModification.getCommitters()).thenReturn(Collections.singletonList(myUser));
    when(sVcsModification2.getCommitters()).thenReturn(Collections.emptyList());
    Pair<User, String> responsible = myHeuristic.findResponsibleUser(problemInfoWithMock);
    Assert.assertNotNull(responsible);
    Assert.assertEquals(responsible.first, myUser);

    problemInfoWithMock = new ProblemInfo(mySBuildMock, mySProjectMock,
                                          "I contain ./path1/path1/path1/filename and " +
                                          "./path4/path4/path4/filename4");
    when(myVcsModification.getCommitters()).thenReturn(Collections.singletonList(myUser));
    when(sVcsModification2.getCommitters()).thenReturn(Collections.singletonList(myUser));
    responsible = myHeuristic.findResponsibleUser(problemInfoWithMock);
    Assert.assertNotNull(responsible);
    Assert.assertEquals(responsible.first, myUser);
  }

  public void TestManyCommitters() {
    ProblemInfo problemInfoWithMock = new ProblemInfo(mySBuildMock, mySProjectMock,
                                                      "I contain ./path1/path1/path1/filename" +
                                                      "and ./path4/path4/path4/filename4");
    when(myVcsModification.getCommitters()).thenReturn(Collections.singletonList(myUser));
    when(sVcsModification2.getCommitters()).thenReturn(Collections.singletonList(mySecondUser));
    Pair<User, String> responsible = myHeuristic.findResponsibleUser(problemInfoWithMock);
    Assert.assertNull(responsible);

    when(myVcsModification.getCommitters()).thenReturn(Arrays.asList(myUser, mySecondUser));
    responsible = myHeuristic.findResponsibleUser(problemInfoWithMock);
    Assert.assertNull(responsible);
  }
}
