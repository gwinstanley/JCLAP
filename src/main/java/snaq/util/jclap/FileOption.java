package snaq.util.jclap;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 *
 * @author Giles Winstanley
 */
public class FileOption extends StringOption
{
  /**
   * Creates a new {@code FileOption} instance.
   *
   * @param shortName short name of the option (e.g. -t)
   * @param longName long name of the option (e.g. --type)
   * @param description helpful description of the option (printed for usage message)
   * @param minCount minimum number of occurrences required for this option
   * @param maxCount maximum number of occurrences required for this option
   */
  public FileOption(String shortName, String longName, String description, int minCount, int maxCount)
  {
    super(shortName, longName, description, minCount, maxCount);
  }

  /**
   * Creates a new {@code FileOption} instance.
   *
   * @param shortName short name of the option (e.g. -t)
   * @param longName long name of the option (e.g. --type)
   * @param description helpful description of the option (printed for usage message)
   * @param mandatory whether this option must be specified
   * @param allowMany whether this option can take more than one value (i.e. be specified more than once)
   */
  public FileOption(String shortName, String longName, String description, boolean mandatory, boolean allowMany)
  {
    super(shortName, longName, description, mandatory, allowMany);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getUsageTypeString(ResourceBundle res)
  {
    Objects.requireNonNull(res);
    final FileFilter f = (FileFilter)getFilter().orElse(s -> true);
    String type = null;
    switch (f.fType)
    {
      case ACCEPT_FILE:
        type = "file";
        break;
      case ACCEPT_DIR:
        type = "dir";
        break;
      case ACCEPT_ALL:
      default:
        type = "all";
    }
    return res.getString("option.type.String.file." + type);
  }

  /**
   * Implementation of an {@code StringOption.Filter} which filters based
   * on existence and type of file to which the string refers.
   */
  public static final class FileFilter implements StringOption.Filter
  {
    /** Enumeration of possible file existence states to accept. */
    public enum AcceptExistance
    {
      /** Use to accept only existing files. */
      ACCEPT_EXISTING,
      /** Use to accept only non-existing files. */
      ACCEPT_NON_EXISTING,
      /** Use to accept existing or non-existing files. */
      ACCEPT_ALL
    }
    /** Enumeration of possible types to accept. */
    public enum AcceptFileType
    {
      /** Use to accept only files (not directories). */
      ACCEPT_FILE,
      /** Use to accept only directories (not files). */
      ACCEPT_DIR,
      /** Use to accept only files or directories. */
      ACCEPT_ALL
    }
    /** Determines acceptance of file based on existence of file. */
    private AcceptExistance eType = AcceptExistance.ACCEPT_ALL;
    /** Determines the file types to accept. */
    private AcceptFileType fType = AcceptFileType.ACCEPT_ALL;

    /**
     * Creates a new {@code FileOption} instance (allows files/folders).
     *
     * @param eType type of files to accept/reject based on existence
     * @param fType type of files to accept/reject based on file/directory status
     */
    public FileFilter(AcceptExistance eType, AcceptFileType fType)
    {
      this.eType = eType;
      this.fType = fType;
    }

    /**
     * Determines whether the string argument is valid as an option value.
     *
     * @param arg string argument to check for validity as an option value
     * @return Whether the string argument is valid as an option value.
     */
    @Override
    public boolean accept(String arg)
    {
      try
      {
        final File f = new File(arg).getCanonicalFile();
        switch (eType)
        {
          case ACCEPT_NON_EXISTING:
            if (f.exists())
              return false;
            break;
          case ACCEPT_EXISTING:
            if (!f.exists())
              return false;
            break;
          case ACCEPT_ALL:
          default:
        }
        switch (fType)
        {
          case ACCEPT_DIR:
            if (!f.isDirectory())
              return false;
            break;
          case ACCEPT_FILE:
            if (!f.isFile())
              return false;
            break;
          // This case performs checks to ensure File instance actually
          // denotes a real system-level file, not just garbage.
          case ACCEPT_ALL:
            if (f.isFile() || f.isDirectory())
              return true;
            break;
          default:
        }
        return true;
      }
      catch (IOException ex)
      {
        return false;
      }
    }
  }
}
