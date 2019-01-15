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

public class StatisticsPersistentInfo {
  public String version;
  String shownButtonsCount;
  String clickedButtonsCount;
  String assignedInvestigationsCount;
  String wrongInvestigationsCount;

  public StatisticsPersistentInfo(Statistics statistics) {
    this.version = statistics.version;
    this.shownButtonsCount = String.valueOf(statistics.shownButtonsCount);
    this.clickedButtonsCount = String.valueOf(statistics.clickedButtonsCount);
    this.assignedInvestigationsCount = String.valueOf(statistics.assignedInvestigationsCount);
    this.wrongInvestigationsCount = String.valueOf(statistics.wrongInvestigationsCount);
  }

  public StatisticsPersistentInfo(final String version,
                                  final String shownButtonsCount,
                                  final String clickedButtonsCount,
                                  final String assignedInvestigationsCount,
                                  final String wrongInvestigationsCount) {
    this.version = version;
    this.shownButtonsCount = shownButtonsCount;
    this.clickedButtonsCount = clickedButtonsCount;
    this.assignedInvestigationsCount = assignedInvestigationsCount;
    this.wrongInvestigationsCount = wrongInvestigationsCount;
  }

  public Statistics getStatistics() {
    return new Statistics(version, Long.parseLong(shownButtonsCount), Long.parseLong(clickedButtonsCount),
                          Long.parseLong(assignedInvestigationsCount), Long.parseLong(wrongInvestigationsCount));
  }

}
