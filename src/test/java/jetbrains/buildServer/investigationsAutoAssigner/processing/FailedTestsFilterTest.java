

package jetbrains.buildServer.investigationsAutoAssigner.processing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.common.FailedBuildInfo;
import jetbrains.buildServer.investigationsAutoAssigner.utils.FlakyTestDetector;
import jetbrains.buildServer.investigationsAutoAssigner.utils.InvestigationsManager;
import jetbrains.buildServer.parameters.ParametersProvider;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.responsibility.TestNameResponsibilityEntry;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STest;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.tests.TestName;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@Test
public class FailedTestsFilterTest extends BaseTestCase {

  private FailedTestFilter myFailedTestFilter;
  private SProject mySProject;
  private SBuild mySBuild;
  private STestRun mySTestRun;
  private TestNameResponsibilityEntry myTestNameResponsibilityEntry;
  private FlakyTestDetector myFlakyTestDetector;
  private InvestigationsManager myInvestigationsManager;
  private STest mySTest;
  private FailedBuildInfo myFailedBuildInfo;
  private List<STestRun> myTestsWrapper;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFlakyTestDetector = Mockito.mock(FlakyTestDetector.class);
    mySProject = Mockito.mock(SProject.class);
    SProject parentProject = Mockito.mock(SProject.class);
    final SProject project2 = Mockito.mock(SProject.class);
    mySBuild = Mockito.mock(SBuild.class);
    mySTestRun = Mockito.mock(STestRun.class);
    myTestNameResponsibilityEntry = Mockito.mock(TestNameResponsibilityEntry.class);
    myInvestigationsManager = Mockito.mock(InvestigationsManager.class);
    final TestNameResponsibilityEntry responsibilityEntry2 = Mockito.mock(TestNameResponsibilityEntry.class);
    mySTest = Mockito.mock(STest.class);
    when(mySProject.getProjectId()).thenReturn("Project ID");
    when(project2.getProjectId()).thenReturn("Project ID 2");
    when(parentProject.getProjectId()).thenReturn("Parent Project ID");
    when(mySProject.getParentProject()).thenReturn(parentProject);
    when(mySTestRun.isMuted()).thenReturn(false);
    when(mySTestRun.isFixed()).thenReturn(false);
    when(mySTestRun.isNewFailure()).thenReturn(true);
    when(mySTestRun.getTest()).thenReturn(mySTest);
    when(mySTest.getAllResponsibilities())
      .thenReturn(Arrays.asList(myTestNameResponsibilityEntry, responsibilityEntry2));
    when(mySTest.getName()).thenReturn(new TestName("some.test.package.Test.method" + Math.random()));
    when(myFlakyTestDetector.isFlaky(anyLong())).thenReturn(false);
    when(myTestNameResponsibilityEntry.getState()).thenReturn(ResponsibilityEntry.State.NONE);
    when(responsibilityEntry2.getState()).thenReturn(ResponsibilityEntry.State.NONE);
    when(myInvestigationsManager.checkUnderInvestigation(mySProject, mySBuild, mySTest)).thenReturn(false);

