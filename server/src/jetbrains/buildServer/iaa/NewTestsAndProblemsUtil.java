/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package jetbrains.buildServer.iaa;

import com.intellij.openapi.util.Pair;
import jetbrains.buildServer.serverSide.BuildPromotion;
import jetbrains.buildServer.serverSide.BuildPromotionEx;
import jetbrains.buildServer.serverSide.ChangeDescriptor;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.vcs.SVcsModification;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import jetbrains.buildServer.vcs.VcsFileModification;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

import static com.intellij.openapi.util.text.StringUtil.join;

/**
 * @author Maxim.Manuylov
 *         Date: 10.04.2014
 */
public class NewTestsAndProblemsUtil {
  @NotNull private static final String REASON_PREFIX = "This investigation was assigned automatically by TeamCity since ";

  public static Pair<SUser, String> findResponsibleUser(@NotNull final SBuild build, @Nullable final String problemText) {
    // todo if problem is a test, that already ran before in some build and was green there, should get committers since that build
    final SelectPrevBuildPolicy selectPrevBuildPolicy = SelectPrevBuildPolicy.SINCE_LAST_BUILD;

    final Set<SUser> committers = build.getCommitters(selectPrevBuildPolicy).getUsers();
    if (committers.isEmpty()) return null;

    final SUser firstComitter = committers.iterator().next();
    if (committers.size() == 1) {
      return Pair.create(firstComitter, REASON_PREFIX + "you were the only committer to the following build: " + build.getFullName() + " #" + build.getBuildNumber());
    }

    final Pair<SUser, String> userThatMadeBadChanges = findUserThatMadeBadChanges(build, problemText, selectPrevBuildPolicy);
    if (userThatMadeBadChanges != null)
      return userThatMadeBadChanges;
    else
      return Pair.create(firstComitter, REASON_PREFIX + "you were the first committer to the following build: " + build.getFullName() + " #" + build.getBuildNumber());
  }

  private static Pair<SUser, String> findUserThatMadeBadChanges(@NotNull SBuild build, @Nullable String problemText, SelectPrevBuildPolicy selectPrevBuildPolicy) {
    if (problemText == null) return null;

    final BuildPromotion buildPromotion = build.getBuildPromotion();
    if (!(buildPromotion instanceof BuildPromotionEx)) return null;

    SUser badUser = null;
    String badFile = null;

    for (ChangeDescriptor change : ((BuildPromotionEx) buildPromotion).getDetectedChanges(selectPrevBuildPolicy, true)) {
      final SVcsModification vcsChange = change.getRelatedVcsChange();
      if (vcsChange == null) continue;

      final String changeBadFile = findBadFile(vcsChange, problemText);
      if (changeBadFile == null) continue;

      final Collection<SUser> changeCommitters = vcsChange.getCommitters();
      if (changeCommitters.size() != 1) return null;

      final SUser changeBadUser = changeCommitters.iterator().next();
      if (badUser != null && !badUser.equals(changeBadUser)) return null;

      badUser = changeBadUser;
      badFile = changeBadFile;
    }

    if (badUser == null) return null;

    return Pair.create(badUser, REASON_PREFIX + "you changed the \"" + badFile + "\" file, which could probably cause this failure");
  }

  @Nullable
  private static String findBadFile(@NotNull final SVcsModification vcsChange, @NotNull final String problemText) {
    for (VcsFileModification modification : vcsChange.getChanges()) {
      final String filePath = modification.getRelativeFileName();
      for (String pattern : getPatterns(filePath)) {
        if (problemText.contains(pattern)) {
          return filePath;
        }
      }
    }
    return null;
  }

  @NotNull
  private static List<String> getPatterns(@NotNull final String filePath) {
    final List<String> parts = new ArrayList<String>();

    parts.add(FileUtil.getNameWithoutExtension(new File(filePath)));

    String path = getParentPath(filePath);
    if (path != null) {
      parts.add(0, new File(path).getName());
      path = getParentPath(path);
      if (path != null) {
        parts.add(0, new File(path).getName());
      }
    }

    return Arrays.asList(join(parts, "."), join(parts, "/"), join(parts, "\\"));
  }

  // we do not use File#getParentFile() instead because we must not take current
  // working directory into account, i.e. getParentPath("abc") must return null
  @Nullable
  private static String getParentPath(@NotNull final String path) {
    final int lastSlashPos = path.replace('\\', '/').lastIndexOf('/');
    return lastSlashPos == -1 ? null : path.substring(0, lastSlashPos);
  }
}
