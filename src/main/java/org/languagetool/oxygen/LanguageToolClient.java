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

import org.languagetool.remote.RemoteLanguageTool;
import org.languagetool.remote.RemoteResult;
import org.languagetool.remote.RemoteRuleMatch;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Access to a LanguageTool instance via HTTP.
 */
class LanguageToolClient {

  enum SpellingRules { Consider, Ignore }
  enum WhitespaceRules { Consider, Ignore }

  private final String serverUrl;
  private final SpellingRules spellingRules;
  private final WhitespaceRules whitespaceRules;

  LanguageToolClient(String serverUrl, SpellingRules spellingRules, WhitespaceRules whitespaceRules) {
    this.serverUrl = serverUrl;
    this.spellingRules = spellingRules;
    this.whitespaceRules = whitespaceRules;
  }

  String getServerUrl() {
    return serverUrl;
  }

  List<RuleMatch> checkText(TextWithMapping text, String langCode) {
    try {
      List<RuleMatch> matches = new ArrayList<RuleMatch>();
      RemoteLanguageTool lt = new RemoteLanguageTool(new URL(serverUrl));
      RemoteResult result = lt.check(text.getText(), langCode.replace('_', '-'));
      List<RemoteRuleMatch> remoteMatches = result.getMatches();
      for (RemoteRuleMatch remoteMatch : remoteMatches) {
        RuleMatch ruleMatch = getRuleMatch(text, remoteMatch);
        boolean ignoreRule = isRuleIgnored(ruleMatch);
        if (!ignoreRule) {
          matches.add(ruleMatch);
        }
      }
      return matches;
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    } catch (MappingException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(
              "Could not check text using LanguageTool server at URL '" + serverUrl + "':\n\n" +
              e.getMessage() + "\n\n" +
              "Please make sure the LanguageTool server is running at this URL or\n" +
              "change the location at Options -> Preferences... -> Plugins.", e);
    }
  }

  // Should the rule be ignored? We ignore them client-side instead of sending the disabled=...
  // parameter to the server as that would cause the other settings the user made to be ignored.
  // We ignore the rules so the user doesn't need to visit the configuration dialog and
  // set them to ignored themselves (also, this way LT's public HTTP server can be used).
  private boolean isRuleIgnored(RuleMatch ruleMatch) {
    boolean ignoreRule = false;
    String ruleId = ruleMatch.getRuleId();
    if (spellingRules == SpellingRules.Ignore) {
      // There's no common id for a spell checker rule and locqualityissuetype="misspelling" isn't 
      // exactly what we need:
      if (ruleId.equals("HUNSPELL_RULE") || ruleId.startsWith("MORFOLOGIK_RULE_") || ruleId.endsWith("_SPELLER_RULE")) {
        ignoreRule = true;
      }
    }
    if (whitespaceRules == WhitespaceRules.Ignore) {
      // whitespace rule causes false alarms in text mode with XML indentation
      if (ruleId.equals("WHITESPACE_RULE")) {
        ignoreRule = true;
      }
    }
    return ignoreRule;
  }

  private RuleMatch getRuleMatch(TextWithMapping text, RemoteRuleMatch m) {
    int offset = m.getErrorOffset();
    int length = m.getErrorLength();
    List<String> replacements = m.getReplacements().orElse(new ArrayList<String>());
    RuleMatch ruleMatch = new RuleMatch(m.getRuleId(), m.getMessage(), offset,
            offset + length, replacements, m.getLocQualityIssueType().orElse(null));
    try {
      ruleMatch.setOxygenOffsetStart(text.getOxygenPositionFor(offset) + 1);
      ruleMatch.setOxygenOffsetEnd(text.getOxygenPositionFor(offset + length - 1) + 1);
    } catch (Exception e) {
      throw new MappingException("Could not map start or end offset of rule match '" +
              ruleMatch.getMessage() + "': " + offset + "-" + (offset + length - 1), e);
    }
    return ruleMatch;
  }

}
