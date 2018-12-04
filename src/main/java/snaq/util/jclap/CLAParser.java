package snaq.util.jclap;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.joining;
import static snaq.util.jclap.Option.REGEX_LONG_NAME;
import static snaq.util.jclap.Option.REGEX_SHORT_NAME;
import static snaq.util.jclap.OptionException.Type.*;

/**
 * Utility class to provide support for command-line options parsing.
 * <p>
 * Supports both POSIX-style short-form (e.g.&nbsp;{@code -v}) and GNU-style
 * long-form (e.g.&nbsp;{@code --verbose}) options.
 * The short-form is mandatory, and the long-form optional.
 * <p>
 * Processing of options can be explicitly terminated by the argument
 * &quot;{@code --}&quot;, to allow (non-option) arguments to be specified.
 * <p>
 * All options must have at least a short name, and are recommended to also
 * have a long name.
 * Options that require a value have the value specified immediately after the
 * option name on the command-line.
 * <p>
 * For example, an option to display a help message might be specified
 * with the short name {@code ?} and the long name {@code help}
 * (shown as {@code [-?,--help]} in the long usage message).
 * An option for a size value might be specified as {@code [-s,--size]}
 * and require a value parameter, which may be specified in any of these forms:
 * <ul>
 * <li>{@code -s 123}</li>
 * <li>{@code -s=123}</li>
 * <li>{@code -s:123}</li>
 * <li>{@code /s 123}</li>
 * <li>{@code /s=123}</li>
 * <li>{@code /s:123}</li>
 * <li>{@code --size 123}</li>
 * <li>{@code --size=123}</li>
 * <li>{@code --size:123}</li>
 * </ul>
 * <p>
 * There is also support for automatic usage message based on the configured
 * options. For example, an instance which has three options configured
 * {@code { w, h, ? }} with long-names {@code { width, height, help }} and
 * only {@code w} being mandatory, might have a short usage message as follows:
 * <pre>
 *     java AppName -w &lt;integer&gt; [-h &lt;integer&gt;] [-?]
 * </pre>
 * and a long usage message as follows:
 * <pre>
 *     java AppName &lt;options&gt;
 *     Options:
 *             -w,--width &lt;integer&gt;    (mandatory)
 *                     Width of image.
 *
 *             [-h,--height &lt;integer&gt;]
 *                     Height of image.
 *
 *             [-?,--help]
 *                     Display help information.
 * </pre>
 * <p>
 * Here's a summary of JCLAP features:
 * <ul>
 * <li>Option short names are mandatory, and long names optional
 * (as per POSIX standard, but retaining GNU enhancements).</li>
 * <li>Supports enumerated option types (String, Integer).</li>
 * <li>Supports filtered string option types.</li>
 * <li>Supports file/folder option types (using filtered-string feature).</li>
 * <li>Support for automatic usage printing (both long &amp; short styles).</li>
 * <li>Supports custom application launch text for automatic usage.</li>
 * <li>Supports non-option argument text for automatic usage.</li>
 * <li>Supports custom extra info text for automatic usage.</li>
 * <li>Uses a single {@code OptionException} type for signalling problems.</li>
 * <li>Supports query for single-hyphen argument to facilitate reading from STDIN.</li>
 * <li>Internationalization support.</li>
 * </ul>
 * <p>
 * Automatic usage printing can be accessed via the {@code printUsage(...)}
 * method, allowing printing to a specified {@code PrintStream} in either long
 * or short versions.
 * The options are printed in the same order in which they were added to the
 * {@code CLAParser} instance. The command used to launch the application
 * can either be user-specified, or can be deduced automatically if required.
 * Internationalization support is included for a few locales
 * (en/fr/de/es/pt), and may be extended in future. If required users may
 * author their own locale property files to reference.
 * <p>
 * For an example of how to use the CLAParser class, visit the website.
 *
 * @see <a href="https://www.snaq.net/" target="_blank">JCLAP website</a>
 * @author Giles Winstanley
 */
public final class CLAParser
{
  private static final boolean DEBUG = false;
  private static final int INIT_MAP_SIZE = 8;
  /** {@code Locale} for help/error display. */
  private static final Locale LOCALE;
  /** {@code Locale} for help/error display. */
  private final Locale locale;
  /** Resources for internationalisation. */
  private static final ResourceBundle BUNDLE;
  /** Resources for internationalisation. */
  private final ResourceBundle bundle;
  /** Flag determining whether to show long option names in short usage message. */
  private boolean showLongNamesInShortUsage = false;
  /**
   * List of options. While a map would seem to make more sense, using a list
   * adds insignificant overhead for the few elements used in this scenario,
   * and adds the benefit of easily maintaining the order in which the options
   * were specified.
   */
  private final List<Option> options = new ArrayList<>(INIT_MAP_SIZE);
  /** List of non-option arguments from the command-line. */
  private List<String> nonOptionArgs = null;

  static
  {
    LOCALE = Locale.getDefault();
    BUNDLE = ResourceBundle.getBundle(CLAParser.class.getName(), LOCALE);
  }

  /**
   * Creates a new {@code CLAParser} instance, using the default locale.
   * @param locale Locale to use for i18n resources
   */
  public CLAParser(Locale locale)
  {
    this.locale = locale;
    this.bundle = ResourceBundle.getBundle(CLAParser.class.getName(), locale);
  }

  /**
   * Creates a new {@code CLAParser} instance, using the default locale.
   */
  public CLAParser()
  {
    this(Locale.getDefault());
  }

  /** Returns the locale. */
  static Locale getDefaultLocale()
  {
    return LOCALE;
  }

  /** Returns the locale. */
  Locale getLocale()
  {
    return locale;
  }

  /** Returns the ResourceBundle for localization. */
  static ResourceBundle getDefaultResources()
  {
    return BUNDLE;
  }

  /** Returns the ResourceBundle for localization. */
  ResourceBundle getResources()
  {
    return bundle;
  }

  /**
   * Allows overriding default behaviour of only showing short option names
   * in the short automatic usage message. Once this method has been called,
   * any long option names are also shown (as in older versions).
   */
  public void showLongNamesInShortUsage()
  {
    this.showLongNamesInShortUsage = true;
  }

  /**
   * Returns whether a single {@code -} argument was found.
   * This is typically used to specify that an application should read from
   * STDIN or write to STDOUT. The argument is written to the non-option
   * arguments, so this can be tested independently, but this is just a
   * convenience method to do the same.
   * @return true if solitary hyphen was parsed, false otherwise
   */
  public boolean parsedSolitaryHyphen()
  {
    return nonOptionArgs.stream().anyMatch(x -> "-".equals(x));
  }

  /**
   * Adds the specified {@code Option} to the list of those to be parsed
   * from the command-line arguments.
   *
   * @param <T> the return type of the option
   * @param opt {@code Option} instance to add
   * @throws IllegalArgumentException if either short/long name of the specified
   *   {@code Option} is already in use
   * @return the {@code Option} instance that was added
   */
  public <T> Option<T> addOption(Option<T> opt)
  {
    Objects.requireNonNull(opt);
    // Check whether option's short/long name are already in use.
    for (Option o : options)
    {
      final String optSN = opt.getShortName();
      final String optLN = opt.getLongName();
      final String oLN = o.getLongName();
      if (o.getShortName().equals(optSN))
        throw new IllegalArgumentException(MessageFormat.format(BUNDLE.getString("err.OptionNameInUse"), optSN));
      if (oLN != null && optLN != null && oLN.equals(optLN))
        throw new IllegalArgumentException(MessageFormat.format(BUNDLE.getString("err.OptionNameInUse"), optLN));
    }
    // Add to collection and return.
    options.add(opt);
    return opt;
  }

  <T> boolean removeOption(Option<T> opt)
  {
    return options.remove(Objects.requireNonNull(opt));
  }

  <T> boolean removeOption(String optionName) throws OptionException
  {
    Option opt = getOptionByShortName(Objects.requireNonNull(optionName));
    if (opt == null)
      opt = getOptionByLongName(optionName);
    if (opt == null)
      throw new OptionException(UNKNOWN_OPTION, optionName);
    return options.remove(opt);
  }

