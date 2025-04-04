package jetbrains.buildServer.investigationsAutoAssigner.processing;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the context for performing heuristics on a build, including information about the project, build problems,
 * test runs, and users to ignore. This context is used for processing the build and its associated data.
 */
public final class HeuristicContext {

  private final SProject sproject;
  private final List<BuildProblem> buildProblems;
  private final List<STestRun> stestRuns;
  private final SBuild sbuild;
  private final Set<String> usersToIgnore;
  private Set<Long> committersIds = null;

  /**
   * Constructs a new {@link HeuristicContext} instance.
   *
   * @param sBuild the build this context is associated with
   * @param sProject the project this build belongs to
   * @param buildProblems a list of build problems related to this build
   * @param sTestRuns a list of test runs related to this build
   * @param usernameBlackList a set of usernames to ignore
   */
  public HeuristicContext(SBuild sBuild,
                          SProject sProject,
                          List<BuildProblem> buildProblems,
                          List<STestRun> sTestRuns,
                          @NotNull Set<String> usernameBlackList) {
    this.sbuild = sBuild;
    this.sproject = sProject;
    this.buildProblems = buildProblems;
    this.stestRuns = sTestRuns;
    this.usersToIgnore = usernameBlackList;
  }

  /**
   * Returns the build associated with this context.
   *
   * @return the build
   */
  @NotNull
  public SBuild getBuild() {
    return this.sbuild;
  }

  /**
   * Returns the project associated with the build.
   *
   * @return the project
   */
  @NotNull
  public SProject getProject() {
    return this.sproject;
  }

  /**
   * Returns a list of build problems associated with this build.
   *
   * @return the list of build problems
   */
  public List<BuildProblem> getBuildProblems() {
    return this.buildProblems;
  }

  /**
   * Returns a list of test runs associated with this build.
   *
   * @return the list of test runs
   */
  public List<STestRun> getTestRuns() {
    return this.stestRuns;
  }

  /**
   * Returns a set of usernames that should be ignored in this context.
   *
   * @return the set of usernames to ignore
   */
  @NotNull
  public Set<String> getUsersToIgnore() {
    return this.usersToIgnore;
  }

  /**
   * Returns a set of committer IDs associated with the build. If the committer IDs are not calculated yet,
   * they are computed lazily.
   *
   * @return the set of committer IDs
   */
  @NotNull
  public Set<Long> getCommittersIds() {
    if (this.committersIds == null) {
      this.committersIds = calculateCommittersIds(sbuild);
    }

    return this.committersIds;
  }

  /**
   * Calculates the set of committer IDs for the specified build.
   *
   * @param sBuild the build for which to calculate the committer IDs
   * @return the set of committer IDs
   */
  private static Set<Long> calculateCommittersIds(SBuild sBuild) {
    return sBuild.getCommitters(SelectPrevBuildPolicy.SINCE_LAST_BUILD)
                 .getUsers()
                 .stream()
                 .map(User::getId)
                 .collect(Collectors.toSet());
  }
}
