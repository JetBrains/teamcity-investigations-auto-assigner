/*
 * Copyright 2000-2019 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.investigationsAutoAssigner.persistent;

import java.util.concurrent.TimeUnit;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.utils.CustomParameters;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.serverSide.executors.ExecutorServices;
import jetbrains.buildServer.util.StringUtil;

public class StatisticsReporter {
  private final StatisticsDao myStatisticsDao;
  private Statistics myStatistics;

  public StatisticsReporter(StatisticsDaoFactory statisticsDaoFactory,
                            ExecutorServices executorServices) {
    myStatisticsDao = statisticsDaoFactory.get();
    myStatistics = myStatisticsDao.read();
    StatisticsReporter instance = this;
    int delayInSeconds = CustomParameters.getProcessingDelayInSeconds();
    executorServices
      .getNormalExecutorService()
      .scheduleWithFixedDelay(instance::saveDataOnDisk, delayInSeconds, delayInSeconds, TimeUnit.SECONDS);
  }

  public synchronized void reportShownButton() {
    myStatistics.increaseShownButtonsCounter();
  }

  public synchronized void reportClickedButton() {
    myStatistics.increaseClickedButtonsCounter();
  }

  public synchronized void reportAssignedInvestigations(int count) {
    myStatistics.increaseAssignedInvestigationsCounter(count);
  }

  public synchronized void reportWrongInvestigation(int count) {
    myStatistics.increaseWrongInvestigationsCounter(count);
  }

  private void saveDataOnDisk() {
    if (StringUtil.isTrue(TeamCityProperties.getProperty(Constants.STATISTICS_ENABLED, "false"))) {
      myStatisticsDao.write(myStatistics);
    }
  }

  public synchronized String generateReport() {
    return String.format("Short statistics of plugin usage:" +
                         "%s investigations assigned;\n" +
                         "%s of them were wrong;\n" +
                         "%s shown suggestions;\n" +
                         "%s of assignments from them.\n",
                         myStatistics.getAssignedInvestigationsCount(),
                         myStatistics.getWrongInvestigationsCount(),
                         myStatistics.getShownButtonsCount(),
                         myStatistics.getClickedButtonsCount());
  }
}
