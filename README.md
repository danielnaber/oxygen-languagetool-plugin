oxygen-languagetool-plugin
==========================

**Prototype** of a LanguageTool plugin for the Oxygen XML editor

Major limitations, as this is just a prototype:
* requires a [LanguageTool](http://languagetool.org) server running on localhost, port 8081 (HTTP, not HTTPS)
* requires Java 7, but it seems Oxygen for the Mac comes with a bundled version of Java 6
* works in Author mode only
* requires clicking the "Check Text" button once
* uses the default language configured for the spell checker (ignores `lang` attributes)
* simply checks anything not inside XML tags (has no advanced logic how to transform XML to plain text, as needed by LanguageTool)
* ~~cannot highlight errors that affect only one character due to [a bug](http://www.oxygenxml.com/forum/topic10702.html) in Oxygen~~ fixed with Oxygen build 2014060420
* tested with Oxygen 16.0 only

Thanks to [Oxygen XML](http://www.oxygenxml.com) for providing me with a free license.

![Oxygen XML editor](http://www.oxygenxml.com/img/resources/oxygen190x62.png)

### Download and Install

Got to [the release tab](https://github.com/danielnaber/oxygen-languagetool-plugin/releases) and get the
latest release. Unzip it in the `plugins` directory of your Oxygen installation and restart Oxygen.

### Usage

Load an XML file, switch to author mode, and click the "Check text" button (you only need to do this
once after loading a file). Errors that can be detected by LanguageTool should become underlined.

### Building

Building the source code requires Java 7 or later, Maven, and your `.m2/settings.xml` to be set up
[as described here](http://www.oxygenxml.com/oxygen_sdk_maven.html#maven_sdk_configuration).

Call `mvn package`, then unzip the resulting `target/oxygen-sample-plugin-workspace-access-1.0-SNAPSHOT-plugin.jar`
to a sub-directory of your `oxygen/plugins` folder and restart Oxygen.
