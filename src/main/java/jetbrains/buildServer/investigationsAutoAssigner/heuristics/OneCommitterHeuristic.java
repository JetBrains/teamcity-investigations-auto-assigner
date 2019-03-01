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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.processing.HeuristicContext;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.users.UserModelEx;
import jetbrains.buildServer.vcs.Modification;
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
    final Set<String> usernames = getCommitterUsernames(heuristicContext);

    @Nullable final User responsible = getOnlyUserOrNull(usernames, heuristicContext.getBuild());
    if (responsible != null) {
      Responsibility responsibility = new Responsibility(responsible, "was the only committer to the build");
      heuristicContext.getTestRuns().forEach(sTestRun -> result.addResponsibility(sTestRun, responsibility));

      heuristicContext.getBuildProblems()
                      .forEach(buildProblem -> result.addResponsibility(buildProblem, responsibility));
    }
    return result;
  }

  @NotNull
  private Set<String> getCommitterUsernames(@NotNull HeuristicContext heuristicContext) {
    SBuild build = heuristicContext.getBuild();

    final SelectPrevBuildPolicy selectPrevBuildPolicy = SelectPrevBuildPolicy.SINCE_LAST_BUILD;
    return build.getChanges(selectPrevBuildPolicy, true)
                .stream().map(Modification::getUserName)
                .filter(Objects::nonNull)
                .filter(userName -> !heuristicContext.getUserFilter().contains(userName))
                .collect(Collectors.toSet());
  }

  @Nullable
  private User getOnlyUserOrNull(final Set<String> usernames, SBuild build) {
    if (usernames.isEmpty()) {
      LOGGER.debug("There are no committers since last build for failed build #" + build.getBuildId());
      return null;
    }
    if (usernames.size() > 1) {
      LOGGER.debug(String.format("There are more than one committer (total: %d) since last build for failed build #%s",
                                 usernames.size(), build.getBuildId()));
      return null;
    }

    String username = usernames.iterator().next();
    @Nullable
    User responsible = myUserModel.findUserAccount(null, username);
    if (responsible == null) {
      LOGGER.debug(String.format("Changes from Unknown TeamCity user '%s' in build #%s", username, build.getBuildId()));
      return null;
    }

    return responsible;
  }
}