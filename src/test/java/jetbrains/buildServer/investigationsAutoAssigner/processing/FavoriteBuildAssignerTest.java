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

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.favoriteBuilds.FavoriteBuildsManager;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.serverSide.BuildPromotion;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.SimplePropertyKey;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;

@Test
public class FavoriteBuildAssignerTest extends BaseTestCase {

  private FavoriteBuildAssigner myFavoriteBuildAssigner;
  private FavoriteBuildsManager myFavoriteBuildsManager;
  private SBuild mySBuild;
  private SUser myUser1;

  @BeforeMethod
  @Override
  public void setUp() throws Exception {
    super.setUp();

    mySBuild = Mockito.mock(SBuild.class);
    myUser1 = Mockito.mock(SUser.class);
    myFavoriteBuildsManager = Mockito.mock(FavoriteBuildsManager.class);

    //SBuild
    final BuildPromotion buildPromotion = Mockito.mock(BuildPromotion.class);
    when(mySBuild.getBuildPromotion()).thenReturn(buildPromotion);

    //FavoriteBuildsManager
    doNothing().when(myFavoriteBuildsManager).tagBuild(buildPromotion, myUser1);

    //FavoriteBuildAssigner
    myFavoriteBuildAssigner = new FavoriteBuildAssigner(myFavoriteBuildsManager);
  }

  public void Test_TeamCityPropertyDisabledUsersCheckboxTrue() {
    when(myUser1.getPropertyValue(new SimplePropertyKey(Constants.USER_AUTOMATICALLY_MARK_IMPORTANT_BUILDS_AS_FAVORITE_INTERNAL_PROPERTY))).thenReturn("true");
    myFavoriteBuildAssigner.markAsFavorite(mySBuild, myUser1);
    Mockito.verify(myFavoriteBuildsManager, Mockito.never()).tagBuild(mySBuild.getBuildPromotion(), myUser1);
  }

  public void Test_TeamCityPropertyDisabledUsersCheckboxFalse() {
    myFavoriteBuildAssigner.markAsFavorite(mySBuild, myUser1);
    Mockito.verify(myFavoriteBuildsManager, Mockito.never()).tagBuild(mySBuild.getBuildPromotion(), myUser1);
  }

  public void Test_TeamCityPropertyEnabledUsersCheckboxFalse() {
    setInternalProperty(Constants.SHOULD_AUTOMATICALLY_MARK_IMPORTANT_BUILDS_AS_FAVORITE, "true");
    myFavoriteBuildAssigner.markAsFavorite(mySBuild, myUser1);
    Mockito.verify(myFavoriteBuildsManager, Mockito.never()).tagBuild(mySBuild.getBuildPromotion(), myUser1);
  }

  public void Test_TeamCityPropertyEnabledUsersCheckboxTrue() {
    setInternalProperty(Constants.SHOULD_AUTOMATICALLY_MARK_IMPORTANT_BUILDS_AS_FAVORITE, "true");
    when(myUser1.getBooleanProperty(new SimplePropertyKey(Constants.USER_AUTOMATICALLY_MARK_IMPORTANT_BUILDS_AS_FAVORITE_INTERNAL_PROPERTY))).thenReturn(true);
    myFavoriteBuildAssigner.markAsFavorite(mySBuild, myUser1);
    Mockito.verify(myFavoriteBuildsManager, Mockito.only()).tagBuild(mySBuild.getBuildPromotion(), myUser1);
  }
}
