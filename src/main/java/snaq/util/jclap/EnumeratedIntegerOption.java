package snaq.util.jclap;

import java.util.Collection;
import java.util.Locale;

/**
 * Implementation of an {@code Option} with value restricted to a
 * specified enumeration of type {@code Integer}.
 *
 * @author Giles Winstanley
 */
public final class EnumeratedIntegerOption extends EnumeratedOption<Integer>
{
  /**
   * Creates a new {@code EnumeratedIntegerOption} instance.
   *
   * @param shortName short name of the option (e.g. -t)
   * @param longName long name of the option (e.g. --type)
   * @param description helpful description of the option (printed for usage message)
   * @param minCount minimum number of occurrences required for this option
   * @param maxCount maximum number of occurrences required for this option
   * @param allowedValues collection of possible values this option can take
   */
  public EnumeratedIntegerOption(String shortName, String longName,
    String description, int minCount, int maxCount,
    Collection<Integer> allowedValues)
  {
    super(shortName, longName, description, minCount, maxCount, allowedValues);
  }

  /**
   * Creates a new {@code EnumeratedIntegerOption} instance.
   *
   * @param shortName short name of the option (e.g. -t)
   * @param longName long name of the option (e.g. --type)
   * @param description helpful description of the option (printed for usage message)
   * @param mandatory whether this option must be specified
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   * @param allowedValues collection of possible values this option can take
   */
  public EnumeratedIntegerOption(String shortName, String longName,
    String description, boolean mandatory, boolean allowMany,
    Collection<Integer> allowedValues)
  {
    super(shortName, longName, description, mandatory, allowMany, allowedValues);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Integer parseValue(String arg, Locale locale) throws OptionException
  {
    try
    {
      final Integer val = Integer.valueOf(arg);
      for (Integer s : getAllowedValues())
      {
        if (s.equals(val))
          return val;
      }
      throw new OptionException(OptionException.Type.ILLEGAL_OPTION_VALUE, this, arg);
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
  public Class<Integer> getType()
  {
    return Integer.class;
  }
}
