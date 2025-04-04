

package jetbrains.buildServer.investigationsAutoAssigner.common;

/**
 * Exception thrown when a heuristic cannot be applied to a given context.
 */
public class HeuristicNotApplicableException extends RuntimeException {
  public HeuristicNotApplicableException(String message) {
    super(message);
  }
}