  /**
   * Hides the specified {@code Option} from being printed in the usage message.
   *
   * @param optionName short/long name of option to &quot;hide&quot;
   * @throws OptionException if the specified option can't be found
   */
  public void setHidden(String optionName) throws OptionException
  {
    Option opt = getOptionByShortName(optionName);
    if (opt == null)
      opt = getOptionByLongName(optionName);
    if (opt == null)
      throw new OptionException(UNKNOWN_OPTION, optionName);
    opt.setHidden();
  }

  /**
   * Convenience method to add an {@code Option} of type {@code Boolean}.
   *
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @param minCount minimum number of occurrences required for this option
   * @param maxCount maximum number of occurrences required for this option
   * @return the new {@code Option} instance
   */
  public Option<Boolean> addBooleanOption(String shortName,
                                          String longName,
                                          String description,
                                          int minCount, int maxCount)
  {
    return addOption(new BooleanOption(shortName, longName, description, minCount, maxCount));
  }

  /**
   * Convenience method to add an {@code Option} of type {@code Boolean}.
   *
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   * @return the new {@code Option} instance
   */
  public Option<Boolean> addBooleanOption(String shortName,
                                          String longName,
                                          String description,
                                          boolean allowMany)
  {
    return addOption(new BooleanOption(shortName, longName, description, allowMany));
  }

  /**
   * Convenience method to add an {@code Option} of type {@code Boolean}.
   *
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @param maxCount maximum number of occurrences required for this option
   * @return the new {@code Option} instance
   */
  public Option<Boolean> addBooleanOption(String shortName, String longName, String description, int maxCount)
  {
    return addOption(new BooleanOption(shortName, longName, description, 0, maxCount));
  }

  /**
   * Convenience method to add an {@code Option} of type {@code Boolean}
   * (single value allowed).
   *
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @return the new {@code Option} instance
   */
  public Option<Boolean> addBooleanOption(String shortName, String longName, String description)
  {
    return addBooleanOption(shortName, longName, description, false);
  }

  /**
   * Convenience method to add an {@code Option} of type {@code Boolean}.
   *
   * @param shortName short name of the option
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   * @return the new {@code Option} instance
   */
  public Option<Boolean> addBooleanOption(String shortName, boolean allowMany)
  {
    return addOption(new BooleanOption(shortName, null, null, allowMany));
  }

  /**
   * Convenience method to add an {@code Option} of type {@code Boolean}.
   *
   * @param shortName short name of the option
   * @param maxCount maximum number of occurrences required for this option
   * @return the new {@code Option} instance
   */
  public Option<Boolean> addBooleanOption(String shortName, int maxCount)
  {
    return addOption(new BooleanOption(shortName, null, null, 0, maxCount));
  }

  /**
   * Convenience method to add an {@code Option} of type {@code Boolean}
   * (single value allowed).
   *
   * @param shortName short name of the option
   * @return the new {@code Option} instance
   */
  public Option<Boolean> addBooleanOption(String shortName)
  {
    return addBooleanOption(shortName, false);
  }

  /**
   * Convenience method to add an {@code Option} of type {@code Integer}.
   *
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @param mandatory whether this option must be specified
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   * @return the new {@code Option} instance
   */
  public Option<Integer> addIntegerOption(String shortName,
                                          String longName,
                                          String description,
                                          boolean mandatory,
                                          boolean allowMany)
  {
    return addOption(new IntegerOption(shortName, longName, description, mandatory, allowMany));
  }

  /**
   * Convenience method to add an {@code Option} of type {@code Integer}.
   *
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @param minCount minimum number of occurrences required for this option
   * @param maxCount maximum number of occurrences required for this option
   * @return the new {@code Option} instance
   */
  public Option<Integer> addIntegerOption(String shortName,
                                          String longName,
                                          String description,
                                          int minCount, int maxCount)
  {
    return addOption(new IntegerOption(shortName, longName, description, minCount, maxCount));
  }

  /**
   * Convenience method to add an {@code Option} of type {@code Integer}.
   *
   * @param shortName short name of the option
   * @param mandatory whether this option must be specified
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   * @return the new {@code Option} instance
   */
  public Option<Integer> addIntegerOption(String shortName, boolean mandatory, boolean allowMany)
  {
    return addOption(new IntegerOption(shortName, null, null, mandatory, allowMany));
  }

  /**
   * Convenience method to add an {@code Option} of type {@code Integer}.
   *
   * @param shortName short name of the option
   * @param minCount minimum number of occurrences required for this option
   * @param maxCount maximum number of occurrences required for this option
   * @return the new {@code Option} instance
   */
  public Option<Integer> addIntegerOption(String shortName, int minCount, int maxCount)
  {
    return addOption(new IntegerOption(shortName, null, null, minCount, maxCount));
  }

  /**
   * Convenience method to add an {@code Option} of type {@code Long}.
   *
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @param mandatory whether this option must be specified
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   * @return the new {@code Option} instance
   */
  public Option<Long> addLongOption(String shortName,
                                    String longName,
                                    String description,
                                    boolean mandatory, boolean allowMany)
  {
    return addOption(new LongOption(shortName, longName, description, mandatory, allowMany));
  }

  /**
   * Convenience method to add an {@code Option} of type {@code Long}.
   *
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @param minCount minimum number of occurrences required for this option
   * @param maxCount maximum number of occurrences required for this option
   * @return the new {@code Option} instance
   */
  public Option<Long> addLongOption(String shortName,
                                    String longName,
                                    String description,
                                    int minCount, int maxCount)
  {
    return addOption(new LongOption(shortName, longName, description, minCount, maxCount));
  }

  /**
   * Convenience method to add an {@code Option} of type {@code Long}.
   *
   * @param shortName short name of the option
   * @param mandatory whether this option must be specified
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   * @return the new {@code Option} instance
   */
  public Option<Long> addLongOption(String shortName, boolean mandatory, boolean allowMany)
  {
    return addOption(new LongOption(shortName, null, null, mandatory, allowMany));
  }

  /**
   * Convenience method to add an {@code Option} of type {@code Long}.
   *
   * @param shortName short name of the option
   * @param minCount minimum number of occurrences required for this option
   * @param maxCount maximum number of occurrences required for this option
   * @return the new {@code Option} instance
   */
  public Option<Long> addLongOption(String shortName, int minCount, int maxCount)
  {
    return addOption(new LongOption(shortName, null, null, minCount, maxCount));
  }

  /**
   * Convenience method to add an {@code Option} of type {@code Double}.
   *
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @param mandatory whether this option must be specified
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   * @return the new {@code Option} instance
   */
  public Option<Double> addDoubleOption(String shortName,
                                        String longName,
                                        String description,
                                        boolean mandatory, boolean allowMany)
  {
    return addOption(new DoubleOption(shortName, longName, description, mandatory, allowMany));
  }

  /**
   * Convenience method to add an {@code Option} of type {@code Double}.
   *
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @param minCount minimum number of occurrences required for this option
   * @param maxCount maximum number of occurrences required for this option
   * @return the new {@code Option} instance
   */
  public Option<Double> addDoubleOption(String shortName,
                                        String longName,
                                        String description,
                                        int minCount, int maxCount)
  {
    return addOption(new DoubleOption(shortName, longName, description, minCount, maxCount));
  }

  /**
   * Convenience method to add an {@code Option} of type {@code Double}.
   *
   * @param shortName short name of the option
   * @param mandatory whether this option must be specified
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   * @return the new {@code Option} instance
   */
  public Option<Double> addDoubleOption(String shortName, boolean mandatory, boolean allowMany)
  {
    return addOption(new DoubleOption(shortName, null, null, mandatory, allowMany));
  }

  /**
   * Convenience method to add an {@code Option} of type {@code Double}.
   *
   * @param shortName short name of the option
   * @param minCount minimum number of occurrences required for this option
   * @param maxCount maximum number of occurrences required for this option
   * @return the new {@code Option} instance
   */
  public Option<Double> addDoubleOption(String shortName, int minCount, int maxCount)
  {
    return addOption(new DoubleOption(shortName, null, null, minCount, maxCount));
  }

  /**
   * Convenience method to add an {@code Option} of type {@code Float}.
   *
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @param mandatory whether this option must be specified
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   * @return the new {@code Option} instance
   */
  public Option<Float> addFloatOption(String shortName,
                                      String longName,
                                      String description,
                                      boolean mandatory, boolean allowMany)
  {
    return addOption(new FloatOption(shortName, longName, description, mandatory, allowMany));
  }

