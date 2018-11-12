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

package jetbrains.buildServer.iaa.heuristics;

import com.intellij.openapi.diagnostic.Logger;
import java.util.Set;
import java.util.stream.Collectors;
import jetbrains.buildServer.iaa.common.HeuristicResult;
import jetbrains.buildServer.iaa.common.Responsibility;
import jetbrains.buildServer.iaa.processing.HeuristicContext;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import org.jetbrains.annotations.NotNull;

public class OneCommitterHeuristic implements Heuristic {
  private static final Logger LOGGER = Logger.getInstance(OneCommitterHeuristic.class.getName());

  @Override
  @NotNull
  public String getName() {
    return "Only One Committer Heuristic";
  }

  @Override
  public HeuristicResult findResponsibleUser(@NotNull HeuristicContext heuristicContext) {
    HeuristicResult result = new HeuristicResult();

    SBuild build = heuristicContext.getBuild();
    final SelectPrevBuildPolicy selectPrevBuildPolicy = SelectPrevBuildPolicy.SINCE_LAST_BUILD;
    final Set<SUser> committers = build.getCommitters(selectPrevBuildPolicy).getUsers()
                                       .stream()
                                       .filter(user -> !heuristicContext.getUserFilter().contains(user.getUsername()))
                                       .collect(Collectors.toSet());

    if (committers.isEmpty()) {
      LOGGER.debug("There are no committers since last build for failed build #" + build.getBuildId());
      return result;
    }

    if (committers.size() != 1) {
      LOGGER.debug(String.format("There are more than one committer (total: %d) since last build for failed build #%s",
                                 committers.size(), build.getBuildId()));
      return result;
    }


    User responsible = committers.iterator().next();
    Responsibility responsibility = new Responsibility(responsible, "was the only committer to the build");
    heuristicContext.getTestRuns().forEach(sTestRun -> result.addResponsibility(sTestRun, responsibility));

    heuristicContext.getBuildProblems()
                    .forEach(buildProblem -> result.addResponsibility(buildProblem, responsibility));

    return result;
  }
}