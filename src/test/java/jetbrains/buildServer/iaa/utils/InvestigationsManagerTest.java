package jetbrains.buildServer.iaa.utils;

import java.util.Collections;
import java.util.Date;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.responsibility.BuildProblemResponsibilityEntry;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.responsibility.TestNameResponsibilityEntry;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STest;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.audit.AuditLogBuilder;
import jetbrains.buildServer.serverSide.audit.AuditLogProvider;
import jetbrains.buildServer.serverSide.impl.problems.BuildProblemImpl;
import jetbrains.buildServer.users.User;
import org.assertj.core.api.Assertions;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;

@Test
public class InvestigationsManagerTest extends BaseTestCase {

  private InvestigationsManager myInvestigationsManager;
  private SProject myProject;
  private SProject myProject2;

  private BuildProblemImpl myBuildProblem;
  private BuildProblemResponsibilityEntry myBuildProblemResponsibilityEntry;


  private STestRun mySTestRun;
  private STest mySTest;
  private TestNameResponsibilityEntry myResponsibilityEntry;
  private SBuild mySBuild;
  private User myUser;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myProject = Mockito.mock(SProject.class);
    myProject2 = Mockito.mock(SProject.class);
    mySBuild = Mockito.mock(SBuild.class);
    myUser = Mockito.mock(User.class);
    SProject parentProject = Mockito.mock(SProject.class);
    when(myProject.getParentProject()).thenReturn(parentProject);
    when(myProject.getProjectId()).thenReturn("Project ID");
    when(myProject2.getProjectId()).thenReturn("Project ID 2");
    when(parentProject.getProjectId()).thenReturn("Parent Project ID");
    when(mySBuild.getQueuedDate()).thenReturn(new Date(2000000));

    myBuildProblem = Mockito.mock(BuildProblemImpl.class);
    myBuildProblemResponsibilityEntry = Mockito.mock(BuildProblemResponsibilityEntry.class);
    when(myBuildProblemResponsibilityEntry.getTimestamp()).thenReturn(new Date(1000000));
    when(myBuildProblemResponsibilityEntry.getState()).thenReturn(ResponsibilityEntry.State.NONE);
    when(myBuildProblem.getAllResponsibilities()).thenReturn(Collections.singletonList(
      myBuildProblemResponsibilityEntry));
    when(myBuildProblem.getProjectId()).thenReturn("123");

    mySTestRun = Mockito.mock(STestRun.class);
    mySTest = Mockito.mock(STest.class);
    myResponsibilityEntry = Mockito.mock(TestNameResponsibilityEntry.class);
    final AuditLogProvider auditLogProvider = Mockito.mock(AuditLogProvider.class);
    final AuditLogBuilder auditLogBuilder = Mockito.mock(AuditLogBuilder.class);
    when(auditLogProvider.getBuilder()).thenReturn(auditLogBuilder);
    when(myResponsibilityEntry.getTimestamp()).thenReturn(new Date(1000000));
    when(myResponsibilityEntry.getState()).thenReturn(ResponsibilityEntry.State.NONE);
    when(mySTestRun.getTest()).thenReturn(mySTest);
    when(mySTest.getAllResponsibilities()).thenReturn(Collections.singletonList(myResponsibilityEntry));
    when(mySTest.getProjectId()).thenReturn("123");