  /**
   * Convenience method to add an {@code Option} of type {@code Float}.
   *
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @param minCount minimum number of occurrences required for this option
   * @param maxCount maximum number of occurrences required for this option
   * @return the new {@code Option} instance
   */
  public Option<Float> addFloatOption(String shortName,
                                      String longName,
                                      String description,
                                      int minCount, int maxCount)
  {
    return addOption(new FloatOption(shortName, longName, description, minCount, maxCount));
  }

  /**
   * Convenience method to add an {@code Option} of type {@code Float}.
   *
   * @param shortName short name of the option
   * @param mandatory whether this option must be specified
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   * @return the new {@code Option} instance
   */
  public Option<Float> addFloatOption(String shortName, boolean mandatory, boolean allowMany)
  {
    return addOption(new FloatOption(shortName, null, null, mandatory, allowMany));
  }

  /**
   * Convenience method to add an {@code Option} of type {@code Float}.
   *
   * @param shortName short name of the option
   * @param minCount minimum number of occurrences required for this option
   * @param maxCount maximum number of occurrences required for this option
   * @return the new {@code Option} instance
   */
  public Option<Float> addFloatOption(String shortName, int minCount, int maxCount)
  {
    return addOption(new FloatOption(shortName, null, null, minCount, maxCount));
  }

  /**
   * Convenience method to add an {@code Option} of type {@code String}.
   *
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @param mandatory whether this option must be specified
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   * @return the new {@code Option} instance
   */
  public Option<String> addStringOption(String shortName,
                                        String longName,
                                        String description,
                                        boolean mandatory, boolean allowMany)
  {
    return addOption(new StringOption(shortName, longName, description, mandatory, allowMany));
  }

  /**
   * Convenience method to add an {@code Option} of type {@code String}.
   *
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @param minCount minimum number of occurrences required for this option
   * @param maxCount maximum number of occurrences required for this option
   * @return the new {@code Option} instance
   */
  public Option<String> addStringOption(String shortName,
                                        String longName,
                                        String description,
                                        int minCount, int maxCount)
  {
    return addOption(new StringOption(shortName, longName, description, minCount, maxCount));
  }

  /**
   * Convenience method to add an {@code Option} of type {@code String}
   * (single value allowed, non-mandatory).
   *
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @return the new {@code Option} instance
   */
  public Option<String> addStringOption(String shortName, String longName, String description)
  {
    return addOption(new StringOption(shortName, longName, description, false, false));
  }

  /**
   * Convenience method to add an {@code Option} of type {@code String}
   * (no long name, no description).
   *
   * @param shortName short name of the option
   * @param mandatory whether this option must be specified
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   * @return the new {@code Option} instance
   */
  public Option<String> addStringOption(String shortName, boolean mandatory, boolean allowMany)
  {
    return addOption(new StringOption(shortName, null, null, mandatory, allowMany));
  }

  /**
   * Convenience method to add an {@code Option} of type {@code String}
   * (no long name, no description).
   *
   * @param shortName short name of the option
   * @param minCount minimum number of occurrences required for this option
   * @param maxCount maximum number of occurrences required for this option
   * @return the new {@code Option} instance
   */
  public Option<String> addStringOption(String shortName, int minCount, int maxCount)
  {
    return addOption(new StringOption(shortName, null, null, minCount, maxCount));
  }

  /**
   * Convenience method to add an {@code Option} of type {@code String} which
   * refers to a file/folder.
   * For example, use this method to:
   * <ul>
   * <li>Allow specification of a existing or new file/folder.</li>
   * </ul>
   *
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @param mandatory whether this option must be specified
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   * @return the new {@code Option} instance
   */
  public Option<String> addFileOption(String shortName,
                                      String longName,
                                      String description,
                                      boolean mandatory, boolean allowMany)
  {
    final FileOption opt = new FileOption(shortName, longName, description, mandatory, allowMany);
    final FileOption.FileFilter.AcceptExistance eType = FileOption.FileFilter.AcceptExistance.ACCEPT_ALL;
    final FileOption.FileFilter.AcceptFileType fType = FileOption.FileFilter.AcceptFileType.ACCEPT_ALL;
    final FileOption.Filter filter = new FileOption.FileFilter(eType, fType);
    opt.setFilter(filter);
    return addOption(opt);
  }

  /**
   * Convenience method to add an {@code Option} of type {@code String} which
   * refers to a new or existing file/folder.
   *
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @param mandatory whether this option must be specified
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   * @return the new {@code Option} instance
   */
  public Option<String> addFileNewOption(String shortName,
                                         String longName,
                                         String description,
                                         boolean mandatory, boolean allowMany)
  {
    final FileOption opt = new FileOption(shortName, longName, description, mandatory, allowMany);
    final FileOption.FileFilter.AcceptExistance eType = FileOption.FileFilter.AcceptExistance.ACCEPT_NON_EXISTING;
    final FileOption.FileFilter.AcceptFileType fType = FileOption.FileFilter.AcceptFileType.ACCEPT_ALL;
    final FileOption.Filter filter = new FileOption.FileFilter(eType, fType);
    opt.setFilter(filter);
    return addOption(opt);
  }

  /**
   * Convenience method to add an {@code Option} of type {@code String} which
   * refers to an existing file (which is not a folder).
   * For example, use this method to:
   * <ul>
   * <li>Enforce specification of an existing file (which is not a folder)</li>
   * </ul>
   *
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @param minCount minimum number of occurrences required for this option
   * @param maxCount maximum number of occurrences required for this option
   * @return the new {@code Option} instance
   */
  public Option<String> addFileExistingOption(String shortName,
                                              String longName,
                                              String description,
                                              int minCount, int maxCount)
  {
    final FileOption opt = new FileOption(shortName, longName, description, minCount, maxCount);
    final FileOption.FileFilter.AcceptExistance eType = FileOption.FileFilter.AcceptExistance.ACCEPT_EXISTING;
    final FileOption.FileFilter.AcceptFileType fType = FileOption.FileFilter.AcceptFileType.ACCEPT_FILE;
    final FileOption.Filter filter = new FileOption.FileFilter(eType, fType);
    opt.setFilter(filter);
    return addOption(opt);
  }

  /**
   * Convenience method to add an {@code Option} of type {@code String} which
   * refers to an existing file (which is not a folder).
   * For example, use this method to:
   * <ul>
   * <li>Enforce specification of an existing file (which is not a folder)</li>
   * </ul>
   *
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @param mandatory whether this option must be specified
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   * @return the new {@code Option} instance
   */
  public Option<String> addFileExistingOption(String shortName,
                                              String longName,
                                              String description,
                                              boolean mandatory, boolean allowMany)
  {
    return addFileExistingOption(shortName,
                                 longName,
                                 description,
                                 mandatory ? 1 : 0,
                                 allowMany ? Option.MAX_COUNT_LIMIT : 1);
  }

  /**
   * Convenience method to add an {@code Option} of type {@code String} which
   * refers to an existing folder.
   * For example, use this method to enforce specification of an existing folder.
   * To allow specification of a new folder (one that does not yet exist),
   * use the {@code addFileOption()} method instead.
   *
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @param minCount minimum number of occurrences required for this option
   * @param maxCount maximum number of occurrences required for this option
   * @return the new {@code Option} instance
   */
  public Option<String> addDirectoryExistingOption(String shortName,
                                                String longName,
                                                String description,
                                                int minCount, int maxCount)
  {
    final FileOption opt = new FileOption(shortName, longName, description, minCount, maxCount);
    final FileOption.FileFilter.AcceptExistance eType = FileOption.FileFilter.AcceptExistance.ACCEPT_EXISTING;
    final FileOption.FileFilter.AcceptFileType fType = FileOption.FileFilter.AcceptFileType.ACCEPT_DIR;
    final FileOption.Filter filter = new FileOption.FileFilter(eType, fType);
    opt.setFilter(filter);
    return addOption(opt);
  }

  /**
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @param minCount minimum number of occurrences required for this option
   * @param maxCount maximum number of occurrences required for this option
   * @return the new {@code Option} instance
   * @deprecated Replaced by {@link #addDirectoryExistingOption(String, String, String, int, int)}
   */
  @Deprecated
  public Option<String> addFolderExistingOption(String shortName,
                                                String longName,
                                                String description,
                                                int minCount, int maxCount)
  {
    return addDirectoryExistingOption(shortName, longName, description, minCount, maxCount);
  }

