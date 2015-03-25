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

class TextRange {

  private final int from;
  private final int to;

  TextRange(int from, int to) {
    this.from = from;
    this.to = to;
  }

  int getFrom() {
    return from;
  }

  int getTo() {
    return to;
  }

  @Override
  public String toString() {
    return from + "-" + to;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TextRange textRange = (TextRange) o;
    if (from != textRange.from) return false;
    if (to != textRange.to) return false;
    return true;
  }

  @Override
  public int hashCode() {
    int result = from;
    result = 31 * result + to;
    return result;
  }
}
