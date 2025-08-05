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
import jetbrains.buildServer.users.PropertyKey;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.SimplePropertyKey;
import org.jetbrains.annotations.NotNull;

public final class FavoriteBuildAssigner {
  private final FavoriteBuildsManager myFavoriteBuildsManager;

  public FavoriteBuildAssigner(@NotNull FavoriteBuildsManager favoriteBuildsManager) {
    myFavoriteBuildsManager = favoriteBuildsManager;
  }

  private boolean shouldMarkAsFavorite(@NotNull SUser user) {
    return TeamCityProperties.getBoolean(Constants.SHOULD_AUTOMATICALLY_MARK_IMPORTANT_BUILDS_AS_FAVORITE) &&
           Boolean.getBoolean(user.getProperties()
                                  .getOrDefault(new SimplePropertyKey(Constants.USER_AUTOMATICALLY_MARK_IMPORTANT_BUILDS_AS_FAVORITE_INTERNAL_PROPERTY), "false"));
  }

  public void markAsFavorite(@NotNull SBuild sBuild, @NotNull SUser user) {
    if (shouldMarkAsFavorite()) {
      myFavoriteBuildsManager.tagBuild(sBuild.getBuildPromotion(), user);
    }
  }
}
