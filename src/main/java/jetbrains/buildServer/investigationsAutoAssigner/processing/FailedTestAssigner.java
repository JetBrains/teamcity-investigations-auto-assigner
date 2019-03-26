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
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.persistent.StatisticsReporter;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.responsibility.ResponsibilityEntryEx;
import jetbrains.buildServer.responsibility.TestNameResponsibilityFacade;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.WebLinks;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.util.Dates;
import org.jetbrains.annotations.NotNull;

public class FailedTestAssigner extends BaseAssigner {
  @NotNull private final TestNameResponsibilityFacade myTestNameResponsibilityFacade;
  private WebLinks myWebLinks;
  private StatisticsReporter myStatisticsReporter;
  private static final Logger LOGGER = Constants.LOGGER;

  public FailedTestAssigner(@NotNull final TestNameResponsibilityFacade testNameResponsibilityFacade,
                            @NotNull final WebLinks webLinks,
                            @NotNull final StatisticsReporter statisticsReporter) {
    myTestNameResponsibilityFacade = testNameResponsibilityFacade;
    myWebLinks = webLinks;
    myStatisticsReporter = statisticsReporter;
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

    Set<Long> committersIds = calculateCommitersIds(sBuild);

    Set<Responsibility> uniqueResponsibilities = responsibilityToTestNames.keySet();
    for (Responsibility responsibility : uniqueResponsibilities) {
      if (shouldAssignInvestigation(responsibility, committersIds)) {
        List<TestName> testNameList = responsibilityToTestNames.get(responsibility);
        LOGGER.info(String.format("Automatically assigning investigation(s) to %s in %s # %s because user %s",
                                  responsibility.getUser().getUsername(),
                                  sProject.describe(false),
                                  testNameList,
                                  responsibility.getDescription()));

        String linkToBuild = myWebLinks.getViewResultsUrl(sBuild);
        myTestNameResponsibilityFacade.setTestNameResponsibility(
          testNameList, sProject.getProjectId(),
          new ResponsibilityEntryEx(
            ResponsibilityEntry.State.TAKEN, responsibility.getUser(), null, Dates.now(),
            responsibility.getAssignDescription(linkToBuild), ResponsibilityEntry.RemoveMethod.WHEN_FIXED)
        );

        myStatisticsReporter.reportAssignedInvestigations(testNameList.size());
      }
    }
  }
}
