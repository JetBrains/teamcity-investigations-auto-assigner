package jetbrains.buildServer.investigationsAutoAssigner.utils;

import java.util.concurrent.atomic.AtomicInteger;
import jetbrains.buildServer.BuildProblemTypes;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.STest;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.serverSide.buildLog.LogMessage;
import jetbrains.buildServer.serverSide.problems.BuildLogCompileErrorCollector;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.util.ItemProcessor;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.serverSide.impl.problems.types.CompilationErrorTypeDetailsProvider.COMPILE_BLOCK_INDEX;

public class ProblemTextExtractor {

  /**
   * Extracts the problem text for a given build problem and build.
   *
   * @param problem the build problem
   * @param build   the build to extract the problem text from
   * @return the problem description and associated log messages
   */
  public String getBuildProblemText(@NotNull final BuildProblem problem, @NotNull final SBuild build) {
    StringBuilder problemSpecificText = new StringBuilder();

    if (isCompilationError(problem)) {
      appendCompilationErrors(problem, build, problemSpecificText);
    }

    problemSpecificText.append(" ").append(problem.getBuildProblemDescription());
    return problemSpecificText.toString();
  }

  /**
   * Checks if the given build problem is related to a compilation error.
   *
   * @param problem the build problem
   * @return true if the problem is a compilation error, false otherwise
   */
  private boolean isCompilationError(@NotNull final BuildProblem problem) {
    return BuildProblemTypes.TC_COMPILATION_ERROR_TYPE.equals(problem.getBuildProblemData().getType());
  }

  /**
   * Appends compilation errors to the provided StringBuilder.
   *
   * @param problem             the build problem
   * @param build               the build to extract the compilation errors from
   * @param problemSpecificText the StringBuilder to append the errors to
   */
  private void appendCompilationErrors(@NotNull final BuildProblem problem,
                                       @NotNull final SBuild build,
                                       StringBuilder problemSpecificText) {
    final Integer compileBlockIndex = getCompileBlockIndex(problem);
    if (compileBlockIndex != null) {
      AtomicInteger maxErrors =
        new AtomicInteger(TeamCityProperties.getInteger(Constants.MAX_COMPILE_ERRORS_TO_PROCESS, 100));
      BuildLogCompileErrorCollector.collectCompileErrors(compileBlockIndex, build,
                                                         createLogMessageProcessor(problemSpecificText, maxErrors));
    }
  }

  /**
   * Creates a log message processor that appends log messages to the problemSpecificText.
   *
   * @param problemSpecificText the StringBuilder to append the log messages to
   * @param maxErrors           the maximum number of errors to process
   * @return the log message processor
   */
  private ItemProcessor<LogMessage> createLogMessageProcessor(final StringBuilder problemSpecificText,
                                                              final AtomicInteger maxErrors) {
    return item -> {
      problemSpecificText.append(item.getText()).append(" ");
      return maxErrors.decrementAndGet() > 0;
    };
  }

  /**
   * Extracts compile block index from the build problem.
   *
   * @param problem the build problem
   * @return compile block index or null if not found
   */
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

  /**
   * Extracts the problem text for a given test run.
   *
   * @param sTestRun the test run
   * @return the problem description for the test run
   */
  public String getBuildProblemText(STestRun sTestRun) {
    final STest test = sTestRun.getTest();
    final TestName testName = test.getName();
    return testName.getAsString() + " " + sTestRun.getFullText();
  }
}
