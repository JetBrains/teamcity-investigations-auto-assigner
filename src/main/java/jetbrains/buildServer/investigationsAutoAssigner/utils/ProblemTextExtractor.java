

package jetbrains.buildServer.investigationsAutoAssigner.utils;

import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.BuildProblemTypes;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.STest;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.tests.TestName;

import org.jetbrains.annotations.NotNull;


public class ProblemTextExtractor {
  private final Map<String, BuildProblemTextExtractor> textExtractorsMap = new HashMap<>();
  private final BuildProblemTextExtractor defaultExtractor;

  public ProblemTextExtractor() {
    this.textExtractorsMap.put(BuildProblemTypes.TC_COMPILATION_ERROR_TYPE, new CompilationErrorTextExtractor());

    this.defaultExtractor = (problem, build) -> {
      String description = problem.getBuildProblemDescription();
      return description != null ? description : "no problem description available";
    };
  }
  public String getBuildProblemText(@NotNull BuildProblem problem, @NotNull SBuild build) {
    String type = problem.getBuildProblemData().getType();
    BuildProblemTextExtractor extractor = textExtractorsMap.getOrDefault(type, defaultExtractor);
    return extractor.extractText(problem, build);
  }


  public String getFailedTestText(@NotNull STestRun sTestRun) {
    final STest test = sTestRun.getTest();
    final TestName testName = test.getName();
    return testName.getAsString() + " " + sTestRun.getFullText();
  }
}