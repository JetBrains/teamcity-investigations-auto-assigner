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

package jetbrains.buildServer.investigationsAutoAssigner.utils.errors;

import java.util.concurrent.atomic.AtomicInteger;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.serverSide.problems.BuildLogCompileErrorCollector;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompilationErrorProcessor implements BuildProblemProcessor {
  @Override
  public String process(@NotNull final BuildProblem buildProblem, @NotNull final SBuild build, @Nullable Integer compileBlockIndex) {
    final StringBuilder problemSpecificText = new StringBuilder();
    if (compileBlockIndex != null) {
      final AtomicInteger maxErrors =
        new AtomicInteger(TeamCityProperties.getInteger(Constants.MAX_COMPILE_ERRORS_TO_PROCESS, 100));
      BuildLogCompileErrorCollector.collectCompileErrors(compileBlockIndex, build, item -> {
        problemSpecificText.append(item.getText()).append(" ");
        return maxErrors.decrementAndGet() > 0;
      });
    }
    return problemSpecificText.toString();
  }
}
