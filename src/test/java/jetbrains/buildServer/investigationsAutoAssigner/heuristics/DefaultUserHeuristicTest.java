

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

    myBuildFeatureParams = new HashMap<>();
    when(myDescriptor.getParameters()).thenReturn(myBuildFeatureParams);
    when(mySBuild.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE)).thenReturn(
      Collections.singletonList(myDescriptor));
    when(mySBuild.getBuildType()).thenReturn(null);
    when(mySBuild.isCompositeBuild()).thenReturn(false);

    myHeuristicContext = new HeuristicContext(
      mySBuild, mySProject,
      Collections.emptyList(), Collections.singletonList(mySTestRun),
      Collections.emptySet()
    );
  }

  public void testFeatureIsDisabled() {
    when(mySBuild.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE)).thenReturn(Collections.emptyList());

    HeuristicResult result = myHeuristic.findResponsibleUser(myHeuristicContext);
    Assert.assertTrue(result.isEmpty(), "Expected no responsibility when feature is disabled");
  }

  public void testNoResponsibleSpecified() {
    myBuildFeatureParams.put(Constants.DEFAULT_RESPONSIBLE, "");

    HeuristicResult result = myHeuristic.findResponsibleUser(myHeuristicContext);
    Assert.assertTrue(result.isEmpty(), "Expected no responsibility when default user is blank");
  }

  public void testResponsibleNotFound() {
    myBuildFeatureParams.put(Constants.DEFAULT_RESPONSIBLE, USER_NAME);
    when(myUserModelEx.findUserAccount(null, USER_NAME)).thenReturn(null);

    HeuristicResult result = myHeuristic.findResponsibleUser(myHeuristicContext);
    Assert.assertTrue(result.isEmpty(), "Expected no responsibility when user is not found");
  }

  public void testResponsibleFound() {
    myBuildFeatureParams.put(Constants.DEFAULT_RESPONSIBLE, USER_NAME);
    when(myUserModelEx.findUserAccount(null, USER_NAME)).thenReturn(myUserEx);

    HeuristicResult result = myHeuristic.findResponsibleUser(myHeuristicContext);
    Responsibility responsibility = result.getResponsibility(mySTestRun);

    Assert.assertFalse(result.isEmpty(), "Expected non-empty result when user is found");
    Assert.assertNotNull(responsibility, "Expected responsibility to be assigned");
    Assert.assertEquals(responsibility.getUser(), myUserEx, "Expected correct user assigned");
  }

  public void testSnapshotDependencyErrorFilteredWhenNonComposite() {
    myBuildFeatureParams.put(Constants.DEFAULT_RESPONSIBLE, USER_NAME);
    when(myUserModelEx.findUserAccount(null, USER_NAME)).thenReturn(myUserEx);

    BuildProblemData data = Mockito.mock(BuildProblemData.class);
    BuildProblem problem = Mockito.mock(BuildProblem.class);
    when(data.getType()).thenReturn(Constants.SNAPSHOT_DEPENDENCY_ERROR_TYPE);
    when(problem.getBuildProblemData()).thenReturn(data);

    HeuristicContext context = new HeuristicContext(
      mySBuild, mySProject, Collections.singletonList(problem),
      Collections.emptyList(), Collections.emptySet()
    );

    HeuristicResult result = myHeuristic.findResponsibleUser(context);
    Assert.assertTrue(result.isEmpty(), "Snapshot dependency errors should be filtered for non-composite builds");
  }

  public void testSnapshotDependencyAllowedForCompositeBuild() {
    SBuild compositeBuild = Mockito.mock(SBuild.class);
    when(compositeBuild.isCompositeBuild()).thenReturn(true);
    when(compositeBuild.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE)).thenReturn(
      Collections.singletonList(myDescriptor));
    when(compositeBuild.getBuildType()).thenReturn(null);

    myBuildFeatureParams.put(Constants.DEFAULT_RESPONSIBLE, USER_NAME);
    when(myUserModelEx.findUserAccount(null, USER_NAME)).thenReturn(myUserEx);

    BuildProblemData data = Mockito.mock(BuildProblemData.class);
    BuildProblem problem = Mockito.mock(BuildProblem.class);
    when(data.getType()).thenReturn(Constants.SNAPSHOT_DEPENDENCY_ERROR_TYPE);
    when(problem.getBuildProblemData()).thenReturn(data);

    HeuristicContext context = new HeuristicContext(
      compositeBuild, mySProject, Collections.singletonList(problem),
      Collections.emptyList(), Collections.emptySet()
    );

    HeuristicResult result = myHeuristic.findResponsibleUser(context);
    Assert.assertFalse(result.isEmpty(), "Expected responsibility assigned for composite build with snapshot error");
  }
}