    myInvestigationsManager = new InvestigationsManager(auditLogProvider);
  }

  public void Test_BuildIsUnderInvestigationInSameProject() {
    when(myBuildProblemResponsibilityEntry.getState()).thenReturn(ResponsibilityEntry.State.TAKEN);
    when(myBuildProblemResponsibilityEntry.getProject()).thenReturn(myProject);

    Assertions.assertThat(myInvestigationsManager.checkUnderInvestigation(myProject, mySBuild, myBuildProblem))
              .isTrue();
  }

  public void Test_BuildProblemIsUnderInvestigationParentProject() {
    when(myBuildProblemResponsibilityEntry.getState()).thenReturn(ResponsibilityEntry.State.TAKEN);
    SProject parentProject = myProject.getParentProject();
    when(myBuildProblemResponsibilityEntry.getProject()).thenReturn(parentProject);

    Assertions.assertThat(myInvestigationsManager.checkUnderInvestigation(myProject, mySBuild, myBuildProblem))
              .isTrue();
  }

  public void Test_BuildProblemIsUnderInvestigationOtherProject() {
    when(myBuildProblemResponsibilityEntry.getState()).thenReturn(ResponsibilityEntry.State.TAKEN);
    when(myBuildProblemResponsibilityEntry.getProject()).thenReturn(myProject2);

    Assertions.assertThat(myInvestigationsManager.checkUnderInvestigation(myProject, mySBuild, myBuildProblem))
              .isFalse();
  }

  public void Test_BuildProblemAlreadyFixed() {
    when(myBuildProblemResponsibilityEntry.getState()).thenReturn(ResponsibilityEntry.State.FIXED);
    when(myBuildProblemResponsibilityEntry.getProject()).thenReturn(myProject);
    when(myBuildProblemResponsibilityEntry.getTimestamp()).thenReturn(new Date(3000000));
    when(mySBuild.getQueuedDate()).thenReturn(new Date(2000000));

    Assertions.assertThat(myInvestigationsManager.checkUnderInvestigation(myProject, mySBuild, myBuildProblem))
              .isTrue();
  }

  public void Test_BuildProblemBeforeWasFixed() {
    when(myBuildProblemResponsibilityEntry.getState()).thenReturn(ResponsibilityEntry.State.FIXED);
    when(myBuildProblemResponsibilityEntry.getProject()).thenReturn(myProject);
    when(myBuildProblemResponsibilityEntry.getTimestamp()).thenReturn(new Date(1000000));
    when(mySBuild.getQueuedDate()).thenReturn(new Date(2000000));

    Assertions.assertThat(myInvestigationsManager.checkUnderInvestigation(myProject, mySBuild, myBuildProblem))
              .isFalse();
  }

  public void Test_BuildProblemNotUnderInvestigation() {
    when(myBuildProblem.getAllResponsibilities()).thenReturn(Collections.emptyList());

    Assertions.assertThat(myInvestigationsManager.checkUnderInvestigation(myProject, mySBuild, myBuildProblem))
              .isFalse();
  }

  public void Test_TestIsUnderInvestigationParentProject() {
    when(myResponsibilityEntry.getState()).thenReturn(ResponsibilityEntry.State.TAKEN);
    SProject parentProject = myProject.getParentProject();
    when(myResponsibilityEntry.getProject()).thenReturn(parentProject);

    Assertions.assertThat(myInvestigationsManager.checkUnderInvestigation(myProject, mySBuild, mySTest)).isTrue();
  }

  public void Test_TestIsUnderInvestigationOtherProject() {
    when(myResponsibilityEntry.getState()).thenReturn(ResponsibilityEntry.State.TAKEN);
    when(myResponsibilityEntry.getProject()).thenReturn(myProject2);

    Assertions.assertThat(myInvestigationsManager.checkUnderInvestigation(myProject, mySBuild, mySTest)).isFalse();
  }

  public void Test_TestAlreadyFixed() {
    when(myResponsibilityEntry.getState()).thenReturn(ResponsibilityEntry.State.FIXED);
    when(myResponsibilityEntry.getProject()).thenReturn(myProject);
    when(myResponsibilityEntry.getTimestamp()).thenReturn(new Date(3000000));
    when(mySBuild.getQueuedDate()).thenReturn(new Date(2000000));

    Assertions.assertThat(myInvestigationsManager.checkUnderInvestigation(myProject, mySBuild, mySTest)).isTrue();
  }

  public void Test_TestBeforeWasAlreadyFixed() {
    when(myResponsibilityEntry.getState()).thenReturn(ResponsibilityEntry.State.FIXED);
    when(myResponsibilityEntry.getProject()).thenReturn(myProject);
    when(myResponsibilityEntry.getTimestamp()).thenReturn(new Date(1000000));
    when(mySBuild.getQueuedDate()).thenReturn(new Date(2000000));

    Assertions.assertThat(myInvestigationsManager.checkUnderInvestigation(myProject, mySBuild, mySTest)).isFalse();
  }

  public void Test_TestNotUnderInvestigation() {
    STest sTest = Mockito.mock(STest.class);
    when(mySTestRun.getTest()).thenReturn(sTest);
    when(sTest.getAllResponsibilities()).thenReturn(Collections.emptyList());

    Assertions.assertThat(myInvestigationsManager.checkUnderInvestigation(myProject, mySBuild, sTest)).isFalse();
  }

  public void Test_BuildProblemFindPreviousResponsible_FixedBeforeQueued() {
    when(myBuildProblemResponsibilityEntry.getState()).thenReturn(ResponsibilityEntry.State.FIXED);
    when(myBuildProblemResponsibilityEntry.getResponsibleUser()).thenReturn(myUser);
    when(myBuildProblemResponsibilityEntry.getProject()).thenReturn(myProject);
    when(myBuildProblemResponsibilityEntry.getTimestamp()).thenReturn(new Date(2000000));
    when(mySBuild.getQueuedDate()).thenReturn(new Date(3000000));

    Assertions.assertThat(myInvestigationsManager.findPreviousResponsible(myProject, mySBuild, myBuildProblem))
              .isEqualTo(
                myUser);
  }

  public void Test_BuildProblemFindPreviousResponsible_FixedAfterQueued() {
    when(myBuildProblemResponsibilityEntry.getState()).thenReturn(ResponsibilityEntry.State.FIXED);
    when(myBuildProblemResponsibilityEntry.getProject()).thenReturn(myProject);
    when(myBuildProblemResponsibilityEntry.getTimestamp()).thenReturn(new Date(3000000));
    when(mySBuild.getQueuedDate()).thenReturn(new Date(2000000));

    Assertions.assertThat(myInvestigationsManager.findPreviousResponsible(myProject, mySBuild, myBuildProblem))
              .isNull();
  }

  public void Test_TestProblemFindPreviousResponsible_FixedBeforeQueued() {
    when(myResponsibilityEntry.getState()).thenReturn(ResponsibilityEntry.State.FIXED);
    when(myResponsibilityEntry.getResponsibleUser()).thenReturn(myUser);
    when(myResponsibilityEntry.getProject()).thenReturn(myProject);
    when(myResponsibilityEntry.getTimestamp()).thenReturn(new Date(2000000));
    when(mySBuild.getQueuedDate()).thenReturn(new Date(3000000));

    Assertions.assertThat(myInvestigationsManager.findPreviousResponsible(myProject, mySBuild, mySTest)).isEqualTo(
      myUser);
  }

  public void Test_TestProblemFindPreviousResponsible_FixedAfterQueued() {
    when(myResponsibilityEntry.getState()).thenReturn(ResponsibilityEntry.State.FIXED);
    when(myResponsibilityEntry.getProject()).thenReturn(myProject);
    when(myResponsibilityEntry.getTimestamp()).thenReturn(new Date(3000000));
    when(mySBuild.getQueuedDate()).thenReturn(new Date(2000000));

    Assertions.assertThat(myInvestigationsManager.findPreviousResponsible(myProject, mySBuild, mySTest)).isNull();
  }
}
