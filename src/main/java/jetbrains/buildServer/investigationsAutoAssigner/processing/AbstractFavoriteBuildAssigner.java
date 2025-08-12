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

import jetbrains.buildServer.favoriteBuilds.FavoriteBuildsManager;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.SimplePropertyKey;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractFavoriteBuildAssigner {

  protected final FavoriteBuildsManager myFavoriteBuildsManager;
  protected AbstractFavoriteBuildAssigner(@NotNull final FavoriteBuildsManager favoriteBuildsManager) {
    myFavoriteBuildsManager = favoriteBuildsManager;
  }

  /**
   * Check if it is possible to mark as favorite an important build.
   * @param user {@link SUser} object used for querying specific internal properties.
   * @return a boolean representing if it is possible to mark continue the procedure of marking.
   */
  protected boolean shouldMarkAsFavorite(@NotNull final SUser user) {
    return user.getBooleanProperty(new SimplePropertyKey(Constants.USER_AUTOMATICALLY_MARK_IMPORTANT_BUILDS_AS_FAVORITE_INTERNAL_PROPERTY));
  }

  /**
   * Mark the input build as favorite for the input user.
   * @param sBuild {@link  SBuild} object that represents the current build.
   * @param user {@link SUser} the user for whom the build should be marked as favorite.
   */
  abstract void markAsFavorite(@NotNull final SBuild sBuild, @NotNull final SUser user);
}
