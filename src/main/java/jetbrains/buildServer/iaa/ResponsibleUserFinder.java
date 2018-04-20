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
import com.intellij.openapi.util.Pair;
import java.util.List;
import jetbrains.buildServer.iaa.heuristics.Heuristic;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.users.SUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ResponsibleUserFinder {
  private static final Logger LOGGER = Logger.getInstance(ResponsibleUserFinder.class.getName());
  private List<Heuristic> myOrderedHeuristics;

  public ResponsibleUserFinder(@NotNull final List<Heuristic> orderedHeuristics) {
    myOrderedHeuristics = orderedHeuristics;
  }

  @Nullable
  Pair<SUser, String> findResponsibleUser(@NotNull final SBuild sBuild, @Nullable final String problemText) {
    long buildId = sBuild.getBuildId();
    LOGGER.debug(String.format("Attempt to find responsible user for failed build #%s. ProblemText is %s",
                               buildId, problemText));
    ProblemInfo problemInfo = new ProblemInfo(sBuild, problemText);
    Pair<SUser, String> responsibleUser = null;
    for (Heuristic heuristic: myOrderedHeuristics) {
      LOGGER.debug(String.format("Attempt to find responsible user for failed build #%s with heuristic %s",
                                 buildId,heuristic.getName()));
      responsibleUser = heuristic.findResponsibleUser(problemInfo);
      if (responsibleUser != null) {
        LOGGER.info(String.format("Responsible user %s for failed build #%s has been found according to %s",
                                  responsibleUser.first, sBuild.getBuildId(), responsibleUser.second));
        break;
      }
    }
    if (responsibleUser == null) {
      LOGGER.info(String.format("Responsible user for failed build #%s not found", sBuild.getBuildId()));
    }
    return responsibleUser;
  }
}
