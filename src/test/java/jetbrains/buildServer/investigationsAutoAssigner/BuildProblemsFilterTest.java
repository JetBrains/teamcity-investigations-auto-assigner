import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BuildProblemsFilterTest {

  private BuildProblemsFilter buildProblemsFilter;

  @BeforeEach
  public void setUp() {
    buildProblemsFilter = new BuildProblemsFilter();
  }

  @Test
  public void testApplicableBuildProblem() {
    BuildProblemDescriptor compilationError = new BuildProblemDescriptor("TC_COMPILATION_ERROR_TYPE");
    assertTrue(buildProblemsFilter.isApplicable(compilationError),
               "Compilation errors should be applicable.");
  }

  @Test
  public void testNonApplicableBuildProblem() {
    BuildProblemDescriptor exitCodeError = new BuildProblemDescriptor("TC_EXIT_CODE_TYPE");
    assertFalse(buildProblemsFilter.isApplicable(exitCodeError),
                "Exit code errors should not be applicable.");
  }

  @Test
  public void testNullBuildProblem() {
    assertFalse(buildProblemsFilter.isApplicable(null),
                "Null build problems should not be applicable.");
  }
}
