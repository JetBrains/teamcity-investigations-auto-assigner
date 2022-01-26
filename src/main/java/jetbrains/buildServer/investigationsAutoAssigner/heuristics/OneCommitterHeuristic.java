/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

package jetbrains.buildServer.investigationsAutoAssigner.heuristics;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicNotApplicableException;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.processing.BuildProblemsFilter;
import jetbrains.buildServer.investigationsAutoAssigner.processing.HeuristicContext;
import jetbrains.buildServer.investigationsAutoAssigner.processing.ModificationAnalyzerFactory;
import jetbrains.buildServer.log.LogUtil;
import jetbrains.buildServer.serverSide.BuildStatisticsOptions;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.vcs.SVcsModification;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OneCommitterHeuristic implements Heuristic {
  private static final Logger LOGGER = Constants.LOGGER;
  private final ModificationAnalyzerFactory myModificationAnalyzerFactory;

  public OneCommitterHeuristic(@NotNull ModificationAnalyzerFactory modificationAnalyzerFactory) {
    myModificationAnalyzerFactory = modificationAnalyzerFactory;
  }

  @Override
  @NotNull
  public String getId() {
    return "OneCommitter";
  }

  @NotNull
  @Override
  public HeuristicResult findResponsibleUser(@NotNull HeuristicContext heuristicContext) {
    HeuristicResult result = new HeuristicResult();
    SBuild build = heuristicContext.getBuild();
    User responsible = null;
    final SelectPrevBuildPolicy selectPrevBuildPolicy = SelectPrevBuildPolicy.SINCE_LAST_BUILD;
    for (SVcsModification vcsChange : build.getChanges(selectPrevBuildPolicy, true)) {
      try {
        ModificationAnalyzerFactory.ModificationAnalyzer vcsChangeWrapped = myModificationAnalyzerFactory.getInstance(vcsChange);
        User probableResponsible = vcsChangeWrapped.getOnlyCommitter(heuristicContext.getUsersToIgnore());
        if (probableResponsible == null) continue;
        ensureSameUsers(responsible, probableResponsible);
        responsible = probableResponsible;
      } catch (HeuristicNotApplicableException ex) {
        LOGGER.debug("Heuristic \"OneCommitter\" is ignored as " + ex.getMessage() + ". Build: " +
                     LogUtil.describe(build));
        return result;
      }
    }

    if (responsible != null) {
      if (isCompilationErrorFixed(build)) {
        LOGGER.debug("Heuristic \"OneCommitter\" found " + responsible.getDescriptiveName() + "as responsible but " +
                     "results are ignored as previous build contained compilation errors." +
                     "  Build: " + LogUtil.describe(build));
        return result;
      }

      Responsibility responsibility = new Responsibility(responsible, "was the only committer to the build");
      heuristicContext.getTestRuns().forEach(sTestRun -> result.addResponsibility(sTestRun, responsibility));

      heuristicContext.getBuildProblems()
                      .stream()
                      .filter(problem -> BuildProblemsFilter.supportedEverywhereTypes.contains(problem.getBuildProblemData().getType()))
                      .forEach(buildProblem -> result.addResponsibility(buildProblem, responsibility));
    }
    return result;
  }

  private boolean isCompilationErrorFixed(final SBuild build) {
    SBuild previousFinished = build.getPreviousFinished();
    return !containsCompilationErrors(build) && previousFinished != null && containsCompilationErrors(previousFinished);
  }

  private boolean containsCompilationErrors(@NotNull SBuild build) {
      BuildStatisticsOptions opts = new BuildStatisticsOptions(BuildStatisticsOptions.COMPILATION_ERRORS, 0);
      return build.getBuildStatistics(opts).getCompilationErrorsCount() > 0;
  }
  private void ensureSameUsers(@Nullable User first,
                               @Nullable User second) {
    if (first != null && second != null && !first.equals(second)) {
      throw new HeuristicNotApplicableException("there are more then one TeamCity user");
    }
  }
}