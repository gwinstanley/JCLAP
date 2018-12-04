package snaq.util.jclap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * Base implementation of a command-line option.
 *
 * @param <E> the return type of the option
 *
 * @author Giles Winstanley
 */
public abstract class Option<E>
{
  /** Regular expression for valid long names. */
  static final String REGEX_SHORT_NAME = "[A-Za-z\\d@?]";
  /** Regular expression for valid long names. */
  static final String REGEX_LONG_NAME = "[A-Za-z\\d][A-Za-z\\d-]*[A-Za-z\\d]";
  /** Limit to minimum number of occurrences required for this option. */
  protected static final int MIN_COUNT_LIMIT = 0;
  /** Limit to maximum number of occurrences required for this option (set to an arbitrary 100). */
  protected static final int MAX_COUNT_LIMIT = 100;
  /** {@code Locale} for help/error display. */
  protected static final Locale LOCALE;
  /** Resources for internationalization. */
  protected static final ResourceBundle BUNDLE;
  /** Short version of the option flag. */
  private String shortName;
  /** Long version of the option flag. */
  private String longName;
  /** Description of the option flag. */
  private String description;
  /** Flag to indicate whether the option requires a value to be specified. */
  private boolean requiresValue = false;
  /** Minimum number of occurrences required for this option. */
  private int minCount = MIN_COUNT_LIMIT;
  /** Maximum number of occurrences required for this option. */
  private int maxCount = MAX_COUNT_LIMIT;
  /** List of values for this option. */
  private final List<E> values = new ArrayList<>();
  /** Flag to indicate whether the option should be omitted from the usage message. */
  private boolean hideFromUsage = false;

  static
  {
    LOCALE = CLAParser.getDefaultLocale();
    BUNDLE = CLAParser.getDefaultResources();
  }

  /**
   * Creates a new {@code Option} instance.
   *
   * @param shortName short name of the option (must be non-null; e.g. -?)
   * @param longName long name of the option (e.g. --help)
   * @param description helpful description of the option (printed for usage message)
   * @param requiresValue whether this option requires a subsequently-specified value
   * @param minCount minimum number of occurrences required for this option
   * @param maxCount maximum number of occurrences required for this option
   */
  protected Option(String shortName, String longName,
    String description, boolean requiresValue, int minCount, int maxCount)
  {
    // Perform different validation for regular Option compared to NonOptionArg.
    if (shortName == null)
      throw new IllegalArgumentException(BUNDLE.getString("err.NullShortName"));
    if (!shortName.matches(REGEX_SHORT_NAME))
      throw new IllegalArgumentException(BUNDLE.getString("err.BadShortName"));
    if (longName != null && !longName.matches(REGEX_LONG_NAME))
      throw new IllegalArgumentException(BUNDLE.getString("err.BadLongName"));
    if (minCount < MIN_COUNT_LIMIT)
      throw new IllegalArgumentException(BUNDLE.getString("err.InvalidMinCount"));
    if (maxCount < minCount || maxCount > MAX_COUNT_LIMIT)
      throw new IllegalArgumentException(BUNDLE.getString("err.InvalidMaxCount"));

    this.shortName = shortName;
    this.longName = longName;
    this.description = description;
    this.requiresValue = requiresValue;
    this.minCount = minCount;
    this.maxCount = maxCount;
  }

