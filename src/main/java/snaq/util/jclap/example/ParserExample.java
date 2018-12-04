package snaq.util.jclap.example;

import java.io.File;
import java.util.List;
import snaq.util.jclap.CLAParser;
import snaq.util.jclap.Option;
import snaq.util.jclap.OptionException;

/**
 * Example usage of {@link CLAParser} class, showing how options may be added,
 * parsed, and referenced, along with a recommended exception-handling strategy.
 *
 * @author Giles Winstanley
 */
public final class ParserExample
{
  private ParserExample() {}

  public static void main(String[] args) throws OptionException
  {
    // Create an instance of the parser class.
    final CLAParser parser = new CLAParser();
    // Define string to describe the non-option arguments in the usage message.
    final String extraArgs = "<file> [<file>] ...";
    // Define string to describe the additional info in the usage message.
    final String extraInfo = "(THIS IS A NON-FUNCTIONAL EXAMPLE FOR DEMONSTRATION PURPOSES ONLY)";

    // Arguments:
    //   -w/--width   : mandatory,   single
    //   -h/--height  : mandatory,   single
    //   -d/--dir     :  optional,   single
    //   -f/--format  :  optional,   single, enum{"jpg", "png"}
    //   -v/--verbose :  optional, multiple
    //   -@/--version :  optional,   single
    //   -?/--help    :  optional,   single

    // Note: a reference to an Option instance may be retained in a variable,
    // or simply added & retrieved by name (which is generally easier).
    final Option<Integer> oWidth = parser.addIntegerOption("w", "width", "Width of resized images.", true, false);
    parser.addIntegerOption("h", "height", "Height of resized images.", true, false);
    parser.addDirectoryExistingOption("d", "dir", "Output directory for resized images.", false, false);
    parser.addEnumStringOption("f", "format", "Output format of images.", false, false, new String[]{"jpg", "png"});
    parser.addBooleanOption("v", "verbose", "Displays extra runtime information.", true);
    parser.addBooleanOption("@", "version", "Displays the application version information.", false);
    parser.addBooleanOption("?", "help", "Displays help information.", false);
    try
    {
      // Parse the command-line arguments into options.
      parser.parse(args);

      // Check for display of version information.
      // This checks a boolean/flag option value, with an specified default value.
      if (parser.getBooleanOptionValue("@", false))
        System.out.printf("Version: %s%n", "x.x.x");

      // Check for help option, and display if requested.
      // This checks the boolean/flag option values, with an assumed default value (false).
      if (parser.getBooleanOptionValue("?") || args.length == 0)
      {
        // Usage message is generated automatically from the defined options.
        // The last string defines extra (non-option) arguments.
        parser.printUsage(System.out, true, null, extraArgs, extraInfo);
        System.exit(0);
      }

      // Get resized image dimensions.
      // Don't care about a default value, as options are mandatory.
      final Integer imageW = parser.getOptionValue(oWidth, null);      // Get by object reference.
      final Integer imageH = parser.getIntegerOptionValue("h", null);  // Get by short name.

      // Check for verbose option, and see how verbose user wants.
      // Get typed option value by long name.
      final List<Boolean> lVerbose = parser.getBooleanOptionValues("verbose");
      final int verbosity = lVerbose.size();
      if (verbosity > 0)
        System.out.printf("Width:%d, Height:%d%n", imageW, imageH);

      // Get folder.
      final File dir = parser.getFileOptionValue("d", new File("."));

      // Get output image format.
      // EnumeratedString option is retrieved with string-typed method.
      final String format = parser.getStringOptionValue("format", "jpg");
      if (verbosity > 0)
        System.out.printf("Image format: %s%n", format);

      // Display arguments not related to any defined options.
      if (verbosity > 0)
      {
        for (String s : parser.getNonOptionArguments())
          System.out.printf("Unknown argument: %s%n", s);
      }

      // Output specified options.
      System.out.printf("width  : %d%n", imageW);
      System.out.printf("height : %d%n", imageH);
      System.out.printf("dir    : %s%n", dir);
      System.out.printf("format : %s%n", format);
      System.out.printf("verbose: %b (%d)%n", verbosity > 0, verbosity);
    }
    catch (OptionException ox)
    {
      try
      {
        // If help required, print usage message.
        if (parser.getBooleanOptionValue("?") || args.length == 0)
        {
          parser.printUsage(System.out, true, null, extraArgs, extraInfo);
          System.exit(0);
        }
        System.err.println(ox.getMessage());
        parser.printUsage(System.out, false, null, extraArgs, extraInfo);
        System.exit(1);
      }
      catch (OptionException ox2)
      {
        ox2.printStackTrace();
      }
    }
  }
}
