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
 * Considers comments to be text.
 */
class TextModeTextCollector {

  TextWithMapping collectTexts(String content) {
    StringBuilder sb = new StringBuilder();
    TextWithMapping mapping = new TextWithMapping();
    int inTag = 0;
    int xmlStart = 0;
    int plainTextStart = 0;
    for (int i = 0; i < content.length(); i++) {
      char c = content.charAt(i);
      boolean commentStart = i < content.length()-4 && content.substring(i, i+4).equals("<!--");
      if (c == '<' && !commentStart) {
        inTag++;
        if (i - xmlStart > 0) {
          TextRange plainTextRange = new TextRange(plainTextStart, sb.length());
          TextRange xmlRange = new TextRange(xmlStart, i);
          mapping.addMapping(plainTextRange, xmlRange);
        }
      } else if (c == '>' && inTag > 0) {
        inTag--;
        xmlStart = i + 1;
        plainTextStart = sb.length();
      } else {
        if (inTag == 0) {
          sb.append(c);
        }
      }
    }
    if (sb.length() > 0 && xmlStart != content.length()) {
      // also map remaining text (typically whitespace) after the last tag:
      TextRange xmlRange = new TextRange(xmlStart, content.length());
      TextRange plainTextRange = new TextRange(plainTextStart, sb.length());
      mapping.addMapping(plainTextRange, xmlRange);
    }
    mapping.setText(sb.toString());
    return mapping;
  }

}
