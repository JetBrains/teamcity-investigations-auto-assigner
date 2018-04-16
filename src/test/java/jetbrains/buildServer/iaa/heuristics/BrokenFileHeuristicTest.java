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
import jetbrains.buildServer.users.SUser;
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

  private BrokenFileHeuristic heuristic;
  private SBuild sBuildMock;
  private SUser oneUser;
  private SUser secondUser;
  private ProblemInfo problemInfoWithMock;
  private BuildPromotionEx buildPromotion;
  private ChangeDescriptor changeDescriptor;
  private ChangeDescriptor changeDescriptor2;
  private SVcsModification sVcsModification;
  private SVcsModification sVcsModification2;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    heuristic = new BrokenFileHeuristic();
    sBuildMock = Mockito.mock(SBuild.class);
    oneUser = Mockito.mock(SUser.class);
    secondUser = Mockito.mock(SUser.class);
    problemInfoWithMock = new ProblemInfo(sBuildMock, "problem text");

    buildPromotion = Mockito.mock(BuildPromotionEx.class);
    when(sBuildMock.getBuildPromotion()).thenReturn(buildPromotion);

    changeDescriptor = Mockito.mock(ChangeDescriptor.class);
    sVcsModification = Mockito.mock(SVcsModification.class);
    when(changeDescriptor.getRelatedVcsChange()).thenReturn(sVcsModification);

    changeDescriptor2 = Mockito.mock(ChangeDescriptor.class);
    sVcsModification2 = Mockito.mock(SVcsModification.class);
    when(changeDescriptor2.getRelatedVcsChange()).thenReturn(sVcsModification2);

    List<ChangeDescriptor> descriptors = Arrays.asList(changeDescriptor, changeDescriptor2);
    when(buildPromotion.getDetectedChanges(SelectPrevBuildPolicy.SINCE_LAST_BUILD, true)).thenReturn(descriptors);

    VcsFileModification mod1 = Mockito.mock(VcsFileModification.class);
    VcsFileModification mod2 = Mockito.mock(VcsFileModification.class);
    VcsFileModification mod3 = Mockito.mock(VcsFileModification.class);
    when(mod1.getRelativeFileName()).thenReturn("./path1/path1/path1/filename");
    when(mod2.getRelativeFileName()).thenReturn("./path2/path2/path2/filename2");
    when(mod3.getRelativeFileName()).thenReturn("./path3/path3/path3/filename3");
    when(sVcsModification.getChanges()).thenReturn(Arrays.asList(mod1, mod2, mod3));

    VcsFileModification mod4 = Mockito.mock(VcsFileModification.class);
    VcsFileModification mod5 = Mockito.mock(VcsFileModification.class);
    VcsFileModification mod6 = Mockito.mock(VcsFileModification.class);
    when(mod4.getRelativeFileName()).thenReturn("./path4/path4/path4/filename4");
    when(mod5.getRelativeFileName()).thenReturn("./path5/path5/path5/filename5");
    when(mod6.getRelativeFileName()).thenReturn("./path6/path6/path6/filename6");
    when(sVcsModification2.getChanges()).thenReturn(Arrays.asList(mod4, mod5, mod6));
  }

  public void TestNoDetectedChanges() {
    when(buildPromotion.getDetectedChanges(SelectPrevBuildPolicy.SINCE_LAST_BUILD, true)).thenReturn(Collections.emptyList());
    Assert.assertNull(heuristic.findResponsibleUser(problemInfoWithMock));
  }

  public void TestNoRelatedVcsChange() {
    when(changeDescriptor.getRelatedVcsChange()).thenReturn(null);
    when(changeDescriptor2.getRelatedVcsChange()).thenReturn(null);
    Assert.assertNull(heuristic.findResponsibleUser(problemInfoWithMock));
  }

  public void TestWithDetectedBrokenFileWithoutCommitters() {
    when(sVcsModification.getCommitters()).thenReturn(Collections.emptyList());
    Pair<SUser, String> responsible = heuristic.findResponsibleUser(problemInfoWithMock);
    Assert.assertNull(responsible);
  }

  public void TestCorrectCase() {
    ProblemInfo problemInfoWithMock = new ProblemInfo(sBuildMock, "I contain ./path1/path1/path1/filename");

    when(sVcsModification.getCommitters()).thenReturn(Collections.singletonList(oneUser));
    when(sVcsModification2.getCommitters()).thenReturn(Collections.emptyList());
    Pair<SUser, String> responsible = heuristic.findResponsibleUser(problemInfoWithMock);
    Assert.assertNotNull(responsible);
    Assert.assertEquals(responsible.first, oneUser);

    problemInfoWithMock = new ProblemInfo(sBuildMock, "I contain ./path1/path1/path1/filename" +
                                                                  "and ./path4/path4/path4/filename4");
    when(sVcsModification.getCommitters()).thenReturn(Collections.singletonList(oneUser));
    when(sVcsModification2.getCommitters()).thenReturn(Collections.singletonList(oneUser));
    responsible = heuristic.findResponsibleUser(problemInfoWithMock);
    Assert.assertNotNull(responsible);
    Assert.assertEquals(responsible.first, oneUser);
  }

  public void TestManyCommitters() {
    ProblemInfo problemInfoWithMock = new ProblemInfo(sBuildMock, "I contain ./path1/path1/path1/filename" +
                                                                  "and ./path4/path4/path4/filename4");
    when(sVcsModification.getCommitters()).thenReturn(Collections.singletonList(oneUser));
    when(sVcsModification2.getCommitters()).thenReturn(Collections.singletonList(secondUser));
    Pair<SUser, String> responsible = heuristic.findResponsibleUser(problemInfoWithMock);
    Assert.assertNull(responsible);

    when(sVcsModification.getCommitters()).thenReturn(Arrays.asList(oneUser, secondUser));
    responsible = heuristic.findResponsibleUser(problemInfoWithMock);
    Assert.assertNull(responsible);
  }
}
