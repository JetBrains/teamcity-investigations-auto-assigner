

package jetbrains.buildServer.investigationsAutoAssigner.utils;

import jetbrains.buildServer.serverSide.STestRun;

public class Utils {
  public static String getLogPrefix(STestRun sTestRun) {
    return String.format("Build: id:%s , test: %s ::", sTestRun.getBuildId(), sTestRun.getTest().getTestNameId());
  }
}