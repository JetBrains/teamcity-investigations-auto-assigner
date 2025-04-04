package jetbrains.buildServer.investigationsAutoAssigner.heuristics;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicNotApplicableException;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.processing.BuildProblemsFilter;
import jetbrains.buildServer.investigationsAutoAssigner.processing.HeuristicContext;
import jetbrains.buildServer.investigationsAutoAssigner.processing.ModificationAnalyzerFactory;
import jetbrains.buildServer.investigationsAutoAssigner.utils.ProblemTextExtractor;
import jetbrains.buildServer.log.LogUtil;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.vcs.SVcsModification;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class implements the heuristic for determining the responsible user for a broken file
 * in a build. It analyzes the build's test results and build problems to find the user who
 * changed the file responsible for causing the build failure.
 * <p>
 * The heuristic works by analyzing modifications to source code files and checking if
 * those changes are linked to any problems in the build. The heuristic considers the text of
 * the build problem and the test results to identify the responsible user. If multiple users
 * are found, the heuristic will not be applicable, and an exception is thrown.
 * </p>
 * <p>
 * The heuristic uses a {@link ProblemTextExtractor} to extract the problem description
 * from the build's test and build problem data. It also uses a {@link ModificationAnalyzerFactory}
 * to determine which modifications in the source code are related to the identified problem.
 * </p>
 */
public class BrokenFileHeuristic implements Heuristic {

  private static final Logger LOGGER = Constants.LOGGER;
  private final ProblemTextExtractor problemTextExtractor;
  private final ModificationAnalyzerFactory modificationAnalyzerFactory;

  /**
   * Constructor for the BrokenFileHeuristic.
   *
   * @param problemTextExtractor        the extractor used to retrieve the problem description from build data
   * @param modificationAnalyzerFactory the factory used to analyze VCS modifications
   */
  public BrokenFileHeuristic(@NotNull ProblemTextExtractor problemTextExtractor,
                             @NotNull ModificationAnalyzerFactory modificationAnalyzerFactory) {
    this.problemTextExtractor = problemTextExtractor;
    this.modificationAnalyzerFactory = modificationAnalyzerFactory;
  }

  /**
   * Finds the responsible user for the broken file in the given build context.
   *
   * @param heuristicContext the context for the heuristic, containing build and test data
   * @return the result containing the assigned responsibility or an empty result if not applicable
   */
  @NotNull
  public HeuristicResult findResponsibleUser(@NotNull HeuristicContext heuristicContext) {
    final HeuristicResult emptyResult = new HeuristicResult();
    SBuild sBuild = heuristicContext.getBuild();

    // Check if the build promotion is of type BuildPromotionEx
    final BuildPromotion buildPromotion = sBuild.getBuildPromotion();
    if (!(buildPromotion instanceof BuildPromotionEx)) return emptyResult;

    // Get the VCS changes related to the build promotion
    SelectPrevBuildPolicy prevBuildPolicy = SelectPrevBuildPolicy.SINCE_LAST_BUILD;
    List<SVcsModification> vcsChanges =
      ((BuildPromotionEx)buildPromotion).getDetectedChanges(prevBuildPolicy, false).stream()
                                        .map(ChangeDescriptor::getRelatedVcsChange).filter(Objects::nonNull)
                                        .collect(Collectors.toList());
    // Try to process test and build problems, log if exception happened
    try {
      return processTestsAndBuildProblems(heuristicContext, vcsChanges);

    } catch (HeuristicNotApplicableException ex) {
      LOGGER.debug("Heuristic \"BrokenFile\" is ignored as " + ex.getMessage() + ". Build: " +
                   LogUtil.describe(heuristicContext.getBuild()));
      return emptyResult;
    }
  }

  /**
   * Processes the test and build problems to identify the responsible user for each issue.
   *
   * @param heuristicContext the context for the heuristic, containing test and build problem data
   * @param vcsChanges       the list of VCS changes related to the build
   * @return the result containing the assigned responsibilities
   */
  private HeuristicResult processTestsAndBuildProblems(@NotNull final HeuristicContext heuristicContext,
                                                       final List<SVcsModification> vcsChanges) {
    LOGGER.debug("Processing tests and build problems for build: " + heuristicContext.getBuild().getBuildId());
    HeuristicResult result = new HeuristicResult();
    SBuild sBuild = heuristicContext.getBuild();

    // Process test runs
    for (STestRun sTestRun : heuristicContext.getTestRuns()) {
      String problemText = this.problemTextExtractor.getBuildProblemText(sTestRun);
      Responsibility responsibility = findResponsibleUser(vcsChanges, problemText, heuristicContext);
      if (responsibility != null) {
        result.addResponsibility(sTestRun, responsibility);
      }
    }

    // Process build problems
    for (BuildProblem buildProblem : heuristicContext.getBuildProblems()) {
      String buildProblemType = buildProblem.getBuildProblemData().getType();
      if (!BuildProblemsFilter.supportedEverywhereTypes.contains(buildProblemType)) {
        continue;
      }

      String problemText = this.problemTextExtractor.getBuildProblemText(buildProblem, sBuild);
      Responsibility responsibility = findResponsibleUser(vcsChanges, problemText, heuristicContext);
      if (responsibility != null) {
        result.addResponsibility(buildProblem, responsibility);
      }
    }

    return result;
  }

  /**
   * Finds the responsible user for a given problem text based on the VCS changes.
   *
   * @param vcsChanges       the list of VCS modifications related to the build
   * @param problemText      the problem description extracted from the build problem or test result
   * @param heuristicContext the context of the heuristic containing additional build data
   * @return the responsibility object representing the user and their associated problem description
   */
  @Nullable
  private Responsibility findResponsibleUser(List<SVcsModification> vcsChanges,
                                             String problemText,
                                             HeuristicContext heuristicContext) {
    Pair<User, String> foundBrokenFile = null;
    for (SVcsModification vcsChange : vcsChanges) {
      ModificationAnalyzerFactory.ModificationAnalyzer vcsChangeWrapped =
        this.modificationAnalyzerFactory.getInstance(vcsChange);
      Pair<User, String> brokenFile =
        vcsChangeWrapped.findProblematicFile(problemText, heuristicContext.getUsersToIgnore());
      if (brokenFile == null) continue;

      ensureSameUsers(foundBrokenFile, brokenFile);
      foundBrokenFile = brokenFile;
    }

    if (foundBrokenFile == null) return null;

    String description =
      String.format("changed the suspicious file \"%s\" which probably broke the build", foundBrokenFile.second);
    return new Responsibility(foundBrokenFile.first, description);
  }

  /**
   * Ensures that both found broken file pairs have the same responsible user.
   * If the users differ, an exception is thrown.
   *
   * @param foundBrokenFile the previously found broken file and its responsible user
   * @param broken          the current broken file and its responsible user
   */
  private void ensureSameUsers(@Nullable Pair<User, String> foundBrokenFile,
                               @Nullable final Pair<User, String> broken) {
    if (foundBrokenFile != null && broken != null && !foundBrokenFile.first.equals(broken.first)) {
      throw new HeuristicNotApplicableException(
        "Multiple TeamCity users detected: " + foundBrokenFile.first.getUsername() + " and " +
        broken.first.getUsername());
    }
  }

  /**
   * Gets the ID of the heuristic.
   *
   * @return the ID of the heuristic
   */
  @Override
  @NotNull
  public String getId() {
    return "BrokenFile";
  }
}
