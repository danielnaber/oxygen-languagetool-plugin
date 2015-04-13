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

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import ro.sync.net.protocol.http.HttpExceptionWithDetails;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    HttpURLConnection connection = null;
    try {
      String urlParameters =
              "language=" + langCode.replace('_', '-') +
              "&text=" + URLEncoder.encode(text.getText(), "utf-8");
      URL languageToolUrl = new URL(serverUrl);
      connection = openConnection(languageToolUrl);
      writeParameters(urlParameters, connection);
      InputStream inputStream = connection.getInputStream();
      return parseXml(inputStream, text);
    } catch (MappingException e) {
      throw e;
    } catch (HttpExceptionWithDetails e) {
      String reason = getErrorReason(e);
      System.err.println(e.getMessage() + ": " + reason);
      throw new RuntimeException("Could not check text using LanguageTool server at URL '" + serverUrl + "':\n\n" +
              e.getMessage() + "\n" +
              reason + "\n\n" +
              "Please make sure the LanguageTool server is running at this URL or\n" +
              "change the location at Options -> Preferences... -> Plugins.", e);
    } catch (Exception e) {
      throw new RuntimeException(
              "Could not check text using LanguageTool server at URL '" + serverUrl + "':\n\n" +
              e.getMessage() + "\n\n" +
              "Please make sure the LanguageTool server is running at this URL or\n" +
              "change the location at Options -> Preferences... -> Plugins.", e);
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  private String getErrorReason(HttpExceptionWithDetails e) {
    String reason = e.getReason();
    if (reason.length() > 80) {
      reason = Helper.join(Helper.splitAtSpace(reason, 80), "\n");
      if (reason.length() > 500) {
        reason = reason.substring(0, 500) + "...";
      }
    }
    return reason;
  }

  private HttpURLConnection openConnection(URL languageToolUrl) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) languageToolUrl.openConnection();
    connection.setDoOutput(true);
    connection.setDoInput(true);
    connection.setInstanceFollowRedirects(false);
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    connection.setRequestProperty("charset", "utf-8");
    connection.setUseCaches(false);
    return connection;
  }

  private void writeParameters(String urlParameters, HttpURLConnection connection) throws IOException {
    DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
    try {
      wr.write(urlParameters.getBytes("UTF-8"));
    } finally {
      wr.close();
    }
  }

  private List<RuleMatch> parseXml(InputStream xml, TextWithMapping text) throws XPathExpressionException {
    XPath xPath = XPathFactory.newInstance().newXPath();
    Document document = XmlTools.getDocument(xml);
    NodeList nodeSet = (NodeList) xPath.evaluate("//error", document, XPathConstants.NODESET);
    List<RuleMatch> matches = new ArrayList<RuleMatch>();
    for (int i = 0; i < nodeSet.getLength(); i++) {
      Node errorNode = nodeSet.item(i);
      RuleMatch ruleMatch = getRuleMatch(text, errorNode);
      boolean ignoreRule = isRuleIgnored(ruleMatch);
      if (!ignoreRule) {
        matches.add(ruleMatch);
      }
    }
    return matches;
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

  private RuleMatch getRuleMatch(TextWithMapping text, Node errorNode) {
    NamedNodeMap attributes = errorNode.getAttributes();
    String ruleId = attributes.getNamedItem("ruleId").getNodeValue();
    String message = attributes.getNamedItem("msg").getNodeValue();
    Node replacementAttribute = attributes.getNamedItem("replacements");
    List<String> replacements = replacementAttribute != null ?
            Arrays.asList(replacementAttribute.getNodeValue().split("#")) : Collections.<String>emptyList();
    String offsetStr = attributes.getNamedItem("offset").getNodeValue();
    String lengthStr = attributes.getNamedItem("errorlength").getNodeValue();
    Node issueType = attributes.getNamedItem("locqualityissuetype");
    int offset = Integer.parseInt(offsetStr);
    int length = Integer.parseInt(lengthStr);
    RuleMatch ruleMatch = new RuleMatch(ruleId, message, offset, offset + length, replacements, issueType != null ? issueType.getNodeValue() : null);
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