  /**
   * Creates a new {@code Option} instance.
   *
   * @param shortName short name of the option (must be non-null; e.g. -?)
   * @param longName long name of the option (e.g. --help)
   * @param description helpful description of the option (printed for usage message)
   * @param requiresValue whether this option requires a subsequently-specified value
   * @param mandatory whether this option must be specified
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   */
  protected Option(String shortName, String longName,
    String description, boolean requiresValue, boolean mandatory, boolean allowMany)
  {
    this(shortName, longName, description, requiresValue, mandatory ? 1 : 0, allowMany ? MAX_COUNT_LIMIT : 1);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final Option<?> other = (Option<?>)obj;
    if (this.requiresValue != other.requiresValue)
      return false;
    if (this.minCount != other.minCount)
      return false;
    if (this.maxCount != other.maxCount)
      return false;
    if (this.hideFromUsage != other.hideFromUsage)
      return false;
    if (!Objects.equals(this.shortName, other.shortName))
      return false;
    if (!Objects.equals(this.longName, other.longName))
      return false;
    if (!Objects.equals(this.description, other.description))
      return false;
    if (!Objects.equals(this.values, other.values))
      return false;
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode()
  {
    int hash = 5;
    hash = 97 * hash + Objects.hashCode(this.shortName);
    hash = 97 * hash + Objects.hashCode(this.longName);
    hash = 97 * hash + Objects.hashCode(this.description);
    hash = 97 * hash + (this.requiresValue ? 1 : 0);
    hash = 97 * hash + this.minCount;
    hash = 97 * hash + this.maxCount;
    hash = 97 * hash + Objects.hashCode(this.values);
    hash = 97 * hash + (this.hideFromUsage ? 1 : 0);
    return hash;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("[-");
    sb.append(shortName);
    if (longName != null)
    {
      sb.append(",--");
      sb.append(longName);
    }
    sb.append(",");
    sb.append(getType().getName());
    sb.append("]");
    return sb.toString();
  }

  /**
   * Returns the class type of value this option can take.
   *
   * @return Class instance
   */
  public abstract Class<E> getType();

  /**
   * Returns the option type, for display in the usage message.
   * This may be overridden by sub-classes to provide a more meaningful type.
   * By default this returns an appropriate type for the primitives,
   * and string type for all others.
   *
   * @param res {@code ResourceBundle} to use for collating usage text
   * @return string describing type
   */
  String getUsageTypeString(ResourceBundle res)
  {
    Objects.requireNonNull(res);
    String valType = getType().getName();  // e.g. "java.lang.Integer"
    valType = valType.substring(valType.lastIndexOf('.') + 1);  // e.g. "Integer"

    String usageType = null;
    try
    {
      usageType = res.getString("option.type." + valType);
    }
    catch (MissingResourceException mrx)
    {
      usageType = res.getString("option.type.unknown");
    }
    return usageType;
  }

  /**
   * @return list of parsed values
   */
  public List<E> getValues()
  {
    return Collections.unmodifiableList(values);
  }

  /**
   * Adds the specified value to the list of those parsed.
   *
   * @param value the value to add to this option
   * @throws OptionException if a single-value option already has a value
   */
  protected void addValue(E value) throws OptionException
  {
    if (maxCount == 0)
      throw new OptionException(OptionException.Type.ILLEGAL_OPTION_VALUE, this, value.toString());
    if (maxCount == 1 && !values.isEmpty())
      throw new OptionException(OptionException.Type.OPTION_HAS_VALUE, this, value.toString());
    if (values.size() >= maxCount)
      throw new OptionException(OptionException.Type.INVALID_OPTION_VALUE_COUNT, this, value.toString());
    values.add(value);
  }

  /**
   * Parses the argument string for an option value, optionally using the
   * specified locale for reference (e.g. for date parsing).
   *
   * @param arg string argument from which a value is to be parsed
   * @param locale locale as specified when initializing the {@code CLAParser} instance
   * @throws OptionException if a problem occurs while parsing the argument string
   * @return Value of the parsed argument string
   */
  protected abstract E parseValue(String arg, Locale locale) throws OptionException;

  /**
   * @return short name of this option
   */
  public String getShortName() { return shortName; }

  /**
   * @return long name of this option
   */
  public String getLongName()
  {
    return longName;
  }

  /**
   * @return description text of this option
   */
  public String getDescription()
  {
    return description;
  }

  /**
   * @return whether this option requires a value
   */
  public boolean requiresValue()
  {
    return requiresValue;
  }

  /**
   * @return minimum acceptable value count for this option
   */
  public int getMinCount()
  {
    return minCount;
  }

  /**
   * @return maximum acceptable value count for this option
   */
  public int getMaxCount()
  {
    return maxCount;
  }

  /**
   * @return whether this option is mandatory
   */
  public boolean isMandatory()
  {
    return minCount > 0;
  }

  /**
   * @return whether this option may have multiple values set
   */
  public boolean isAllowMany()
  {
    return maxCount > 1;
  }

  /**
   * Sets the minimum/maximum value counts for this option.
   *
   * @param minCount minimum count for the option
   * @param maxCount maximum count for the option
   */
  public void setMinMaxCounts(int minCount, int maxCount)
  {
    if (minCount < MIN_COUNT_LIMIT)
      throw new IllegalArgumentException(BUNDLE.getString("err.InvalidMinCount"));
    if (maxCount < minCount || maxCount > MAX_COUNT_LIMIT)
      throw new IllegalArgumentException(BUNDLE.getString("err.InvalidMaxCount"));
    this.minCount = minCount;
    this.maxCount = maxCount;
  }

  /**
   * Sets the flag to hide this option from the usage message.
   *
   * @return reference to option
   */
  public Option<E> setHidden()
  {
    hideFromUsage = true;
    return this;
  }

  /**
   * @return whether this option is hidden from the usage message
   */
  public boolean isHidden()
  {
    return hideFromUsage;
  }
}
