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

import jetbrains.buildServer.BuildProject;
import jetbrains.buildServer.iaa.utils.FlakyTestDetector;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.responsibility.TestNameResponsibilityEntry;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STest;
import jetbrains.buildServer.serverSide.STestRun;
import org.jetbrains.annotations.NotNull;

class TestApplicabilityChecker {

  @NotNull private FlakyTestDetector myFlakyTestDetector;

  TestApplicabilityChecker(@NotNull FlakyTestDetector flakyTestDetector) {
    myFlakyTestDetector = flakyTestDetector;
  }

  boolean check(@NotNull final SProject project,
                @NotNull final STestRun testRun) {
    final STest test = testRun.getTest();

    return !testRun.isMuted() &&
           !testRun.isFixed() &&
           testRun.isNewFailure() &&
           !isInvestigated(test, project) &&
           !myFlakyTestDetector.isFlaky(test.getTestNameId());
  }

  private static boolean isInvestigated(@NotNull final STest test, @NotNull final SProject project) {
    for (TestNameResponsibilityEntry entry : test.getAllResponsibilities()) {
      if (isActiveOrFixed(entry) && isSameProjectOrParent(entry.getProject(), project)) return true;
    }
    return false;
  }

  private static boolean isActiveOrFixed(@NotNull final ResponsibilityEntry entry) {
    final ResponsibilityEntry.State state = entry.getState();
    return state.isActive() || state.isFixed();
  }

  private static boolean isSameProjectOrParent(@NotNull final BuildProject parent,
                                               @NotNull final BuildProject project) {
    if (parent.getProjectId().equals(project.getProjectId())) return true;
    final BuildProject parentProject = project.getParentProject();
    return parentProject != null && isSameProjectOrParent(parent, parentProject);
  }
}
