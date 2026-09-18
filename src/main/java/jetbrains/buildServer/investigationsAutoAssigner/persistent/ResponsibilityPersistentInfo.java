

package jetbrains.buildServer.investigationsAutoAssigner.persistent;

public class ResponsibilityPersistentInfo {
  String testNameId;
  String investigatorId;
  String reason;

  ResponsibilityPersistentInfo(String testNameId, String investigatorId, String reason) {
    this.testNameId = testNameId;
    this.investigatorId = investigatorId;
    this.reason = reason;
  }
}