  /**
   * Convenience method to add an {@code Option} of type {@code String} which
   * refers to an existing folder.
   * For example, use this method to enforce specification of an existing folder.
   * To allow specification of a new folder (one that does not yet exist),
   * use the {@code addFileOption()} method instead.
   *
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @param mandatory whether this option must be specified
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   * @return the new {@code Option} instance
   */
  public Option<String> addDirectoryExistingOption(String shortName,
                                                String longName,
                                                String description,
                                                boolean mandatory, boolean allowMany)
  {
    return addDirectoryExistingOption(shortName,
                                   longName,
                                   description,
                                   mandatory ? 1 : 0,
                                   allowMany ? Option.MAX_COUNT_LIMIT : 1);
  }

  /**
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @param mandatory whether this option must be specified
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   * @return the new {@code Option} instance
   * @deprecated Replaced by {@link #addDirectoryExistingOption(String, String, String, boolean, boolean)}
   */
  @Deprecated
  public Option<String> addFolderExistingOption(String shortName,
                                                String longName,
                                                String description,
                                                boolean mandatory, boolean allowMany)
  {
    return addDirectoryExistingOption(shortName, longName, description, mandatory, allowMany);
  }

  /**
   * Convenience method to add an {@code Option} of enumerated {@code String} type.
   *
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @param mandatory whether this option must be specified
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   * @param allowedValues array of string values allowed by this enumerated option
   * @param ignoreCase whether to ignore the case of string values
   * @return the new {@code Option} instance
   */
  public Option<String> addEnumStringOption(String shortName,
                                            String longName,
                                            String description,
                                            boolean mandatory, boolean allowMany,
                                            String[] allowedValues, boolean ignoreCase)
  {
    final Collection<String> x = new ArrayList<>();
    x.addAll(Arrays.asList(allowedValues));
    return addOption(new EnumeratedStringOption(shortName,
                                                longName,
                                                description,
                                                mandatory, allowMany,
                                                x, ignoreCase));
  }

  /**
   * Convenience method to add an {@code Option} of enumerated {@code String} type.
   *
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @param minCount minimum number of occurrences required for this option
   * @param maxCount maximum number of occurrences required for this option
   * @param allowedValues array of string values allowed by this enumerated option
   * @param ignoreCase whether to ignore the case of string values
   * @return the new {@code Option} instance
   */
  public Option<String> addEnumStringOption(String shortName,
                                            String longName,
                                            String description,
                                            int minCount, int maxCount,
                                            String[] allowedValues, boolean ignoreCase)
  {
    final Collection<String> x = new ArrayList<>();
    x.addAll(Arrays.asList(allowedValues));
    return addOption(new EnumeratedStringOption(shortName,
                                                longName,
                                                description,
                                                minCount, maxCount,
                                                x, ignoreCase));
  }

  /**
   * Convenience method to add an {@code Option} of enumerated {@code String} type
   * (case-insensitive string matching).
   *
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @param mandatory whether this option must be specified
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   * @param allowedValues array of string values allowed by this enumerated option
   * @return the new {@code Option} instance
   */
  public Option<String> addEnumStringOption(String shortName,
                                            String longName,
                                            String description,
                                            boolean mandatory,
                                            boolean allowMany,
                                            String[] allowedValues)
  {
    return addEnumStringOption(shortName, longName, description, mandatory, allowMany, allowedValues, true);
  }

  /**
   * Convenience method to add an {@code Option} of enumerated {@code String} type
   * (single value allowed, non-mandatory).
   *
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @param allowedValues array of string values allowed by this enumerated option
   * @return the new {@code Option} instance
   */
  public Option<String> addEnumStringOption(String shortName,
                                            String longName,
                                            String description,
                                            String[] allowedValues)
  {
    return addEnumStringOption(shortName, longName, description, false, false, allowedValues);
  }

  /**
   * Convenience method to add an {@code Option} of enumerated {@code Integer} type.
   *
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @param mandatory whether this option must be specified
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   * @param allowedValues array of string values allowed by this enumerated option
   * @return the new {@code Option} instance
   */
  public Option<Integer> addEnumIntegerOption(String shortName,
                                              String longName,
                                              String description,
                                              boolean mandatory,
                                              boolean allowMany,
                                              int[] allowedValues)
  {
    final Collection<Integer> x = new ArrayList<>();
    for (Integer s : allowedValues)
      x.add(s);
    return addOption(new EnumeratedIntegerOption(shortName, longName, description, mandatory, allowMany, x));
  }

  /**
   * Convenience method to add an {@code Option} of enumerated {@code Integer} type
   * (single value allowed, non-mandatory).
   *
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @param allowedValues array of string values allowed by this enumerated option
   * @return the new {@code Option} instance
   */
  public Option<Integer> addEnumIntegerOption(String shortName,
                                              String longName,
                                              String description,
                                              int[] allowedValues)
  {
    return addEnumIntegerOption(shortName, longName, description, false, false, allowedValues);
  }

  /**
   * Convenience method to add an {@code Option} of type {@code LocalDate}.
   *
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @param mandatory whether this option must be specified
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   * @return the new {@code Option} instance
   */
  public Option<LocalDate> addLocalDateOption(String shortName,
                                              String longName,
                                              String description,
                                              boolean mandatory, boolean allowMany)
  {
    return addOption(new LocalDateOption(shortName, longName, description, mandatory, allowMany));
  }

  /**
   * Convenience method to add an {@code Option} of type {@code LocalDate}.
   *
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @param minCount minimum number of occurrences required for this option
   * @param maxCount maximum number of occurrences required for this option
   * @return the new {@code Option} instance
   */
  public Option<LocalDate> addLocalDateOption(String shortName,
                                              String longName,
                                              String description,
                                              int minCount, int maxCount)
  {
    return addOption(new LocalDateOption(shortName, longName, description, minCount, maxCount));
  }

  /**
   * Convenience method to add an {@code Option} of type {@code LocalDate}
   * (single value allowed, non-mandatory).
   *
   * @param shortName short name of the option
   * @param longName long name of the option
   * @param description helpful description of the option (printed for usage message)
   * @return the new {@code Option} instance
   */
  public Option<LocalDate> addLocalDateOption(String shortName, String longName, String description)
  {
    return addOption(new LocalDateOption(shortName, longName, description, false, false));
  }

  /**
   * Convenience method to add an {@code Option} of type {@code LocalDate}
   * (no long name, no description).
   *
   * @param shortName short name of the option
   * @param mandatory whether this option must be specified
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   * @return the new {@code Option} instance
   */
  public Option<LocalDate> addLocalDateOption(String shortName, boolean mandatory, boolean allowMany)
  {
    return addOption(new LocalDateOption(shortName, null, null, mandatory, allowMany));
  }

  /**
   * Convenience method to add an {@code Option} of type {@code LocalDate}
   * (no long name, no description).
   *
   * @param shortName short name of the option
   * @param minCount minimum number of occurrences required for this option
   * @param maxCount maximum number of occurrences required for this option
   * @return the new {@code Option} instance
   */
  public Option<LocalDate> addLocalDateOption(String shortName, int minCount, int maxCount)
  {
    return addOption(new LocalDateOption(shortName, null, null, minCount, maxCount));
  }

  /**
   * Returns the {@code Option} with the specified short name, or null if not found.
   *
   * @param shortName short name of the option
   */
  private Option getOptionByShortName(String shortName)
  {
    if (shortName == null || "".equals(shortName))
      throw new IllegalArgumentException();
    for (Option o : options)
    {
      if (shortName.equals(o.getShortName()))
        return o;
    }
    return null;
  }

  /**
   * Returns the {@code Option} with the specified short name, or null if not found.
   *
   * @param shortName short name of the option
   */
  private Option getOptionByShortName(char shortName)
  {
    return getOptionByShortName(new String(new char[]{shortName}));
  }

  /**
   * Returns the {@code Option} with the specified short name, or null if not found.
   *
   * @param longName long name of the option
   */
  private Option getOptionByLongName(String longName)
  {
    if (longName == null || "".equals(longName))
      throw new IllegalArgumentException();
    for (Option o : options)
    {
      if (longName.equals(o.getLongName()))
        return o;
    }
    return null;
  }

