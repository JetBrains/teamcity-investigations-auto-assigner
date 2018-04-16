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

package jetbrains.buildServer.iaa.heuristics;

import com.intellij.openapi.util.Pair;
import java.util.Collections;
import java.util.HashMap;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.iaa.ProblemInfo;
import jetbrains.buildServer.iaa.common.Constants;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.UserModelEx;
import jetbrains.buildServer.users.impl.UserEx;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;

@Test
public class DefaultUserHeuristicTest extends BaseTestCase {
  private DefaultUserHeuristic heuristic;
  private UserModelEx userModelEx;
  private SBuild sBuildMock;
  private UserEx firstUser;
  private ProblemInfo problemInfo;
  private static final String USER_NAME = "rugpanov";
  private HashMap<String, String> buildFeatureParams;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    userModelEx = Mockito.mock(UserModelEx.class);
    heuristic = new DefaultUserHeuristic(userModelEx);
    final SBuildFeatureDescriptor descriptor = Mockito.mock(SBuildFeatureDescriptor.class);
    sBuildMock = Mockito.mock(SBuild.class);
    firstUser = Mockito.mock(UserEx.class);
    problemInfo = new ProblemInfo(sBuildMock, "problem text");

    buildFeatureParams = new HashMap<>();
    when(sBuildMock.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE)).thenReturn(Collections.singletonList(descriptor));
    when(descriptor.getParameters()).thenReturn(buildFeatureParams);
  }

  public void TestFeatureIsDisabled() {
    when(sBuildMock.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE)).thenReturn(Collections.emptyList());
    Pair<SUser, String> responsible = heuristic.findResponsibleUser(problemInfo);
    Assert.assertNull(responsible);
  }

  public void TestNoResponsibleSpecified() {
    //buildFeatureParams is empty
    Pair<SUser, String> responsible = heuristic.findResponsibleUser(problemInfo);
    Assert.assertNull(responsible);

    buildFeatureParams.put(Constants.DEFAULT_RESPONSIBLE, "");
    responsible = heuristic.findResponsibleUser(problemInfo);
    Assert.assertNull(responsible);
  }

  public void TestResponsibleNotFound() {
    buildFeatureParams.put(Constants.DEFAULT_RESPONSIBLE, USER_NAME);
    when(userModelEx.findUserAccount(null, USER_NAME)).thenReturn(null);
    Pair<SUser, String> responsible = heuristic.findResponsibleUser(problemInfo);

    Assert.assertNull(responsible);
  }

  public void TestResponsibleFound() {
    buildFeatureParams.put(Constants.DEFAULT_RESPONSIBLE, USER_NAME);
    when(userModelEx.findUserAccount(null, USER_NAME)).thenReturn(firstUser);
    Pair<SUser, String> responsible = heuristic.findResponsibleUser(problemInfo);

    Assert.assertNotNull(responsible);
    Assert.assertEquals(responsible.first, firstUser);
  }
}
