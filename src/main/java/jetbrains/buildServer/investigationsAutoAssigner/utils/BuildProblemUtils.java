

package jetbrains.buildServer.investigationsAutoAssigner.utils;

import java.util.List;
import jetbrains.buildServer.serverSide.BuildPromotion;
import jetbrains.buildServer.serverSide.BuildPromotionEx;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for handling build problems.
 */
public class BuildProblemUtils {

  // Private constructor to prevent instantiation of this utility class
  private BuildProblemUtils() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  /**
   * Determines if a build problem is new, meaning it did not exist in the previous build.
   *
   * @param buildProblem The build problem to check.
   * @return {@code true} if the problem is new, {@code false} otherwise.
   */
  public boolean isNew(@NotNull BuildProblem buildProblem) {
    BuildPromotion problemsOwner = buildProblem.getBuildPromotion();

    BuildPromotionEx prevBuildPromo =
      (BuildPromotionEx)problemsOwner.getPreviousBuildPromotion(SelectPrevBuildPolicy.SINCE_LAST_BUILD);
    List<BuildProblem> prevProblems = prevBuildPromo == null ? null : prevBuildPromo.getBuildProblems();

    return !containsBuildProblem(prevProblems, buildProblem);
  }

  /**
   * Checks if a given list of build problems contains a specific problem.
   *
   * @param problems     The list of previous build problems.
   * @param buildProblem The build problem to check.
   * @return {@code true} if the problem exists in the list, {@code false} otherwise.
   */
  private boolean containsBuildProblem(@Nullable List<BuildProblem> problems, @NotNull BuildProblem buildProblem) {
    if (problems == null) return false;

    for (BuildProblem problem : problems) {
      if (buildProblem.getId() == problem.getId()) return true;
    }
    return false;
  }
}