  /**
   * Returns the {@code Option} with the specified name (either short or long).
   *
   * @param <T> the return type of the option
   * @param optionName option name for which to get value (either short/long)
   * @param type class type for option value compatibility check
   * @return Option instance
   * @throws OptionException if {@code optionName} is null, cannot be found, or is of the wrong type
   */
  @SuppressWarnings("unchecked")
  public <T> Option<T> getOption(String optionName, Class<T> type) throws OptionException
  {
    if (optionName == null)
      throw new OptionException(UNKNOWN_OPTION, optionName);
    Option opt = null;
    if (optionName.length() == 1)
      opt = getOptionByShortName(optionName);
    if (opt == null)
      opt = getOptionByLongName(optionName);
    if (opt == null)
      throw new OptionException(UNKNOWN_OPTION, optionName);
    if (type == null || !type.equals(opt.getType()))
      throw new OptionException(INVALID_OPTION_TYPE, optionName);
    return opt;
  }

  /**
   * Returns all the {@code Option} instances registered with the parser.
   *
   * @return list of options
   */
  public List<Option> getOptions()
  {
    return Collections.unmodifiableList(options);
  }

  /**
   * Returns a list of all the parsed values for the specified option,
   * or an empty list if the option was not set.
   *
   * @param <T> the return type of the option
   * @param opt option for which to get values
   * @throws OptionException if {@code opt} is null, cannot be found, or is of the wrong type
   * @return A list of all the parsed values for the specified option,
   * or an empty list if the option was not set.
   */
  public <T> List<T> getOptionValues(Option<T> opt) throws OptionException
  {
    if (opt == null)
      throw new OptionException(UNKNOWN_OPTION, opt);
    return Collections.unmodifiableList(opt.getValues());
  }

  /**
   * Returns a list of all the parsed values for the specified option,
   * or an empty list if the option was not set.
   * This method performs type-checking of the {@code Option}.
   *
   * @param <T> the return type of the option
   * @param optionName option name for which to get value (either short/long)
   * @param type class type for option value compatibility check
   * @return list of option values
   * @throws OptionException if {@code optionName} is null, cannot be found, or is of the wrong type
   */
  public <T> List<T> getOptionValues(String optionName, Class<T> type) throws OptionException
  {
    return getOptionValues(getOption(optionName, type));
  }

  /**
   * Convenience method to return the parsed value of the specified option,
   * or null if the option was not set.
   * This method is only usable with options that cannot take multiple values.
   *
   * @param <T> the return type of the option
   * @param opt option for which to get value
   * @return option value
   * @throws OptionException if {@code opt} is null, cannot be found, or is of the wrong type
   */
  public <T> T getOptionValue(Option<T> opt) throws OptionException
  {
    if (opt == null)
      throw new OptionException(UNKNOWN_OPTION, opt);
    if (opt.isAllowMany())
      throw new OptionException(INVALID_RETRIEVAL_TYPE, opt);
    final List<T> vals = getOptionValues(opt);
    return vals.isEmpty() ? null : vals.get(0);
  }

  /**
   * Convenience method to return the parsed value of the specified option,
   * or the specified default value if the option was not set.
   * This method is only usable with options that cannot take multiple values.
   *
   * @param <T> the return type of the option
   * @param opt option for which to get value
   * @param def default value to use if the option was not set
   * @return option value
   * @throws OptionException if {@code opt} is null, cannot be found, or is of the wrong type
   */
  @SuppressWarnings("unchecked")
  public <T> T getOptionValue(Option<T> opt, T def) throws OptionException
  {
    if (opt == null)
      throw new OptionException(UNKNOWN_OPTION, opt);
    if (opt.isAllowMany())
      throw new OptionException(INVALID_RETRIEVAL_TYPE, opt);

    // If EnumeratedOption, check that default value is valid.
    // This is checked regardless of parsed values to help avoid problems
    // at development time.
    if (def != null && opt instanceof EnumeratedOption)
    {
      final EnumeratedOption eo = (EnumeratedOption)opt;
      if (!eo.isValueValid(def, getLocale()))
        throw new OptionException(ILLEGAL_OPTION_VALUE, opt, def.toString());
    }

    final List<T> vals = getOptionValues(opt);
    return vals.isEmpty() ? def : vals.get(0);
  }

  /**
   * Convenience method to return the parsed value of the named option,
   * or the specified default value if the option was not set.
   * This method should only be used with typed options that cannot take
   * multiple values.
   *
   * @param <T> the return type of the option
   * @param optionName name of the option
   * @param type class type for option value compatibility check
   * @param def default value to use if the option was not set
   * @return option value, or default value if option is unset
   * @throws OptionException if {@code optionName} is null, cannot be found, or is of the wrong type
   */
  public <T> T getOptionValue(String optionName, Class<T> type, T def) throws OptionException
  {
    return getOptionValue(getOption(optionName, type), def);
  }

  /**
   * Convenience method to return the parsed value of the named option,
   * or null if the option was not set,
   * equivalent to {@code getOptionValue(optionName, type, null)}.
   * This method should only be used with typed options that cannot take
   * multiple values.
   *
   * @param <T> the return type of the option
   * @param optionName name of the option
   * @param type class type for option value compatibility check
   * @return option value
   * @throws OptionException if {@code optionName} is null, cannot be found, or is of the wrong type
   */
  public <T> T getOptionValue(String optionName, Class<T> type) throws OptionException
  {
    return getOptionValue(optionName, type, null);
  }

  /**
   * Returns a list of all the parsed values for the specified option,
   * or an empty list if the option was not set.
   *
   * @param optionName name of the option
   * @return list of option values
   * @throws OptionException if {@code optionName} is null, cannot be found, or is of the wrong type
   */
  public List<Boolean> getBooleanOptionValues(String optionName) throws OptionException
  {
    return getOptionValues(optionName, Boolean.class);
  }

  /**
   * Convenience method to return the parsed value of the named option,
   * or the specified default value if the option was not set. This method
   * should only be used with options that cannot take multiple values.
   *
   * @param optionName name of the option
   * @param def default value to use if the option was not set
   * @return option value, or default value if option is unset
   * @throws OptionException if {@code optionName} is null, cannot be found, or is of the wrong type
   */
  public Boolean getBooleanOptionValue(String optionName, Boolean def) throws OptionException
  {
    return getOptionValue(optionName, Boolean.class, def);
  }

  /**
   * Convenience method to return the parsed value of the named option,
   * or {@code false} if the option was not set. This method
   * should only be used with options that cannot take multiple values.
   * NOTE: this method operates slightly differently compared to the other
   * {@code getXOptionValue(String)} methods, in that when the option is not
   * defined, it returned {@code Boolean.FALSE} instead of {@code null}.
   *
   * @param optionName name of the option
   * @return option value, or null if option is unset
   * @throws OptionException if {@code optionName} is null, cannot be found, or is of the wrong type
   */
  public Boolean getBooleanOptionValue(String optionName) throws OptionException
  {
    return getOptionValue(optionName, Boolean.class, Boolean.FALSE);
  }

  /**
   * Returns a list of all the parsed values for the specified option,
   * or an empty list if the option was not set.
   *
   * @param optionName name of the option
   * @return list of option values
   * @throws OptionException if {@code optionName} is null, cannot be found, or is of the wrong type
   */
  public List<Integer> getIntegerOptionValues(String optionName) throws OptionException
  {
    return getOptionValues(optionName, Integer.class);
  }

  /**
   * Convenience method to return the parsed value of the named option,
   * or the specified default value if the option was not set. This method
   * should only be used with options that cannot take multiple values.
   *
   * @param optionName name of the option
   * @param def default value to use if the option was not set
   * @return option value, or default value if option is unset
   * @throws OptionException if {@code optionName} is null, cannot be found, or is of the wrong type
   */
  public Integer getIntegerOptionValue(String optionName, Integer def) throws OptionException
  {
    return getOptionValue(optionName, Integer.class, def);
  }

  /**
   * Convenience method to return the parsed value of the named option,
   * or null if the option was not set. This method
   * should only be used with options that cannot take multiple values.
   *
   * @param optionName name of the option
   * @return option value, or null if option is unset
   * @throws OptionException if {@code optionName} is null, cannot be found, or is of the wrong type
   */
  public Integer getIntegerOptionValue(String optionName) throws OptionException
  {
    return getOptionValue(optionName, Integer.class);
  }

