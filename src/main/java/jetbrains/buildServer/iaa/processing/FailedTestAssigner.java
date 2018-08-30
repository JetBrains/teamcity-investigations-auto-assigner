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

package jetbrains.buildServer.iaa.processing;

import com.intellij.openapi.diagnostic.Logger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import jetbrains.buildServer.iaa.common.HeuristicResult;
import jetbrains.buildServer.iaa.common.Responsibility;
import jetbrains.buildServer.iaa.utils.AssignerArtifactDao;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.responsibility.ResponsibilityEntryEx;
import jetbrains.buildServer.responsibility.TestNameResponsibilityFacade;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.util.Dates;
import org.jetbrains.annotations.NotNull;

class FailedTestAssigner {
  @NotNull private final TestNameResponsibilityFacade myTestNameResponsibilityFacade;
  private static final Logger LOGGER = Logger.getInstance(FailedTestAssigner.class.getName());
  private final AssignerArtifactDao myAssignerDao;

  FailedTestAssigner(@NotNull final TestNameResponsibilityFacade testNameResponsibilityFacade,
                     @NotNull final AssignerArtifactDao assignerArtifactDao) {
    myTestNameResponsibilityFacade = testNameResponsibilityFacade;
    myAssignerDao = assignerArtifactDao;
  }

  void assign(final HeuristicResult heuristicsResult,
              final SProject sProject,
              final List<STestRun> sTestRuns) {
    assign(heuristicsResult, sProject,sTestRuns, false);
  }

  void assign(final HeuristicResult heuristicsResult,
              final SProject sProject,
              final List<STestRun> sTestRuns,
              final boolean silentModeOn) {
    HashMap<Responsibility, List<TestName>> responsibilityToTestNames = new HashMap<>();
    for (STestRun sTestRun : sTestRuns) {
      Responsibility responsibility = heuristicsResult.getResponsibility(sTestRun);
      responsibilityToTestNames.putIfAbsent(responsibility, new ArrayList<>());
      List<TestName> testNameList = responsibilityToTestNames.get(responsibility);
      testNameList.add(sTestRun.getTest().getName());

      myAssignerDao.put(sTestRun, responsibility);
    }

    Set<Responsibility> uniqueResponsibilities = responsibilityToTestNames.keySet();

    for (Responsibility responsibility : uniqueResponsibilities) {
      if (responsibility != null) {
        List<TestName> testNameList = responsibilityToTestNames.get(responsibility);
        String prefix = silentModeOn ? "Silently found " : "Automatically assigning";
        LOGGER.info(String.format("%s investigation(s) to %s in %s # %s because of %s",
                                  prefix,
                                  responsibility.getUser().getUsername(),
                                  sProject.describe(false),
                                  testNameList,
                                  responsibility.getDescription()));
        if (!silentModeOn) {
          myTestNameResponsibilityFacade.setTestNameResponsibility(
            testNameList, sProject.getProjectId(),
            new ResponsibilityEntryEx(
              ResponsibilityEntry.State.TAKEN, responsibility.getUser(), null, Dates.now(),
              responsibility.getDescription(), ResponsibilityEntry.RemoveMethod.WHEN_FIXED)
          );
        }
      }
    }
  }
}
