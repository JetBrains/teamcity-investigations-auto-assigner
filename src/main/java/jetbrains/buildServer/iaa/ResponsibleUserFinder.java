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

import com.intellij.openapi.diagnostic.Logger;
import java.util.List;
import jetbrains.buildServer.iaa.heuristics.Heuristic;
import org.jetbrains.annotations.NotNull;

public class ResponsibleUserFinder {
  private static final Logger LOGGER = Logger.getInstance(ResponsibleUserFinder.class.getName());
  private List<Heuristic> myOrderedHeuristics;

  ResponsibleUserFinder(@NotNull final List<Heuristic> orderedHeuristics) {
    myOrderedHeuristics = orderedHeuristics;
  }

 void findResponsibleUser(@NotNull FailedBuildContext failedBuildContext) {
    for (Heuristic heuristic: myOrderedHeuristics) {
      heuristic.findResponsibleUser(failedBuildContext);
    }
  }
}
