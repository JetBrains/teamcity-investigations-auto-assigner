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

import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;

class Statistics implements Cloneable {

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
    version = Constants.STATISTICS_FILE_VERSION;
    values = new HashMap<>();
  }

  private Statistics(String version, Map<StatisticsValuesEnum, Integer> values) {
    this.version = version;
    this.values = new HashMap<>(values);
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

  @Override
  protected Statistics clone() {
    return new Statistics(version, values);
  }
}
