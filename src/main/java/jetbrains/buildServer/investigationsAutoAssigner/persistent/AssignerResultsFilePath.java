

package jetbrains.buildServer.investigationsAutoAssigner.persistent;

import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.log.LogUtil;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.STestRun;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AssignerResultsFilePath {

  @NotNull
  public Path get(@NotNull final SBuild build) throws IOException {
    Path resultPath = resolvePath(build, true, null);
    if (resultPath == null) {
      throw new IllegalStateException("TeamCity artifact directory does not exist for " + LogUtil.describe(build));
    }
    return resultPath;
  }

  @Nullable
  public Path getIfExist(@NotNull final SBuild build, @Nullable final STestRun testRun) throws IOException {
    return resolvePath(build, false, testRun);
  }

  @Nullable
  private Path resolvePath(@NotNull final SBuild build,
                           boolean createIfNotExist,
                           @Nullable final STestRun testRun) throws IOException {
    Path artifactsDir = build.getArtifactsDirectory().toPath();
    Path teamcityDir = artifactsDir.resolve(Constants.TEAMCITY_DIRECTORY);

    if (!Files.exists(teamcityDir)) {
      logSkipReason(testRun, teamcityDir);
      return null;
    }

    Path autoAssignerDir = teamcityDir.resolve(Constants.ARTIFACT_DIRECTORY);
    createIfMissing(autoAssignerDir, createIfNotExist);

    Path resultFile = autoAssignerDir.resolve(Constants.ARTIFACT_FILENAME);
    createIfMissing(resultFile, createIfNotExist);

    return Files.exists(resultFile) ? resultFile : null;
  }

  private void createIfMissing(@NotNull Path path, boolean create) throws IOException {
    if (Files.notExists(path) && create) {
      if (path.toString().endsWith(Constants.ARTIFACT_FILENAME)) {
        Files.createFile(path);
      } else {
        Files.createDirectory(path);
      }
    }
  }

  private void logSkipReason(@Nullable STestRun testRun, @NotNull Path missingPath) {
    Constants.LOGGER.debug("Skip investigation suggestion logic for " +
                           (testRun != null ? LogUtil.describe(testRun) : "test runs") +
                           " as " + missingPath + " doesn't exist.");
  }
}