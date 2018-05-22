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

package jetbrains.buildServer.iaa.heuristics;

import jetbrains.buildServer.iaa.FailedBuildContext;
import jetbrains.buildServer.iaa.HeuristicResult;
import org.jetbrains.annotations.NotNull;

/**
 * Presents heuristic that try to detect which person is probably responsible.
 * Order of provided heuristics should be specified in the spring xml-config file.
 */
public interface Heuristic {

  /**
   * @return short user-readable name of the heuristic.
   */
  @NotNull
  String getName();

  /**
   * @return sufficient description of the heuristic.
   */
  @NotNull
  String getDescription();

  /**
   * Try to detect which person is probably responsible.
   * @param failedBuildContext {@link FailedBuildContext} object which presents known information about the problem.
   */
  HeuristicResult findResponsibleUser(@NotNull FailedBuildContext failedBuildContext);
}
