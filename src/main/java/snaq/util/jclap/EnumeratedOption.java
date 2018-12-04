package snaq.util.jclap;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;

import static java.util.stream.Collectors.joining;

/**
 * Implementation of an {@code Option} with value restricted to an
 * enumeration of a specified return value type.
 * @param <E> the return type of the option
 *
 * @author Giles Winstanley
 */
public abstract class EnumeratedOption<E> extends Option<E>
{
  /** Collection of allowed values for this enumeration. */
  private final Collection<E> allowedValues;

  /**
   * Creates a new {@code EnumeratedOption} instance.
   *
   * @param shortName short name of the option (e.g. -t)
   * @param longName long name of the option (e.g. --type)
   * @param description helpful description of the option (printed for usage message)
   * @param minCount minimum number of occurrences required for this option
   * @param maxCount maximum number of occurrences required for this option
   * @param allowedValues collection of possible values this option can take
   */
  public EnumeratedOption(String shortName, String longName,
    String description, int minCount, int maxCount,
    Collection<E> allowedValues)
  {
    super(shortName, longName, description, true, minCount, maxCount);
    this.allowedValues = allowedValues;
  }

  /**
   * Creates a new {@code EnumeratedOption} instance.
   *
   * @param shortName short name of the option (e.g. -t)
   * @param longName long name of the option (e.g. --type)
   * @param description helpful description of the option (printed for usage message)
   * @param mandatory whether this option must be specified
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   * @param allowedValues collection of possible values this option can take
   */
  public EnumeratedOption(String shortName, String longName,
    String description, boolean mandatory, boolean allowMany,
    Collection<E> allowedValues)
  {
    super(shortName, longName, description, true, mandatory, allowMany);
    this.allowedValues = allowedValues;
  }

  /**
   * @param value value to check for validity
   * @param locale Locale to use for i18n (if needed)
   * @return Whether the specified value is valid.
   */
  public boolean isValueValid(E value, Locale locale)
  {
    for (E av : allowedValues)
    {
      if (av.equals(value))
        return true;
    }
    return false;
  }

  /**
   * Returns an unmodifiable collection of the values that can be
   * assigned to this option.
   *
   * @return collection of allowed option values
   */
  public Collection<E> getAllowedValues()
  {
    return Collections.unmodifiableCollection(allowedValues);
  }

  /**
   * Returns a string denoting the values that can be assigned to this option.
   *
   * @param prefix prefix string for each allowed value
   * @param suffix suffix string for each allowed value
   * @param separator string to use for delimiting individual values
   * @return A string denoting the values that can be assigned to this option.
   */
  public String getAllowedValuesString(String prefix, String suffix, final String separator)
  {
    return allowedValues.stream().map(s -> prefix + s + suffix).collect(joining(separator));
  }

  /**
   * @return The default version of the string denoting the values that
   * can be assigned to this option.
   */
  public String getAllowedValuesString()
  {
    return getAllowedValuesString("", "", ", ");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("[-");
    sb.append(getShortName());
    if (getLongName() != null)
    {
      sb.append(",--");
      sb.append(getLongName());
    }
    sb.append(",");
    sb.append(getType().getName());
    sb.append(",{");
    for (Iterator<E> iter = allowedValues.iterator(); iter.hasNext();)
    {
      sb.append(iter.next());
      if (iter.hasNext())
        sb.append(' ');
    }
    sb.append("}]");
    return sb.toString();
  }
}
