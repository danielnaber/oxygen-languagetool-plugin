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

import java.awt.*;
import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Load color configuration that might be in the user's configuration
 * file of LanguageTool.
 */
class ColorConfiguration {

  private final Map<String, Color> typeToColorMap;

  ColorConfiguration() throws IOException {
    this(new File(System.getProperty("user.home"), ".languagetool.cfg"));
  }

  ColorConfiguration(File file) throws IOException {
    this(new FileInputStream(file));
  }

  ColorConfiguration(InputStream configStream) throws IOException {
    Properties properties = new Properties();
    properties.load(configStream);
    typeToColorMap = getTypeToColorMap(properties.getProperty("errorColors"));
  }

  Map<String, Color> getTypeToColorMap() {
    return typeToColorMap;
  }

  // copied and adapted from LanguageTool's Configuration class:
  private Map<String, Color> getTypeToColorMap(String colorsString) {
    Map<String, Color> map = new HashMap<String, Color>();
    if (colorsString != null && !colorsString.isEmpty()) {
      String[] typeToColorList = colorsString.split(",\\s*");
      for (String typeToColor : typeToColorList) {
        String[] typeAndColor = typeToColor.split(":");
        if (typeAndColor.length != 2) {
          throw new RuntimeException("Could not parse type and color, colon expected: '" + typeToColor + "'");
        }
        String hexColor = typeAndColor[1];
        map.put(typeAndColor[0], Color.decode(hexColor));
      }
    }
    return Collections.unmodifiableMap(map);
  }

}
