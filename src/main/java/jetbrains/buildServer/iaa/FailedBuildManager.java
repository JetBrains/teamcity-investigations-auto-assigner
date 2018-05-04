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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import jetbrains.buildServer.serverSide.SBuild;
import org.jetbrains.annotations.NotNull;

class FailedBuildManager {
  private ConcurrentHashMap<Long, FailedBuildInfo> failedBuilds = new ConcurrentHashMap<>();

  void addFailedBuild(SBuild sBuild) {
    failedBuilds.putIfAbsent(sBuild.getBuildId(), new FailedBuildInfo());
  }

  void removeBuild(SBuild sBuild) {
    failedBuilds.remove(sBuild.getBuildId());
  }

  Set<Long> getBuilds() {
    return failedBuilds.keySet();
  }

  @NotNull
  FailedBuildInfo getFailedBuildInfo(SBuild sBuild) {
    FailedBuildInfo buildInfo = failedBuilds.get(sBuild.getBuildId());
    if (buildInfo == null) {
      throw new IllegalArgumentException("Failed Build Manager doesn't contains a build #" + sBuild.getBuildId());
    }
    return buildInfo;
  }
}
