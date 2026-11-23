

package jetbrains.buildServer.investigationsAutoAssigner.persistent;

import jetbrains.buildServer.BaseTestCase;
import org.testng.annotations.Test;

@Test
public class ResponsibilityPersistentInfoTest extends BaseTestCase {
  public void TestFields() {
    String testNameId = "238";
    String testInvestigatorId = "239";
    String testReason = "testReason";
    ResponsibilityPersistentInfo rp = new ResponsibilityPersistentInfo(testNameId, testInvestigatorId, testReason);
    assertEquals(testNameId, rp.testNameId);
    assertEquals(testInvestigatorId, rp.investigatorId);
    assertEquals(testReason, rp.reason);
  }
}