

package jetbrains.buildServer.investigationsAutoAssigner.utils;

import java.util.List;
import jetbrains.buildServer.serverSide.BuildPromotion;
import jetbrains.buildServer.serverSide.BuildPromotionEx;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BuildProblemUtils {

  public boolean isNew(@NotNull BuildProblem buildProblem) {
    BuildPromotion problemsOwner = buildProblem.getBuildPromotion();

    BuildPromotionEx prevBuildPromo =
      (BuildPromotionEx)problemsOwner.getPreviousBuildPromotion(SelectPrevBuildPolicy.SINCE_LAST_BUILD);
    List<BuildProblem> prevProblems = prevBuildPromo == null ? null : prevBuildPromo.getBuildProblems();

    return !containsBuildProblem(prevProblems, buildProblem);
  }

  private boolean containsBuildProblem(@Nullable List<BuildProblem> problems,
                                              @NotNull BuildProblem buildProblem) {
    if (problems == null) return false;

    for (BuildProblem problem: problems) {
      if (buildProblem.getId() == problem.getId()) return true;
    }
    return false;
  }
}