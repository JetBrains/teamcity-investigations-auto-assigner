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

import com.intellij.openapi.util.Pair;
import jetbrains.buildServer.BuildProblemTypes;
import jetbrains.buildServer.BuildProject;
import jetbrains.buildServer.responsibility.*;
import jetbrains.buildServer.responsibility.impl.BuildProblemResponsibilityEntryImpl;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.buildLog.BuildLogReaderEx;
import jetbrains.buildServer.serverSide.impl.problems.BuildProblemImpl;
import jetbrains.buildServer.serverSide.problems.BuildLogCompileErrorCollector;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.Dates;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static jetbrains.buildServer.serverSide.impl.problems.types.CompilationErrorTypeDetailsProvider.COMPILE_BLOCK_INDEX;

/**
 * @author Maxim.Manuylov
 *         Date: 09.04.2014
 */
public class NewTestsAndProblemsProcessorImpl implements NewTestsAndProblemsProcessor {
  @NotNull private final TestNameResponsibilityFacade myTestNameResponsibilityFacade;
  @NotNull private final BuildProblemResponsibilityFacade myBuildProblemResponsibilityFacade;

  public NewTestsAndProblemsProcessorImpl(@NotNull final TestNameResponsibilityFacade testNameResponsibilityFacade,
                                          @NotNull final BuildProblemResponsibilityFacade buildProblemResponsibilityFacade) {
    myTestNameResponsibilityFacade = testNameResponsibilityFacade;
    myBuildProblemResponsibilityFacade = buildProblemResponsibilityFacade;
  }

  public void onTestFailed(@NotNull final SRunningBuild build, @NotNull final STestRun testRun) {
    final SBuildType buildType = build.getBuildType();
    if (buildType == null) return;

    final STest test = testRun.getTest();
    final SProject project = buildType.getProject();

    if (testRun.isMuted() || testRun.isFixed() || !testRun.isNewFailure() || isInvestigated(test, project)) return;

    final TestName testName = test.getName();
    final String text = testName.getAsString() + " " + testRun.getFullText();

    final Pair<SUser, String> info = NewTestsAndProblemsUtil.findResponsibleUser(build, text);
    if (info == null) return;

    myTestNameResponsibilityFacade.setTestNameResponsibility(
      testName,
      project.getProjectId(),
      ResponsibilityEntryFactory.createEntry(
        testName, test.getTestNameId(), ResponsibilityEntry.State.TAKEN, info.getFirst(), null,
        Dates.now(), info.getSecond(), project, ResponsibilityEntry.RemoveMethod.WHEN_FIXED
      )
    );
  }

  public void onBuildProblemOccurred(@NotNull final SBuild build, @NotNull final BuildProblemImpl problem) {
    final SBuildType buildType = build.getBuildType();
    if (buildType == null) return;

    final SProject project = buildType.getProject();
    if (problem.isMuted() || !isNew(problem) || isInvestigated(problem, project)) return;

    final String text = getBuildProblemText(problem, build);

    final Pair<SUser, String> info = NewTestsAndProblemsUtil.findResponsibleUser(build, text);
    if (info == null) return;

    myBuildProblemResponsibilityFacade.setBuildProblemResponsibility(
      problem,
      project.getProjectId(),
      new BuildProblemResponsibilityEntryImpl(
        ResponsibilityEntry.State.TAKEN, info.getFirst(), null, Dates.now(), info.getSecond(),
        ResponsibilityEntry.RemoveMethod.WHEN_FIXED, project, problem.getId()
      )
    );
  }

  private static String getBuildProblemText(@NotNull final BuildProblem problem, @NotNull final SBuild build) {
    String problemSpecificText = "";

    // todo make an extension point here
    if (problem.getBuildProblemData().getType().equals(BuildProblemTypes.TC_COMPILATION_ERROR_TYPE)) {
      final Integer compileBlockIndex = getCompileBlockIndex(problem);
      if (compileBlockIndex != null) {
        final List<String> errors = new BuildLogCompileErrorCollector().collectCompileErrors(compileBlockIndex, (BuildLogReaderEx) build.getBuildLog());
        problemSpecificText = StringUtil.join(errors, " ");
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
    }
    catch (Exception e) {
      return null;
    }
  }

  private static boolean isNew(@NotNull final BuildProblemImpl problem) {
    final Boolean isNew = problem.isNew();
    return isNew != null && isNew;
  }

  private static boolean isInvestigated(@NotNull final STest test, @NotNull final SProject project) {
    for (TestNameResponsibilityEntry entry : test.getAllResponsibilities()) {
      if (isActiveOrFixed(entry) && isSameOrParent(entry.getProject(), project)) return true;
    }
    return false;
  }

  private static boolean isInvestigated(@NotNull final BuildProblem problem, @NotNull final SProject project) {
    for (BuildProblemResponsibilityEntry entry : problem.getAllResponsibilities()) {
      if (isActiveOrFixed(entry) && isSameOrParent(entry.getProject(), project)) return true;
    }
    return false;
  }

  private static boolean isActiveOrFixed(@NotNull final ResponsibilityEntry entry) {
    final ResponsibilityEntry.State state = entry.getState();
    return state.isActive() || state.isFixed();
  }

  public static boolean isSameOrParent(@NotNull final BuildProject parent, @NotNull final BuildProject project) {
    if (parent.getProjectId().equals(project.getProjectId())) return true;
    final BuildProject parentProject = project.getParentProject();
    return parentProject != null && isSameOrParent(parent, parentProject);
  }
}
