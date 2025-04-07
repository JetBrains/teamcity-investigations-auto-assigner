

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

    Path autoAssignerDirectoryPath = checkIfExists(teamcityDirectoryPath.resolve(Constants.ARTIFACT_DIRECTORY), createIfNotExist, false);
    if (autoAssignerDirectoryPath == null) return null;
    return checkIfExists(autoAssignerDirectoryPath.resolve(Constants.ARTIFACT_FILENAME), createIfNotExist, true);
  }

  private Path checkIfExists(Path path, boolean createIfNotExist, boolean fileFlag) throws IOException {
    if (Files.exists(path)) return path;
    if (createIfNotExist) return fileFlag ? Files.createFile(path) : Files.createDirectory(path);
    return null;
  }

}