  /**
   * Returns a list of all the parsed values for the specified option,
   * or an empty list if the option was not set.
   *
   * @param optionName name of the option
   * @return list of option values
   * @throws OptionException if {@code optionName} is null, cannot be found, or is of the wrong type
   */
  public List<Long> getLongOptionValues(String optionName) throws OptionException
  {
    return getOptionValues(optionName, Long.class);
  }

  /**
   * Convenience method to return the parsed value of the named option,
   * or the specified default value if the option was not set. This method
   * should only be used with options that cannot take multiple values.
   *
   * @param optionName name of the option
   * @param def default value to use if the option was not set
   * @return option value, or default value if option is unset
   * @throws OptionException if {@code optionName} is null, cannot be found, or is of the wrong type
   */
  public Long getLongOptionValue(String optionName, Long def) throws OptionException
  {
    return getOptionValue(optionName, Long.class, def);
  }

  /**
   * Convenience method to return the parsed value of the named option,
   * or null if the option was not set. This method
   * should only be used with options that cannot take multiple values.
   *
   * @param optionName name of the option
   * @return option value, or null if option is unset
   * @throws OptionException if {@code optionName} is null, cannot be found, or is of the wrong type
   */
  public Long getLongOptionValue(String optionName) throws OptionException
  {
    return getOptionValue(optionName, Long.class);
  }

  /**
   * Returns a list of all the parsed values for the specified option,
   * or an empty list if the option was not set.
   *
   * @param optionName name of the option
   * @return list of option values
   * @throws OptionException if {@code optionName} is null, cannot be found, or is of the wrong type
   */
  public List<Double> getDoubleOptionValues(String optionName) throws OptionException
  {
    return getOptionValues(optionName, Double.class);
  }

  /**
   * Convenience method to return the parsed value of the named option,
   * or the specified default value if the option was not set. This method
   * should only be used with options that cannot take multiple values.
   *
   * @param optionName name of the option
   * @param def default value to use if the option was not set
   * @return option value, or default value if option is unset
   * @throws OptionException if {@code optionName} is null, cannot be found, or is of the wrong type
   */
  public Double getDoubleOptionValue(String optionName, Double def) throws OptionException
  {
    return getOptionValue(optionName, Double.class, def);
  }

  /**
   * Convenience method to return the parsed value of the named option,
   * or null if the option was not set. This method
   * should only be used with options that cannot take multiple values.
   *
   * @param optionName name of the option
   * @return option value, or null if option is unset
   * @throws OptionException if {@code optionName} is null, cannot be found, or is of the wrong type
   */
  public Double getDoubleOptionValue(String optionName) throws OptionException
  {
    return getOptionValue(optionName, Double.class);
  }

  /**
   * Returns a list of all the parsed values for the specified option,
   * or an empty list if the option was not set.
   *
   * @param optionName name of the option
   * @return list of option values
   * @throws OptionException if {@code optionName} is null, cannot be found, or is of the wrong type
   */
  public List<Float> getFloatOptionValues(String optionName) throws OptionException
  {
    return getOptionValues(optionName, Float.class);
  }

  /**
   * Convenience method to return the parsed value of the named option,
   * or the specified default value if the option was not set. This method
   * should only be used with options that cannot take multiple values.
   *
   * @param optionName name of the option
   * @param def default value to use if the option was not set
   * @return option value, or default value if option is unset
   * @throws OptionException if {@code optionName} is null, cannot be found, or is of the wrong type
   */
  public Float getFloatOptionValue(String optionName, Float def) throws OptionException
  {
    return getOptionValue(optionName, Float.class, def);
  }

  /**
   * Convenience method to return the parsed value of the named option,
   * or null if the option was not set. This method
   * should only be used with options that cannot take multiple values.
   *
   * @param optionName name of the option
   * @return option value, or null if option is unset
   * @throws OptionException if {@code optionName} is null, cannot be found, or is of the wrong type
   */
  public Float getFloatOptionValue(String optionName) throws OptionException
  {
    return getOptionValue(optionName, Float.class);
  }

  /**
   * Returns a list of all the parsed values for the specified option,
   * or an empty list if the option was not set.
   *
   * @param optionName name of the option
   * @return list of option values
   * @throws OptionException if {@code optionName} is null, cannot be found, or is of the wrong type
   */
  public List<String> getStringOptionValues(String optionName) throws OptionException
  {
    return getOptionValues(optionName, String.class);
  }

  /**
   * Convenience method to return the parsed value of the named option,
   * or the specified default value if the option was not set. This method
   * should only be used with options that cannot take multiple values.
   *
   * @param optionName name of the option
   * @param def default value to use if the option was not set
   * @return option value, or default value if option is unset
   * @throws OptionException if {@code optionName} is null, cannot be found, or is of the wrong type
   */
  public String getStringOptionValue(String optionName, String def) throws OptionException
  {
    return getOptionValue(optionName, String.class, def);
  }

  /**
   * Convenience method to return the parsed value of the named option,
   * or null if the option was not set. This method
   * should only be used with options that cannot take multiple values.
   *
   * @param optionName name of the option
   * @return option value, or null if option is unset
   * @throws OptionException if {@code optionName} is null, cannot be found, or is of the wrong type
   */
  public String getStringOptionValue(String optionName) throws OptionException
  {
    return getOptionValue(optionName, String.class);
  }

  /**
   * Returns a list of all the parsed values for the specified option,
   * or an empty list if the option was not set.
   *
   * @param optionName name of the option
   * @return list of option values
   * @throws OptionException if {@code optionName} is null, cannot be found, or is of the wrong type
   */
  public List<LocalDate> getLocalDateOptionValues(String optionName) throws OptionException
  {
    return getOptionValues(optionName, LocalDate.class);
  }

  /**
   * Convenience method to return the parsed value of the named option,
   * or the specified default value if the option was not set. This method
   * should only be used with options that cannot take multiple values.
   *
   * @param optionName name of the option
   * @param def default value to use if the option was not set
   * @return option value, or default value if option is unset
   * @throws OptionException if {@code optionName} is null, cannot be found, or is of the wrong type
   */
  public LocalDate getLocalDateOptionValue(String optionName, LocalDate def) throws OptionException
  {
    return getOptionValue(optionName, LocalDate.class, def);
  }

  /**
   * Convenience method to return the parsed value of the named option,
   * or null if the option was not set. This method
   * should only be used with options that cannot take multiple values.
   *
   * @param optionName name of the option
   * @return option value, or null if option is unset
   * @throws OptionException if {@code optionName} is null, cannot be found, or is of the wrong type
   */
  public LocalDate getLocalDateOptionValue(String optionName) throws OptionException
  {
    return getOptionValue(optionName, LocalDate.class);
  }

  /**
   * Returns a list of all the parsed values for the specified option,
   * or an empty list if the option was not set.
   *
   * @param optionName name of the option
   * @return list of option values
   * @throws OptionException if {@code optionName} is null, cannot be found, or is of the wrong type
   */
  public List<File> getFileOptionValues(String optionName) throws OptionException
  {
    final List<String> ov = getOptionValues(optionName, String.class);
    final List<File> list = new ArrayList<>();
    try
    {
      for (String s : ov)
        list.add(new File(s).getCanonicalFile());
    }
    catch (IOException ex)
    {
      final Option<String> opt = getOption(optionName, String.class);
      throw new OptionException(ILLEGAL_OPTION_VALUE, opt);
    }
    return list;
  }

  /**
   * Convenience method to return the parsed value of the named option,
   * or the specified default value if the option was not set. This method
   * should only be used with options that cannot take multiple values.
   *
   * @param optionName name of the option
   * @param def default value to use if the option was not set
   * @return option value, or default value if option is unset
   * @throws OptionException if {@code optionName} is null, cannot be found, or is of the wrong type
   */
  public File getFileOptionValue(String optionName, File def) throws OptionException
  {
    final String ov = getOptionValue(optionName, String.class);
    try
    {
      return (ov == null) ? def : new File(ov).getCanonicalFile();
    }
    catch (IOException ex)
    {
      final Option<String> opt = getOption(optionName, String.class);
      throw new OptionException(ILLEGAL_OPTION_VALUE, opt, ov);
    }
  }

