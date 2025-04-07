

package jetbrains.buildServer.investigationsAutoAssigner.persistent;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class StatisticsDao {

  private final Path statisticsPath;
  private final Path pluginDataDirectory;
  private final Gson gson;
  private Statistics statisticsOnDisk;

  public StatisticsDao(@NotNull final Path pluginDataDir) {
    this.gson = new Gson();
    this.pluginDataDirectory = pluginDataDir.resolve(Constants.PLUGIN_DATA_DIR);
    this.statisticsPath = pluginDataDirectory.resolve(Constants.STATISTICS_FILE_NAME);
    this.statisticsOnDisk = new Statistics();
  }

  @NotNull
  public Statistics read() {
    if (!Files.exists(statisticsPath)) {
      statisticsOnDisk = new Statistics();
      return statisticsOnDisk.clone();
    }

    try (BufferedReader reader = Files.newBufferedReader(statisticsPath)) {
      statisticsOnDisk = parseStatistics(reader);
    } catch (IOException e) {
      throw new RuntimeException("Error reading statistics from disk", e);
    }

    return statisticsOnDisk.clone();
  }

  private Statistics parseStatistics(@NotNull BufferedReader reader) {
    try {
      Statistics stats = gson.fromJson(reader, Statistics.class);
      return isValid(stats) ? stats : new Statistics();
    } catch (JsonParseException e) {
      return new Statistics();
    }
  }

  private boolean isValid(@Nullable Statistics stats) {
    return stats != null && Constants.STATISTICS_FILE_VERSION.equals(stats.getVersion());
  }

  public void write(@NotNull Statistics newStats) {
    if (statisticsOnDisk.equals(newStats)) return;

    try {
      ensurePluginDirExists();
      try (BufferedWriter writer = Files.newBufferedWriter(statisticsPath)) {
        gson.toJson(newStats, writer);
      }
      statisticsOnDisk = newStats;
    } catch (IOException e) {
      throw new RuntimeException("Error writing statistics to disk", e);
    }
  }

  private void ensurePluginDirExists() throws IOException {
    if (!Files.exists(pluginDataDirectory)) {
      Files.createDirectories(pluginDataDirectory);
    }
  }
}
