package snaq.util.jclap;

import java.util.Locale;

/**
 * Implementation of an {@code Option} with value of type {@code Long}.
 *
 * @author Giles Winstanley
 */
public class LongOption extends Option<Long>
{
  /**
   * Creates a new {@code LongOption} instance.
   *
   * @param shortName short name of the option (e.g. -?)
   * @param longName long name of the option (e.g. --help)
   * @param description helpful description of the option (printed for usage message)
   * @param minCount minimum number of occurrences required for this option
   * @param maxCount maximum number of occurrences required for this option
   */
  public LongOption(String shortName, String longName, String description,
    int minCount, int maxCount)
  {
    super(shortName, longName, description, true, minCount, maxCount);
  }

  /**
   * Creates a new {@code LongOption} instance.
   *
   * @param shortName short name of the option (e.g. -?)
   * @param longName long name of the option (e.g. --help)
   * @param description helpful description of the option (printed for usage message)
   * @param mandatory whether this option must be specified
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   */
  public LongOption(String shortName, String longName, String description,
    boolean mandatory, boolean allowMany)
  {
    super(shortName, longName, description, true, mandatory, allowMany);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Long parseValue(String arg, Locale locale) throws OptionException
  {
    try
    {
      return Long.valueOf(arg);
    }
    catch (NumberFormatException nfx)
    {
      throw new OptionException(OptionException.Type.ILLEGAL_OPTION_VALUE, this, arg).withCause(nfx);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<Long> getType()
  {
    return Long.class;
  }
}
