package snaq.util.jclap;

import java.util.Locale;
import java.util.Optional;

/**
 * Implementation of an {@code Option} with value of type {@code String}.
 *
 * @author Giles Winstanley
 */
public class StringOption extends Option<String>
{
  /** Filter instance to use for determining whether to accept argument. */
  private Filter filter;

  /**
   * Creates a new {@code StringOption} instance.
   *
   * @param shortName short name of the option (e.g. -n)
   * @param longName long name of the option (e.g. --name)
   * @param description helpful description of the option (printed for usage message)
   * @param minCount minimum number of occurrences required for this option
   * @param maxCount maximum number of occurrences required for this option
   */
  public StringOption(String shortName, String longName, String description,
    int minCount, int maxCount)
  {
    super(shortName, longName, description, true, minCount, maxCount);
  }

  /**
   * Creates a new {@code StringOption} instance.
   *
   * @param shortName short name of the option (e.g. -n)
   * @param longName long name of the option (e.g. --name)
   * @param description helpful description of the option (printed for usage message)
   * @param mandatory whether this option must be specified
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   */
  public StringOption(String shortName, String longName, String description,
    boolean mandatory, boolean allowMany)
  {
    super(shortName, longName, description, true, mandatory, allowMany);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String parseValue(String arg, Locale locale) throws OptionException
  {
    if (filter == null)
      return arg;
    if (filter.accept(arg))
      return arg;
    throw new OptionException(OptionException.Type.ILLEGAL_OPTION_VALUE, this, arg);
  }

  /**
   * Sets the {@code StringOption.Filter} for this instance.
   *
   * @param filter {@code Filter} instance to use for accepting/rejecting values
   */
  public void setFilter(Filter filter)
  {
    this.filter = filter;
  }

  /**
   * Returns the {@code StringOption.Filter} for this instance.
   *
   * @return optional Filter instance
   */
  public Optional<Filter> getFilter()
  {
    return Optional.ofNullable(this.filter);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<String> getType()
  {
    return String.class;
  }

  /**
   * Acceptance filter for {@code StringOption} instances.
   */
  @FunctionalInterface
  public interface Filter
  {
    /**
     * Determines whether the specified argument should be accepted for
     * use by the host {@code StringOption} instance.
     *
     * @param arg string argument to assess for acceptance
     * @return true if the argument should be accepted, false otherwise
     */
    boolean accept(String arg);
  }
}
