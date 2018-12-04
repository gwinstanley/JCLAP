package snaq.util.jclap;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * Implementation of an {@code Option} with value of type {@code Float}.
 *
 * @author Giles Winstanley
 */
public class FloatOption extends Option<Float>
{
  /**
   * Creates a new {@code FloatOption} instance.
   *
   * @param shortName short name of the option (e.g. -?)
   * @param longName long name of the option (e.g. --help)
   * @param description helpful description of the option (printed for usage message)
   * @param minCount minimum number of occurrences required for this option
   * @param maxCount maximum number of occurrences required for this option
   */
  public FloatOption(String shortName, String longName, String description,
    int minCount, int maxCount)
  {
    super(shortName, longName, description, true, minCount, maxCount);
  }

  /**
   * Creates a new {@code FloatOption} instance.
   *
   * @param shortName short name of the option (e.g. -?)
   * @param longName long name of the option (e.g. --help)
   * @param description helpful description of the option (printed for usage message)
   * @param mandatory whether this option must be specified
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   */
  public FloatOption(String shortName, String longName, String description,
    boolean mandatory, boolean allowMany)
  {
    super(shortName, longName, description, true, mandatory, allowMany);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Float parseValue(String arg, Locale locale) throws OptionException
  {
    try
    {
      final NumberFormat format = NumberFormat.getNumberInstance(LOCALE);
      final Number num = format.parse(arg);
      return num.floatValue();
    }
    catch (ParseException px)
    {
      throw new OptionException(OptionException.Type.ILLEGAL_OPTION_VALUE, this, arg).withCause(px);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<Float> getType()
  {
    return Float.class;
  }
}
