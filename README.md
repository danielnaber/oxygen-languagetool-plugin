oxygen-languagetool-plugin
==========================

**Prototype** of a LanguageTool plugin for the Oxygen XML editor

Major limitations, as this is just a prototype:

* requires a [LanguageTool](https://languagetool.org) server running on localhost,
  port 8081 (HTTP, not HTTPS)
* Simply checks anything not inside XML tags (has no advanced logic how to
  transform XML to plain text, as needed by LanguageTool). As the logic for
  text extraction is different in Author mode and Text mode, this can sometimes
  lead to different error messages in both modes.
* uses the default language configured for the spell checker (ignores `lang` attributes)
* in text mode, XML comments are ignored
* in text mode, it will not properly work on XML that is not well-formed
* switching between tabs can sometimes lead to the same error showing up more than
  once in the context menu
* tested with Oxygen 16.1 only

Thanks to [Oxygen XML](http://www.oxygenxml.com) for providing me with a free license.

![Oxygen XML editor](http://www.oxygenxml.com/img/resources/oxygen190x62.png)

### Download and Install

Got to [the release tab](https://github.com/danielnaber/oxygen-languagetool-plugin/releases) and get the
latest release. Unzip it in the `plugins` directory of your Oxygen installation and restart Oxygen.

### Usage

Load an XML file, switch to author mode, and click the "LanguageTool Check" button or press Ctrl+Shift+Return.
Errors that can be detected by LanguageTool will become underlined.

### Building

Building the source code requires Java 6 or later, Maven, and your `.m2/settings.xml` to be set up
[as described here](http://www.oxygenxml.com/oxygen_sdk_maven.html#maven_sdk_configuration).

Call `mvn package`, then unzip the resulting `target/oxygen-sample-plugin-workspace-access-1.0-SNAPSHOT-plugin.jar`
to a sub-directory of your `oxygen/plugins` folder and restart Oxygen.

### Changelog

* 2015-04-08:
    * checking text should now work properly for nested XML tags
    * colors error markers are loaded from `~/.languagetool.cfg`,
      if set there with e.g. `errorColors=style:#ffb8b8, typographical:#b8b8ff`
* 2015-04-03:
    * added Ctrl+Shift+Return as a shortcut to start the check
* 2015-03-06:
    * fixes for texts with special characters
* 2014-08-12:
    * works with Java 1.6
    * some cleanups
* 2014-06-14:
    * first snapshot release
