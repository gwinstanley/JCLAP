package snaq.util.jclap;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static java.util.stream.Collectors.toList;

/**
 * Implementation of an {@code Option} with value restricted to a
 * specified enumeration of type {@code String}.
 *
 * @author Giles Winstanley
 */
public final class EnumeratedStringOption extends EnumeratedOption<String>
{
  private boolean ignoreCase = true;

  /**
   * Creates a new {@code EnumeratedStringOption} instance.
   *
   * @param shortName short name of the option (e.g. -t)
   * @param longName long name of the option (e.g. --type)
   * @param description helpful description of the option (printed for usage message)
   * @param minCount minimum number of occurrences required for this option
   * @param maxCount maximum number of occurrences required for this option
   * @param allowedValues collection of possible values this option can take
   * @param ignoreCase whether to ignore the case in string evaluations
   */
  public EnumeratedStringOption(String shortName, String longName,
    String description, int minCount, int maxCount,
    Collection<String> allowedValues, boolean ignoreCase)
  {
    super(shortName, longName, description, minCount, maxCount, allowedValues);
    this.ignoreCase = ignoreCase;
  }

  /**
   * Creates a new {@code EnumeratedStringOption} instance.
   *
   * @param shortName short name of the option (e.g. -t)
   * @param longName long name of the option (e.g. --type)
   * @param description helpful description of the option (printed for usage message)
   * @param mandatory whether this option must be specified
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   * @param allowedValues collection of possible values this option can take
   * @param ignoreCase whether to ignore the case in string evaluations
   */
  public EnumeratedStringOption(String shortName, String longName,
    String description, boolean mandatory, boolean allowMany,
    Collection<String> allowedValues, boolean ignoreCase)
  {
    super(shortName, longName, description, mandatory, allowMany, allowedValues);
    this.ignoreCase = ignoreCase;
  }

  @Override
  public boolean isValueValid(String value, Locale locale)
  {
    try
    {
      return parseValue(value, locale) != null;
    }
    catch (OptionException ex)
    {
      return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String parseValue(String arg, Locale locale) throws OptionException
  {
    // Check if option value can be isolated by lowercase substring match.
    List<String> vals = getAllowedValues().stream()
            .filter(x -> x.toLowerCase(locale).contains(arg))
            .collect(toList());
    if (vals.isEmpty())
      throw new OptionException(OptionException.Type.ILLEGAL_OPTION_VALUE, this, arg);
    if (vals.size() > 1)
    {
      // Check if option value can be isolated by case-sensitive substring match.
      vals = vals.stream()
            .filter(x -> x.contains(arg))
            .collect(toList());
      if (vals.isEmpty() || vals.size() > 1)
        throw new OptionException(OptionException.Type.ILLEGAL_OPTION_VALUE, this, arg);
    }
    return vals.get(0);
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
   * Returns the default version of the string denoting the values that
   * can be assigned to this option.
   *
   * @return comma-separated quoted string of allowed values
   */
  @Override
  public String getAllowedValuesString()
  {
    return getAllowedValuesString("\"", "\"", ", ");
  }
}
