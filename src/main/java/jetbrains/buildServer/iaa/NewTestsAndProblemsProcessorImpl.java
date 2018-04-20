/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import java.util.List;
import jetbrains.buildServer.BuildProblemTypes;
import jetbrains.buildServer.responsibility.BuildProblemResponsibilityFacade;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.responsibility.ResponsibilityEntryFactory;
import jetbrains.buildServer.responsibility.TestNameResponsibilityFacade;
import jetbrains.buildServer.responsibility.impl.BuildProblemResponsibilityEntryImpl;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.buildLog.LogMessage;
import jetbrains.buildServer.serverSide.impl.problems.BuildProblemImpl;
import jetbrains.buildServer.serverSide.problems.BuildLogCompileErrorCollector;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.Dates;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.serverSide.impl.problems.types.CompilationErrorTypeDetailsProvider.COMPILE_BLOCK_INDEX;

public class NewTestsAndProblemsProcessorImpl implements NewTestsAndProblemsProcessor {
  @NotNull private final TestNameResponsibilityFacade myTestNameResponsibilityFacade;
  @NotNull private final BuildProblemResponsibilityFacade myBuildProblemResponsibilityFacade;
  @NotNull private ResponsibleUserFinder myResponsibleUserFinder;
  @NotNull private TestApplicabilityChecker myTestApplicabilityChecker;
  @NotNull private BuildApplicabilityChecker myBuildApplicabilityChecker;

  private static final Logger LOGGER = Logger.getInstance(NewTestsAndProblemsProcessorImpl.class.getName());

  public NewTestsAndProblemsProcessorImpl(@NotNull final TestNameResponsibilityFacade testNameResponsibilityFacade,
                                          @NotNull final BuildProblemResponsibilityFacade buildProblemResponsibilityFacade,
                                          @NotNull final ResponsibleUserFinder responsibleUserFinder,
                                          @NotNull final TestApplicabilityChecker testApplicabilityChecker,
                                          @NotNull final BuildApplicabilityChecker buildApplicabilityChecker) {
    myTestNameResponsibilityFacade = testNameResponsibilityFacade;
    myBuildProblemResponsibilityFacade = buildProblemResponsibilityFacade;
    myResponsibleUserFinder = responsibleUserFinder;
    myTestApplicabilityChecker = testApplicabilityChecker;
    myBuildApplicabilityChecker = buildApplicabilityChecker;
  }

  public void onTestFailed(@NotNull final SRunningBuild build, @NotNull final STestRun testRun) {
    final SBuildType buildType = build.getBuildType();
    if (buildType == null) return;
    final STest test = testRun.getTest();
    final SProject project = buildType.getProject();

    if (!myTestApplicabilityChecker.check(project, testRun)) {
      LOGGER.debug(String.format("Stop processing a failed test %s as it's incompatible", test.getTestNameId()));
      return;
    }

    final TestName testName = test.getName();
    final String text = testName.getAsString() + " " + testRun.getFullText();

    Pair<SUser, String> responsibleUser = myResponsibleUserFinder.findResponsibleUser(build, text);
    if (responsibleUser == null) return;

    myTestNameResponsibilityFacade.setTestNameResponsibility(
      testName,
      project.getProjectId(),
      ResponsibilityEntryFactory.createEntry(
        testName, test.getTestNameId(), ResponsibilityEntry.State.TAKEN, responsibleUser.getFirst(), null,
        Dates.now(), responsibleUser.getSecond(), project, ResponsibilityEntry.RemoveMethod.WHEN_FIXED
      )
    );
  }

  public void onBuildProblemOccurred(@NotNull final SBuild build, @NotNull final BuildProblemImpl problem) {
    final SBuildType buildType = build.getBuildType();
    if (buildType == null) return;
    final SProject project = buildType.getProject();

    if (!myBuildApplicabilityChecker.check(project, problem)) {
      LOGGER.debug(String.format("Stop processing a failed build #%s as it's applicable", build.getBuildId()));
      return;
    }

    final String text = getBuildProblemText(problem, build);
    Pair<SUser, String> responsibleUser = myResponsibleUserFinder.findResponsibleUser(build, text);
    if (responsibleUser == null) return;

    myBuildProblemResponsibilityFacade.setBuildProblemResponsibility(
      problem,
      project.getProjectId(),
      new BuildProblemResponsibilityEntryImpl(
        ResponsibilityEntry.State.TAKEN, responsibleUser.getFirst(), null, Dates.now(), responsibleUser.getSecond(),
        ResponsibilityEntry.RemoveMethod.WHEN_FIXED, project, problem.getId()
      )
    );
  }

  private static String getBuildProblemText(@NotNull final BuildProblem problem, @NotNull final SBuild build) {
    StringBuilder problemSpecificText = new StringBuilder();

    // todo make an extension point here
    if (problem.getBuildProblemData().getType().equals(BuildProblemTypes.TC_COMPILATION_ERROR_TYPE)) {
      final Integer compileBlockIndex = getCompileBlockIndex(problem);
      if (compileBlockIndex != null) {
        final List<LogMessage> errors = new BuildLogCompileErrorCollector().collectCompileErrors(compileBlockIndex, (SBuild)build.getBuildLog());
        for (LogMessage error : errors) {
          problemSpecificText.append(error.getText()).append(" ");
        }
      }
    }

    return problemSpecificText + " " + problem.getBuildProblemDescription();
  }

  @Nullable
  private static Integer getCompileBlockIndex(@NotNull final BuildProblem problem) {
    final String compilationBlockIndex = problem.getBuildProblemData().getAdditionalData();
    if (compilationBlockIndex == null) return null;

    try {
      return Integer.parseInt(StringUtil.stringToProperties(compilationBlockIndex, StringUtil.STD_ESCAPER2).get(COMPILE_BLOCK_INDEX));
    } catch (Exception e) {
      return null;
    }
  }
}
