

package jetbrains.buildServer.investigationsAutoAssigner.persistent;

import java.util.concurrent.TimeUnit;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.common.DefaultUserResponsibility;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.utils.CustomParameters;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.serverSide.executors.ExecutorServices;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

public class StatisticsReporter {
  private final StatisticsDao myStatisticsDao;
  private final Statistics myStatistics;

  public StatisticsReporter(@NotNull StatisticsDaoFactory statisticsDaoFactory,
                            @NotNull ExecutorServices executorServices) {
    myStatisticsDao = statisticsDaoFactory.get();
    myStatistics = myStatisticsDao.read();
    StatisticsReporter instance = this;
    int delayInSeconds = CustomParameters.getProcessingDelayInSeconds();
    executorServices
      .getNormalExecutorService()
      .scheduleWithFixedDelay(instance::saveDataOnDisk, delayInSeconds, delayInSeconds, TimeUnit.SECONDS);
  }

  public synchronized void reportShownButton() {
    myStatistics.increment(StatisticsValuesEnum.shownButtonsCount);
  }

  public synchronized void reportClickedButton() {
    myStatistics.increment(StatisticsValuesEnum.clickedButtonsCount);
  }

  public synchronized void reportAssignedInvestigations(int count, Responsibility responsibility) {
    if (responsibility instanceof DefaultUserResponsibility) {
      myStatistics.increase(StatisticsValuesEnum.defaultInvestigationsCount, count);
    }
    myStatistics.increase(StatisticsValuesEnum.assignedInvestigationsCount, count);
  }

  public synchronized void reportWrongInvestigation(int count) {
    myStatistics.increase(StatisticsValuesEnum.wrongInvestigationsCount, count);
  }

  synchronized void reportSavedSuggestions(final int count) {
    myStatistics.increase(StatisticsValuesEnum.savedSuggestionsCount, count);
  }

  synchronized void reportBuildWithSuggestions() {
    myStatistics.increment(StatisticsValuesEnum.buildWithSuggestionsCount);
  }

  public synchronized void reportProcessedBuildWithChanges(final int numberOfChanges) {
    myStatistics.increment(StatisticsValuesEnum.processedBuildsCount);
    myStatistics.increase(StatisticsValuesEnum.changesInBuildsCount, numberOfChanges);
  }

  private void saveDataOnDisk() {
    if (StringUtil.isTrue(TeamCityProperties.getProperty(Constants.STATISTICS_ENABLED, "false"))) {
      myStatisticsDao.write(myStatistics);
    }
  }

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
                         myStatistics.get(StatisticsValuesEnum.assignedInvestigationsCount),
                         myStatistics.get(StatisticsValuesEnum.wrongInvestigationsCount),
                         myStatistics.get(StatisticsValuesEnum.defaultInvestigationsCount),
                         myStatistics.get(StatisticsValuesEnum.shownButtonsCount),
                         myStatistics.get(StatisticsValuesEnum.clickedButtonsCount),
                         myStatistics.get(StatisticsValuesEnum.buildWithSuggestionsCount),
                         myStatistics.get(StatisticsValuesEnum.savedSuggestionsCount),
                         myStatistics.get(StatisticsValuesEnum.changesInBuildsCount),
                         myStatistics.get(StatisticsValuesEnum.processedBuildsCount));
  }
}
