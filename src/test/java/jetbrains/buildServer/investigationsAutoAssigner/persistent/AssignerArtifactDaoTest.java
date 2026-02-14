

package jetbrains.buildServer.investigationsAutoAssigner.persistent;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.STest;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.ServerSettings;
import jetbrains.buildServer.users.UserModelEx;
import jetbrains.buildServer.users.impl.UserEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AssignerArtifactDaoTest {

  private SBuild mySBuild;
  private HeuristicResult myHeuristicResult;
  private STestRun mySTestRun;
  private STestRun mySTestRun2;
  private UserEx myUser;
  private STest mySTest;
  private Path myPath;
  private MySuggestedDaoChecker mySuggestedDaoChecker;
  private AssignerArtifactDao myAssignerArtifactDaoForTest;

  @BeforeMethod
  public void setUp() throws IOException {
    final UserModelEx userModelEx = Mockito.mock(UserModelEx.class);
    myUser = Mockito.mock(UserEx.class);
    mySBuild = Mockito.mock(SBuild.class);
    myHeuristicResult = new HeuristicResult();
    mySTestRun = Mockito.mock(STestRun.class);
    mySTest = Mockito.mock(STest.class);
    final STest STest2 = Mockito.mock(STest.class);
    mySTestRun2 = Mockito.mock(STestRun.class);
    myPath = Mockito.mock(Path.class);
    final AssignerResultsFilePath assignerResultsFilePath = Mockito.mock(AssignerResultsFilePath.class);

    Mockito.when(mySTestRun.getTest()).thenReturn(mySTest);
    Mockito.when(mySTestRun.getTestRunId()).thenReturn(1);
    Mockito.when(mySTestRun2.getTest()).thenReturn(STest2);
    Mockito.when(mySTestRun.getTestRunId()).thenReturn(2);
    Mockito.when(mySTest.getTestNameId()).thenReturn(111L);
    Mockito.when(STest2.getTestNameId()).thenReturn(112L);
    Mockito.when(myUser.getId()).thenReturn(239L);
    Mockito.when(userModelEx.findUserById(myUser.getId())).thenReturn(myUser);
    Mockito.when(assignerResultsFilePath.get(mySBuild)).thenReturn(myPath);
    mySuggestedDaoChecker = new MySuggestedDaoChecker();
    myAssignerArtifactDaoForTest = new AssignerArtifactDao(userModelEx,
                                                           mySuggestedDaoChecker,
                                                           assignerResultsFilePath,
                                                           Mockito.mock(StatisticsReporter.class));
  }

  @Test
  public void testAppendHeuristicsResultNoOldNoNew() {
    mySuggestedDaoChecker.mockReadResult(Collections.emptyList());

    myAssignerArtifactDaoForTest
      .appendHeuristicsResult(mySBuild, Arrays.asList(mySTestRun, mySTestRun2), myHeuristicResult);

    Assert.assertFalse(mySuggestedDaoChecker.wasCalled);
  }

  @Test
  public void testAppendHeuristicsResultNoOldOneNew() {
    String description = "any description";
    myHeuristicResult.addResponsibility(mySTestRun, new Responsibility(myUser, description));
    mySuggestedDaoChecker.mockReadResult(Collections.emptyList());

    myAssignerArtifactDaoForTest
      .appendHeuristicsResult(mySBuild, Arrays.asList(mySTestRun, mySTestRun2), myHeuristicResult);

    Assert.assertEquals(mySuggestedDaoChecker.setResultsFilePath, myPath);
    Assert.assertEquals(mySuggestedDaoChecker.setInfoToAdd.get(0).investigatorId, String.valueOf(myUser.getId()));
    Assert.assertEquals(mySuggestedDaoChecker.setInfoToAdd.get(0).testNameId, String.valueOf(mySTest.getTestNameId()));
    Assert.assertEquals(mySuggestedDaoChecker.setInfoToAdd.get(0).reason, description);
  }

  @Test
  public void testAppendHeuristicsResultOneOldNoNew() {
    String testNameId = "239";
    String investigatorId = "932";
    String reason = "any reason";
    ResponsibilityPersistentInfo info = new ResponsibilityPersistentInfo(testNameId, investigatorId, reason);
    mySuggestedDaoChecker.mockReadResult(Collections.singletonList(info));

    myAssignerArtifactDaoForTest
      .appendHeuristicsResult(mySBuild, Arrays.asList(mySTestRun, mySTestRun2), myHeuristicResult);

    Assert.assertFalse(mySuggestedDaoChecker.wasCalled);
  }

  @Test
  public void testAppendHeuristicsResultOneOldOneNew() {
    String testNameId = "239";
    String investigatorId = "932";
    String reason = "any reason";
    ResponsibilityPersistentInfo info = new ResponsibilityPersistentInfo(testNameId, investigatorId, reason);
    mySuggestedDaoChecker.mockReadResult(Collections.singletonList(info));

    String description = "any description";
    myHeuristicResult.addResponsibility(mySTestRun, new Responsibility(myUser, description));

    myAssignerArtifactDaoForTest
      .appendHeuristicsResult(mySBuild, Arrays.asList(mySTestRun, mySTestRun2), myHeuristicResult);

    Assert.assertEquals(mySuggestedDaoChecker.setResultsFilePath, myPath);
    Assert.assertEquals(mySuggestedDaoChecker.setInfoToAdd.size(), 2);
    Assert.assertEquals(mySuggestedDaoChecker.setInfoToAdd.get(0).investigatorId, String.valueOf(myUser.getId()));
    Assert.assertEquals(mySuggestedDaoChecker.setInfoToAdd.get(0).testNameId, String.valueOf(mySTest.getTestNameId()));
    Assert.assertEquals(mySuggestedDaoChecker.setInfoToAdd.get(0).reason, description);

    Assert.assertEquals(mySuggestedDaoChecker.setInfoToAdd.get(1).investigatorId, investigatorId);
    Assert.assertEquals(mySuggestedDaoChecker.setInfoToAdd.get(1).testNameId, testNameId);
    Assert.assertEquals(mySuggestedDaoChecker.setInfoToAdd.get(1).reason, reason);
  }

  private static class MySuggestedDaoChecker extends SuggestionsDao {

    Path setResultsFilePath;
    List<ResponsibilityPersistentInfo> setInfoToAdd;
    boolean wasCalled = false;
    private List<ResponsibilityPersistentInfo> myReadResult;

    MySuggestedDaoChecker() {
      super(Mockito.mock(ServerSettings.class));
    }


    @Override
    public void write(final Path resultsFilePath, final List<ResponsibilityPersistentInfo> infoToAdd) {
      wasCalled = true;
      setResultsFilePath = resultsFilePath;
      setInfoToAdd = infoToAdd;
    }

    @NotNull
    @Override
    public List<ResponsibilityPersistentInfo> read(@Nullable final Path resultsFilePath) {
      return myReadResult == null ? Collections.emptyList() : myReadResult;
    }

    void mockReadResult(List<ResponsibilityPersistentInfo> readResult) {
      myReadResult = readResult;
    }
  }
}