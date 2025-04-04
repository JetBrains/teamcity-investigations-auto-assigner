

package jetbrains.buildServer.investigationsAutoAssigner.processing;

import com.intellij.openapi.diagnostic.Logger;
import java.util.*;
import java.util.stream.Collectors;
import jetbrains.buildServer.BuildProblemTypes;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.common.FailedBuildInfo;
import jetbrains.buildServer.investigationsAutoAssigner.utils.BuildProblemUtils;
import jetbrains.buildServer.investigationsAutoAssigner.utils.CustomParameters;
import jetbrains.buildServer.investigationsAutoAssigner.utils.InvestigationsManager;
import jetbrains.buildServer.messages.ErrorData;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

/**
 * Filters build problems based on various conditions and criteria for build problem investigations.
 * <p>
 * This class is responsible for filtering out irrelevant build problems that should not be considered
 * for investigation. It considers factors like previously processed problems, problem types, and
 * ongoing investigations.
 * </p>
 *
 * @see InvestigationsManager
 * @see BuildProblemUtils
 * @see CustomParameters
 */
@Component
public class BuildProblemsFilter {

  private static final Logger LOGGER = Constants.LOGGER;
  public final static Set<String> supportedEverywhereTypes = Collections.unmodifiableSet(
    new HashSet<>(Arrays.asList(BuildProblemTypes.TC_COMPILATION_ERROR_TYPE, BuildProblemTypes.TC_EXIT_CODE_TYPE)));
  public final static Set<String> snapshotDependencyErrorTypes = Collections.unmodifiableSet(new HashSet<>(
    Arrays.asList(ErrorData.SNAPSHOT_DEPENDENCY_ERROR_BUILD_PROCEEDS_TYPE, ErrorData.SNAPSHOT_DEPENDENCY_ERROR_TYPE)));
  private final BuildProblemUtils buildProblemUtils;
  private final CustomParameters customParameters;
  private final InvestigationsManager investigationsManager;

  /**
   * Constructs a {@link BuildProblemsFilter} instance with the provided dependencies.
   *
   * @param investigationsManager the manager for handling investigations
   * @param buildProblemUtils     utility class for working with build problems
   * @param customParameters      parameters related to custom build problem types to ignore
   */
  public BuildProblemsFilter(@NotNull final InvestigationsManager investigationsManager,
                             @NotNull final BuildProblemUtils buildProblemUtils,
                             @NotNull final CustomParameters customParameters) {
    this.investigationsManager = investigationsManager;
    this.buildProblemUtils = buildProblemUtils;
    this.customParameters = customParameters;
  }

  /**
   * Applies filtering on the given build problems based on the criteria defined by the failed build info and project.
   *
   * @param failedBuildInfo information related to the failed build
   * @param sProject        the project associated with the build
   * @param buildProblems   list of build problems to filter
   * @return a filtered list of applicable build problems
   */
  List<BuildProblem> apply(final FailedBuildInfo failedBuildInfo,
                           final SProject sProject,
                           final List<BuildProblem> buildProblems) {
    SBuild sBuild = failedBuildInfo.getBuild();
    logDebug("Filtering of build problems for build id:%s started", sBuild.getBuildId());

    List<BuildProblem> filteredBuildProblems = buildProblems.stream().filter(failedBuildInfo::checkNotProcessed)
                                                            .filter(problem -> isApplicable(sProject, sBuild, problem))
                                                            .limit(failedBuildInfo.getLimitToProcess())
                                                            .collect(Collectors.toList());

    failedBuildInfo.addProcessedBuildProblems(buildProblems);
    failedBuildInfo.increaseProcessedNumber(filteredBuildProblems.size());

    return filteredBuildProblems;
  }

  /**
   * Filters out build problems that are still applicable for the given build, before the assignment.
   *
   * @param failedBuildInfo  information about the failed build
   * @param sProject         the project associated with the build
   * @param allBuildProblems list of all build problems to filter
   * @return a list of still applicable build problems
   */
  List<BuildProblem> getStillApplicable(final FailedBuildInfo failedBuildInfo,
                                        final SProject sProject,
                                        final List<BuildProblem> allBuildProblems) {
    SBuild sBuild = failedBuildInfo.getBuild();
    logDebug("Filtering before assign of build problems for build id:%s started", sBuild.getBuildId());

    return allBuildProblems.stream().filter(problem -> isApplicable(sProject, sBuild, problem))
                           .collect(Collectors.toList());
  }

  /**
   * Checks whether a given build problem is applicable for further investigation based on multiple conditions.
   *
   * @param project the project associated with the build
   * @param sBuild  the build where the problem occurred
   * @param problem the build problem to check
   * @return {@code true} if the problem is applicable, {@code false} otherwise
   */
  private boolean isApplicable(@NotNull final SProject project,
                               @NotNull final SBuild sBuild,
                               @NotNull final BuildProblem problem) {
    String reason = null;
    String buildProblemType = problem.getBuildProblemData().getType();

    // Determine if the problem meets any exclusion conditions
    if (problem.isMuted()) {
      reason = "is muted";
    } else if (!this.buildProblemUtils.isNew(problem)) {
      reason = "occurs not for the first time";
    } else if (this.investigationsManager.checkUnderInvestigation(project, sBuild, problem)) {
      reason = "is already under an investigation";
    } else if (BuildProblemTypes.TC_FAILED_TESTS_TYPE.equals(problem.getBuildProblemData().getType())) {
      reason = "has unsupported failed tests build problem type";
    } else if (this.customParameters.getBuildProblemTypesToIgnore(sBuild).contains(buildProblemType)) {
      reason = "is among build problem types to ignore";
    }

    // Log the result of the applicability check
    boolean isApplicable = reason == null;
    logDebug("Build problem id:%s:%s is %s.%s",
             sBuild.getBuildId(),
             problem.getTypeDescription(),
             (isApplicable ? "applicable" : "not applicable"),
             (isApplicable ? "" : String.format(" Reason: this build problem %s.", reason)));

    return isApplicable;
  }

  /**
   * Helper method for debug logging that checks debug level before formatting.
   *
   * @param format the format string
   * @param args   the format arguments
   */
  private void logDebug(String format, Object... args) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(String.format(format, args));
    }
  }
}