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

package jetbrains.buildServer.investigationsAutoAssigner.persistent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.log.LogUtil;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.STestRun;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AssignerResultsFilePath {
  @NotNull
  public Path get(@NotNull final SBuild build) throws IOException {
    Path resultPath = get(build, true, null);
    if (resultPath == null) {
      throw new IllegalStateException("TeamCity artifact directory does not exist for " + LogUtil.describe(build));
    }

    return resultPath;
  }

  @Nullable
  public Path getIfExist(@NotNull final SBuild build,
                         @Nullable final STestRun testRun) throws IOException {
    return get(build, false, testRun);
  }

  @Nullable
  private Path get(@NotNull final SBuild build,
                   boolean createIfNotExist,
                   @Nullable final STestRun testRun) throws IOException {
    Path artifactDirectoryPath = build.getArtifactsDirectory().toPath();
    Path teamcityDirectoryPath = artifactDirectoryPath.resolve(Constants.TEAMCITY_DIRECTORY);
    if (!Files.exists(teamcityDirectoryPath)) {
      Constants.LOGGER.debug("Skip investigation suggestion logic for " +
                             (testRun != null ? LogUtil.describe(testRun) : " test runs ") +
                             " as " + teamcityDirectoryPath + " doesn't exists.");
      return null;
    }

    Path autoAssignerDirectoryPath = teamcityDirectoryPath.resolve(Constants.ARTIFACT_DIRECTORY);
    if (!Files.exists(autoAssignerDirectoryPath)) {
      if (createIfNotExist) {
        Files.createDirectory(autoAssignerDirectoryPath);
      } else {
        return null;
      }
    }

    Path resultsPath = autoAssignerDirectoryPath.resolve(Constants.ARTIFACT_FILENAME);
    if (!Files.exists(resultsPath)) {
      if (createIfNotExist) {
        Files.createFile(resultsPath);
      } else {
        return null;
      }
    }

    return resultsPath;
  }

}
