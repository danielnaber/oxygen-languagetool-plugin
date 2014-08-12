/* LanguageTool plugin for Oxygen XML editor 
 * Copyright (C) 2014 Daniel Naber (http://www.danielnaber.de)
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

import java.util.HashMap;
import java.util.Map;

/**
 * Mapping from position in ASCII text as sent to LanguageTool to position in Oxygen.
 */
class TextWithMapping {

  private String text;

  private final Map<TextRange, TextRange> mapping = new HashMap<TextRange, TextRange>();

  void setText(String text) {
    this.text = text;
  }

  void addMapping(TextRange fromRange, TextRange toRange) {
    mapping.put(fromRange, toRange);
  }

  String getText() {
    return text;
  }

  int getOxygenPositionFor(int offset) {
    for (Map.Entry<TextRange, TextRange> entry : mapping.entrySet()) {
      TextRange langToolRange = entry.getKey();
      TextRange oxygenRange = entry.getValue();
      if (offset >= langToolRange.getFrom() && offset <= langToolRange.getTo()) {
        int subOffset = offset - langToolRange.getFrom();
        return oxygenRange.getFrom() + subOffset;
      }
    }
    throw new RuntimeException("Could not map offset " + offset + ", not found in mapping of size " + mapping.size());
  }

  @Override
  public String toString() {
    return "text='" + text + "' mapping=" + mapping;
  }
}
