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

import com.intellij.openapi.util.Pair;
import java.util.Set;
import jetbrains.buildServer.iaa.ProblemInfo;
import jetbrains.buildServer.iaa.common.Constants;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OneCommitterHeuristic extends Heuristic {

  @Override
  public long getUniqueOrder() {
    return 0;
  }

  @Override
  @NotNull
  public String getName() {
    return "Only One Committer Heuristic";
  }

  @Override
  @NotNull
  public String getDescription() {
    return "Assign an investigation to a user, if there are no other committers but him.";
  }

  @Override
  @Nullable
  public Pair<SUser, String> findResponsibleUser(@NotNull ProblemInfo problemInfo) {
    SBuild build = problemInfo.mySBuild;
    final SelectPrevBuildPolicy selectPrevBuildPolicy = SelectPrevBuildPolicy.SINCE_LAST_BUILD;
    final Set<SUser> committers = build.getCommitters(selectPrevBuildPolicy).getUsers();
    if (committers.isEmpty() || committers.size() != 1) return null;

    return Pair.create(committers.iterator().next(), String.format("%s you were the only committer to the following " +
                                                                   "build: %s # %s", Constants.REASON_PREFIX,
                                                                   build.getFullName(), build.getBuildNumber()));
  }
}
