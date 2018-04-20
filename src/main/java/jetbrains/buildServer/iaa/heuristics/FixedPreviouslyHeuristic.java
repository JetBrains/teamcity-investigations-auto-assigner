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

import com.intellij.openapi.util.Pair;
import jetbrains.buildServer.iaa.ProblemInfo;
import jetbrains.buildServer.iaa.utils.InvestigationsManager;
import jetbrains.buildServer.users.SUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class FixedPreviouslyHeuristic implements Heuristic {

  private InvestigationsManager myInvestigationsManager;

  FixedPreviouslyHeuristic(InvestigationsManager investigationsManager) {
    myInvestigationsManager = investigationsManager;
  }

  @NotNull
  @Override
  public String getName() {
    return "Fixed Previously Heuristic";
  }

  @NotNull
  @Override
  public String getDescription() {
    return "Assign an investigation to a user if the user was responsible previous time.";
  }

  @Nullable
  @Override
  public Pair<SUser, String> findResponsibleUser(@NotNull final ProblemInfo problemInfo) {
    throw new NotImplementedException();
  }
}
