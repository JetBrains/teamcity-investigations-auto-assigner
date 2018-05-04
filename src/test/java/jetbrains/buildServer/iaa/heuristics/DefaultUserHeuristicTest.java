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
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.users.UserModelEx;
import jetbrains.buildServer.users.impl.UserEx;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;

@Test
public class DefaultUserHeuristicTest extends BaseTestCase {
  private DefaultUserHeuristic myHeuristic;
  private UserModelEx myUserModelEx;
  private SBuild mySBuildMock;
  private UserEx myUserEx;
  private ProblemInfo myProblemInfo;
  private static final String USER_NAME = "rugpanov";
  private HashMap<String, String> myBuildFeatureParams;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myUserModelEx = Mockito.mock(UserModelEx.class);
    myHeuristic = new DefaultUserHeuristic(myUserModelEx);
    final SBuildFeatureDescriptor descriptor = Mockito.mock(SBuildFeatureDescriptor.class);
    mySBuildMock = Mockito.mock(SBuild.class);
    final SProject sProjectMock = Mockito.mock(SProject.class);
    myUserEx = Mockito.mock(UserEx.class);
    myProblemInfo = new ProblemInfo(mySBuildMock, sProjectMock, "problem text");

    myBuildFeatureParams = new HashMap<>();
    when(
      mySBuildMock.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE))
      .thenReturn(Collections.singletonList(descriptor));
    when(descriptor.getParameters()).thenReturn(myBuildFeatureParams);
  }

  public void TestFeatureIsDisabled() {
    when(mySBuildMock.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE)).thenReturn(Collections.emptyList());
    Pair<User, String> responsible = myHeuristic.findResponsibleUser(myProblemInfo);
    Assert.assertNull(responsible);
  }

  public void TestNoResponsibleSpecified() {
    //myBuildFeatureParams is empty
    Pair<User, String> responsible = myHeuristic.findResponsibleUser(myProblemInfo);
    Assert.assertNull(responsible);

    myBuildFeatureParams.put(Constants.DEFAULT_RESPONSIBLE, "");
    responsible = myHeuristic.findResponsibleUser(myProblemInfo);
    Assert.assertNull(responsible);
  }

  public void TestResponsibleNotFound() {
    myBuildFeatureParams.put(Constants.DEFAULT_RESPONSIBLE, USER_NAME);
    when(myUserModelEx.findUserAccount(null, USER_NAME)).thenReturn(null);
    Pair<User, String> responsible = myHeuristic.findResponsibleUser(myProblemInfo);

    Assert.assertNull(responsible);
  }

  public void TestResponsibleFound() {
    myBuildFeatureParams.put(Constants.DEFAULT_RESPONSIBLE, USER_NAME);
    when(myUserModelEx.findUserAccount(null, USER_NAME)).thenReturn(myUserEx);
    Pair<User, String> responsible = myHeuristic.findResponsibleUser(myProblemInfo);

    Assert.assertNotNull(responsible);
    Assert.assertEquals(responsible.first, myUserEx);
  }
}