    myTestsWrapper = Collections.singletonList(mySTestRun);
    when(mySBuild.getParametersProvider()).thenReturn(Mockito.mock(ParametersProvider.class));
    myFailedBuildInfo = new FailedBuildInfo(mySBuild);
    myFailedTestFilter = new FailedTestFilter(myFlakyTestDetector, myInvestigationsManager);
  }

  public void Test_TestRunIsMuted() {
    when(mySTestRun.isMuted()).thenReturn(true);

    List<STestRun> applicableTestRuns = myFailedTestFilter.apply(myFailedBuildInfo, mySProject, myTestsWrapper);

    Assert.assertEquals(applicableTestRuns.size(), 0);
  }

  public void Test_TestRunIsNotMuted() {
    when(mySTestRun.isMuted()).thenReturn(false);

    List<STestRun> applicableTestRuns = myFailedTestFilter.apply(myFailedBuildInfo, mySProject, myTestsWrapper);

    Assert.assertEquals(applicableTestRuns.size(), 1);
  }

  public void Test_TestRunIsFixed() {
    when(mySTestRun.isFixed()).thenReturn(true);

    List<STestRun> applicableTestRuns = myFailedTestFilter.apply(myFailedBuildInfo, mySProject, myTestsWrapper);

    Assert.assertEquals(applicableTestRuns.size(), 0);
  }

  public void Test_TestRunIsNotFixed() {
    when(mySTestRun.isFixed()).thenReturn(false);

    List<STestRun> applicableTestRuns = myFailedTestFilter.apply(myFailedBuildInfo, mySProject, myTestsWrapper);

    Assert.assertEquals(applicableTestRuns.size(), 1);
  }

  public void Test_TestRunNotNewFailure() {
    when(mySTestRun.isNewFailure()).thenReturn(false);

    List<STestRun> applicableTestRuns = myFailedTestFilter.apply(myFailedBuildInfo, mySProject, myTestsWrapper);

    Assert.assertEquals(applicableTestRuns.size(), 0);
  }

  public void Test_TestRunIsNewFailure() {
    when(mySTestRun.isNewFailure()).thenReturn(true);

    List<STestRun> applicableTestRuns = myFailedTestFilter.apply(myFailedBuildInfo, mySProject, myTestsWrapper);

    Assert.assertEquals(applicableTestRuns.size(), 1);
  }

  public void Test_BuildProblemIsUnderInvestigation() {
    when(myInvestigationsManager.checkUnderInvestigation(mySProject, mySBuild, mySTest)).thenReturn(true);
    when(myTestNameResponsibilityEntry.getProject()).thenReturn(mySProject);

    List<STestRun> applicableTestRuns = myFailedTestFilter.apply(myFailedBuildInfo, mySProject, myTestsWrapper);

    Assert.assertEquals(applicableTestRuns.size(), 0);
  }

  public void Test_BuildProblemNotUnderInvestigation() {
    when(myInvestigationsManager.checkUnderInvestigation(mySProject, mySBuild, mySTest)).thenReturn(false);
    when(myTestNameResponsibilityEntry.getProject()).thenReturn(mySProject);

    List<STestRun> applicableTestRuns = myFailedTestFilter.apply(myFailedBuildInfo, mySProject, myTestsWrapper);

    Assert.assertEquals(applicableTestRuns.size(), 1);
  }

  public void Test_TestIsFlaky() {
    when(myFlakyTestDetector.isFlaky(anyLong())).thenReturn(true);

    List<STestRun> applicableTestRuns = myFailedTestFilter.apply(myFailedBuildInfo, mySProject, myTestsWrapper);

    Assert.assertEquals(applicableTestRuns.size(), 0);
  }

  public void Test_TestNotFlaky() {
    when(myFlakyTestDetector.isFlaky(anyLong())).thenReturn(false);

    List<STestRun> applicableTestRuns = myFailedTestFilter.apply(myFailedBuildInfo, mySProject, myTestsWrapper);

    Assert.assertEquals(applicableTestRuns.size(), 1);
  }

  public void ignore_setup_and_teardown() {
    STest setup = Mockito.mock(STest.class);
    when(setup.getName()).thenReturn(new TestName("com.package.Test.setUp1"));

    STest teardown = Mockito.mock(STest.class);
    when(teardown.getName()).thenReturn(new TestName("com.package.Test.teardown"));

    STestRun setupTest = Mockito.mock(STestRun.class);
    when(setupTest.getTest()).thenReturn(setup);
    when(setupTest.isNewFailure()).thenReturn(true);

    STestRun teardownTest = Mockito.mock(STestRun.class);
    when(teardownTest.getTest()).thenReturn(teardown);
    when(teardownTest.isNewFailure()).thenReturn(true);

    myTestsWrapper = new ArrayList<>();
    myTestsWrapper.add(mySTestRun);
    myTestsWrapper.add(setupTest);
    myTestsWrapper.add(teardownTest);

    List<STestRun> applicableTestRuns = myFailedTestFilter.apply(myFailedBuildInfo, mySProject, myTestsWrapper);

    Assert.assertEquals(applicableTestRuns.size(), 1);
    Assert.assertEquals(applicableTestRuns.get(0), mySTestRun);

    setInternalProperty(Constants.IGNORE_SETUP_TEARDOWN_METHODS, "false");

    FailedTestFilter filter = new FailedTestFilter(myFlakyTestDetector, myInvestigationsManager);
    applicableTestRuns = filter.apply(new FailedBuildInfo(mySBuild), mySProject, myTestsWrapper);

    Assert.assertEquals(applicableTestRuns.size(), 3);
    Assert.assertEquals(applicableTestRuns.get(0), mySTestRun);
    Assert.assertEquals(applicableTestRuns.get(1), setupTest);
    Assert.assertEquals(applicableTestRuns.get(2), teardownTest);
  }
}