LanguageTool for oXygen
=======================

A [LanguageTool](https://languagetool.org/) plugin
for [oXygen XML Author](http://www.oxygenxml.com/download_oxygenxml_author.html).


### Download and Install

* oXygen:
    * In the oXygen menu, open `Help -> Install new add-ons...`
    * In the field `Show add-ons from`, add this URL:
      https://raw.githubusercontent.com/danielnaber/oxygen-languagetool-plugin/master/extensions.xml
    * The LanguageTool add-on will be displayed. Follow the steps to install it and
      restart oXygen.
* Without installing LanguageTool - this might be okay for you if you just want to
  test the plugin:
    * In oXygen, go to `Options -> Preferences...` and on the `LanguageTool Plugin`
      page, check `Use internet server`
    * Limitations: slower performance; will not work on large texts (>50KB) or on texts
      where checking takes more than 10 seconds; will send 
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
* Tested with oXygen 16.1 only.


### Building

Building the source code requires Java 6 or later, Maven, and your `.m2/settings.xml` to be set up
[as described here](http://www.oxygenxml.com/oxygen_sdk_maven.html#maven_sdk_configuration).
Call `mvn package`, then unzip the resulting `target/oxygen-languagetool-plugin-(...)-plugin.jar`
to a sub-directory of your oXygen's `plugins` directory and restart oXygen.

The LanguageTool wiki contains [internal release documentation](http://wiki.languagetool.org/how-to-make-a-languagetool-for-oxygen-release). 

Thanks to [oXygen XML](http://www.oxygenxml.com) for providing me with a free license.

![oXygen XML editor](http://www.oxygenxml.com/img/resources/oxygen190x62.png)


### Changelog

* version 1.0.1 (not yet released):
    * the popup menu over errors now replaces the oXygen entries
      and only shows the error and corrections
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
