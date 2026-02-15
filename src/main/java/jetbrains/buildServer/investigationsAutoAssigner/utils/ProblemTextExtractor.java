

package jetbrains.buildServer.investigationsAutoAssigner.utils;

import jetbrains.buildServer.investigationsAutoAssigner.utils.errors.BuildProblemProcessorFactory;
import jetbrains.buildServer.investigationsAutoAssigner.utils.errors.BuildProblemProcessorNotFoundException;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.STest;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.serverSide.impl.problems.types.CompilationErrorTypeDetailsProvider.COMPILE_BLOCK_INDEX;

public class ProblemTextExtractor {
  public String getBuildProblemText(@NotNull final BuildProblem problem, @NotNull final SBuild build) {
    return BuildProblemProcessorFactory.getProcessor(problem.getBuildProblemData().getType())
      .map(processor -> processor.process(problem, build, getCompileBlockIndex(problem)))
      .map(text -> text + " " + problem.getBuildProblemDescription())
      .orElseThrow(() -> new BuildProblemProcessorNotFoundException("Processor not found for problem type: " + problem.getBuildProblemData().getType()));
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
