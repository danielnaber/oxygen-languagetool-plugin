oxygen-languagetool-plugin
==========================

**Beta version** of a LanguageTool plugin for the Oxygen XML editor.

Known limitations:

* Requires a [LanguageTool](https://languagetool.org) server (2.9 or later).
  By default, it needs to be running at `http://localhost:8081`. Setting this up
  is very easy, see the "Download and Install" section below.
* Basically, it simply checks anything not inside XML tags. As the logic for
  text extraction is different in Author mode and Text mode, this can sometimes
  lead to different error messages in both modes.
* Uses the default language configured for the spell checker (ignores `lang` attributes).
* In text mode:
    * Will not properly work on XML that is not well-formed
    * Entities are not expanded
* Tested with Oxygen 16.1 only.

Thanks to [Oxygen XML](http://www.oxygenxml.com) for providing me with a free license.

![Oxygen XML editor](http://www.oxygenxml.com/img/resources/oxygen190x62.png)

### Download and Install

* Got to [the release tab](https://github.com/danielnaber/oxygen-languagetool-plugin/releases) and get the
  latest release. Unzip it in the `plugins` directory of your Oxygen installation and restart Oxygen.
* Start the [LanguageTool](https://languagetool.org) stand-alone version.
* Select the text language you have also configured as the default language in Oxygen.
* Go to `Text Checking -> Options...`
* Select `Run as server on port 8081` and `Use above settings for the server`
* Turn **off** the whitespace and spell checker rules. For English, these are at:
    * `Miscellaneous` -> `Whitespace repetition (bad formatting)`
    * `Possible Typo` -> `Possible spelling mistake`

### Usage

Load an XML file and click the `LanguageTool Check` button or press `Ctrl + Shift + Return`.
Errors detected by LanguageTool will become highlighted.

### Building

Building the source code requires Java 6 or later, Maven, and your `.m2/settings.xml` to be set up
[as described here](http://www.oxygenxml.com/oxygen_sdk_maven.html#maven_sdk_configuration).

Call `mvn package`, then unzip the resulting `target/oxygen-sample-plugin-workspace-access-1.0-SNAPSHOT-plugin.jar`
to a sub-directory of your `oxygen/plugins` folder and restart Oxygen.

### Changelog

* version 2015-xx-yy:
    * LanguageTool server can be configured at `Options -> Preferences... -> Plugins`
    * no more on-the-fly checking while typing
    * added `Ctrl + Shift + Return` as a shortcut to start the check
    * checking text now works properly for nested XML tags in author more
    * checking text now also works in text mode
    * colors error markers are loaded from `~/.languagetool.cfg`,
      if set there with e.g. `errorColors=style:#ffb8b8, typographical:#b8b8ff`
    * fixes for texts with special characters
    * works with Java 1.6
    * several cleanups
* version 2014-06-02:
    * first snapshot release
