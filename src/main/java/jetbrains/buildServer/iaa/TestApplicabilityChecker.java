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

package jetbrains.buildServer.iaa;

import jetbrains.buildServer.iaa.utils.FlakyTestDetector;
import jetbrains.buildServer.iaa.utils.InvestigationsManager;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STest;
import jetbrains.buildServer.serverSide.STestRun;
import org.jetbrains.annotations.NotNull;

class TestApplicabilityChecker {

  @NotNull private FlakyTestDetector myFlakyTestDetector;
  @NotNull private InvestigationsManager myInvestigationsManager;

  TestApplicabilityChecker(@NotNull FlakyTestDetector flakyTestDetector,
                           @NotNull final InvestigationsManager investigationsManager) {
    myFlakyTestDetector = flakyTestDetector;
    myInvestigationsManager = investigationsManager;
  }

  boolean isApplicable(@NotNull final SProject project,
                       @NotNull final SBuild sBuild,
                       @NotNull final STestRun testRun) {
    final STest test = testRun.getTest();

    return !testRun.isMuted() &&
           !testRun.isFixed() &&
           testRun.isNewFailure() &&
           !myInvestigationsManager.checkUnderInvestigation(project, sBuild, test) &&
           !myFlakyTestDetector.isFlaky(test.getTestNameId());
  }
}
