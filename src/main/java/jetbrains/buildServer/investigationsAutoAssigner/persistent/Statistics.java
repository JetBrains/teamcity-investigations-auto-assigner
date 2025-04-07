

package jetbrains.buildServer.investigationsAutoAssigner.persistent;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;

class Statistics {
  private final String version;
  private final Map<StatisticsValuesEnum, Integer> values;

  public String getVersion() {
    return version;
  }

  int get(StatisticsValuesEnum statisticsKey) {
    return values.getOrDefault(statisticsKey, 0);
  }

  void increment(StatisticsValuesEnum statisticsKey) {
    increase(statisticsKey, 1);
  }

  void increase(StatisticsValuesEnum statisticsKey, int delta) {
    int previousValue = values.getOrDefault(statisticsKey, 0);
    values.put(statisticsKey, previousValue + delta);
  }

  Statistics() {
    this.version = Constants.STATISTICS_FILE_VERSION;
    this.values = new EnumMap<>(StatisticsValuesEnum.class);
  }

  private Statistics(String version, Map<StatisticsValuesEnum, Integer> values) {
    this.version = version;
    this.values = new EnumMap<>(values);
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof Statistics)) {
      return false;
    }

    Statistics another = (Statistics)obj;
    return version.equals(another.version) &&
           values.equals(another.values);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, values);
  }

  @Override
  public String toString() {
    return String.format("version: %s, shownButtonsCount: %s, clickedButtonsCount: %s, " +
                         "assignedInvestigationsCount: %s, wrongInvestigationsCount: %s, " +
                         "buildWithSuggestionsCount: %s, savedSuggestionsCount: %s",
                         version, get(StatisticsValuesEnum.shownButtonsCount),
                         get(StatisticsValuesEnum.clickedButtonsCount),
                         get(StatisticsValuesEnum.assignedInvestigationsCount),
                         get(StatisticsValuesEnum.wrongInvestigationsCount),
                         get(StatisticsValuesEnum.buildWithSuggestionsCount),
                         get(StatisticsValuesEnum.savedSuggestionsCount));
  }

  public Statistics copy() {
    return new Statistics(version, values);
  }
}