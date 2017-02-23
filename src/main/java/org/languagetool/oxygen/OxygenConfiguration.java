/* LanguageTool plugin for Oxygen XML editor
 * Copyright (C) 2015 Daniel Naber (http://www.danielnaber.de)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package org.languagetool.oxygen;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;

/**
 * We cannot access the global preferences via API it seems (http://www.oxygenxml.com/forum/topic9966.html#p29244),
 * so we access the configuration file on disk.
 */
class OxygenConfiguration {

  private final String prefsFileTemplate;

  private final StandalonePluginWorkspace pluginWorkspaceAccess;

  OxygenConfiguration(StandalonePluginWorkspace pluginWorkspaceAccess) {
    this.pluginWorkspaceAccess = pluginWorkspaceAccess;
    Frame parentFrame = (Frame)pluginWorkspaceAccess.getParentFrame();
    String title = parentFrame.getTitle();
    // see https://www.oxygenxml.com/forum/post41681.html#p41681:
    if (title.contains("XML Editor")) {
      prefsFileTemplate = "oxyOptionsSa<VERSION>.xml";
    } else if (title.contains("XML Author")) {
      prefsFileTemplate = "oxyAuthorOptionsSa<VERSION>.xml";
    } else if (title.contains("XML Developer")) {
      prefsFileTemplate = "oxyDeveloperOptionsSa<VERSION>.xml";
    } else {
      throw new RuntimeException("Could not detect app, unexpected title: '" + title + "'");
    }
  }

  String getDefaultLanguageCode() {
    String preferencesDir = pluginWorkspaceAccess.getPreferencesDirectory();
    File preferencesFile = new File(preferencesDir, prefsFileTemplate.replace("<VERSION>", pluginWorkspaceAccess.getVersion()));
    if (preferencesFile.exists()) {
      try {
        FileInputStream stream = null;
        try {
          stream = new FileInputStream(preferencesFile);
          Document preferencesDoc = XmlTools.getDocument(stream);
          XPath xPath = XPathFactory.newInstance().newXPath();
          Node node = (Node) xPath.evaluate("//field[@name='language']/String/text()", preferencesDoc, XPathConstants.NODE);
          return node.getNodeValue();
        } finally {
          if (stream != null) {
            stream.close();
          }
        }
      } catch (Exception e) {
        System.err.println("Could not load language from " + preferencesFile + ": " + e.getMessage() + ", will use English for LanguageTool check");
        //noinspection CallToPrintStackTrace
        e.printStackTrace();
        return "en";
      }
    } else {
      System.err.println("Warning: No preference file found at " + preferencesFile + ", will use English for LanguageTool check");
      return "en";
    }
  }

}
