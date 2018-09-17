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

package jetbrains.buildServer.iaa.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.iaa.common.Constants;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test
public class CustomParametersTest extends BaseTestCase {

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  public void getBlackListTestNoDescriptor() {
    SBuild sBuildMock = Mockito.mock(SBuild.class);
    Mockito.when(sBuildMock.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE)).thenReturn(Collections.emptyList());
    assertListEquals(CustomParameters.getBlackList(sBuildMock));
  }

  public void getBlackListTestHasOneInList() {
    SBuild sBuildMock = Mockito.mock(SBuild.class);
    SBuildFeatureDescriptor sBuildFeatureDescriptor =
      Mockito.mock(jetbrains.buildServer.serverSide.SBuildFeatureDescriptor.class);
    Mockito.when(sBuildMock.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE))
           .thenReturn(Collections.singletonList(sBuildFeatureDescriptor));
    Map<String, String> params = new HashMap<>();
    params.put(Constants.BLACK_LIST, "username1");
    Mockito.when(sBuildFeatureDescriptor.getParameters()).thenReturn(params);
    assertListEquals(CustomParameters.getBlackList(sBuildMock), "username1");

    params.put(Constants.BLACK_LIST, "username2 ");
    assertListEquals(CustomParameters.getBlackList(sBuildMock), "username2");

    params.put(Constants.BLACK_LIST, "  username3    ");
    assertListEquals(CustomParameters.getBlackList(sBuildMock), "username3");
  }

  public void getBlackListTestHasTwo() {
    SBuild sBuildMock = Mockito.mock(SBuild.class);
    SBuildFeatureDescriptor sBuildFeatureDescriptor =
      Mockito.mock(jetbrains.buildServer.serverSide.SBuildFeatureDescriptor.class);
    Mockito.when(sBuildMock.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE))
           .thenReturn(Collections.singletonList(sBuildFeatureDescriptor));
    Map<String, String> params = new HashMap<>();
    params.put(Constants.BLACK_LIST, "username1, username2, username3");
    Mockito.when(sBuildFeatureDescriptor.getParameters()).thenReturn(params);
    assertListEquals(CustomParameters.getBlackList(sBuildMock), "username1", "username2", "username3");
  }
}
