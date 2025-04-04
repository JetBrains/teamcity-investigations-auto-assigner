package jetbrains.buildServer.investigationsAutoAssigner.persistent;

import java.nio.file.Path;
import java.nio.file.Paths;
import jetbrains.buildServer.serverSide.ServerPaths;
import org.jetbrains.annotations.NotNull;

/**
 * Factory class responsible for creating instances of {@link StatisticsDao}.
 * This class is used to encapsulate the creation logic of the {@link StatisticsDao},
 * ensuring it receives the correct directory for plugin data.
 */
public class StatisticsDaoFactory {
  private final Path pluginDataDirectory;

  /**
   * Constructs a new {@link StatisticsDaoFactory} instance with the given server paths.
   * It initializes the plugin data directory where the statistics data will be stored.
   *
   * @param serverPaths The server paths containing the plugin data directory.
   */
  public StatisticsDaoFactory(@NotNull final ServerPaths serverPaths) {
    this.pluginDataDirectory = Paths.get(serverPaths.getPluginDataDirectory().getPath());
  }

  /**
   * Creates and returns a new instance of {@link StatisticsDao} initialized with the plugin data directory.
   *
   * @return A new instance of {@link StatisticsDao}.
   */
  public StatisticsDao get() {
    return new StatisticsDao(this.pluginDataDirectory);
  }
}
