

package jetbrains.buildServer.investigationsAutoAssigner.representation;

import javax.servlet.http.HttpServletRequest;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.persistent.AssignerArtifactDao;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static jetbrains.buildServer.investigationsAutoAssigner.common.Constants.TEST_RUN_IN_REQUEST;
import static org.mockito.Mockito.when;

@Test
public class TestDetailsExtensionTest extends BaseTestCase {

  private AssignerArtifactDao myAssignerArtifactDaoMock;
  private HttpServletRequest myHttpServletRequestMock;
  private Responsibility myResponsibilityMock;
  private STestRun mySTestRunMock;
  private TestDetailsExtension myTestedTestDetailsExtension;
  private SBuild mySBuildMock;

  @BeforeMethod
  @Override
  protected void setUp() {
    final PagePlaces pagePlacesMock = Mockito.mock(PagePlaces.class);
    final PluginDescriptor pluginDescriptorMock = Mockito.mock(PluginDescriptor.class);
    myAssignerArtifactDaoMock = Mockito.mock(AssignerArtifactDao.class);
    myHttpServletRequestMock = Mockito.mock(HttpServletRequest.class);
    myResponsibilityMock = Mockito.mock(Responsibility.class);
    mySTestRunMock = Mockito.mock(STestRun.class);
    mySBuildMock = Mockito.mock(SBuild.class);

    when(mySTestRunMock.getFirstFailed()).thenReturn(null);
    when(myHttpServletRequestMock.getAttribute(TEST_RUN_IN_REQUEST)).thenReturn(mySTestRunMock);
    when(myAssignerArtifactDaoMock.get(null, mySTestRunMock)).thenReturn(myResponsibilityMock);

    myTestedTestDetailsExtension = new TestDetailsExtension(pagePlacesMock, pluginDescriptorMock);
  }

  //public void testFillModelWithoutSTestRunObject() {
  //  when(myHttpServletRequestMock.getAttribute(TEST_RUN_IN_REQUEST)).thenReturn(null);
  //
  //  Map<String, Object> testedMap = new HashMap<>();
  //  myTestedTestDetailsExtension.fillModel(testedMap, myHttpServletRequestMock);
  //
  //  Mockito.verify(myAssignerArtifactDaoMock, Mockito.never()).get(Mockito.any(), Mockito.any());
  //  assertFalse(testedMap.containsKey("autoAssignedResponsibility"));
  //}
  //
  //public void testFillModelNoResponsibilityForTest() {
  //  when(myHttpServletRequestMock.getAttribute(TEST_RUN_IN_REQUEST)).thenReturn(mySTestRunMock);
  //  when(myAssignerArtifactDaoMock.get(null, mySTestRunMock)).thenReturn(null);
  //
  //  Map<String, Object> testedMap = new HashMap<>();
  //  myTestedTestDetailsExtension.fillModel(testedMap, myHttpServletRequestMock);
  //
  //  Mockito.verify(myAssignerArtifactDaoMock, Mockito.atLeastOnce()).get(null, mySTestRunMock);
  //  assertFalse(testedMap.containsKey("autoAssignedResponsibility"));
  //}
  //
  //public void testFillModelFine() {
  //  when(myHttpServletRequestMock.getAttribute(TEST_RUN_IN_REQUEST)).thenReturn(mySTestRunMock);
  //  when(myAssignerArtifactDaoMock.get(null, mySTestRunMock)).thenReturn(myResponsibilityMock);
  //
  //  Map<String, Object> testedMap = new HashMap<>();
  //  myTestedTestDetailsExtension.fillModel(testedMap, myHttpServletRequestMock);
  //
  //  Mockito.verify(myAssignerArtifactDaoMock, Mockito.atLeastOnce()).get(Mockito.any(), Mockito.any());
  //  assertTrue(testedMap.containsKey("autoAssignedResponsibility"));
  //}
  //
  //public void testFillModelFineInFirstFailedBuild() {
  //  when(myHttpServletRequestMock.getAttribute(TEST_RUN_IN_REQUEST)).thenReturn(mySTestRunMock);
  //  when(myAssignerArtifactDaoMock.get(mySBuildMock, mySTestRunMock)).thenReturn(myResponsibilityMock);
  //  when(mySTestRunMock.getFirstFailed()).thenReturn(mySBuildMock);
  //
  //  Map<String, Object> testedMap = new HashMap<>();
  //  myTestedTestDetailsExtension.fillModel(testedMap, myHttpServletRequestMock);
  //
  //  Mockito.verify(myAssignerArtifactDaoMock, Mockito.atLeastOnce()).get(Mockito.any(), Mockito.any());
  //  assertTrue(testedMap.containsKey("autoAssignedResponsibility"));
  //}
}