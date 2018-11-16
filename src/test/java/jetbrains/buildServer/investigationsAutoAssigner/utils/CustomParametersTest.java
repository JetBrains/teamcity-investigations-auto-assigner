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

package jetbrains.buildServer.investigationsAutoAssigner.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
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

  public void getUsersToIgnoreTestNoDescriptor() {
    SBuild sBuildMock = Mockito.mock(SBuild.class);
    Mockito.when(sBuildMock.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE)).thenReturn(Collections.emptyList());
    assertListEquals(CustomParameters.getUsersToIgnore(sBuildMock));
  }

  public void getUsersToIgnoreTestHasOneInList() {
    SBuild sBuildMock = Mockito.mock(SBuild.class);
    SBuildFeatureDescriptor sBuildFeatureDescriptor =
      Mockito.mock(jetbrains.buildServer.serverSide.SBuildFeatureDescriptor.class);
    Mockito.when(sBuildMock.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE))
           .thenReturn(Collections.singletonList(sBuildFeatureDescriptor));
    Map<String, String> params = new HashMap<>();
    params.put(Constants.USERS_TO_IGNORE, "username1");
    Mockito.when(sBuildFeatureDescriptor.getParameters()).thenReturn(params);
    assertListEquals(CustomParameters.getUsersToIgnore(sBuildMock), "username1");

    params.put(Constants.USERS_TO_IGNORE, "username2 ");
    assertListEquals(CustomParameters.getUsersToIgnore(sBuildMock), "username2");

    params.put(Constants.USERS_TO_IGNORE, "  username3    ");
    assertListEquals(CustomParameters.getUsersToIgnore(sBuildMock), "username3");
  }

  public void getUsersToIgnoreTestHasTwo() {
    SBuild sBuildMock = Mockito.mock(SBuild.class);
    SBuildFeatureDescriptor sBuildFeatureDescriptor =
      Mockito.mock(jetbrains.buildServer.serverSide.SBuildFeatureDescriptor.class);
    Mockito.when(sBuildMock.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE))
           .thenReturn(Collections.singletonList(sBuildFeatureDescriptor));
    Map<String, String> params = new HashMap<>();
    params.put(Constants.USERS_TO_IGNORE, "username1\nusername2\nusername3");
    Mockito.when(sBuildFeatureDescriptor.getParameters()).thenReturn(params);
    assertListEquals(CustomParameters.getUsersToIgnore(sBuildMock), "username1", "username2", "username3");
  }
}
