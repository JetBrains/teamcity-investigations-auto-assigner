package jetbrains.buildServer.investigationsAutoAssigner.persistent;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Data access object (DAO) responsible for reading and writing statistics data to a file.
 * It manages the statistics persistence by serializing and deserializing the {@link Statistics} object
 * using Gson. The DAO ensures that statistics data is stored and retrieved correctly while validating the data.
 */
class StatisticsDao {

  private final Path statisticsPath;
  private final Path pluginDataDirectory;
  private final Gson gson;
  private Statistics statisticsOnDisc;

  /**
   * Constructs a new {@link StatisticsDao} instance with the specified plugin data directory.
   * It initializes the Gson parser and resolves the paths for the statistics file.
   *
   * @param pluginDataDir The directory where the plugin data is stored.
   */
  StatisticsDao(@NotNull final Path pluginDataDir) {
    this.gson = new Gson();
    this.pluginDataDirectory = pluginDataDir.resolve(Constants.PLUGIN_DATA_DIR);
    this.statisticsPath = this.pluginDataDirectory.resolve(Constants.STATISTICS_FILE_NAME);
    this.statisticsOnDisc = new Statistics();
  }

  /**
   * Reads the statistics data from the file. If the file does not exist or is invalid,
   * a new {@link Statistics} object is returned.
   *
   * @return A clone of the {@link Statistics} object read from the file or a new instance if reading fails.
   */
  @NotNull
  Statistics read() {
    if (!Files.exists(this.statisticsPath)) {
      this.statisticsOnDisc = new Statistics();
      return this.statisticsOnDisc.clone();
    }

    try (BufferedReader reader = Files.newBufferedReader(this.statisticsPath)) {
      this.statisticsOnDisc = parseStatistics(reader);
      return this.statisticsOnDisc.clone();
    } catch (IOException ex) {
      throw new RuntimeException("An error occurred during reading statistics", ex);
    }
  }

  /**
   * Parses the statistics data from the given BufferedReader.
   * If parsing fails or the data is invalid, a new {@link Statistics} object is returned.
   *
   * @param reader The BufferedReader to read statistics data from.
   * @return The parsed {@link Statistics} object or a new instance if parsing fails.
   */
  @NotNull
  private Statistics parseStatistics(final BufferedReader reader) {
    Statistics statistics;

    try {
      statistics = this.gson.fromJson(reader, Statistics.class);

      if (!isValidStatisticsFile(statistics)) {
        statistics = new Statistics();
      }
    } catch (JsonParseException err) {
      statistics = new Statistics();
    }

    return statistics;
  }

  /**
   * Validates the statistics file based on the version of the statistics data.
   *
   * @param statistics The {@link Statistics} object to validate.
   * @return {@code true} if the statistics file is valid, {@code false} otherwise.
   */
  private boolean isValidStatisticsFile(@Nullable Statistics statistics) {
    return statistics != null && Constants.STATISTICS_FILE_VERSION.equals(statistics.getVersion());
  }

  /**
   * Writes the given {@link Statistics} object to the statistics file.
   * If the current statistics are equal to the provided statistics, the write operation is skipped.
   *
   * @param statistics The {@link Statistics} object to write to the file.
   * @throws RuntimeException if an error occurs during the write operation.
   */
  void write(@NotNull Statistics statistics) {
    if (this.statisticsOnDisc.equals(statistics)) {
      return;
    }

    try {
      if (!Files.exists(this.pluginDataDirectory)) {
        Files.createDirectory(this.pluginDataDirectory);
      }

      try (BufferedWriter writer = Files.newBufferedWriter(this.statisticsPath)) {
        this.gson.toJson(statistics, writer);
      }

      this.statisticsOnDisc = statistics;
    } catch (IOException ex) {
      throw new RuntimeException("An error occurred during writing statistics", ex);
    }
  }
}
