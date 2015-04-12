LanguageTool for oXygen
=======================

**Beta version** of a [LanguageTool](https://languagetool.org/) plugin
for [oXygen XML Author](http://www.oxygenxml.com/download_oxygenxml_author.html).

### Download and Install

* Go to [the release tab](https://github.com/danielnaber/oxygen-languagetool-plugin/releases) and get the
  latest release. Unzip it in the `plugins` directory of your oXygen installation and restart oXygen.
* Start the [LanguageTool](https://languagetool.org) stand-alone version.
* Go to `Text Checking -> Options...`
* Select `Run as server on port 8081` and `Use above settings for the server`
* Turn **off** the whitespace and spell checker rules. For English, these are at:
    * `Miscellaneous` -> `Whitespace repetition (bad formatting)`
    * `Possible Typo` -> `Possible spelling mistake`

### Usage

Load an XML file and click the `LanguageTool Check` button or press `Ctrl + Shift + Return`.
Errors detected by LanguageTool will become highlighted. If something doesn't work,
please start oXygen from the command line and see if you get any error messages there.
[Submit a bug report](https://github.com/danielnaber/oxygen-languagetool-plugin/issues) if
you have problems.

### Known Limitations

* Requires a [LanguageTool](https://languagetool.org) server (2.9 or later).
  By default, it needs to be running at `http://localhost:8081`. Setting this up
  is very easy, see the "Download and Install" section above.
* Basically, it simply checks anything not inside XML tags. As the logic for
  text extraction is different in Author mode and Text mode, this can sometimes
  lead to different error messages in both modes.
* Supports only one language per document. If it doesn't find a `lang` or `xml:lang`
  attribute, it uses the default language configured for the spell checker.
* In text mode:
    * Will not properly work on XML that is not well-formed
    * Entities are not expanded
* Tested with oXygen 16.1 only.


### Building

Building the source code requires Java 6 or later, Maven, and your `.m2/settings.xml` to be set up
[as described here](http://www.oxygenxml.com/oxygen_sdk_maven.html#maven_sdk_configuration).
Call `mvn package`, then unzip the resulting `target/oxygen-sample-plugin-workspace-access-1.0-SNAPSHOT-plugin.jar`
to a sub-directory of your `oxygen/plugins` folder and restart oXygen.

Thanks to [oXygen XML](http://www.oxygenxml.com) for providing me with a free license.

![oXygen XML editor](http://www.oxygenxml.com/img/resources/oxygen190x62.png)

### Changelog

* version 2015-xx-yy:
    * LanguageTool server can be configured at `Options -> Preferences... -> Plugins`
    * no more on-the-fly checking while typing
    * added `Ctrl + Shift + Return` as a shortcut to start the check
    * uses the first `lang` or `xml:lang` attribute to detect the document language,
      if none is found, it uses the default language
    * checking text now works properly for nested XML tags in author more
    * checking text now also works in text mode
    * colors error markers are loaded from `~/.languagetool.cfg`,
      if set there with e.g. `errorColors=style:#ffb8b8, typographical:#b8b8ff`
    * fixes for texts with special characters
    * works with Java 1.6
    * several cleanups
* version 2014-06-02:
    * first snapshot release
