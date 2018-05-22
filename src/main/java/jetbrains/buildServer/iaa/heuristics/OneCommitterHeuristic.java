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
import jetbrains.buildServer.iaa.FailedBuildContext;
import jetbrains.buildServer.iaa.HeuristicResult;
import jetbrains.buildServer.iaa.Responsibility;
import jetbrains.buildServer.iaa.common.Constants;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.users.SUser;
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
  @NotNull
  public String getDescription() {
    return "Assign an investigation to a user if the user is the only committer.";
  }

  @Override
  public HeuristicResult findResponsibleUser(@NotNull FailedBuildContext failedBuildContext) {
    HeuristicResult result = new HeuristicResult();

    SBuild build = failedBuildContext.getSBuild();
    final SelectPrevBuildPolicy selectPrevBuildPolicy = SelectPrevBuildPolicy.SINCE_LAST_BUILD;
    final Set<SUser> committers = build.getCommitters(selectPrevBuildPolicy).getUsers();
    if (committers.isEmpty()) {
      LOGGER.debug("There are no committers since last build for failed build #" + build.getBuildId());
    }

    if (committers.size() != 1) {
      LOGGER.debug(String.format("There are more then one committers (total: %d) since last build for failed build #%s",
                                 committers.size(), build.getBuildId()));
    }


    Responsibility responsibility = new Responsibility(
      committers.iterator().next(), String.format("%s you were responsible the only committer to the " +
                                                  "build: %s # %s", Constants.REASON_PREFIX,
                                                  build.getFullName(), build.getBuildNumber()));

    failedBuildContext.sTestRuns.forEach(sTestRun -> result.addResponsibility(sTestRun, responsibility));

    failedBuildContext.buildProblems
      .forEach(buildProblem -> result.addResponsibility(buildProblem, responsibility));

    return result;
  }
}