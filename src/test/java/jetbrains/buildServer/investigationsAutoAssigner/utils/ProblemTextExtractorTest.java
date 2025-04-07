/*
 * Copyright 2000-2025 JetBrains s.r.o.
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


import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.BuildProblemTypes;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.STest;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.tests.TestName;


import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ProblemTextExtractorTest {

  private ProblemTextExtractor problemTextExtractor;
  private BuildProblem buildProblem;
  private SBuild sBuild;
  private STest sTest;
  private STestRun sTestRun;
  private TestName testName;

  @BeforeMethod
  public void setUp() {
    problemTextExtractor = new ProblemTextExtractor();
    buildProblem = mock(BuildProblem.class);
    sBuild = mock(SBuild.class);
    sTest = mock(STest.class);
    sTestRun = mock(STestRun.class);
    testName = mock(TestName.class);
  }

  @Test
  public void testGetBuildProblemText_WithCompilationError() {
    when(buildProblem.getBuildProblemData()).thenReturn(mock(BuildProblemData.class));
    when(buildProblem.getBuildProblemData().getType()).thenReturn(BuildProblemTypes.TC_COMPILATION_ERROR_TYPE);
    when(buildProblem.getBuildProblemDescription()).thenReturn("Compilation Error Description");

    String result = problemTextExtractor.getBuildProblemText(buildProblem, sBuild);

    assertTrue(result.contains("Compilation Error Description"), "The result should contain a description of the compilation error");
  }

  @Test
  public void testGetBuildProblemText_WithDefaultExtractor() {
    when(buildProblem.getBuildProblemData()).thenReturn(mock(BuildProblemData.class));
    when(buildProblem.getBuildProblemData().getType()).thenReturn("UNKNOWN_ERROR");
    when(buildProblem.getBuildProblemDescription()).thenReturn("Generic Error Description");

    String result = problemTextExtractor.getBuildProblemText(buildProblem, sBuild);

    assertEquals(result, "Generic Error Description");
  }

  @Test
  public void testGetBuildProblemText_EmptyDescription() {
    when(buildProblem.getBuildProblemData()).thenReturn(mock(BuildProblemData.class));
    when(buildProblem.getBuildProblemData().getType()).thenReturn("UNKNOWN_ERROR");
    when(buildProblem.getBuildProblemDescription()).thenReturn(null);

    String result = problemTextExtractor.getBuildProblemText(buildProblem, sBuild);

    assertEquals(result, "no problem description available");
  }

  @Test
  public void testGetFailedTestText() {
    when(sTestRun.getTest()).thenReturn(sTest);
    when(sTest.getName()).thenReturn(testName);
    when(testName.getAsString()).thenReturn("testExampleName");
    when(sTestRun.getFullText()).thenReturn("test failed with assertion error");

    String result = problemTextExtractor.getFailedTestText(sTestRun);

    assertEquals(result, "testExampleName test failed with assertion error");
  }
}