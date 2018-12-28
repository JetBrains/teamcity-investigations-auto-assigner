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

package jetbrains.buildServer.investigationsAutoAssigner.processing;

import com.intellij.openapi.diagnostic.Logger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jetbrains.buildServer.investigationsAutoAssigner.common.DefaultUserResponsibility;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.responsibility.ResponsibilityEntryEx;
import jetbrains.buildServer.responsibility.TestNameResponsibilityFacade;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.util.Dates;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import org.jetbrains.annotations.NotNull;

public class FailedTestAssigner {
  @NotNull private final TestNameResponsibilityFacade myTestNameResponsibilityFacade;
  private static final Logger LOGGER = Logger.getInstance(FailedTestAssigner.class.getName());

  public FailedTestAssigner(@NotNull final TestNameResponsibilityFacade testNameResponsibilityFacade) {
    myTestNameResponsibilityFacade = testNameResponsibilityFacade;
  }

  void assign(final HeuristicResult heuristicsResult,
              final SProject sProject,
              final SBuild sBuild,
              final List<STestRun> sTestRuns) {
    if (heuristicsResult.isEmpty()) return;

    HashMap<Responsibility, List<TestName>> responsibilityToTestNames = new HashMap<>();
    for (STestRun sTestRun : sTestRuns) {
      Responsibility responsibility = heuristicsResult.getResponsibility(sTestRun);
      responsibilityToTestNames.computeIfAbsent(responsibility, devNull -> new ArrayList<>());
      List<TestName> testNameList = responsibilityToTestNames.get(responsibility);
      testNameList.add(sTestRun.getTest().getName());
    }

    Set<Long> committersIds = sBuild.getCommitters(SelectPrevBuildPolicy.SINCE_LAST_BUILD)
                                    .getUsers()
                                    .stream()
                                    .map(User::getId)
                                    .collect(Collectors.toSet());

    Set<Responsibility> uniqueResponsibilities = responsibilityToTestNames.keySet();
    for (Responsibility responsibility : uniqueResponsibilities) {
      if (shouldAssignInvestigation(responsibility, committersIds)) {
        List<TestName> testNameList = responsibilityToTestNames.get(responsibility);
        LOGGER.info(String.format("Automatically assigning investigation(s) to %s in %s # %s because of %s",
                                  responsibility.getUser().getUsername(),
                                  sProject.describe(false),
                                  testNameList,
                                  responsibility.getAssignDescription()));

        myTestNameResponsibilityFacade.setTestNameResponsibility(
          testNameList, sProject.getProjectId(),
          new ResponsibilityEntryEx(
            ResponsibilityEntry.State.TAKEN, responsibility.getUser(), null, Dates.now(),
            responsibility.getAssignDescription(), ResponsibilityEntry.RemoveMethod.WHEN_FIXED)
        );
      }
    }
  }

  private boolean shouldAssignInvestigation(final Responsibility responsibility, final Set<Long> committersIds) {
    return responsibility != null &&
           (responsibility instanceof DefaultUserResponsibility ||
            committersIds.contains(responsibility.getUser().getId()));
  }
}
