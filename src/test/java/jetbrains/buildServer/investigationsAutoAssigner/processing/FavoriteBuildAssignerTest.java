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
import jetbrains.buildServer.serverSide.BuildPromotion;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.User;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

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
}
