package jetbrains.buildServer.investigationsAutoAssigner.persistent;

import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;

/**
 * Represents the statistics of the investigations auto-assigner plugin.
 * This class holds statistical data related to the plugin's usage, including counts for various actions
 * such as button clicks, investigations assigned, and suggestions saved.
 * <p>
 * The class supports cloning and comparing statistics objects. It uses a map to store the values associated with
 * different statistics keys and provides methods for modifying and retrieving those values.
 * </p>
 */
class Statistics implements Cloneable {

  private final String version;
  private final Map<StatisticsValuesEnum, Integer> values;

  /**
   * Constructs a new {@link Statistics} object with a default version and an empty map of statistics values.
   */
  Statistics() {
    this.version = Constants.STATISTICS_FILE_VERSION;
    this.values = new HashMap<>();
  }

  /**
   * Retrieves the value associated with the specified statistics key.
   * If the key is not found, it returns 0.
   *
   * @param statisticsKey The key representing the statistic to retrieve.
   * @return The value of the statistic, or 0 if not found.
   */
  int get(StatisticsValuesEnum statisticsKey) {
    return values.getOrDefault(statisticsKey, 0);
  }

  /**
   * Increments the value of the specified statistic by 1.
   *
   * @param statisticsKey The key representing the statistic to increment.
   */
  void increment(StatisticsValuesEnum statisticsKey) {
    modifyStatistic(statisticsKey, 1);
  }

  /**
   * Increases the value of the specified statistic by the given delta.
   *
   * @param statisticsKey The key representing the statistic to increase.
   * @param delta The amount to increase the statistic by.
   */
  void increase(StatisticsValuesEnum statisticsKey, int delta) {
    modifyStatistic(statisticsKey, delta);
  }

  /**
   * Modifies the value of the specified statistic by the given delta.
   * This is an internal helper method used by the {@link #increment(StatisticsValuesEnum)}
   * and {@link #increase(StatisticsValuesEnum, int)} methods.
   *
   * @param statisticsKey The key representing the statistic to modify.
   * @param delta The amount to modify the statistic by.
   */
  private void modifyStatistic(StatisticsValuesEnum statisticsKey, int delta) {
    values.put(statisticsKey, get(statisticsKey) + delta);
  }

  /**
   * Constructs a new {@link Statistics} object with the given version and values.
   * This is used during the cloning process.
   *
   * @param version The version of the statistics.
   * @param values The map containing the statistics values.
   */
  private Statistics(String version, Map<StatisticsValuesEnum, Integer> values) {
    this.version = version;
    this.values = new HashMap<>(values);
  }

  /**
   * Compares this {@link Statistics} object to another for equality.
   * Two statistics objects are considered equal if they have the same version and identical values.
   *
   * @param obj The object to compare with.
   * @return {@code true} if the objects are equal, {@code false} otherwise.
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    Statistics that = (Statistics) obj;
    return version.equals(that.version) && values.equals(that.values);
  }

  /**
   * Returns a string representation of this {@link Statistics} object.
   * The string includes the version and counts for various statistics.
   *
   * @return A string representation of the statistics.
   */
  @Override
  public String toString() {
    return String.format("version: %s, shownButtonsCount: %d, clickedButtonsCount: %d, " +
                         "assignedInvestigationsCount: %d, wrongInvestigationsCount: %d, " +
                         "buildWithSuggestionsCount: %d, savedSuggestionsCount: %d",
                         version,
                         get(StatisticsValuesEnum.shownButtonsCount),
                         get(StatisticsValuesEnum.clickedButtonsCount),
                         get(StatisticsValuesEnum.assignedInvestigationsCount),
                         get(StatisticsValuesEnum.wrongInvestigationsCount),
                         get(StatisticsValuesEnum.buildWithSuggestionsCount),
                         get(StatisticsValuesEnum.savedSuggestionsCount));
  }

  /**
   * Creates and returns a clone of this {@link Statistics} object.
   *
   * @return A clone of this statistics object.
   */
  @Override
  protected Statistics clone() {
    return new Statistics(version, values);
  }

  /**
   * Retrieves the version of the statistics data.
   *
   * @return The version of the statistics.
   */
  public String getVersion() {
    return version;
  }
}
