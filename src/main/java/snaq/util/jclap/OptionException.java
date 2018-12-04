package snaq.util.jclap;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Exception for command-line option parsing problems.
 *
 * @author Giles Winstanley
 */
public final class OptionException extends Exception
{
  /** Defines the types of {@code OptionException} that exist. */
  public enum Type {
    // === INCORRECT API USAGE MESSAGES ===
    /** Thrown for trying to retrieve an option without a valid type specified. */
    INVALID_OPTION_TYPE,
    /** Thrown for trying to retrieve single value for multiple value option. */
    INVALID_RETRIEVAL_TYPE,
    // === INCORRECT USAGE MESSAGES ===
    /** Thrown for unrecognized option. */
    UNKNOWN_OPTION,
    /** Thrown for unknown flag in concatenated short options. */
    UNKNOWN_FLAG,
    /** Thrown for value-requiring option in concatenated short options. */
    NOT_FLAG,
    /** Thrown for illegal/missing value in value-requiring option. */
    ILLEGAL_OPTION_VALUE,
    /** Thrown for invalid option value count. */
    INVALID_OPTION_COUNT,
    /** Thrown for trying to reassign value to single-value option. */
    OPTION_HAS_VALUE,
    /** Thrown for trying to assign an invalid number of value to option. */
    INVALID_OPTION_VALUE_COUNT,
  };
  /** {@code Locale} for help/error display. */
  protected static final Locale LOCALE;
  /** Resources for internationalization. */
  protected static final ResourceBundle BUNDLE;
  /** Defines the types of {@code OptionException} that exist. */
  private final Type type;
  /** {@code Option} instance causing exception. */
  private Option option = null;
  /** Option name causing exception. */
  private String optionName = null;
  /** Option value causing exception (as a string). */
  private String optionValue = null;
  /** Flag character causing exception. */
  private String flag = null;

  static
  {
    LOCALE = CLAParser.getDefaultLocale();
    BUNDLE = CLAParser.getDefaultResources();
  }

  /**
   * Creates a new {@code OptionException} instance.
   *
   * @param type type of {@code OptionException} to create (i.e. reason)
   * @param option {@code Option} instance for which this exception is being created
   * @param optionValue value of the option for which this exception is being created
   */
  public OptionException(Type type, Option option, String optionValue)
  {
    this(type, option);
    this.optionValue = optionValue;
  }

  /**
   * Creates a new {@code OptionException} instance.
   *
   * @param type type of {@code OptionException} to create (i.e. reason)
   * @param optionName name of the option for which this exception is being created
   * @param optionValue value of the option for which this exception is being created
   */
  public OptionException(Type type, String optionName, String optionValue)
  {
    this(type, optionName);
    this.optionValue = optionValue;
  }

  /**
   * Creates a new {@code OptionException} instance.
   *
   * @param type type of {@code OptionException} to create (i.e. reason)
   * @param optionName name of the option for which this exception is being created
   * @param flag flag character for which this exception is being created
   */
  public OptionException(Type type, String optionName, char flag)
  {
    this(type, optionName);
    this.flag = new String(new char[]{flag});
  }

  /**
   * Creates a new {@code OptionException} instance.
   *
   * @param type type of {@code OptionException} to create (i.e. reason)
   * @param option {@code Option} instance for which this exception is being created
   */
  public OptionException(Type type, Option option)
  {
    super();
    this.type = type;
    this.option = option;
  }

  /**
   * Creates a new {@code OptionException} instance.
   *
   * @param type type of {@code OptionException} to create (i.e. reason)
   * @param optionName name of the option for which this exception is being created
   */
  public OptionException(Type type, String optionName)
  {
    super();
    this.type = type;
    this.optionName = optionName;
  }

  /**
   * Initializes the cause of this {@code OptionException}, and returns self reference.
   *
   * @param cause cause of exception
   * @return this {@code OptionException} instance
   */
  OptionException withCause(Throwable cause)
  {
    this.initCause(cause);
    return this;
  }

  /**
   * @return exception type
   */
  public Type getType()
  {
    return type;
  }

  @Override
  public String getMessage()
  {
    return getLocalizedMessage(BUNDLE);
  }

  @Override
  public String getLocalizedMessage()
  {
    final ResourceBundle res = ResourceBundle.getBundle(CLAParser.class.getName(), Locale.getDefault());
    return getLocalizedMessage(res);
  }

  private String getLocalizedMessage(ResourceBundle rb)
  {
    String msg = null;
    Object[] args = null;
    String optionStr = optionName;
    if (option != null)
    {
      if (option.getLongName() != null)
        optionStr = "-" + option.getShortName() + ",--" + option.getLongName();
      else
        optionStr = "-" + option.getShortName();
    }

    switch (type)
    {
      // Incorrect API usage messages.
      case INVALID_OPTION_TYPE:
        msg = rb.getString("err.InvalidOptionType");
        args = new Object[] {optionStr};
        break;
      case INVALID_RETRIEVAL_TYPE:
        msg = rb.getString("err.InvalidRetrievalType");
        args = new Object[] {optionStr};
        break;
      // Incorrect usage messages.
      case UNKNOWN_OPTION:
        msg = rb.getString("err.UnknownOption");
        args = new Object[] {optionStr};
        break;
      case UNKNOWN_FLAG:
        msg = rb.getString("err.UnknownFlag");
        args = new Object[] {optionStr, flag};
        break;
      case NOT_FLAG:
        msg = rb.getString("err.NotFlag");
        args = new Object[] {optionStr, flag};
        break;
      case ILLEGAL_OPTION_VALUE:
        msg = rb.getString("err.IllegalOptionValue");
        args = new Object[] {optionStr, optionValue};
        break;
      case INVALID_OPTION_COUNT:
        msg = rb.getString("err.InvalidOptionValueCount");
        args = new Object[] {optionStr, optionValue};
        break;
      case OPTION_HAS_VALUE:
        msg = rb.getString("err.OptionHasValue");
        args = new Object[] {optionStr, optionValue};
        break;
      case INVALID_OPTION_VALUE_COUNT:
        msg = rb.getString("err.InvalidOptionValueCount");
        args = new Object[] {optionStr, optionValue, option.getMinCount(), option.getMaxCount()};
        break;
      default:
        msg = rb.getString("err.Generic");
        args = new Object[] {optionStr};
    }
    return MessageFormat.format(msg, args);
  }
}
