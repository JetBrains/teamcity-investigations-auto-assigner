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

package jetbrains.buildServer.investigationsAutoAssigner.utils;

import jetbrains.buildServer.BuildProblemTypes;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.problems.BuildLogCompileErrorCollector;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.concurrent.atomic.AtomicInteger;
import jetbrains.buildServer.serverSide.TeamCityProperties;


import static jetbrains.buildServer.serverSide.impl.problems.types.CompilationErrorTypeDetailsProvider.COMPILE_BLOCK_INDEX;

public class CompilationErrorTextExtractor implements BuildProblemTextExtractor {
  @NotNull
  @Override
  public String extractText(@NotNull final BuildProblem problem, @NotNull final SBuild build) {
    StringBuilder problemSpecificText = new StringBuilder();

    if (problem.getBuildProblemData().getType().equals(BuildProblemTypes.TC_COMPILATION_ERROR_TYPE)) {
      final Integer compileBlockIndex = getCompileBlockIndex(problem);
      if (compileBlockIndex != null) {
        AtomicInteger maxErrors = new AtomicInteger(TeamCityProperties.getInteger(Constants.MAX_COMPILE_ERRORS_TO_PROCESS, Constants.DEFAULT_MAX_COMPILE_ERRORS_TO_PROCESS));
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

}
