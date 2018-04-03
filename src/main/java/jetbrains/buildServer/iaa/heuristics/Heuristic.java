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
import jetbrains.buildServer.users.SUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class Heuristic implements Comparable<Heuristic> {
  public abstract long getUniqueOrder();

  @NotNull
  public abstract String getName();

  @NotNull
  public abstract String getDescription();

  @Nullable
  public abstract Pair<SUser, String> findResponsibleUser(@NotNull ProblemInfo problemInfo);

  @Override
  public int compareTo(@NotNull final Heuristic another) {
    Long order = getUniqueOrder();
    return order.compareTo(another.getUniqueOrder());
  }

  @Override
  public boolean equals(final Object obj) {
    return obj instanceof Heuristic && ((Heuristic)obj).getUniqueOrder() == this.getUniqueOrder();
  }
}
