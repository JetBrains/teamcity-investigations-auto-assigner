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
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.processing.HeuristicContext;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.users.UserModelEx;
import jetbrains.buildServer.vcs.Modification;
import jetbrains.buildServer.vcs.SVcsModification;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OneCommitterHeuristic implements Heuristic {
  private static final Logger LOGGER = Logger.getInstance(OneCommitterHeuristic.class.getName());
  @NotNull private UserModelEx myUserModel;

  public OneCommitterHeuristic(@NotNull final UserModelEx userModel) {
    myUserModel = userModel;
  }

  @Override
  @NotNull
  public String getName() {
    return "Only One Committer Heuristic";
  }

  @Override
  public HeuristicResult findResponsibleUser(@NotNull HeuristicContext heuristicContext) {
    HeuristicResult result = new HeuristicResult();
    @Nullable User responsible = getOnlyKnownCommitterOrNull(heuristicContext);

    if (responsible != null) {
      Responsibility responsibility = new Responsibility(responsible, "was the only committer to the build");
      heuristicContext.getTestRuns().forEach(sTestRun -> result.addResponsibility(sTestRun, responsibility));

      heuristicContext.getBuildProblems()
                      .forEach(buildProblem -> result.addResponsibility(buildProblem, responsibility));
    }
    return result;
  }

  @Nullable
  private SUser getOnlyKnownCommitterOrNull(@NotNull HeuristicContext heuristicContext) {
    SBuild build = heuristicContext.getBuild();
    SUser onlyCommitter = null;
    final SelectPrevBuildPolicy selectPrevBuildPolicy = SelectPrevBuildPolicy.SINCE_LAST_BUILD;
    for (SVcsModification vcsChange : build.getChanges(selectPrevBuildPolicy, true)) {
      Collection<SUser> knownCommitters = vcsChange.getCommitters();
      if (knownCommitters.size() == 0 && vcsChange.getUserName() != null) {
        LOGGER.debug(String.format("There are at least one unknown for TeamCity user with vcs name '%s' " +
                                   "for failed build #%s", vcsChange.getUserName(), build.getBuildId()));
        return null;
      }

      if (knownCommitters.size() > 1) {
        LOGGER.debug(String.format("There are more than one committer since last build for failed build #%s",
                                   build.getBuildId()));
        return null;
      }

      SUser probableResponsible = knownCommitters.iterator().next();
      if (heuristicContext.getUserFilter().contains(probableResponsible.getUsername())) {
        continue;
      }

      if (onlyCommitter != null && !onlyCommitter.equals(probableResponsible)) {
        LOGGER.debug(String.format("There are more than one committer since last build for failed build #%s",
                                   build.getBuildId()));
        return null;
      }

      onlyCommitter = probableResponsible;
    }

    return onlyCommitter;
  }
}