  /**
   * Convenience method to return the parsed value of the named option,
   * or null if the option was not set. This method
   * should only be used with options that cannot take multiple values.
   *
   * @param optionName name of the option
   * @return option value, or null if option is unset
   * @throws OptionException if {@code optionName} is null, cannot be found, or is of the wrong type
   */
  public File getFileOptionValue(String optionName) throws OptionException
  {
    final String ov = getOptionValue(optionName, String.class);
    try
    {
      return (ov == null) ? null : new File(ov).getCanonicalFile();
    }
    catch (IOException ex)
    {
      final Option<String> opt = getOption(optionName, String.class);
      throw new OptionException(ILLEGAL_OPTION_VALUE, opt, ov);
    }
  }

  /**
   * Returns a list of the non-option command-line arguments.
   * Note that this list includes all arguments that are not related to options,
   * and as a result may include &quot;orphan&quot; arguments (in between
   * correctly specified options) in addition to the expected arguments that
   * are specified after all the options.
   * <p>For example:
   * <pre>
   *     java DemoApp -i 10 -f 1.2 foo -s bar fu bar
   * </pre>
   * <p>for an application with value-taking options {@code i}/{@code f}/{@code s},
   * would return [{@code foo},{@code fu},{@code bar}] from this method.
   *
   * @return A list of the command-line string arguments that were not parsed as options.
   */
  public List<String> getNonOptionArguments()
  {
    return Collections.unmodifiableList(nonOptionArgs);
  }

  /**
   * Extract the option-mapped and unmapped arguments from the given array of
   * command-line arguments, using the specified locale.
   *
   * @param args command-line arguments (as passed into {@code main} method)
   * @throws OptionException if an problem is encountered parsing the specified arguments
   */
  @SuppressWarnings("unchecked")
  public void parse(String[] args) throws OptionException
  {
    if (DEBUG) System.out.println("Parsing: " + Arrays.toString(args));
    // Create list to hold non-option arguments found.
    final List<String> spareArgs = new ArrayList<>();
    // Create list of arguments for easier processing.
    final List<String> list = new ArrayList<>(args.length);
    list.addAll(Arrays.asList(args));

    // Define regex for group of flag short names.
    final String regexFlags = options.stream()
            .filter(o -> Boolean.class.equals(o.getType()))
            .map(o -> o.getShortName())
            .collect(joining())
            .replace("?", "\\?")
            .replaceFirst("(.+)", "[\\\\Q$1\\\\E]+");
    // Define regex for short name of any non-flag option.
    final String regexNonFlag = options.stream()
            .filter(o -> !Boolean.class.equals(o.getType()))
            .filter(o -> o.requiresValue())
            .map(o -> o.getShortName())
            .collect(joining())
            .replace("?", "\\?")
            .replaceFirst("(.+)", "[\\\\Q$1\\\\E]");
    if (DEBUG) System.out.printf("Collated regex for flags, non-flags: %s, %s%n", regexFlags, regexNonFlag);
    // Define regex for long-named value option.
    final String op1 = String.format("^--(%s)(?:[=:]([^\\s]+))?$", REGEX_LONG_NAME);
    // Define regex for flag option(s).
    final String op2 = String.format("^[-/](%s)$", regexFlags);
    // Define regex for flag(s) followed by short-named value option.
    final String op3 = String.format("^[-/](%s)(%s)(?:[=:]?([^\\s]+))$", regexFlags, regexNonFlag);
    // Define regex for short-named value option.
    final String op4 = String.format("^[-/](%s)(?:[=:]?([^\\s]+))?$", REGEX_SHORT_NAME);

    final Pattern optPattern1 = Pattern.compile(op1);
    final Pattern optPattern2 = Pattern.compile(op2);
    final Pattern optPattern3 = Pattern.compile(op3);
    final Pattern optPattern4 = Pattern.compile(op4);

    // Declare flag to signal end of option processing.
    boolean eoo = false;
    // Loop over arguments to process them.
    // A ListIterator is used to provide easy access to next argument.
    for (ListIterator<String> iter = list.listIterator(); iter.hasNext();)
    {
      // Get next argument to process.
      final String arg = iter.next();
      if (DEBUG) System.out.println("Processing argument: " + arg);

      // Check for end of options, or solitary hyphen.
      if (eoo || "-".equals(arg))
      {
        if (DEBUG) System.out.println("...found solitary hyphen (flag for STDIN)");
        spareArgs.add(arg);
        continue;
      }
      // Check for end of options.
      if ("--".equals(arg))
      {
        if (DEBUG) System.out.println("...found double hyphen (end of arguments)");
        eoo = true;
        continue;  // Continue with next argument.
      }

      // Create regex matcher for each option pattern.
      final Matcher m1 = optPattern1.matcher(arg);
      final Matcher m2 = optPattern2.matcher(arg);
      final Matcher m3 = optPattern3.matcher(arg);
      final Matcher m4 = optPattern4.matcher(arg);

      // Check for long name match.
      if (m1.matches())
      {
        final Option opt = getOptionByLongName(m1.group(1));
        if (opt == null)
          throw new OptionException(UNKNOWN_OPTION, m1.group(1));
        processOptionValue(opt, m1.group(2), iter);
      }
      // Check for flag match.
      else if (m2.matches())
      {
        final String flagNames = m2.group(1);
        for (int i = 0; i < flagNames.length(); i++)
        {
          final String flagSN = flagNames.substring(i, i + 1);
          final Option opt = getOptionByShortName(flagSN);
          if (opt == null)
            throw new OptionException(UNKNOWN_OPTION, flagSN);
          if (opt.requiresValue())
            throw new OptionException(NOT_FLAG, opt.getShortName(), flagSN);
          if (DEBUG) System.out.println("Setting flag option: -" + opt.getShortName());
          opt.addValue(Boolean.TRUE);
        }
      }
      // Check for grouped flags followed by value-taking option.
      else if (m3.matches())
      {
        // Extract & set flags.
        final String flagNames = m3.group(1);
        for (int i = 0; i < flagNames.length(); i++)
        {
          final String flagSN = flagNames.substring(i, i + 1);
          final Option opt = getOptionByShortName(flagSN);
          if (opt == null)
            throw new OptionException(UNKNOWN_OPTION, flagSN);
          if (opt.requiresValue())
            throw new OptionException(NOT_FLAG, opt.getShortName(), flagSN);
          if (DEBUG) System.out.println("Setting flag option: -" + opt.getShortName());
          opt.addValue(Boolean.TRUE);
        }
        // Extract & set value of remaining value option.
        final Option opt = getOptionByShortName(m3.group(2));
        if (opt == null)
          throw new OptionException(UNKNOWN_OPTION, m3.group(1));
        processOptionValue(opt, m3.group(3), iter);
      }
      // Check for short name match (with delimited value or no space).
      else if (m4.matches())
      {
        final Option opt = getOptionByShortName(m4.group(1));
        if (opt == null)
          throw new OptionException(UNKNOWN_OPTION, m4.group(1));
        processOptionValue(opt, m4.group(2), iter);
      }
      // No match to options, so add to non-option arguments.
      else
      {
        spareArgs.add(arg);
      }
    }

    // Save non-option arguments.
    nonOptionArgs = spareArgs;

    // Finally, check for value-requiring options that don't conform.
    for (Option opt : options)
    {
      final int valCount = opt.getValues().size();
      if (opt.isMandatory() && opt.getValues().isEmpty())
        throw new OptionException(ILLEGAL_OPTION_VALUE, opt, null);
      if (valCount < opt.getMinCount() || valCount > opt.getMaxCount())
        throw new OptionException(INVALID_OPTION_COUNT, opt, null);
    }
  }

