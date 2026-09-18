

package jetbrains.buildServer.investigationsAutoAssigner.representation;

import javax.servlet.http.HttpServletRequest;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.persistent.AssignerArtifactDao;
import jetbrains.buildServer.serverSide.STestRun;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static jetbrains.buildServer.investigationsAutoAssigner.common.Constants.TEST_RUN_IN_REQUEST;
import static org.mockito.Mockito.when;

@Test
public class TestDetailsExtensionTest extends BaseTestCase {


  @BeforeMethod
  @Override
  protected void setUp() {
    AssignerArtifactDao myAssignerArtifactDaoMock = Mockito.mock(AssignerArtifactDao.class);
    HttpServletRequest myHttpServletRequestMock = Mockito.mock(HttpServletRequest.class);
    Responsibility myResponsibilityMock = Mockito.mock(Responsibility.class);
    STestRun mySTestRunMock = Mockito.mock(STestRun.class);

    when(mySTestRunMock.getFirstFailed()).thenReturn(null);
    when(myHttpServletRequestMock.getAttribute(TEST_RUN_IN_REQUEST)).thenReturn(mySTestRunMock);
    when(myAssignerArtifactDaoMock.get(null, mySTestRunMock)).thenReturn(myResponsibilityMock);


  }

}