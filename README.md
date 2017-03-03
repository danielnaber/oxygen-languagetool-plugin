LanguageTool for oXygen
=======================

A [LanguageTool](https://languagetool.org/) plugin
for [oXygen XML Author](http://www.oxygenxml.com/download_oxygenxml_author.html).


### Download and Install

* oXygen:
    * In the oXygen menu, open `Help -> Install new add-ons...`
    * In the field `Show add-ons from`, add this URL:
      https://raw.githubusercontent.com/danielnaber/oxygen-languagetool-plugin/master/extensions.xml?v13
    * The LanguageTool add-on will be displayed. Follow the steps to install it and
      restart oXygen.
* Without installing LanguageTool - this might be okay for you if you just want to
  test the plugin:
    * In oXygen, go to `Options -> Preferences...` and on the `LanguageTool Plugin`
      page, check `Use internet server`
    * Limitations: slower performance; will only work on short texts (about 15KB) and on texts
      where checking takes less than 10 seconds; will send 
      your texts over an encrypted connection to a LanguageTool server on the internet
      (see [our privacy policy](https://languagetool.org/privacy/))
* With installing LanguageTool:
    * Start the [LanguageTool](https://languagetool.org) stand-alone version.
    * Go to `Text Checking -> Options...`
    * Select `Run as server on port 8081` and `Use above settings for the server`
      and click `OK`


### Usage

Load an XML file and click the `LanguageTool Check` button or press `Ctrl + Shift + Return`.
Errors detected by LanguageTool will become highlighted. Click the right mouse button on
an error to get an error message and, for some errors, a list of corrections.

If something doesn't work,
please start oXygen from the command line and see if you get any error messages there.
[Submit a bug report](https://github.com/danielnaber/oxygen-languagetool-plugin/issues) if
you have problems.


### Known Limitations

* Basically, it simply checks anything not inside XML tags. As the logic for
  text extraction is different in Author mode and Text mode, this can sometimes
  lead to different error messages in both modes.
* Does not work in Grid view.
* Supports only one language per document. If it doesn't find a `lang` or `xml:lang`
  attribute, it uses the default language configured for the spell checker.
* There's no "ignore" option for individual errors.
* In Text mode:
    * Will not properly work on XML that is not well-formed
    * Entities are not expanded
* Checking long texts might take long. With checks that take longer than roughly
  30 seconds, a timeout error will occur.
* Only tested with oXygen 18.1.


### Building

Building the source code requires Java 8 or later, Maven, and your `.m2/settings.xml` to be set up
[as described here](http://www.oxygenxml.com/oxygen_sdk_maven.html#maven_sdk_configuration).
Call `mvn package`, then unzip the resulting `target/oxygen-languagetool-plugin-(...)-plugin.jar`
to a sub-directory of your oXygen's `plugins` directory and restart oXygen. You'll also
need to manually copy the library dependencies to the `lib` directory.

The LanguageTool wiki contains [internal release documentation](http://wiki.languagetool.org/how-to-make-a-languagetool-for-oxygen-release). 

Thanks to [oXygen XML](http://www.oxygenxml.com) for providing me with a free license.

![oXygen XML editor](http://www.oxygenxml.com/img/resources/oxygen190x62.png)


### Release

* use `deploy-dnaber.sh` to deploy and test locally
* make sure the libs in `plugin.xml` are still correct
* update version number in `extension.xml`
* update version number in `pom.xml`
* add a `<xt:extension>` section in `extensions.xml` with the new version number and filename
* update Changelog section below
* zip the `languagetool` directory in the `plugins` directory and call it `…jar` (as referenced from `extensions.xml`):
  `cd oxygen/plugins && zip -r oxygen-languagetool-plugin-1.X-plugin.jar languagetool/`
* upload the JAR at github

This process doesn't sign the JAR properly, though :-(


### Changelog

* version 1.3 (2017-02-23):
    * fixed loading the configuration file: XML Author, XML Editor, and
      XML Developer configuration files are now considered, depending on the
      currently running application
* version 1.2 (2017-02-20):
    * fixed a bug that caused using the configuration file of an old oXygen version
* version 1.1 (2016-06-21):
    * support and require LanguageTool's new JSON API (requires LanguageTool 3.4 or later)
    * the popup menu over errors now replaces the oXygen entries
      and only shows the error and corrections
    * works with Java 1.8 or later
* version 1.0.0 (2015-04-16):
    * LanguageTool server can be configured at `Options -> Preferences... -> Plugins`
    * no more on-the-fly checking while typing
    * added `Ctrl + Shift + Return` as a shortcut to start the check
    * uses the first `lang` or `xml:lang` attribute to detect the document language,
      if none is found, it uses the default language
    * checking text now works properly for nested XML tags in Author mode
    * checking text now also works in text mode
    * error marker colors are loaded from `.languagetool.cfg` in your home directory,
      if set there with e.g. `errorColors=style:#ffb8b8, typographical:#b8b8ff`
    * fixes for texts with special characters
    * works with Java 1.6
    * several cleanups
* version 2014-06-02:
    * first snapshot release
