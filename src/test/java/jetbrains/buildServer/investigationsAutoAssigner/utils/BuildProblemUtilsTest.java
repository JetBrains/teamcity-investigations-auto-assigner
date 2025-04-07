/*
 * Copyright 2000-2025 JetBrains s.r.o.
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

package jetbrains.buildServer.investigationsAutoAssigner.utils;


import java.util.Collections;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.BuildPromotionEx;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test
public class BuildProblemUtilsTest extends BaseTestCase {

  private BuildProblemUtils myBuildProblemUtils;
  private BuildPromotionEx myCurrentBuildPromotionMock;
  private BuildPromotionEx myPreviousBuildPromotionMock;
  private BuildProblem myBuildProblemMock;

  @BeforeMethod
  @Override
  protected void setUp() {
    myBuildProblemUtils = new BuildProblemUtils();
    myCurrentBuildPromotionMock = Mockito.mock(BuildPromotionEx.class);
    myPreviousBuildPromotionMock = Mockito.mock(BuildPromotionEx.class);
    myBuildProblemMock = Mockito.mock(BuildProblem.class);
  }

  @Test
  void testIsNewPreviousBuildPromotionIsNull() {
    Mockito.when(myCurrentBuildPromotionMock.getPreviousBuildPromotion(SelectPrevBuildPolicy.SINCE_LAST_BUILD)).thenReturn(null);
    Mockito.when(myBuildProblemMock.getBuildPromotion()).thenReturn(myCurrentBuildPromotionMock);

    assertTrue(myBuildProblemUtils.isNew(myBuildProblemMock));
  }

  @Test
  void testIsNewPreviousBuildPromotionWithoutProblems() {
    Mockito.when(myCurrentBuildPromotionMock.getPreviousBuildPromotion(SelectPrevBuildPolicy.SINCE_LAST_BUILD)).thenReturn(myPreviousBuildPromotionMock);
    Mockito.when(myPreviousBuildPromotionMock.getBuildProblems()).thenReturn(Collections.emptyList());
    Mockito.when(myBuildProblemMock.getBuildPromotion()).thenReturn(myCurrentBuildPromotionMock);

    assertTrue(myBuildProblemUtils.isNew(myBuildProblemMock));
  }

  @Test
  void testIsNewPreviousBuildPromotionHasDifferentProblem() {
    Mockito.when(myBuildProblemMock.getId()).thenReturn(1);
    BuildProblem myBuildProblemDifferentMock = Mockito.mock(BuildProblem.class);
    Mockito.when(myBuildProblemDifferentMock.getId()).thenReturn(2);

    Mockito.when(myCurrentBuildPromotionMock.getPreviousBuildPromotion(SelectPrevBuildPolicy.SINCE_LAST_BUILD)).thenReturn(myPreviousBuildPromotionMock);
    Mockito.when(myPreviousBuildPromotionMock.getBuildProblems()).thenReturn(Collections.singletonList(myBuildProblemDifferentMock));
    Mockito.when(myBuildProblemMock.getBuildPromotion()).thenReturn(myCurrentBuildPromotionMock);

    assertTrue(myBuildProblemUtils.isNew(myBuildProblemMock));
  }

  @Test
  void testIsNewPreviousBuildPromotionHasSameProblem() {
    Mockito.when(myBuildProblemMock.getId()).thenReturn(1);
    Mockito.when(myCurrentBuildPromotionMock.getPreviousBuildPromotion(SelectPrevBuildPolicy.SINCE_LAST_BUILD)).thenReturn(myPreviousBuildPromotionMock);
    Mockito.when(myPreviousBuildPromotionMock.getBuildProblems()).thenReturn(Collections.singletonList(myBuildProblemMock));
    Mockito.when(myBuildProblemMock.getBuildPromotion()).thenReturn(myCurrentBuildPromotionMock);

    assertFalse(myBuildProblemUtils.isNew(myBuildProblemMock));
  }

}