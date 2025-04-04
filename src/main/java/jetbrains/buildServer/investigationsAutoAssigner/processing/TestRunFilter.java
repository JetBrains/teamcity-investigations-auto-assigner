/*
 * Copyright 2000-2025 JetBrains s.r.o.
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

import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.investigationsAutoAssigner.utils.FlakyTestDetector;
import jetbrains.buildServer.investigationsAutoAssigner.utils.InvestigationsManager;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Filters test runs based on various conditions such as muting, flakiness, and prior investigations.
 */
public class TestRunFilter {

  private final InvestigationsManager investigationsManager;
  private final FlakyTestDetector flakyTestDetector;
  private final boolean ignoreSetupMethods;

  /**
   * Constructs a new {@code TestRunFilter}.
   *
   * @param investigationsManager The manager for checking existing investigations.
   * @param flakyTestDetector     The detector for identifying flaky tests.
   */
  public TestRunFilter(@NotNull InvestigationsManager investigationsManager,
                       @NotNull FlakyTestDetector flakyTestDetector) {
    this.investigationsManager = investigationsManager;
    this.flakyTestDetector = flakyTestDetector;
    this.ignoreSetupMethods = TeamCityProperties.getBooleanOrTrue(Constants.IGNORE_SETUP_TEARDOWN_METHODS);
  }

  /**
   * Filters applicable test runs.
   *
   * @param project                      The project associated with the build.
   * @param build                        The build in which the tests failed.
   * @param testRuns                     The list of test runs.
   * @param notApplicableTestDescription A map to store reasons why tests are deemed inapplicable.
   * @return A list of applicable test runs.
   */
  public List<STestRun> filterApplicableTests(@NotNull SProject project,
                                              @NotNull SBuild build,
                                              @NotNull List<STestRun> testRuns,
                                              @NotNull Map<Long, String> notApplicableTestDescription) {
    return testRuns.stream().filter(testRun -> isApplicable(project, build, testRun, notApplicableTestDescription))
                   .collect(Collectors.toList());
  }

  private boolean isApplicable(@NotNull SProject project,
                               @NotNull SBuild build,
                               @NotNull STestRun testRun,
                               @NotNull Map<Long, String> notApplicableTestDescription) {
    String reason = null;
    STest test = testRun.getTest();

    if (testRun.isMuted()) {
      reason = "was muted";
    } else if (testRun.isFixed()) {
      reason = "was fixed";
    } else if (!testRun.isNewFailure()) {
      reason = "occurred not for the first time";
    } else if (this.investigationsManager.checkUnderInvestigation(project, build, test)) {
      reason = "was already under an investigation";
    } else if (this.flakyTestDetector.isFlaky(test.getTestNameId())) {
      reason = "was marked as flaky";
    } else if (this.ignoreSetupMethods && isSetUpOrTearDown(test.getName())) {
      reason = "is not a test but rather setUp or tearDown";
    }

    boolean isApplicable = reason == null;
    if (!isApplicable && testRun.isNewFailure()) {
      notApplicableTestDescription.put(test.getTestNameId(), reason);
    }

    return isApplicable;
  }

  private boolean isSetUpOrTearDown(@NotNull TestName testName) {
    String methodName = testName.getTestMethodName().toLowerCase();
    return methodName.contains("setup") || methodName.contains("teardown");
  }
}

