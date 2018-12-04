# JCLAP: Java Command-Line Argument Parser

## What is it?

Command-line applications are very useful for getting things done, particularly when time is critical. Designing and developing a graphical user interface (GUI) for an application is often a lengthy and tedious process, and a large number of applications have no need for an interface so complicated. That said, the interface part is very important, and when an application needs to be run from the command-line, it helps enormously if it has been made simple to use. That's where JCLAP becomes useful. JCLAP helps Java developers to create simple-to-use command-line interfaces for their applications.

For example, if you wanted to write an application to process text files, you might imagine issuing a command such as:

```
java -jar Application.jar --folder ../foo --type xml
```

Or perhaps using short versions of the options:

```
java -jar Application.jar --folder ../foo --type xml
```

This utility allows you to easily parse all these command-line arguments, retrieve the values assigned, and even semi-automatically display help information about the available arguments. The primary aim is to parse command-line argument in the [POSIX](https://en.wikipedia.org/wiki/POSIX) standard, which is what most end users will already recognise. In addition JCLAP supports long names for options, and an option to stop argument parsing, in the style of the [GNU](https://en.wikipedia.org/wiki/GNU) standard. JCLAP takes this hybrid approach to provide a highly flexible way to specify arguments.

### Licence Agreement

JCLAP is available under a [BSD-style licence](https://github.com/gwinstanley/JCLAP/blob/master/src/main/resources/LICENSE.txt) as described below. This licence permits redistribution of the binary or source code (or both) for commercial or non-commercial use, provided the licence conditions are followed to acknowledge origination and authorship of the library.

## Where can I get it?

The simplest way is using [Apache Maven](https://maven.apache.org/), adding the following details to your POM file's dependencies section:

```
<dependency>
	<groupId>net.snaq</groupId>
	<artifactId>jclap</artifactId>
	<version>2.2</version>
</dependency>
```

Otherwise you can download JCLAP using the following links:

- [Download JAR archive](https://www.snaq.net/java/JCLAP/jclap-2.2.jar)
- [Download API documentation](https://www.snaq.net/java/JCLAP/jclap-2.2-javadoc.jar) (or [view online](https://www.snaq.net/java/JCLAP/2.2/apidocs/))

JCLAP requires Java Platform 8 or greater installed. The JAR also includes module support for Java 9+ (module name: ```net.snaq.jclap```).

## What about support?

All queries/issues/requests can be handled via the project [issues](https://github.com/gwinstanley/JCLAP/issues) page. Please include as much information as possible to help diagnose the problem (e.g. stacktraces, etc.). The library includes basic CLI translations for a few European languages (en/fr/de/es/pt), and chooses based on the default system locale. However, given my limited grasp of non-English technical jargon, some of the translations may be incorrect. Please contact me if you know more accurate translations for the terms, or want to provide another translation.

---

## Usage

- Each option must have a short-name (single alphanumeric character, ```?```, or ```@```)
- Each option may also have a long-name (multiple alphanumeric characters)
- Option long-name may include hyphens as non-terminal characters
- An option can be referenced by a single hyphen or forward-slash followed by its short name (e.g. ```-o``` or ```/o```)
- An option can be referenced by a double hyphen followed by its long name (e.g. ```--option```)
- An option which requires a value must have the value specified as the next argument
- An option and its value may be separated using a space, colon, or equals
	(e.g. ```-o value```, ```-o:value```, ```/o=value```, ```--option=value```, etc.)
- An option specified by short name and its value may be unseparated if not ambiguous
	(e.g. ```/ovalue```)
- Options not requiring values can be grouped in the short-form
	(e.g. "```-abc```" is equivalent to "```-a -b -c```")
- Options can appear in any order (e.g. ```-abc``` is equivalent to ```-cba```, and "```-i foo /o:bar```" is equivalent to "```-o bar -i=foo```"
- Options can appear multiple times (if defined to support multiple values)
- Options always precede non-option arguments (e.g. "```-abc -d val nonOption```")
- The ```--``` argument terminates options (e.g. -d is a non-option argument here: "```-abc -- -d```")
- The ```-``` option is typically used to represent the standard input stream; it is always stored as a non-option argument

JCLAP is simple to use, but it's recommended to follow certain guidelines to make the flow of your application predictable for end users. The basic rules for specifying options are shown in the table above. If you know the POSIX and GNU standards, then you'll quickly recognise how they operate, and even if not you may be familiar with the patterns. The easiest way to demonstrate is with a real-world example (see the [ParserExample](https://github.com/gwinstanley/JCLAP/blob/master/src/main/java/snaq/util/jclap/example/ParserExample.java) class).

## Creating your own option types

JCLAP also makes it straightforward to provide your own custom option types, if the built-in ones are not sufficient for your needs. As an example of how to create your own, take a look at the source code for the [LocalDateOption](https://github.com/gwinstanley/JCLAP/blob/master/src/main/java/snaq/util/jclap/LocalDateOption.java) class.

## Notes

### Strange characters on command-line

Some people are confused when trying to use "non-standard" (e.g. accented) characters in command-line applications, as non-English messages often show up with unexpected and/or garbled characters. Java supports the Unicode UTF-8 character set well, but many operating systems launch command-line utilities using the platform's default character/file encoding, and to complicate matters each terminal application has differing support for these encodings. These are some possible ways to help work around this:

- Ensure you use a UTF-8 compliant terminal shell
- Use an extra launch parameter: ```java -Dfile.encoding=UTF-8 ...```
- Set an environmental variable: ```JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8```

### Other libraries

JCLAP is by no means unique, and many similar utilities are available both for free and commercially. JCLAP was originally written to fulfil a specific need, then simply evolved as time passed, and it's proved useful in many applications. So many similar solutions now exist that it seems redundant to have yet another, but having already created JCLAP it seems beneficial to make it publicly available.
