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

import java.util.Arrays;
import java.util.List;
import jetbrains.buildServer.favoriteBuilds.FavoriteBuildsManager;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.users.SUser;
import org.jetbrains.annotations.NotNull;

public class FavoriteBuildAssigner {

  private static final List<String> tagLabels = Arrays.asList(FavoriteBuildsManager.FAVORITE_BUILD_TAG, Constants.INVESTIGATION_LABEL);

  public FavoriteBuildAssigner() {
  }


  /**
   * Check if it is possible to mark as favorite an important build.
   * @param user {@link SUser} object used for querying specific internal properties.
   * @return a boolean representing if it is possible to mark continue the procedure of marking.
   */
  private boolean shouldMarkAsFavorite(@NotNull final SUser user) {
    return user.getBooleanProperty(Constants.USER_CHECKBOX_VALUE);
  }

  /**
   * Mark the input build as favorite for the input user.
   * @param sBuild {@link  SBuild} object that represents the current build.
   * @param user {@link SUser} the user for whom the build should be marked as favorite.
   */
  public void markAsFavorite(@NotNull final SBuild sBuild, @NotNull final SUser user) {
    if (shouldMarkAsFavorite(user)) {
      sBuild.getBuildPromotion().setPrivateTags(tagLabels, user);
    }
  }
}
