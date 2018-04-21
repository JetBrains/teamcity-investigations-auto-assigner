/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.impl.problems.BuildProblemImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BuildProblemInfo extends ProblemInfo {
  @NotNull public final BuildProblemImpl myBuildProblem;

  BuildProblemInfo(@NotNull final BuildProblemImpl problem,
                          @NotNull final SBuild sBuild,
                          @NotNull final SProject project,
                          @Nullable final String problemText) {
    super(sBuild, project, problemText);
    myBuildProblem = problem;
  }
}
