package snaq.util.jclap;

import java.util.Locale;

/**
 * Implementation of an {@code Option} with value of type {@code Boolean}
 * (otherwise known as a &quot;flag&quot;).
 *
 * @author Giles Winstanley
 */
public final class BooleanOption extends Option<Boolean>
{
  private static final String[] VALS_TRUE = BUNDLE.getString("boolean.true").split(",");
  private static final String[] VALS_FALSE = BUNDLE.getString("boolean.false").split(",");

  /**
   * Creates a new {@code BooleanOption} instance.
   *
   * @param shortName short name of the option (e.g. -?)
   * @param longName long name of the option (e.g. --help)
   * @param description helpful description of the option (printed for usage message)
   * @param minCount minimum number of occurrences required for this option
   * @param maxCount maximum number of occurrences required for this option
   */
  public BooleanOption(String shortName, String longName, String description, int minCount, int maxCount)
  {
    super(shortName, longName, description, false, minCount, maxCount);
  }

  /**
   * Creates a new {@code BooleanOption} instance.
   *
   * @param shortName short name of the option (e.g. -?)
   * @param longName long name of the option (e.g. --help)
   * @param description helpful description of the option (printed for usage message)
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   */
  public BooleanOption(String shortName, String longName, String description, boolean allowMany)
  {
    super(shortName, longName, description, false, false, allowMany);
  }

  /**
   * Parsing of a flag option makes little sense.
   * Possible argument for case of -f=true / --flag=1 / etc.
   *
   * @param arg string argument from which a value is to be parsed
   * @throws OptionException if a problem occurs while parsing the argument string
   * @return Value of the parsed argument string
   */
  @Override
  protected Boolean parseValue(String arg, Locale locale) throws OptionException
  {
    // Included for completeness, but parsing arguments is redudant for a flag.
    if (arg == null)
      throw new OptionException(OptionException.Type.ILLEGAL_OPTION_VALUE, this, arg);
    final String s = arg.toLowerCase(locale).trim();
    for (String st : VALS_TRUE)
    {
      if (s.equalsIgnoreCase(st))
        return Boolean.TRUE;
    }
    for (String sf : VALS_FALSE)
    {
      if (s.equalsIgnoreCase(sf))
        return Boolean.FALSE;
    }
    // Throw exception if none of the above formats is used.
    throw new OptionException(OptionException.Type.ILLEGAL_OPTION_VALUE, this, arg);
  }

  @Override
  public Class<Boolean> getType()
  {
    return Boolean.class;
  }
}
