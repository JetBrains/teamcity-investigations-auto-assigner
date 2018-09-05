/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

package jetbrains.buildServer.iaa.representation;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.iaa.common.Responsibility;
import jetbrains.buildServer.iaa.utils.AssignerArtifactDao;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static jetbrains.buildServer.iaa.common.Constants.TEST_RUN_IN_REQUEST;
import static org.mockito.Mockito.when;

@Test
public class TestDetailsExtensionTest extends BaseTestCase {

  private AssignerArtifactDao myAssignerArtifactDaoMock;
  private HttpServletRequest myHttpServletRequestMock;
  private Responsibility myResponsibilityMock;
  private STestRun mySTestRunMock;
  private TestDetailsExtension myTestedTestDetailsExtension;

  @BeforeMethod
  @Override
  protected void setUp() {
    final PagePlaces pagePlacesMock = Mockito.mock(PagePlaces.class);
    final PluginDescriptor pluginDescriptorMock = Mockito.mock(PluginDescriptor.class);
    myAssignerArtifactDaoMock = Mockito.mock(AssignerArtifactDao.class);
    myHttpServletRequestMock = Mockito.mock(HttpServletRequest.class);
    myResponsibilityMock = Mockito.mock(Responsibility.class);
    mySTestRunMock = Mockito.mock(STestRun.class);

    when(myHttpServletRequestMock.getAttribute(TEST_RUN_IN_REQUEST)).thenReturn(mySTestRunMock);
    when(myAssignerArtifactDaoMock.get(mySTestRunMock)).thenReturn(myResponsibilityMock);

    myTestedTestDetailsExtension =
      new TestDetailsExtension(pagePlacesMock, pluginDescriptorMock, myAssignerArtifactDaoMock);
  }

  public void testFillModelWithoutSTestRunObject() {
    when(myHttpServletRequestMock.getAttribute(TEST_RUN_IN_REQUEST)).thenReturn(null);

    Map<String, Object> testedMap = new HashMap<>();
    myTestedTestDetailsExtension.fillModel(testedMap, myHttpServletRequestMock);

    Mockito.verify(myAssignerArtifactDaoMock, Mockito.never()).get(Mockito.any());
    assertFalse(testedMap.containsKey("autoAssignedResponsibility"));
  }

  public void testFillModelNoResponsibilityForTest() {
    when(myHttpServletRequestMock.getAttribute(TEST_RUN_IN_REQUEST)).thenReturn(mySTestRunMock);
    when(myAssignerArtifactDaoMock.get(mySTestRunMock)).thenReturn(null);

    Map<String, Object> testedMap = new HashMap<>();
    myTestedTestDetailsExtension.fillModel(testedMap, myHttpServletRequestMock);

    Mockito.verify(myAssignerArtifactDaoMock, Mockito.atLeastOnce()).get(mySTestRunMock);
    assertFalse(testedMap.containsKey("autoAssignedResponsibility"));
  }

  public void testFillModelFine() {
    when(myHttpServletRequestMock.getAttribute(TEST_RUN_IN_REQUEST)).thenReturn(mySTestRunMock);
    when(myAssignerArtifactDaoMock.get(mySTestRunMock)).thenReturn(myResponsibilityMock);

    Map<String, Object> testedMap = new HashMap<>();
    myTestedTestDetailsExtension.fillModel(testedMap, myHttpServletRequestMock);

    Mockito.verify(myAssignerArtifactDaoMock, Mockito.atLeastOnce()).get(Mockito.any());
    assertTrue(testedMap.containsKey("autoAssignedResponsibility"));
  }
}
