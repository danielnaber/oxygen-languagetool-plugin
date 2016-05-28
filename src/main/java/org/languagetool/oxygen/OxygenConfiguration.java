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
import java.io.File;
import java.io.FileInputStream;

/**
 * We cannot access the global preferences via API it seems (http://www.oxygenxml.com/forum/topic9966.html#p29244),
 * so we access the configuration file on disk.
 */
class OxygenConfiguration {

  private static final String PREFS_FILE = "oxyOptionsSa16.0.xml";

  private final StandalonePluginWorkspace pluginWorkspaceAccess;

  OxygenConfiguration(StandalonePluginWorkspace pluginWorkspaceAccess) {
    this.pluginWorkspaceAccess = pluginWorkspaceAccess;
  }

  String getDefaultLanguageCode() {
    String preferencesDir = pluginWorkspaceAccess.getPreferencesDirectory();
    File preferencesFile = new File(preferencesDir, PREFS_FILE);
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