  /**
   * Helper method for processing parsed option values.
   * For the specified {@code Option} try to assign a value from either the
   * specified string, or from the next argument from the iterator.
   */
  @SuppressWarnings("unchecked")
  private void processOptionValue(Option opt, String val, ListIterator<String> iter) throws OptionException
  {
    if (DEBUG) System.out.printf("...processOptionValue(%s, %s)%n", opt, val == null ? null : "\"" + "\"");
    // If value specified in this argument, process it.
    if (val != null)
    {
      if (opt.requiresValue() ||
              Boolean.class.equals(opt.getType()) &&
              opt.isAllowMany() &&
              opt.getValues().size() < opt.getMaxCount())
        opt.addValue(opt.parseValue(val, getLocale()));
      else
        throw new OptionException(ILLEGAL_OPTION_VALUE, opt);
    }
    // Else value not specified, so get from next argument.
    else if (opt.requiresValue())
    {
      if (iter.hasNext())
        opt.addValue(opt.parseValue(iter.next(), getLocale()));
      else
        throw new OptionException(ILLEGAL_OPTION_VALUE, opt, "null");
    }
    else if (opt.getType().equals(Boolean.class))
    {
      opt.addValue(Boolean.TRUE);
    }
  }

  /**
   * Tries to determine the likely command-line used to launch the host
   * application, and returns it.
   *
   * @return best guess at launch command-line
   */
  private static String getLaunchCmd()
  {
    String launchCmd = null;
    // Get CLASSPATH to find if launched from JAR or CLASS.
    final String cp = System.getProperty("java.class.path");
    if (Pattern.matches("^[^\\?%*:|\"<>]+\\.jar$", cp))
    {
      launchCmd = "java -jar " + cp;
    }
    else
    {
      // Find calling class with main method.
      final Exception ex = new Exception();
      final StackTraceElement[] st = ex.getStackTrace();
      for (int i = 2; i < st.length; i++)
      {
        if (st[i].getMethodName().equals("main"))
          launchCmd = "java " + st[i].getClassName();
      }
    }
    return (launchCmd != null) ? launchCmd : "java App";
  }

  /**
   * Prints the command-line usage message to the specified {@code PrintStream},
   * making a guess at the application launch command.
   * The usage message is automatically created using the options specified.
   * The usage can be displayed in either short or long versions, although the
   * long version uses the options' description fields, which are not required,
   * but should generally be included if intending to print the long usage.
   * <p>This is equivalent to calling {@code printUsage(ps, longUsage, null, null, null)}.
   *
   * @param ps {@code PrintStream} to which to print usage message
   * @param longUsage whether to print long version of usage message (otherwise print short version)
   */
  public void printUsage(PrintStream ps, boolean longUsage)
  {
    printUsage(ps, longUsage, null, null, null);
  }

  /**
   * Equivalent to: {@code printUsage(ps, longUsage, null, suffixArgs, null)}.
   *
   * @param ps {@code PrintStream} to which to print usage message
   * @param longUsage whether to print long version of usage message (otherwise print short version)
   * @param suffixArgs usage suffix string for defining extra arguments
   * @deprecated Recommend to use {@link #printUsage(PrintStream, boolean, String, String, String)}
   *             as a replacement to avoid confusion over parameter ordering
   */
  @Deprecated
  public void printUsage(PrintStream ps, boolean longUsage, final String suffixArgs)
  {
    printUsage(ps, longUsage, null, suffixArgs, null);
  }

  /**
   * Equivalent to: {@code printUsage(ps, longUsage, appString, suffixArgs, null)}.
   *
   * @param ps {@code PrintStream} to which to print usage message
   * @param longUsage whether to print long version of usage message (otherwise print short version)
   * @param appString usage prefix string describing how application is launched;
   *                  {@code null} specifies to create value automatically
   *                  (e.g. &quot;java foo.bar.AppName&quot;)
   * @param suffixArgs usage suffix string for defining extra arguments
   * @deprecated Recommend to use {@link #printUsage(PrintStream, boolean, String, String, String)}
   *             as a replacement to avoid confusion over parameter ordering
   */
  @Deprecated
  public void printUsage(PrintStream ps, boolean longUsage, final String appString, final String suffixArgs)
  {
    printUsage(ps, longUsage, appString, suffixArgs, null);
  }

  /**
   * Prints the command-line usage message to the specified {@code PrintStream}.
   * The usage message is automatically created using the options specified.
   * The usage can be displayed in either short or long versions, although the
   * long version uses the options' description fields, which are not required,
   * but should generally be included if intending to print the long usage.
   *
   * @param ps {@code PrintStream} to which to print usage message
   * @param longUsage whether to print long version of usage message (otherwise print short version)
   * @param appString usage prefix string describing how application is launched;
   *                  {@code null} specifies to create value automatically
   *                  (e.g. &quot;java foo.bar.AppName&quot;)
   * @param suffixArgs usage suffix string for defining extra arguments
   * @param extraInfo additional information to be displayed before options
   *                  (only when {@code longUsage} is true)
   */
  public void printUsage(PrintStream ps, boolean longUsage, final String appString, final String suffixArgs, final String extraInfo)
  {
    final String lSep = System.getProperty("line.separator");
    final ResourceBundle res = bundle != null ? bundle : BUNDLE;
    final String appLaunch = (appString == null) ? getLaunchCmd() : appString;
    final String ver = longUsage ? "long" : "short";

    // Create buffers to hold option info.
    final StringBuilder sbOpt = new StringBuilder();

    // Prepare usage formatting string for list of options.
    final String fmUsageOption  = res.getString("option." + ver + ".mandatory").replace("\n", lSep);
    final String fmUsageOptionO = res.getString("option." + ver).replace("\n", lSep);

    // Loop over options (in order added), formatting each one for display.
    for (Option option : options)
    {
      // Check if option has a long name.
      if (option.isHidden())
        continue;
      final boolean hasLongName = option.getLongName() != null;

      // Get formatting string for the option's value-type.
      // e.g. "{0},{1} <integer>"
      final String optValType = option.getType().getName().replaceFirst(".+\\.", "");
      // Get type-formatting string for option (e.g. "-{0},--{1} <{2}>" for integer-type).
      String typeFormat = "usage.type." + optValType;
      if (!hasLongName || !longUsage && !showLongNamesInShortUsage)
        typeFormat += ".simple";
      try
      {
        typeFormat = res.getString(typeFormat);
      }
      catch (MissingResourceException mrx)
      {
        typeFormat = "usage.type.unknown";
        if (!hasLongName || !longUsage)
          typeFormat += ".simple";
        typeFormat = res.getString(typeFormat).replace("\n", lSep);
      }

      // Format type string for option (e.g. "-v,--value <integer>").
      Object[] args = new String[] {
        option.getShortName(),
        option.getLongName(),
        option.getUsageTypeString(res)
      };
      final String mUsageType = MessageFormat.format(typeFormat, args);

      // If long usage requested, create description string.
      String desc = longUsage ? (option.getDescription() != null ? option.getDescription() + lSep : "") : "";
      // Make special case for enumerated types: prefix description with allowable values.
      if (option instanceof EnumeratedOption)
      {
        final String enumDescFormat = res.getString("option.description.enum").replace("\n", lSep);
        final EnumeratedOption eo = (EnumeratedOption)option;
        final String enumString = eo.getAllowedValuesString();
        args = new String[] { enumString, desc };
        desc = MessageFormat.format(enumDescFormat, args);
      }
      // Make special case for LocalDate type: add example of date format.
      if (option instanceof LocalDateOption)
      {
        final LocalDateOption ldo = (LocalDateOption)option;
        final String descFormat = res.getString("option.description.LocalDate").replace("\n", lSep);
        args = new String[] { ldo.getDateFormat(getLocale()).format(LocalDate.now()), desc };
        desc = MessageFormat.format(descFormat, args);
      }

      // Format complete option usage string.
      // e.g. "
      //     -v,--value <integer>    (optional)
      //         Value to assign.
      // "
      args = new String[] { mUsageType, desc };
      String mUsageOption = null;
      if (option.isMandatory())
        mUsageOption = MessageFormat.format(fmUsageOption, args);
      else
        mUsageOption = MessageFormat.format(fmUsageOptionO, args);

      if (longUsage)
        sbOpt.append(mUsageOption);
      else
      {
        sbOpt.append(' ');  // Add space between options.
        sbOpt.append(mUsageOption);
      }
    }

    // Collate final usage string.
    final String usageFormat = res.getString("usage." + ver).replace("\n", lSep);
    final Object[] args = new String[] {
      appLaunch,
      sbOpt.toString(),
      (suffixArgs == null) ? "" : suffixArgs,
      (extraInfo == null) ? "" : extraInfo.concat(lSep)
    };
    final String result = MessageFormat.format(usageFormat, args);
    ps.print(result);
  }
}
