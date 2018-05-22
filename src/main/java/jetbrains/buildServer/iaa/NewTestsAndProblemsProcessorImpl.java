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
import java.util.Collections;
import java.util.List;
import jetbrains.buildServer.responsibility.BuildProblemResponsibilityFacade;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.responsibility.ResponsibilityEntryFactory;
import jetbrains.buildServer.responsibility.TestNameResponsibilityFacade;
import jetbrains.buildServer.responsibility.impl.BuildProblemResponsibilityEntryImpl;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.impl.problems.BuildProblemImpl;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.util.Dates;
import org.jetbrains.annotations.NotNull;

public class NewTestsAndProblemsProcessorImpl implements NewTestsAndProblemsProcessor {
  @NotNull private final TestNameResponsibilityFacade myTestNameResponsibilityFacade;
  @NotNull private final BuildProblemResponsibilityFacade myBuildProblemResponsibilityFacade;
  @NotNull private ResponsibleUserFinder myResponsibleUserFinder;
  @NotNull private BuildApplicabilityChecker myBuildApplicabilityChecker;

  private static final Logger LOGGER = Logger.getInstance(NewTestsAndProblemsProcessorImpl.class.getName());

  NewTestsAndProblemsProcessorImpl(@NotNull final TestNameResponsibilityFacade testNameResponsibilityFacade,
                                   @NotNull final BuildProblemResponsibilityFacade buildProblemResponsibilityFacade,
                                   @NotNull final ResponsibleUserFinder responsibleUserFinder,
                                   @NotNull final BuildApplicabilityChecker buildApplicabilityChecker) {
    myTestNameResponsibilityFacade = testNameResponsibilityFacade;
    myBuildProblemResponsibilityFacade = buildProblemResponsibilityFacade;
    myResponsibleUserFinder = responsibleUserFinder;
    myBuildApplicabilityChecker = buildApplicabilityChecker;
  }

  public void processFailedTest(SBuild sBuild, List<BuildProblem> buildProblems, List<STestRun> sTestRuns) {
    final SBuildType buildType = sBuild.getBuildType();
    assert buildType != null;

    HeuristicResult heuristicsResult = myResponsibleUserFinder.findResponsibleUser(sBuild, buildProblems, sTestRuns);

    for (STestRun sTestRun : sTestRuns) {
      Responsibility responsibility = heuristicsResult.getResponsibility(sTestRun);
      if (responsibility != null) {
        final STest test = sTestRun.getTest();
        final SProject project = buildType.getProject();
        final TestName testName = test.getName();

        myTestNameResponsibilityFacade.setTestNameResponsibility(
          testName, project.getProjectId(),
          ResponsibilityEntryFactory.createEntry(
            testName, test.getTestNameId(), ResponsibilityEntry.State.TAKEN, responsibility.getUser(), null,
            Dates.now(), responsibility.getDescription(), project, ResponsibilityEntry.RemoveMethod.WHEN_FIXED
          )
        );
      }
    }

    for (BuildProblem buildProblem : buildProblems) {
      Responsibility responsibility = heuristicsResult.getResponsibility(buildProblem);
      if (responsibility != null) {
        final SProject project = buildType.getProject();

        myBuildProblemResponsibilityFacade.setBuildProblemResponsibility(
          buildProblem, project.getProjectId(),
          new BuildProblemResponsibilityEntryImpl(
            ResponsibilityEntry.State.TAKEN, responsibility.getUser(), null, Dates.now(),
            responsibility.getDescription(), ResponsibilityEntry.RemoveMethod.WHEN_FIXED, project, buildProblem.getId()
          )
        );
      }
    }
  }

  public void onBuildProblemOccurred(@NotNull final SBuild build, @NotNull final BuildProblemImpl problem) {
    final SBuildType buildType = build.getBuildType();
    if (buildType == null) return;
    final SProject project = buildType.getProject();

    if (!myBuildApplicabilityChecker.isApplicable(project, build, problem)) {
      LOGGER.debug(String.format("Stop processing a failed build #%s as it's applicable", build.getBuildId()));
      return;
    }

    HeuristicResult result =
      myResponsibleUserFinder.findResponsibleUser(build, Collections.singletonList(problem), Collections.emptyList());

    Responsibility responsibility = result.getResponsibility(problem);
    if (responsibility == null) return;

    myBuildProblemResponsibilityFacade.setBuildProblemResponsibility(
      problem,
      project.getProjectId(),
      new BuildProblemResponsibilityEntryImpl(
        ResponsibilityEntry.State.TAKEN, responsibility.getUser(), null, Dates.now(),
        responsibility.getDescription(), ResponsibilityEntry.RemoveMethod.WHEN_FIXED, project, problem.getId()
      )
    );
  }
}
