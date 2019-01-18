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

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.serverSide.ServerPaths;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;

public class StatisticsDaoTest {
  //
  //private static final String STATISTICS_GOLD =
  //  String.format("{\"version\":\"%s\",\"shownButtonsCount\":13,\"clickedButtonsCount\":7," +
  //                "\"assignedInvestigationsCount\":3,\"wrongInvestigationsCount\":2}",
  //                Constants.STATISTICS_FILE_VERSION);
  private StatisticsDao myStatisticsDao;
  private Path myPluginsDataDir;

  @BeforeMethod
  public void setUp() throws IOException {
    FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
    myPluginsDataDir = fs.getPath("/some_path");
    Files.createDirectory(myPluginsDataDir);
    myStatisticsDao = new StatisticsDao(myPluginsDataDir);
  }

  @Test
  public void testReadCorrectStatistics() throws IOException {
    Path assignerDataDir = myPluginsDataDir.resolve(Constants.PLUGIN_DATA_DIR);
    Files.createDirectory(assignerDataDir);

    Path myStatisticsPath = assignerDataDir.resolve(Constants.STATISTICS_FILE_NAME);
    Files.write(myStatisticsPath, readGold("StatisticsDaoTest_TestReadCorrectStatistics_Gold.txt").getBytes());

    Statistics statistics = myStatisticsDao.read();

    Assert.assertEquals(statistics.getVersion(), Constants.STATISTICS_FILE_VERSION);
    Assert.assertEquals(statistics.getShownButtonsCount(), 13);
    Assert.assertEquals(statistics.getClickedButtonsCount(), 7);
    Assert.assertEquals(statistics.getAssignedInvestigationsCount(), 3);
    Assert.assertEquals(statistics.getWrongInvestigationsCount(), 2);
  }

  @Test
  public void testReadIncorrect() throws IOException {
    Path assignerDataDir = myPluginsDataDir.resolve(Constants.PLUGIN_DATA_DIR);
    Files.createDirectory(assignerDataDir);

    Path myStatisticsPath = assignerDataDir.resolve(Constants.STATISTICS_FILE_NAME);
    Files.write(myStatisticsPath, readGold("StatisticsDaoTest_TestReadIncorrect_Gold.txt").getBytes());

    Statistics statistics = myStatisticsDao.read();

    Assert.assertEquals(new Statistics(), statistics);
  }

  @Test
  public void testReadOldVersion() throws IOException {
    Path assignerDataDir = myPluginsDataDir.resolve(Constants.PLUGIN_DATA_DIR);
    Files.createDirectory(assignerDataDir);

    Path myStatisticsPath = assignerDataDir.resolve(Constants.STATISTICS_FILE_NAME);
    Files.write(myStatisticsPath, readGold("StatisticsDaoTest_TestReadOldVersion_Gold.txt").getBytes());

    Statistics statistics = myStatisticsDao.read();

    Assert.assertEquals(new Statistics(), statistics);
  }

  @Test
  public void testReadNotExistDirectory() {
    Statistics statistics = myStatisticsDao.read();

    Assert.assertEquals(new Statistics(), statistics);
  }

  @Test
  public void testReadFileNotExist() throws IOException {
    Path assignerDataDir = myPluginsDataDir.resolve(Constants.PLUGIN_DATA_DIR);
    Files.createDirectory(assignerDataDir);

    Statistics statistics = myStatisticsDao.read();

    Assert.assertEquals(new Statistics(), statistics);
  }

  private String readGold(String resourceName) {
    File resource = new File(SuggestionsDao.class.getResource("/gold/" + resourceName).getFile());

    try {
      return new String(Files.readAllBytes(resource.toPath()));
    } catch (IOException e) {
      throw new RuntimeException("Can not read GOLD " + resourceName);
    }
  }

  @Test
  public void testWriteUpdatedStatistics() throws IOException {
    Assert.assertEquals(myStatisticsDao.read(), new Statistics());
    Statistics statistics = new Statistics();

    statistics.increaseShownButtonsCounter();
    statistics.increaseClickedButtonsCounter();
    statistics.increaseClickedButtonsCounter();
    statistics.increaseAssignedInvestigationsCounter(3);
    statistics.increaseWrongInvestigationsCounter(4);
    myStatisticsDao.write(statistics);


    Path assignerDataDir = myPluginsDataDir.resolve(Constants.PLUGIN_DATA_DIR);
    Path myStatisticsPath = assignerDataDir.resolve(Constants.STATISTICS_FILE_NAME);
    String fileContent = new String(Files.readAllBytes(myStatisticsPath));
    Assert.assertEquals(fileContent, readGold("StatisticsDaoTest_TestWriteStatistics_Gold.txt"));
  }

  @Test
  public void testWriteNotUpdatedStatistics() throws IOException {
    Path assignerDataDir = myPluginsDataDir.resolve(Constants.PLUGIN_DATA_DIR);
    Files.createDirectory(assignerDataDir);
    Path myStatisticsPath = assignerDataDir.resolve(Constants.STATISTICS_FILE_NAME);
    Files.write(myStatisticsPath, readGold("StatisticsDaoTest_TestWriteNotUpdatedStatistics_Gold.txt").getBytes());

    Statistics statistics = new Statistics();
    statistics.increaseShownButtonsCounter();
    statistics.increaseClickedButtonsCounter();
    statistics.increaseClickedButtonsCounter();
    statistics.increaseAssignedInvestigationsCounter(3);
    statistics.increaseWrongInvestigationsCounter(4);
    Assert.assertEquals(myStatisticsDao.read(), statistics);
    myStatisticsDao.write(statistics);

    String fileContent = new String(Files.readAllBytes(myStatisticsPath));
    Assert.assertEquals(fileContent, readGold("StatisticsDaoTest_TestWriteNotUpdatedStatistics_Gold.txt"));
  }

  @Test
  public void testWriteNotUpdatedStatisticsForSecondTime() throws IOException {
    Assert.assertEquals(myStatisticsDao.read(), new Statistics());
    Statistics statistics = new Statistics();

    statistics.increaseShownButtonsCounter();
    statistics.increaseClickedButtonsCounter();
    statistics.increaseClickedButtonsCounter();
    statistics.increaseAssignedInvestigationsCounter(3);
    statistics.increaseWrongInvestigationsCounter(4);
    myStatisticsDao.write(statistics);

    Path assignerDataDir = myPluginsDataDir.resolve(Constants.PLUGIN_DATA_DIR);
    Path myStatisticsPath = assignerDataDir.resolve(Constants.STATISTICS_FILE_NAME);

    Files.write(myStatisticsPath, "UPDATED".getBytes());
    myStatisticsDao.write(statistics);

    String fileContent = new String(Files.readAllBytes(myStatisticsPath));
    Assert.assertEquals(fileContent, "UPDATED");
  }
}