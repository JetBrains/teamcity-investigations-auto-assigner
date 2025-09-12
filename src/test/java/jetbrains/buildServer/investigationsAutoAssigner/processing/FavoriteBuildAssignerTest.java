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

package jetbrains.buildServer.investigationsAutoAssigner.processing;

import java.util.*;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.favoriteBuilds.FavoriteBuildsManager;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.serverSide.BuildPromotion;
import jetbrains.buildServer.serverSide.BuildPromotionEx;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.users.SUser;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;

@Test
public class FavoriteBuildAssignerTest extends BaseTestCase {

  private FavoriteBuildAssigner myFavoriteBuildAssigner;
  private SBuild mySBuild;
  private SUser myUser1;
  private BuildPromotionEx myBuildPromotion;

  @BeforeMethod
  @Override
  public void setUp() throws Exception {
    super.setUp();

    mySBuild = Mockito.mock(SBuild.class);
    myUser1 = Mockito.mock(SUser.class);

    when(myUser1.getId()).thenReturn(1L);

    //SBuild
    myBuildPromotion = Mockito.mock(BuildPromotionEx.class);
    when(mySBuild.getBuildPromotion()).thenReturn(myBuildPromotion);
    doNothing().when(myBuildPromotion).setPrivateTags(Mockito.anyList(), Mockito.any());

    //FavoriteBuildAssigner
    myFavoriteBuildAssigner = new FavoriteBuildAssigner();
  }

  public void Test_UsersCheckboxFalse() {
    myFavoriteBuildAssigner.markAsFavorite(mySBuild, myUser1);
    Mockito.verify(myBuildPromotion, Mockito.never()).setPrivateTags(Mockito.anyList(), Mockito.any());
  }

  public void Test_UsersCheckboxTrue() {
    setInternalProperty(Constants.SHOULD_AUTOMATICALLY_MARK_IMPORTANT_BUILDS_AS_FAVORITE, "true");
    final Set<String> expectedTags = new HashSet<>(Arrays.asList(FavoriteBuildsManager.FAVORITE_BUILD_TAG, Constants.INVESTIGATION_LABEL));
    doAnswer(answer -> {
      final List<String> tags = answer.getArgument(0);
      final SUser user = answer.getArgument(1);
      assertEquals(myUser1.getId(), user.getId());
      assertFalse(tags.isEmpty());
      assertTrue(expectedTags.containsAll(tags));
      return null;
    }).when(myBuildPromotion).setPrivateTags(Mockito.anyList(), Mockito.any());
    when(myUser1.getBooleanProperty(Constants.USER_CHECKBOX_VALUE)).thenReturn(true);
    myFavoriteBuildAssigner.markAsFavorite(mySBuild, myUser1);
  }
}
