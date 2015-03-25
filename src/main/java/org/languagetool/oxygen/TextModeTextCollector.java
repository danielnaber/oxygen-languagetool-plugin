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

/**
 * Collects the text part of a Text view in Oxygen.
 */
class TextModeTextCollector {

  TextWithMapping collectTexts(String content) {
    StringBuilder sb = new StringBuilder();
    TextWithMapping mapping = new TextWithMapping();
    boolean inTag = false;
    int xmlStart = 0;
    int plainTextStart = 0;
    for (int i = 0; i < content.length(); i++) {
      char c = content.charAt(i);
      // TODO: <!-- ... -->
      if (c == '<') {
        inTag = true;
        if (i - xmlStart > 0) {
          TextRange xmlRange = new TextRange(xmlStart, i);
          TextRange plainTextRange = new TextRange(plainTextStart, sb.length());
          //System.out.println("MAP: " + plainTextRange + " -> " + xmlRange);
          mapping.addMapping(plainTextRange, xmlRange);
        }
      } else if (c == '>') {
        inTag = false;
        xmlStart = i + 1;
        plainTextStart = sb.length();
      } else {
        if (!inTag) {
          sb.append(c);
        }
      }
    }
    mapping.setText(sb.toString());
    return mapping;
  }

}
