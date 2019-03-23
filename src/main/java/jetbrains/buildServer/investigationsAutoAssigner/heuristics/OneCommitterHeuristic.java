/*
 * Copyright 2000-2018 JetBrains s.r.o.
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
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.processing.HeuristicContext;
import jetbrains.buildServer.investigationsAutoAssigner.processing.ModificationAnalyzerFactory;
import jetbrains.buildServer.log.LogUtil;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.vcs.SVcsModification;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OneCommitterHeuristic implements Heuristic {
  private static final Logger LOGGER = Logger.getInstance(Constants.LOGGING_CATEGORY);
  private ModificationAnalyzerFactory myModificationAnalyzerFactory;

  public OneCommitterHeuristic(ModificationAnalyzerFactory modificationAnalyzerFactory) {
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
      } catch (IllegalStateException ex) {
        LOGGER.debug("Heuristic \"OneCommitter\" is ignored as " + ex.getMessage() + ". Build: " +
                     LogUtil.describe(build));
        return result;
      }
    }

    if (responsible != null) {
      Responsibility responsibility = new Responsibility(responsible, "was the only committer to the build");
      heuristicContext.getTestRuns().forEach(sTestRun -> result.addResponsibility(sTestRun, responsibility));

      heuristicContext.getBuildProblems()
                      .forEach(buildProblem -> result.addResponsibility(buildProblem, responsibility));
    }
    return result;
  }

  private void ensureSameUsers(@Nullable User first,
                               @Nullable User second) {
    if (first != null && second != null && !first.equals(second)) {
      throw new IllegalStateException("there are more then one TeamCity user");
    }
  }
}