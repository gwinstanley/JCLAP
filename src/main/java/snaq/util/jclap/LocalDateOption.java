package snaq.util.jclap;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Option implementation for specifying date values.
 *
 * @author Giles Winstanley
 */
public class LocalDateOption extends Option<LocalDate>
{
  /** Custom {@code DateTimeFormatter}. */
  private DateTimeFormatter dtf = null;

  /**
   * Creates a new {@code LocalDateOption} instance.
   *
   * @param shortName short name of the option (e.g. -t)
   * @param longName long name of the option (e.g. --type)
   * @param description helpful description of the option (printed for usage message)
   * @param mandatory whether this option must be specified
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   */
  public LocalDateOption(String shortName, String longName, String description, boolean mandatory, boolean allowMany)
  {
    super(shortName, longName, description, true, mandatory, allowMany);
  }

  /**
   * Creates a new {@code LocalDateOption} instance.
   *
   * @param shortName short name of the option (e.g. -t)
   * @param longName long name of the option (e.g. --type)
   * @param description helpful description of the option (printed for usage message)
   * @param minCount minimum number of occurrences required for this option
   * @param maxCount maximum number of occurrences required for this option
   */
  public LocalDateOption(String shortName, String longName, String description, int minCount, int maxCount)
  {
    super(shortName, longName, description, true, minCount, maxCount);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<LocalDate> getType()
  {
    return LocalDate.class;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected LocalDate parseValue(String arg, Locale locale) throws OptionException
  {
    if (arg == null)
      throw new OptionException(OptionException.Type.ILLEGAL_OPTION_VALUE, this, arg);
    try
    {
      final DateTimeFormatter df = (dtf != null) ?
              dtf.withLocale(locale) : DateTimeFormatter.ISO_LOCAL_DATE.withLocale(locale);
      return LocalDate.parse(arg, df);
    }
    catch (DateTimeParseException px)
    {
      throw new OptionException(OptionException.Type.ILLEGAL_OPTION_VALUE, this).withCause(px);
    }
  }

  /**
   * Sets the {@code DateTimeFormatter} instance for {@code LocalDate} parsing/display.
   *
   * @param dtf {@code DateTimeFormatter} instance to use
   */
  public void setDateFormat(DateTimeFormatter dtf)
  {
    this.dtf = dtf;
  }

  /**
   * @param locale locale for {@code DateFormatter} instance
   * @return The {@code DateFormatter} instance used to parse dates.
   */
  public DateTimeFormatter getDateFormat(Locale locale)
  {
    if (dtf != null)
      return dtf.withLocale(locale);
    return DateTimeFormatter.ISO_LOCAL_DATE.withLocale(locale);
  }
}
