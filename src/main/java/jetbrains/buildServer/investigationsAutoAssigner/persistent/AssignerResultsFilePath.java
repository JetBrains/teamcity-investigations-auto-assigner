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

/**
 * This class handles the creation and retrieval of file paths for the assignment results
 * related to a specific build in TeamCity. It ensures that the necessary directories
 * and files for storing investigation suggestions exist, creating them if necessary.
 */
public class AssignerResultsFilePath {

  /**
   * Retrieves the path to the results file for a given build. If the path does not exist,
   * an exception is thrown.
   *
   * @param build The build for which the results path is to be retrieved.
   * @return The path to the results file.
   * @throws IOException If an error occurs while accessing or creating the file.
   * @throws IllegalStateException If the artifact directory does not exist for the build.
   */
  @NotNull
  public Path get(@NotNull final SBuild build) throws IOException {
    Path resultPath = getResultPath(build, true, null);
    if (resultPath == null) {
      throw new IllegalStateException("TeamCity artifact directory does not exist for " + LogUtil.describe(build));
    }
    return resultPath;
  }

  /**
   * Retrieves the path to the results file for a given build, if it exists. If the file or directory
   * does not exist, it returns null.
   *
   * @param build The build for which the results path is to be retrieved.
   * @param testRun The test run for which the results path is to be retrieved, or null if not applicable.
   * @return The path to the results file, or null if it doesn't exist.
   * @throws IOException If an error occurs while accessing the file.
   */
  @Nullable
  public Path getIfExist(@NotNull final SBuild build, @Nullable final STestRun testRun) throws IOException {
    return getResultPath(build, false, testRun);
  }

  /**
   * Retrieves the path to the results file for a given build, creating the necessary directories
   * and files if specified.
   *
   * @param build The build for which the results path is to be retrieved.
   * @param createIfNotExist If true, it creates the necessary directories and files if they don't exist.
   * @param testRun The test run for which the results path is to be retrieved, or null if not applicable.
   * @return The path to the results file, or null if it doesn't exist and creation is not allowed.
   * @throws IOException If an error occurs while accessing or creating the file.
   */
  @Nullable
  private Path getResultPath(@NotNull final SBuild build, boolean createIfNotExist, @Nullable final STestRun testRun)
    throws IOException {
    Path teamcityDirectoryPath = getTeamcityDirectory(build);
    if (teamcityDirectoryPath == null) {
      return null;
    }

    Path autoAssignerDirectoryPath =
      createDirectoryIfNotExist(teamcityDirectoryPath.resolve(Constants.ARTIFACT_DIRECTORY), createIfNotExist);
    if (autoAssignerDirectoryPath == null) {
      return null;
    }

    Path resultsPath = autoAssignerDirectoryPath.resolve(Constants.ARTIFACT_FILENAME);
    return createFileIfNotExist(resultsPath, createIfNotExist);
  }

  /**
   * Retrieves the path to the TeamCity artifact directory for the given build.
   * If the directory does not exist, logs a debug message and returns null.
   *
   * @param build The build for which the TeamCity directory is to be retrieved.
   * @return The path to the TeamCity directory, or null if it doesn't exist.
   */
  @Nullable
  private Path getTeamcityDirectory(@NotNull final SBuild build) {
    Path artifactDirectoryPath = build.getArtifactsDirectory().toPath();
    Path teamcityDirectoryPath = artifactDirectoryPath.resolve(Constants.TEAMCITY_DIRECTORY);
    if (!Files.exists(teamcityDirectoryPath)) {
      logSkipping(teamcityDirectoryPath, null);
      return null;
    }
    return teamcityDirectoryPath;
  }

  /**
   * Creates a directory if it does not already exist.
   *
   * @param path The path to the directory.
   * @param createIfNotExist If true, creates the directory if it doesn't exist.
   * @return The path to the directory, or null if it was not created and does not exist.
   * @throws IOException If an error occurs while creating the directory.
   */
  @Nullable
  private Path createDirectoryIfNotExist(@NotNull Path path, boolean createIfNotExist) throws IOException {
    if (!Files.exists(path)) {
      if (createIfNotExist) {
        Files.createDirectory(path);
      } else {
        return null;
      }
    }
    return path;
  }

  /**
   * Creates a file if it does not already exist.
   *
   * @param path The path to the file.
   * @param createIfNotExist If true, creates the file if it doesn't exist.
   * @return The path to the file, or null if it was not created and does not exist.
   * @throws IOException If an error occurs while creating the file.
   */
  @Nullable
  private Path createFileIfNotExist(@NotNull Path path, boolean createIfNotExist) throws IOException {
    if (!Files.exists(path)) {
      if (createIfNotExist) {
        Files.createFile(path);
      } else {
        return null;
      }
    }
    return path;
  }

  /**
   * Logs a message indicating that the logic for investigation suggestion was skipped
   * because the TeamCity directory does not exist.
   *
   * @param teamcityDirectoryPath The path to the TeamCity directory.
   * @param testRun The test run associated with the log, or null if not applicable.
   */
  private void logSkipping(@NotNull Path teamcityDirectoryPath, @Nullable STestRun testRun) {
    String testRunDescription = testRun != null ? LogUtil.describe(testRun) : "test runs";
    Constants.LOGGER.debug(
      "Skip investigation suggestion logic for " + testRunDescription + " as " + teamcityDirectoryPath +
      " doesn't exist.");
  }
}
