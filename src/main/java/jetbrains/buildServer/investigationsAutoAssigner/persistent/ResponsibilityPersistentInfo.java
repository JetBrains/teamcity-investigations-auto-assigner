package jetbrains.buildServer.investigationsAutoAssigner.persistent;

/**
 * Represents the persistent information related to a responsibility assignment.
 * This class holds the details of a responsibility, including the test name ID,
 * investigator ID, and the reason for the assignment.
 */
class ResponsibilityPersistentInfo {

  String testNameId;
  String investigatorId;
  String reason;

  /**
   * Constructs a new {@link ResponsibilityPersistentInfo} object with the given test name ID,
   * investigator ID, and reason for the assignment.
   *
   * @param testNameId     The ID of the test to which the responsibility is assigned.
   * @param investigatorId The ID of the investigator responsible for the test.
   * @param reason         The reason for the responsibility assignment.
   */
  ResponsibilityPersistentInfo(String testNameId, String investigatorId, String reason) {
    this.testNameId = testNameId;
    this.investigatorId = investigatorId;
    this.reason = reason;
  }
}
