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

package jetbrains.buildServer.investigationsAutoAssigner.processing;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import org.jetbrains.annotations.NotNull;

public final class HeuristicContext {
  private final SProject mySProject;
  private final List<BuildProblem> myBuildProblems;
  private final List<STestRun> mySTestRuns;
  private final SBuild mySBuild;
  private final Set<String> myUsersToIgnore;
  private Set<Long> myCommitersIds = null;

  public HeuristicContext(SBuild sBuild,
                          SProject sProject,
                          List<BuildProblem> buildProblems,
                          List<STestRun> sTestRuns,
                          @NotNull Set<String> usernameBlackList) {
    mySBuild = sBuild;
    mySProject = sProject;
    myBuildProblems = buildProblems;
    mySTestRuns = sTestRuns;
    myUsersToIgnore = usernameBlackList;
  }

  @NotNull
  public SBuild getBuild() {
    return mySBuild;
  }

  @NotNull
  public SProject getProject() {
    return mySProject;
  }

  public List<BuildProblem> getBuildProblems() {
    return myBuildProblems;
  }

  public List<STestRun> getTestRuns() {
    return mySTestRuns;
  }

  @NotNull
  public Set<String> getUsersToIgnore() {
    return myUsersToIgnore;
  }

  @NotNull
  public Set<Long> getCommitersIds() {
    if (myCommitersIds == null) {
      myCommitersIds = calculateCommitersIds(mySBuild);
    }

    return myCommitersIds;
  }

  private static Set<Long> calculateCommitersIds(SBuild sBuild) {
    return sBuild.getCommitters(SelectPrevBuildPolicy.SINCE_LAST_BUILD)
                 .getUsers()
                 .stream()
                 .map(User::getId)
                 .collect(Collectors.toSet());
  }
}

