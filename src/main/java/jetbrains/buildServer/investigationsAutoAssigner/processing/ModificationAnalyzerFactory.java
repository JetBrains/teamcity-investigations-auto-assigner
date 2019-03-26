/*
 * Copyright 2000-2019 JetBrains s.r.o.
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

import com.intellij.openapi.util.Pair;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.vcs.SVcsModification;
import jetbrains.buildServer.vcs.VcsFileModification;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.openapi.util.text.StringUtil.join;

/**
 * Helps to find broken files and only committers for corresponding heuristics.
 * Both public methods have three @result states:
 * - non-null result when something was found;
 * - null when nothing was found;
 * - IllegalStateException when more then one committers were found (so we cannot chose from them with these heuristics.
 */
public class ModificationAnalyzerFactory {
  private static final int TOO_SMALL_PATTERN_THRESHOLD = 15;

  public ModificationAnalyzer getInstance(SVcsModification vcsChange) {
    return new ModificationAnalyzer(vcsChange);
  }

  public class ModificationAnalyzer {
    private SVcsModification myVcsChange;

    private ModificationAnalyzer(SVcsModification vcsChange) {
      myVcsChange = vcsChange;
    }

    @Nullable
    public Pair<User, String> findProblematicFile(String problemText, Set<String> usersToIgnore)
      throws IllegalStateException {
      String filePath = findBrokenFile(myVcsChange, problemText);
      if (filePath == null) {
        return null;
      }

      @Nullable
      User committer = getOnlyCommitter(usersToIgnore);

      if (committer == null) {
        return null;
      }

      return Pair.create(committer, filePath);
    }

    @Nullable
    public User getOnlyCommitter(Set<String> usersToIgnore) throws IllegalStateException {
      Collection<SUser> committers = myVcsChange.getCommitters();
      if (committers.size() == 0) {
        throw new IllegalStateException("committer \"" + myVcsChange.getUserName() + "\" does not have corresponding TeamCity user");
      }

      List<User> filteredCommitters = committers.stream()
                                                .filter(user -> !usersToIgnore.contains(user.getUsername()))
                                                .collect(Collectors.toList());

      if (filteredCommitters.isEmpty()) {
        return null;
      }

      if (filteredCommitters.size() > 1) {
        throw new IllegalStateException("there are more than one committer");
      }

      return filteredCommitters.get(0);
    }
  }

  @Nullable
  private static String findBrokenFile(@NotNull final SVcsModification vcsChange, @NotNull final String problemText) {
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

  /**
   * This method is required to separate path1/path2/fileName with path3/path4/fileName.
   * Also it allows to handle different separators. Currently supported: '.','/','\' separators.
   *
   * @param filePath - filePath of the modification
   * @return various combination of fileName and its parents(up to 2th level) with separators.
   */
  @NotNull
  private static List<String> getPatterns(@NotNull final String filePath) {
    final List<String> parts = new ArrayList<>();
    String withoutExtension = FileUtil.getNameWithoutExtension(new File(filePath));
    if (withoutExtension.length() == 0) {
      return Collections.emptyList();
    }
    parts.add(withoutExtension);

    String path = getParentPath(filePath);
    if (path != null) {
      parts.add(0, new File(path).getName());
      path = getParentPath(path);
      if (path != null) {
        parts.add(0, new File(path).getName());
      }
    }

    if (isSmallPattern(parts)) {
      String withExtension = FileUtil.getName(filePath);
      parts.set(0, withExtension);
    }

    return parts.isEmpty() ?
           Collections.emptyList() :
           Arrays.asList(join(parts, "."), join(parts, "/"), join(parts, "\\"));
  }

  private static boolean isSmallPattern(final List<String> parts) {
    return join(parts, ".").length() <= TOO_SMALL_PATTERN_THRESHOLD;
  }

  // we do not use File#getParentFile() instead because we must not take current
  // working directory into account, i.e. getParentPath("abc") must return null
  @Nullable
  private static String getParentPath(@NotNull final String path) {
    final int lastSlashPos = path.replace('\\', '/').lastIndexOf('/');
    return lastSlashPos == -1 ? null : path.substring(0, lastSlashPos);
  }
}
