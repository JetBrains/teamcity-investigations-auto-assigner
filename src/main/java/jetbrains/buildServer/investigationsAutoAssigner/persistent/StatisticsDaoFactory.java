

package jetbrains.buildServer.investigationsAutoAssigner.persistent;

import java.nio.file.Path;
import java.nio.file.Paths;
import jetbrains.buildServer.serverSide.ServerPaths;
import org.jetbrains.annotations.NotNull;

public class StatisticsDaoFactory {
  private final Path myPluginDataDirectory;

  public StatisticsDaoFactory(@NotNull final ServerPaths serverPaths) {
    myPluginDataDirectory = Paths.get(serverPaths.getPluginDataDirectory().getPath());
  }

  public StatisticsDao get() {
    return new StatisticsDao(myPluginDataDirectory);
  }
}