/*
 * Copyright 2000-2019 JetBrains s.r.o.
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

package jetbrains.buildServer.investigationsAutoAssigner.processing;

import java.util.Set;
import java.util.stream.Collectors;
import jetbrains.buildServer.investigationsAutoAssigner.common.DefaultUserResponsibility;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;

abstract public class BaseAssigner {
  protected boolean shouldAssignInvestigation(final Responsibility responsibility, final Set<Long> committersIds) {
    return responsibility != null &&
           (responsibility instanceof DefaultUserResponsibility ||
            committersIds.contains(responsibility.getUser().getId()));
  }

  protected Set<Long> calculateCommitersIds(SBuild sBuild) {
    return sBuild.getCommitters(SelectPrevBuildPolicy.SINCE_LAST_BUILD)
                 .getUsers()
                 .stream()
                 .map(User::getId)
                 .collect(Collectors.toSet());
  }
}
