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
 * Factory class that helps in analyzing modifications to find broken files and corresponding committers for heuristics.
 * The heuristics return three possible result states:
 * - non-null result when something is found,
 * - null when nothing is found,
 * - {@link HeuristicNotApplicableException} when there are multiple committers, making it impossible to choose one.
 */
public class ModificationAnalyzerFactory {

  private static final int TOO_SMALL_PATTERN_THRESHOLD = 15;

  /**
   * Returns an instance of {@link ModificationAnalyzer} for a given VCS modification.
   *
   * @param vcsChange the VCS modification to analyze.
   * @return an instance of {@link ModificationAnalyzer}.
   */
  public ModificationAnalyzer getInstance(SVcsModification vcsChange) {
    return new ModificationAnalyzer(vcsChange);
  }

  /**
   * Analyzer class that handles the logic for finding problematic files and committers.
   */
  public static class ModificationAnalyzer {
    private final SVcsModification myVcsChange;

    private ModificationAnalyzer(@NotNull SVcsModification vcsChange) {
      myVcsChange = vcsChange;
    }

    /**
     * Finds the problematic file and the committer responsible for it.
     *
     * @param problemText the text describing the problem.
     * @param usersToIgnore a set of usernames to ignore.
     * @return a pair of the committer and the file path if a match is found, or null if nothing is found.
     * @throws HeuristicNotApplicableException if more than one committer is found.
     */
    @Nullable
    public Pair<User, String> findProblematicFile(String problemText, Set<String> usersToIgnore)
      throws HeuristicNotApplicableException {
      String filePath = findBrokenFile(myVcsChange, problemText);
      if (filePath == null) {
        return null;
      }

      @Nullable User committer = getOnlyCommitter(usersToIgnore);
      if (committer == null) {
        return null;
      }

      return Pair.create(committer, filePath);
    }

    /**
     * Returns the only committer for the VCS modification, if there is exactly one.
     *
     * @param usersToIgnore a set of usernames to ignore.
     * @return the committer, or null if there are no committers or if there are multiple committers.
     * @throws HeuristicNotApplicableException if more than one committer is found.
     */
    @Nullable
    public User getOnlyCommitter(Set<String> usersToIgnore) throws HeuristicNotApplicableException {
      Collection<SUser> committers = myVcsChange.getCommitters();
      if (committers.isEmpty()) {
        throw new HeuristicNotApplicableException("committer \"" + myVcsChange.getUserName() + "\" does not have corresponding TeamCity user");
      }

      List<User> filteredCommitters = filterCommitters(committers, usersToIgnore);

      if (filteredCommitters.isEmpty()) {
        return null;
      }

      if (filteredCommitters.size() > 1) {
        throw new HeuristicNotApplicableException("there are more than one committer");
      }

      return filteredCommitters.get(0);
    }

    /**
     * Filters the committers by excluding the ones that are in the usersToIgnore list.
     *
     * @param committers the collection of committers to filter.
     * @param usersToIgnore the set of users to ignore.
     * @return the filtered list of committers.
     */
    private List<User> filterCommitters(Collection<SUser> committers, Set<String> usersToIgnore) {
      return committers.stream()
                       .filter(user -> !usersToIgnore.contains(user.getUsername()))
                       .collect(Collectors.toList());
    }
  }

  /**
   * Finds a broken file in the VCS modification by matching it against the given problem text.
   *
   * @param vcsChange the VCS modification to check.
   * @param problemText the text describing the problem to match against file paths.
   * @return the file path if a match is found, or null if no match is found.
   */
  @Nullable
  private static String findBrokenFile(@NotNull final SVcsModification vcsChange, @NotNull final String problemText) {
    for (VcsFileModification modification : vcsChange.getChanges()) {
      String filePath = modification.getRelativeFileName();
      if (matchesProblemText(filePath, problemText)) {
        return filePath;
      }
    }
    return null;
  }

  /**
   * Checks if the file path matches any of the patterns derived from the problem text.
   *
   * @param filePath the file path to check.
   * @param problemText the problem text to match.
   * @return true if the file path matches the problem text, false otherwise.
   */
  private static boolean matchesProblemText(String filePath, String problemText) {
    for (String pattern : getPatterns(filePath)) {
      if (problemText.contains(pattern)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Generates various combinations of patterns from the file path (up to 2 levels of parent directories).
   *
   * @param filePath the file path to analyze.
   * @return a list of pattern strings based on the file path.
   */
  @NotNull
  private static List<String> getPatterns(@NotNull final String filePath) {
    List<String> parts = new ArrayList<>();
    String withoutExtension = FileUtil.getNameWithoutExtension(new File(filePath));

    if (withoutExtension.isEmpty()) {
      return Collections.emptyList();
    }

    parts.add(withoutExtension);
    addParentPaths(filePath, parts);

    if (isSmallPattern(parts)) {
      parts.set(0, FileUtil.getName(filePath));  // Use full file name if pattern is small.
    }

    return generatePatterns(parts);
  }

  /**
   * Adds the parent paths of the file to the pattern list (up to 2 levels).
   *
   * @param filePath the file path.
   * @param parts the list to add parent path parts to.
   */
  private static void addParentPaths(String filePath, List<String> parts) {
    String path = getParentPath(filePath);
    if (path != null) {
      parts.add(0, new File(path).getName());
      path = getParentPath(path);
      if (path != null) {
        parts.add(0, new File(path).getName());
      }
    }
  }

  /**
   * Checks if the combined pattern is smaller than the threshold.
   *
   * @param parts the parts of the pattern.
   * @return true if the combined pattern is small, false otherwise.
   */
  private static boolean isSmallPattern(final List<String> parts) {
    return join(parts, ".").length() <= TOO_SMALL_PATTERN_THRESHOLD;
  }

  /**
   * Generates the possible pattern combinations from the parts list.
   *
   * @param parts the parts to join into patterns.
   * @return a list of pattern strings.
   */
  private static List<String> generatePatterns(List<String> parts) {
    return Arrays.asList(
      join(parts, "."),  // Dot notation (e.g., parent1.parent2.file)
      join(parts, "/"),  // Slash notation (e.g., parent1/parent2/file)
      join(parts, "\\")  // Backslash notation (e.g., parent1\parent2\file)
    );
  }

  /**
   * Retrieves the parent path of the file from the given path.
   *
   * @param path the file path.
   * @return the parent path, or null if there is no parent.
   */
  @Nullable
  private static String getParentPath(@NotNull final String path) {
    final int lastSlashPos = path.replace('\\', '/').lastIndexOf('/');
    return lastSlashPos == -1 ? null : path.substring(0, lastSlashPos);
  }
}
