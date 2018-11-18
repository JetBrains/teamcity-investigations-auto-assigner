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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import jetbrains.buildServer.investigationsAutoAssigner.common.FailedBuildInfo;
import jetbrains.buildServer.investigationsAutoAssigner.utils.CustomParameters;
import jetbrains.buildServer.investigationsAutoAssigner.utils.FlakyTestDetector;
import jetbrains.buildServer.investigationsAutoAssigner.utils.InvestigationsManager;
import jetbrains.buildServer.investigationsAutoAssigner.utils.Utils;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STest;
import jetbrains.buildServer.serverSide.STestRun;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class FailedTestFilter {

  private static final Logger LOGGER = Logger.getInstance(FailedTestFilter.class.getName());
  private final InvestigationsManager myInvestigationsManager;
  private final FlakyTestDetector myFlakyTestDetector;

  public FailedTestFilter(@NotNull FlakyTestDetector flakyTestDetector,
                          @NotNull final InvestigationsManager investigationsManager) {
    myFlakyTestDetector = flakyTestDetector;
    myInvestigationsManager = investigationsManager;
  }

  List<STestRun> apply(final FailedBuildInfo failedBuildInfo, final SProject sProject, final List<STestRun> testRuns) {
    SBuild sBuild = failedBuildInfo.getBuild();
    Integer threshold = CustomParameters.getMaxTestsPerBuildThreshold(sBuild);
    List<STestRun> orderedTestRuns = new ArrayList<>(testRuns);
    orderedTestRuns.sort(Comparator.comparingInt(STestRun::getOrderId));
    List<STestRun> filteredTestRuns = orderedTestRuns.stream()
                                                     .filter(failedBuildInfo::checkNotProcessed)
                                                     .filter(testRun -> isApplicable(sProject, sBuild, testRun))
                                                     .limit(threshold - failedBuildInfo.processed)
                                                     .collect(Collectors.toList());

    failedBuildInfo.addProcessedTestRuns(testRuns);
    failedBuildInfo.processed += filteredTestRuns.size();

    return filteredTestRuns;
  }

  List<STestRun> applyBeforeAssign(final FailedBuildInfo failedBuildInfo,
                                   final SProject sProject,
                                   final List<STestRun> testRuns) {
    SBuild sBuild = failedBuildInfo.getBuild();

    return testRuns.stream()
                   .filter(testRun -> isApplicable(sProject, sBuild, testRun))
                   .collect(Collectors.toList());
  }

  private boolean isApplicable(@NotNull final SProject project,
                               @NotNull final SBuild sBuild,
                               @NotNull final STestRun testRun) {
    String reason = null;

    final STest test = testRun.getTest();
    if (testRun.isMuted()) {
      reason = "is muted";
    } else if (testRun.isFixed()) {
      reason = "is fixed";
    } else if (!testRun.isNewFailure()) {
      reason = "occurs not for the first time";
    } else if (myInvestigationsManager.checkUnderInvestigation(project, sBuild, test)) {
      reason = "is already under an investigation";
    } else if (myFlakyTestDetector.isFlaky(test.getTestNameId())) {
      reason = "is marked as flaky";
    }

    boolean isApplicable = reason == null;
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(String.format("%s Test problem is %s.%s",
                                 Utils.getLogPrefix(testRun),
                                 (isApplicable ? "applicable" : "not applicable"),
                                 (isApplicable ? "" : String.format(" Reason: this test problem %s.", reason))
      ));
    }
    return isApplicable;
  }
}
