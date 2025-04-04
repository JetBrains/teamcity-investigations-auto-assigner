package jetbrains.buildServer.investigationsAutoAssigner.common;

import java.util.Arrays;
import jetbrains.buildServer.users.User;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the responsibility assignment for a user, including the user and their description.
 * <p>
 * Key methods include:
 * <ul>
 *     <li>{@link #getUser()} - Retrieves the user assigned the responsibility.</li>
 *     <li>{@link #getDescription()} - Retrieves the description of the responsibility.</li>
 *     <li>{@link #getAssignDescription(String)} - Returns a formatted string describing the assignment.</li>
 *     <li>{@link #equals(Object)} - Compares this responsibility to another object for equality.</li>
 *     <li>{@link #hashCode()} - Returns the hash code for the responsibility.</li>
 * </ul>
 */
public class Responsibility {
  private final User user;
  private final String description;

  /**
   * Constructs a new Responsibility object with the given user and description.
   *
   * @param user        the user assigned the responsibility
   * @param description the description of the responsibility
   */
  public Responsibility(@NotNull User user, @NotNull String description) {
    this.user = user;
    this.description = description;
  }

  /**
   * Retrieves the user assigned the responsibility.
   *
   * @return the user assigned the responsibility
   */
  @NotNull
  public User getUser() {
    return this.user;
  }

  /**
   * Retrieves the description of the responsibility.
   *
   * @return the description of the responsibility
   */
  @NotNull
  public String getDescription() {
    return this.description;
  }

  /**
   * Returns a formatted string describing the assignment, including the user's name,
   * the responsibility description, and a link to the build.
   *
   * @param linkToBuild the link to the build associated with the responsibility
   * @return a formatted string describing the responsibility assignment
   */
  public String getAssignDescription(String linkToBuild) {
    return String.format("%s %s who %s (initial build: %s).", Constants.ASSIGN_DESCRIPTION_PREFIX,
                         this.user.getDescriptiveName(), this.description, linkToBuild);
  }

  /**
   * Compares this responsibility to another object for equality.
   * Two responsibilities are equal if they have the same user and description.
   *
   * @param another the object to compare this responsibility to
   * @return true if the objects are equal, false otherwise
   */
  @Override
  public boolean equals(final Object another) {
    if (this == another) return true;  // Early return if it's the same object
    if (!(another instanceof Responsibility)) {
      return false;
    }

    Responsibility anotherResponsibility = (Responsibility)another;
    return this.user.getId() == anotherResponsibility.getUser().getId() &&
           this.description.equals(anotherResponsibility.getDescription());
  }

  /**
   * Returns the hash code for the responsibility.
   * The hash code is based on the user's ID and the description.
   *
   * @return the hash code for the responsibility
   */
  @Override
  public int hashCode() {
    int result = Arrays.hashCode(new Object[]{this.user.getId(), this.description});
    return 31 * result; // Apply a prime multiplier for better distribution
  }
}
