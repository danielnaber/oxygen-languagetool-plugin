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

import java.util.List;
import java.util.Objects;

/**
 * A potential problem found by LanguageTool. 
 */
class RuleMatch {

  private final String message;
  private final int offsetStart;
  private final int offsetEnd;
  private final List<String> replacements;
  private final String issueType;
  private final String ruleId;

  private int oxygenOffsetStart;
  private int oxygenOffsetEnd;

  RuleMatch(String ruleId, String message, int offsetStart, int offsetEnd, List<String> replacements, String issueType) {
    this.ruleId = Objects.requireNonNull(ruleId);
    this.message = Objects.requireNonNull(message);
    this.offsetStart = offsetStart;
    this.offsetEnd = offsetEnd;
    this.replacements = replacements;
    this.issueType = issueType;
  }

  String getRuleId() {
    return ruleId;
  }

  String getMessage() {
    return message;
  }

  int getOffsetStart() {
    return offsetStart;
  }

  int getOffsetEnd() {
    return offsetEnd;
  }

  List<String> getReplacements() {
    return replacements;
  }

  int getOxygenOffsetStart() {
    return oxygenOffsetStart;
  }

  void setOxygenOffsetStart(int origOffsetStart) {
    this.oxygenOffsetStart = origOffsetStart;
  }

  int getOxygenOffsetEnd() {
    return oxygenOffsetEnd;
  }

  void setOxygenOffsetEnd(int origOffsetEnd) {
    this.oxygenOffsetEnd = origOffsetEnd;
  }

  String getIssueType() {
    return issueType;
  }

  @Override
  public String toString() {
    return message + "/ " + offsetStart + "-" + offsetEnd + "(orig: " + oxygenOffsetStart + "-" + oxygenOffsetEnd + ")";
  }
}
