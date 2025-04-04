package jetbrains.buildServer.investigationsAutoAssigner.utils;

import jetbrains.buildServer.serverSide.STestRun;

public class Utils {

  /**
   * Generates a log prefix string for a given test run.
   * The prefix contains the build ID and the test name ID.
   *
   * @param sTestRun the test run object from which the log prefix will be generated
   * @return a formatted string with the build ID and test name ID
   */
  public static String getLogPrefix(STestRun sTestRun) {
    return String.format("Build: id:%s , test: %s ::", sTestRun.getBuildId(), sTestRun.getTest().getTestNameId());
  }
}
