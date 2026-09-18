

package jetbrains.buildServer.investigationsAutoAssigner.utils;


import java.util.concurrent.atomic.AtomicInteger;
import jetbrains.buildServer.BuildProblemTypes;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.STest;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.serverSide.problems.BuildLogCompileErrorCollector;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.serverSide.impl.problems.types.CompilationErrorTypeDetailsProvider.COMPILE_BLOCK_INDEX;

public class ProblemTextExtractor {
  public String getBuildProblemText(@NotNull final BuildProblem problem, @NotNull final SBuild build) {
    StringBuilder problemSpecificText = new StringBuilder();

    // todo make an extension point here
    if (problem.getBuildProblemData().getType().equals(BuildProblemTypes.TC_COMPILATION_ERROR_TYPE)) {
      final Integer compileBlockIndex = getCompileBlockIndex(problem);
      if (compileBlockIndex != null) {
        AtomicInteger maxErrors = new AtomicInteger(TeamCityProperties.getInteger(Constants.MAX_COMPILE_ERRORS_TO_PROCESS, 100));
        BuildLogCompileErrorCollector.collectCompileErrors(compileBlockIndex, build, item -> {
          problemSpecificText.append(item.getText()).append(" ");
          return maxErrors.decrementAndGet() > 0;
        });
      }
    }

    return problemSpecificText + " " + problem.getBuildProblemDescription();
  }

  @Nullable
  private static Integer getCompileBlockIndex(@NotNull final BuildProblem problem) {
    final String compilationBlockIndex = problem.getBuildProblemData().getAdditionalData();
    if (compilationBlockIndex == null) return null;

    try {
      return Integer.parseInt(
        StringUtil.stringToProperties(compilationBlockIndex, StringUtil.STD_ESCAPER2).get(COMPILE_BLOCK_INDEX));
    } catch (Exception e) {
      return null;
    }
  }

  public String getBuildProblemText(STestRun sTestRun) {
    final STest test = sTestRun.getTest();
    final TestName testName = test.getName();
    return testName.getAsString() + " " + sTestRun.getFullText();
  }
}