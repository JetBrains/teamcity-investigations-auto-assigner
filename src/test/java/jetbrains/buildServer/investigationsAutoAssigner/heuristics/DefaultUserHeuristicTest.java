

package jetbrains.buildServer.investigationsAutoAssigner.heuristics;

import java.util.Collections;
import java.util.HashMap;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.processing.HeuristicContext;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.users.UserModelEx;
import jetbrains.buildServer.users.impl.UserEx;
import jetbrains.buildServer.util.TestFor;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static jetbrains.buildServer.messages.ErrorData.SNAPSHOT_DEPENDENCY_ERROR_TYPE;
import static org.mockito.Mockito.when;

@Test
public class DefaultUserHeuristicTest extends BaseTestCase {
  private DefaultUserHeuristic myHeuristic;
  private SBuildFeatureDescriptor myDescriptor;
  private UserModelEx myUserModelEx;
  private SBuild mySBuild;
  private SProject mySProject;
  private UserEx myUserEx;
  private static final String USER_NAME = "rugpanov";
  private HashMap<String, String> myBuildFeatureParams;
  private STestRun mySTestRun;
  private HeuristicContext myHeuristicContext;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myUserModelEx = Mockito.mock(UserModelEx.class);
    myHeuristic = new DefaultUserHeuristic(myUserModelEx);
    myDescriptor = Mockito.mock(SBuildFeatureDescriptor.class);
    mySBuild = Mockito.mock(SBuild.class);
    mySProject = Mockito.mock(SProject.class);
    myUserEx = Mockito.mock(UserEx.class);
    mySTestRun = Mockito.mock(STestRun.class);
    myHeuristicContext =
      new HeuristicContext(mySBuild,
                           mySProject,
                           Collections.emptyList(),
                           Collections.singletonList(mySTestRun),
                           Collections.emptySet());
    myBuildFeatureParams = new HashMap<>();
    when(mySBuild.isCompositeBuild()).thenReturn(false);
    when(mySBuild.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE)).thenReturn(Collections.singletonList(myDescriptor));
    when(mySBuild.getBuildType()).thenReturn(null);
    when(myDescriptor.getParameters()).thenReturn(myBuildFeatureParams);
  }

  public void TestFeatureIsDisabled() {
    when(mySBuild.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE)).thenReturn(Collections.emptyList());

    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);

    Assert.assertTrue(heuristicResult.isEmpty());
  }

  public void TestNoResponsibleSpecified() {
    when(mySBuild.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE)).thenReturn(Collections.emptyList());
    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);
    Assert.assertTrue(heuristicResult.isEmpty());

    myBuildFeatureParams.put(Constants.DEFAULT_RESPONSIBLE, "");
    heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);
    Assert.assertTrue(heuristicResult.isEmpty());
  }

  public void TestResponsibleNotFound() {
    myBuildFeatureParams.put(Constants.DEFAULT_RESPONSIBLE, USER_NAME);
    when(myUserModelEx.findUserAccount(null, USER_NAME)).thenReturn(null);

    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);

    Assert.assertTrue(heuristicResult.isEmpty());
  }

  public void TestResponsibleFound() {
    myBuildFeatureParams.put(Constants.DEFAULT_RESPONSIBLE, USER_NAME);
    when(myUserModelEx.findUserAccount(null, USER_NAME)).thenReturn(myUserEx);

    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);

    Assert.assertFalse(heuristicResult.isEmpty());
    Assert.assertNotNull(heuristicResult.getResponsibility(mySTestRun));

    Responsibility responsibility = heuristicResult.getResponsibility(mySTestRun);
    assert responsibility != null;
    Assert.assertEquals(responsibility.getUser(), myUserEx);
  }

  @Test(dataProvider = "true,false")
  @TestFor(issues = "TW-84375")
  public void TestSnapshotDependencyError(boolean ignoreSnapshotDependencyErrors) {
    setInternalProperty(Constants.IGNORE_SNAPSHOT_DEPENDENCY_ERRORS_IN_DEFAULT_HEURISTIC, ignoreSnapshotDependencyErrors);

    myBuildFeatureParams.put(Constants.DEFAULT_RESPONSIBLE, USER_NAME);
    when(myUserModelEx.findUserAccount(null, USER_NAME)).thenReturn(myUserEx);

    BuildProblemData buildProblemData = Mockito.mock(BuildProblemData.class);
    BuildProblem buildProblem = Mockito.mock(BuildProblem.class);

    when(buildProblemData.getType()).thenReturn(SNAPSHOT_DEPENDENCY_ERROR_TYPE);
    when(buildProblem.getBuildProblemData()).thenReturn(buildProblemData);
    HeuristicContext heuristicContext =
      new HeuristicContext(mySBuild,
                           mySProject,
                           Collections.singletonList(buildProblem),
                           Collections.emptyList(),
                           Collections.emptySet());
    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(heuristicContext);

    Assert.assertEquals(ignoreSnapshotDependencyErrors, heuristicResult.isEmpty());
  }

  @TestFor(issues = "TW-84375")
  public void TestSnapshotDependencyForCompositeBuild() {
    SBuild build = Mockito.mock(SBuild.class);
    when(build.isCompositeBuild()).thenReturn(true);
    when(build.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE)).thenReturn(Collections.singletonList(myDescriptor));
    when(build.getBuildType()).thenReturn(null);

    myBuildFeatureParams.put(Constants.DEFAULT_RESPONSIBLE, USER_NAME);
    when(myUserModelEx.findUserAccount(null, USER_NAME)).thenReturn(myUserEx);

    BuildProblemData buildProblemData = Mockito.mock(BuildProblemData.class);
    BuildProblem buildProblem = Mockito.mock(BuildProblem.class);

    when(buildProblemData.getType()).thenReturn(SNAPSHOT_DEPENDENCY_ERROR_TYPE);
    when(buildProblem.getBuildProblemData()).thenReturn(buildProblemData);
    HeuristicContext heuristicContext =
      new HeuristicContext(build,
                           mySProject,
                           Collections.singletonList(buildProblem),
                           Collections.emptyList(),
                           Collections.emptySet());
    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(heuristicContext);

    Assert.assertFalse(heuristicResult.isEmpty());
  }
}