package jetbrains.buildServer.investigationsAutoAssigner.persistent;

import java.util.concurrent.TimeUnit;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.common.DefaultUserResponsibility;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.utils.CustomParameters;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.serverSide.executors.ExecutorServices;
import jetbrains.buildServer.util.StringUtil;

/**
 * Manages the reporting and saving of statistics related to investigations and suggestions in the plugin.
 * It keeps track of various statistics such as assigned investigations, wrong investigations, shown suggestions, 
 * and processed builds with changes. The statistics are periodically saved to disk.
 * <p>
 * This class is responsible for incrementing and updating statistics in response to various events (e.g., button clicks, 
 * investigations assigned, etc.), and generates a formatted report summarizing the plugin usage.
 * </p>
 */
public class StatisticsReporter {

  private final StatisticsDao statisticsDao;
  private Statistics statistics;

  /**
   * Constructs a {@link StatisticsReporter} instance.
   * Initializes the statistics and schedules periodic saving of statistics to disk.
   *
   * @param statisticsDaoFactory Factory used to obtain the {@link StatisticsDao} for reading and writing statistics.
   * @param executorServices The executor services used to schedule periodic tasks.
   */
  public StatisticsReporter(StatisticsDaoFactory statisticsDaoFactory,
                            ExecutorServices executorServices) {
    this.statisticsDao = statisticsDaoFactory.get();
    this.statistics = statisticsDao.read();
    StatisticsReporter instance = this;
    int delayInSeconds = CustomParameters.getProcessingDelayInSeconds();
    executorServices
      .getNormalExecutorService()
      .scheduleWithFixedDelay(instance::saveDataOnDisk, delayInSeconds, delayInSeconds, TimeUnit.SECONDS);
  }

  /**
   * Increments the count of shown buttons in the statistics.
   */
  public synchronized void reportShownButton() {
    this.statistics.increment(StatisticsValuesEnum.shownButtonsCount);
  }

  /**
   * Increments the count of clicked buttons in the statistics.
   */
  public synchronized void reportClickedButton() {
    this.statistics.increment(StatisticsValuesEnum.clickedButtonsCount);
  }

  /**
   * Increments the count of assigned investigations in the statistics.
   * If the responsibility is a {@link DefaultUserResponsibility}, it also increments the default investigations count.
   *
   * @param count The number of investigations assigned.
   * @param responsibility The responsibility associated with the investigations.
   */
  public synchronized void reportAssignedInvestigations(int count, Responsibility responsibility) {
    if (responsibility instanceof DefaultUserResponsibility) {
      this.statistics.increase(StatisticsValuesEnum.defaultInvestigationsCount, count);
    }
    this.statistics.increase(StatisticsValuesEnum.assignedInvestigationsCount, count);
  }

  /**
   * Increments the count of wrong investigations in the statistics.
   *
   * @param count The number of wrong investigations.
   */
  public synchronized void reportWrongInvestigation(int count) {
    this.statistics.increase(StatisticsValuesEnum.wrongInvestigationsCount, count);
  }

  /**
   * Increments the count of saved suggestions in the statistics.
   *
   * @param count The number of suggestions saved.
   */
  synchronized void reportSavedSuggestions(final int count) {
    this.statistics.increase(StatisticsValuesEnum.savedSuggestionsCount, count);
  }

  /**
   * Increments the count of builds with at least one suggestion in the statistics.
   */
  synchronized void reportBuildWithSuggestions() {
    this.statistics.increment(StatisticsValuesEnum.buildWithSuggestionsCount);
  }

  /**
   * Increments the count of processed builds and the number of changes in those builds.
   *
   * @param numberOfChanges The number of changes in the processed builds.
   */
  public synchronized void reportProcessedBuildWithChanges(final int numberOfChanges) {
    this.statistics.increment(StatisticsValuesEnum.processedBuildsCount);
    this.statistics.increase(StatisticsValuesEnum.changesInBuildsCount, numberOfChanges);
  }

  /**
   * Saves the current statistics to disk if the statistics feature is enabled.
   * This method is scheduled to run periodically.
   */
  private void saveDataOnDisk() {
    if (StringUtil.isTrue(TeamCityProperties.getProperty(Constants.STATISTICS_ENABLED, "false"))) {
      this.statisticsDao.write(this.statistics);
    }
  }

  /**
   * Generates a formatted report summarizing the plugin usage statistics.
   *
   * @return A string representing a summary of the statistics.
   */
  public synchronized String generateReport() {
    return String.format("Short statistics of plugin usage:\n\n" +
                         "%s investigations assigned;\n" +
                         "%s of them were wrong;\n" +
                         "%s of them for default user;\n" +
                         "%s shown suggestions;\n" +
                         "%s of assignments from them;\n" +
                         "%s builds have at least one suggestion;\n" +
                         "%s suggestions total;\n" +
                         "with %s changes\n" +
                         "in %s builds.\n",
                         this.statistics.get(StatisticsValuesEnum.assignedInvestigationsCount),
                         this.statistics.get(StatisticsValuesEnum.wrongInvestigationsCount),
                         this.statistics.get(StatisticsValuesEnum.defaultInvestigationsCount),
                         this.statistics.get(StatisticsValuesEnum.shownButtonsCount),
                         this.statistics.get(StatisticsValuesEnum.clickedButtonsCount),
                         this.statistics.get(StatisticsValuesEnum.buildWithSuggestionsCount),
                         this.statistics.get(StatisticsValuesEnum.savedSuggestionsCount),
                         this.statistics.get(StatisticsValuesEnum.changesInBuildsCount),
                         this.statistics.get(StatisticsValuesEnum.processedBuildsCount));
  }
}
