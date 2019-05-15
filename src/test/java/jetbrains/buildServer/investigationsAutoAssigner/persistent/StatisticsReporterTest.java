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

import jetbrains.buildServer.investigationsAutoAssigner.common.DefaultUserResponsibility;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.serverSide.executors.ExecutorServices;
import jetbrains.buildServer.serverSide.impl.executors.CommonExecutorService;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StatisticsReporterTest {

  private StatisticsDao myStatisticsDao;
  private StatisticsReporter myStatisticsReporter;
  private ExecutorServices myExecutorServices;
  private Statistics myStatisticsChecker;

  @BeforeMethod
  public void setUp() {
    final CommonExecutorService scheduledExecutorService = Mockito.mock(CommonExecutorService.class);
    myExecutorServices = Mockito.mock(ExecutorServices.class);
    when(myExecutorServices.getNormalExecutorService()).thenReturn(scheduledExecutorService);

    myStatisticsChecker = new Statistics();
    myStatisticsDao = Mockito.mock(StatisticsDao.class);
    StatisticsDaoFactory statisticsDaoFactory = Mockito.mock(StatisticsDaoFactory.class);
    when(statisticsDaoFactory.get()).thenReturn(myStatisticsDao);
    when(myStatisticsDao.read()).thenReturn(myStatisticsChecker);
    myStatisticsReporter = new StatisticsReporter(statisticsDaoFactory, myExecutorServices);
  }

  @Test
  public void testConstructor() {
    verify(myStatisticsDao, Mockito.atLeastOnce()).read();
    verify(myExecutorServices, Mockito.atLeastOnce()).getNormalExecutorService();
  }

  @Test
  public void testReports() {
    myStatisticsReporter.reportShownButton();
    myStatisticsReporter.reportClickedButton();
    myStatisticsReporter.reportClickedButton();
    myStatisticsReporter.reportAssignedInvestigations(3, Mockito.mock(Responsibility.class));
    myStatisticsReporter.reportWrongInvestigation(4);
    Assert.assertEquals(myStatisticsChecker.get(StatisticsValuesEnum.shownButtonsCount), 1);
    Assert.assertEquals(myStatisticsChecker.get(StatisticsValuesEnum.clickedButtonsCount), 2);
    Assert.assertEquals(myStatisticsChecker.get(StatisticsValuesEnum.assignedInvestigationsCount), 3);
    Assert.assertEquals(myStatisticsChecker.get(StatisticsValuesEnum.wrongInvestigationsCount), 4);
  }

  @Test
  public void testGenerateReport() {
    myStatisticsReporter.reportShownButton();
    myStatisticsReporter.reportClickedButton();
    myStatisticsReporter.reportClickedButton();
    myStatisticsReporter.reportAssignedInvestigations(3, Mockito.mock(Responsibility.class));
    myStatisticsReporter.reportWrongInvestigation(4);
    Assert.assertTrue(myStatisticsReporter.generateReport().contains("1"));
    Assert.assertTrue(myStatisticsReporter.generateReport().contains("2"));
    Assert.assertTrue(myStatisticsReporter.generateReport().contains("3"));
    Assert.assertTrue(myStatisticsReporter.generateReport().contains("4"));
  }


  @Test
  public void testDefaultResponsible() {
    myStatisticsReporter.reportShownButton();
    myStatisticsReporter.reportClickedButton();
    myStatisticsReporter.reportClickedButton();
    myStatisticsReporter.reportAssignedInvestigations(3, Mockito.mock(DefaultUserResponsibility.class));
    myStatisticsReporter.reportAssignedInvestigations(4, Mockito.mock(Responsibility.class));
    myStatisticsReporter.reportWrongInvestigation(5);
    Assert.assertTrue(myStatisticsReporter.generateReport().contains("1 shown suggestion"));
    Assert.assertTrue(myStatisticsReporter.generateReport().contains("2 of assignments from them"));
    Assert.assertTrue(myStatisticsReporter.generateReport().contains("3 of them for default user"));
    Assert.assertTrue(myStatisticsReporter.generateReport().contains("7 investigations assigned"));
    Assert.assertTrue(myStatisticsReporter.generateReport().contains("5 of them were wrong"));
  }
}