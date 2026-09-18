

package jetbrains.buildServer.investigationsAutoAssigner.processing;

import com.intellij.openapi.util.Pair;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicNotApplicableException;
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
 * - HeuristicNotApplicableException when more then one committers were found (so we cannot chose from them with these heuristics.
 */
public class ModificationAnalyzerFactory {
  private static final int TOO_SMALL_PATTERN_THRESHOLD = 15;

  public ModificationAnalyzer getInstance(SVcsModification vcsChange) {
    return new ModificationAnalyzer(vcsChange);
  }

  public static class ModificationAnalyzer {
    private final SVcsModification myVcsChange;

    private ModificationAnalyzer(@NotNull SVcsModification vcsChange) {
      myVcsChange = vcsChange;
    }

    @Nullable
    public Pair<User, String> findProblematicFile(String problemText, Set<String> usersToIgnore)
      throws HeuristicNotApplicableException {
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
    public User getOnlyCommitter(Set<String> usersToIgnore) throws HeuristicNotApplicableException {
      Collection<SUser> committers = myVcsChange.getCommitters();
      if (committers.isEmpty()) {
        throw new HeuristicNotApplicableException(
          "committer \"" + myVcsChange.getUserName() + "\" does not have corresponding TeamCity user");
      }

      List<User> filteredCommitters = committers.stream()
                                                .filter(user -> !usersToIgnore.contains(user.getUsername()))
                                                .collect(Collectors.toList());

      if (filteredCommitters.isEmpty()) {
        return null;
      }

      if (filteredCommitters.size() > 1) {
        throw new HeuristicNotApplicableException("there are more than one committer");
      }

      return filteredCommitters.get(0);
    }
  }

  @Nullable
  private static String findBrokenFile(@NotNull final SVcsModification vcsChange, @NotNull final String problemText) {
    return vcsChange.getChanges()
      .stream()
      .map(VcsFileModification::getRelativeFileName)
      .filter(filePath -> getPatterns(filePath).stream()
                                               .anyMatch(problemText::contains))
      .findFirst()
      .orElse(null);
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
    if (withoutExtension.isEmpty()) {
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

    return Arrays.asList(join(parts, "."), join(parts, "/"), join(parts, "\\"));
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
