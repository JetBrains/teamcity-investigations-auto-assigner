/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

package jetbrains.buildServer.investigationsAutoAssigner.heuristics;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.processing.HeuristicContext;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STestRun;
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
  private SBuild mySBuild;
  private UserEx myUserEx;
  private UserEx myUserEx2;
  private UserEx myUserEx3;
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
    final SBuildFeatureDescriptor descriptor = Mockito.mock(SBuildFeatureDescriptor.class);
    mySBuild = Mockito.mock(SBuild.class);
    SProject sProject = Mockito.mock(SProject.class);
    myUserEx = Mockito.mock(UserEx.class);
    myUserEx2 = Mockito.mock(UserEx.class);
    myUserEx3 = Mockito.mock(UserEx.class);
    mySTestRun = Mockito.mock(STestRun.class);
    myHeuristicContext =
      new HeuristicContext(mySBuild,
                           sProject,
                           Collections.emptyList(),
                           Collections.singletonList(mySTestRun),
                           Collections.emptySet());

    myBuildFeatureParams = new HashMap<>();
    when(
      mySBuild.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE))
      .thenReturn(Collections.singletonList(descriptor));
    when(descriptor.getParameters()).thenReturn(myBuildFeatureParams);
  }

  public void TestFeatureIsDisabled() {
    when(mySBuild.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE)).thenReturn(Collections.emptyList());

    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);

    Assert.assertTrue(heuristicResult.isEmpty());
  }

  public void TestNoResponsibleSpecified() {
    when(mySBuild.getBuildFeaturesOfType(Constants.BUILD_FEATURE_TYPE)).thenReturn(Collections.emptyList());
    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);
    Assert.assertTrue(heuristicResult.isEmpty());

    myBuildFeatureParams.put(Constants.DEFAULT_RESPONSIBLE, "");
    heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);
    Assert.assertTrue(heuristicResult.isEmpty());
  }

  public void TestResponsibleNotFound() {
    myBuildFeatureParams.put(Constants.DEFAULT_RESPONSIBLE, USER_NAME);
    when(myUserModelEx.findUserAccount(null, USER_NAME)).thenReturn(null);

    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);

    Assert.assertTrue(heuristicResult.isEmpty());
  }

  public void TestResponsibleFound() {
    myBuildFeatureParams.put(Constants.DEFAULT_RESPONSIBLE, USER_NAME);
    when(myUserModelEx.findUserAccount(null, USER_NAME)).thenReturn(myUserEx);

    HeuristicResult heuristicResult = myHeuristic.findResponsibleUser(myHeuristicContext);

    Assert.assertFalse(heuristicResult.isEmpty());
    Assert.assertNotNull(heuristicResult.getResponsibility(mySTestRun));

    Responsibility responsibility = heuristicResult.getResponsibility(mySTestRun);
    assert responsibility != null;
    Assert.assertEquals(responsibility.getUser(), myUserEx);
  }
}
