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

package jetbrains.buildServer.investigationsAutoAssigner.processing;

import java.util.Collections;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.investigationsAutoAssigner.common.FailedBuildInfo;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.persistent.AssignerArtifactDao;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.users.SUser;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@Test
public class FailedTestAndBuildProblemsProcessorTest extends BaseTestCase {
  private FailedTestAndBuildProblemsProcessor myProcessor;
  private ResponsibleUserFinder myResponsibleUserFinder;
  private BuildEx mySBuild;
  private SBuildType mySBuildType;
  private AssignerArtifactDao myAssignerArtifactDao;
  private FailedBuildInfo myFailedBuildInfo;
  private HeuristicResult myNotEmptyHeuristicResult;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myResponsibleUserFinder = Mockito.mock(ResponsibleUserFinder.class);
    final FailedTestFilter failedTestFilter = Mockito.mock(FailedTestFilter.class);
    final FailedTestAssigner failedTestAssigner = Mockito.mock(FailedTestAssigner.class);
    final BuildProblemsFilter buildProblemsFilter = Mockito.mock(BuildProblemsFilter.class);
    final BuildProblemsAssigner buildProblemsAssigner = Mockito.mock(BuildProblemsAssigner.class);
    myAssignerArtifactDao = Mockito.mock(AssignerArtifactDao.class);
    myProcessor = new FailedTestAndBuildProblemsProcessor(myResponsibleUserFinder,
                                                          failedTestFilter,
                                                          failedTestAssigner,
                                                          buildProblemsFilter,
                                                          buildProblemsAssigner,
                                                          myAssignerArtifactDao);

    //configure tests
    TestName testNameMock = Mockito.mock(TestName.class);
    when(testNameMock.getAsString()).thenReturn("Test Name as String");

    final STest sTestMock = Mockito.mock(jetbrains.buildServer.serverSide.STest.class);
    when(sTestMock.getName()).thenReturn(testNameMock);

    final STestRun STestRun = Mockito.mock(jetbrains.buildServer.serverSide.STestRun.class);
    when(STestRun.getTest()).thenReturn(sTestMock);
    when(STestRun.getFullText()).thenReturn("Full Text Test Run");

    //configure build stats
    BuildStatistics buildStatistics = Mockito.mock(BuildStatistics.class);
    when(buildStatistics.getFailedTests()).thenReturn(Collections.singletonList(STestRun));

    //configure project
    SProject sProject = Mockito.mock(SProject.class);
    when(sProject.getProjectId()).thenReturn("projectId");

    //configure build type
    mySBuildType = Mockito.mock(SBuildType.class);
    when(mySBuildType.getProject()).thenReturn(sProject);

    //configure build
    mySBuild = Mockito.mock(BuildEx.class);
    when(mySBuild.getBuildType()).thenReturn(mySBuildType);
    when(mySBuild.getBuildStatistics(any())).thenReturn(buildStatistics);
    myFailedBuildInfo = new FailedBuildInfo(mySBuild);

    //configure finder
    final HeuristicResult heuristicsResult = new HeuristicResult();
    when(myResponsibleUserFinder.findResponsibleUser(any(), any(), anyList(), anyList())).thenReturn(heuristicsResult);

    //configure heuristic results
    myNotEmptyHeuristicResult = new HeuristicResult();
    SUser sUser = Mockito.mock(SUser.class);
    myNotEmptyHeuristicResult.addResponsibility(STestRun, new Responsibility(sUser, "Failed description"));
  }

  public void Test_OnTestFailed_BuildTypeIsNull() {
    when(mySBuild.getBuildType()).thenReturn(null);

    myProcessor.processBuild(myFailedBuildInfo);

    Mockito.verify(mySBuild, Mockito.never()).getBuildProblems();
  }

  public void Test_OnTestFailed_BuildTypeNotNull() {
    when(mySBuild.getBuildType()).thenReturn(mySBuildType);

    myProcessor.processBuild(myFailedBuildInfo);

    Mockito.verify(mySBuild, Mockito.atLeastOnce()).getBuildProblems();
  }

  public void Test_OnTestFailed_AssignerHasRightHeuristicsResult() {
    when(myResponsibleUserFinder.findResponsibleUser(any(), any(), anyList(), anyList()))
      .thenReturn(myNotEmptyHeuristicResult);

    Mockito.doAnswer((Answer<Void>)invocation -> {
      final Object[] args = invocation.getArguments();
      assertEquals(3, args.length);
      assertEquals(args[2], myNotEmptyHeuristicResult);
      return null;
    }).when(myAssignerArtifactDao).appendHeuristicsResult(any(), any(), any());

    myProcessor.processBuild(myFailedBuildInfo);

    Mockito.verify(myAssignerArtifactDao, Mockito.atLeastOnce()).appendHeuristicsResult(any(), any(), any());
  }

}
