package jetbrains.buildServer.investigationsAutoAssigner.processing;

import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class BaseProcessorTest {
    static class TestProcessor extends BaseProcessor {}

    private TestProcessor processor;

    @Mock private SBuild build;
    @Mock private SBuildType buildType;
    @Mock private SProject project;
    @Mock private SProject virtualProject;
    @Mock private BuildStatistics statistics;
    @Mock private STestRun testRun;
    @Mock private BuildProblem buildProblem;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        processor = new TestProcessor();
    }

    @Test
    public void shouldRequestBrokenTestsWithStats() {
        BuildStatisticsOptions options = new BuildStatisticsOptions(BuildStatisticsOptions.FIXED_IN_BUILD, 0);
        when(build.getBuildStatistics(options)).thenReturn(statistics);
        when(statistics.getFailedTests()).thenReturn(Collections.singletonList(testRun));

        List<STestRun> result = processor.requestBrokenTestsWithStats(build);

        assertThat(result).containsExactly(testRun);
        verify(build).getBuildStatistics(options);
    }

    @Test
    public void shouldGetProjectFromBuildType() {
        when(build.getBuildId()).thenReturn(123L);
        when(build.getBuildType()).thenReturn(buildType);
        when(buildType.getProject()).thenReturn(project);
        when(project.isVirtual()).thenReturn(false);

        SProject result = processor.getProject(build);

        assertThat(result).isEqualTo(project);
        verify(build).getBuildType();
        verify(buildType).getProject();
    }

    @Test
    public void shouldGetParentProjectWhenProjectIsVirtual() {
        when(build.getBuildId()).thenReturn(123L);
        when(build.getBuildType()).thenReturn(buildType);
        when(buildType.getProject()).thenReturn(virtualProject);
        when(virtualProject.isVirtual()).thenReturn(true);
        when(virtualProject.getParentProject()).thenReturn(project);
        when(project.isVirtual()).thenReturn(false);

        SProject result = processor.getProject(build);

        assertThat(result).isEqualTo(project);
        verify(virtualProject).getParentProject();
    }

    @Test
    public void shouldReturnNullWhenNoBuildType() {
        when(build.getBuildId()).thenReturn(123L);
        when(build.getBuildType()).thenReturn(null);

        SProject result = processor.getProject(build);
        
        assertThat(result).isNull();
        verify(build).getBuildType();
    }
}