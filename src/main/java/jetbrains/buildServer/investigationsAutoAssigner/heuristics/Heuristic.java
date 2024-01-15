

package jetbrains.buildServer.investigationsAutoAssigner.heuristics;

import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.processing.HeuristicContext;
import org.jetbrains.annotations.NotNull;

/**
 * Presents heuristic that try to detect which person is probably responsible.
 * Order of provided heuristics should be specified in the spring xml-config file.
 */
public interface Heuristic {

  /**
   * @return short user-readable id of the heuristic. Should be suitable for part of internal property.
   */
  @NotNull
  String getId();

  /**
   * Try to detect which person is probably responsible.
   * @param heuristicContext {@link HeuristicContext} object which presents known information about the problem.
   */
  @NotNull
  HeuristicResult findResponsibleUser(@NotNull HeuristicContext heuristicContext);
}