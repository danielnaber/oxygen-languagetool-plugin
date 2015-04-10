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

  private final String url;

  LanguageToolClient(String url) {
    this.url = url;
  }

  List<RuleMatch> checkText(TextWithMapping text, String langCode) {
    HttpURLConnection connection = null;
    try {
      String urlParameters =
              "language=" + langCode.replace('_', '-') +
              "&text=" + URLEncoder.encode(text.getText(), "utf-8");
      URL languageToolUrl = new URL(url);
      connection = openConnection(languageToolUrl);
      writeParameters(urlParameters, connection);
      // TODO: properly handle error 500 (which we get e.g. for unsupported languages)
      InputStream inputStream = connection.getInputStream();
      String xml = streamToString(inputStream, "utf-8");
      return parseXml(xml, text);
    } catch (MappingException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(
              "Could not check text using LanguageTool server at URL '" + url + "':\n" +
              e.getMessage() + "\n" +
              "Please make sure the LanguageTool server is running at this URL or\n" +
              "change the location at Options -> Preferences... -> Plugins.", e);
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
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
      wr.flush();
      wr.close();
    }
  }

  private static String streamToString(InputStream is, String charsetName) throws IOException {
    InputStreamReader isr = new InputStreamReader(is, charsetName);
    try {
      return readerToString(isr);
    } finally {
      isr.close();
    }
  }

  private static String readerToString(Reader reader) throws IOException {
    StringBuilder sb = new StringBuilder();
    int readBytes = 0;
    char[] chars = new char[4000];
    while (readBytes >= 0) {
      readBytes = reader.read(chars, 0, 4000);
      if (readBytes <= 0) {
        break;
      }
      sb.append(new String(chars, 0, readBytes));
    }
    return sb.toString();
  }

  private List<RuleMatch> parseXml(String xml, TextWithMapping text) throws XPathExpressionException {
    XPath xPath = XPathFactory.newInstance().newXPath();
    Document document = XmlTools.getDocument(xml);
    NodeList nodeSet = (NodeList) xPath.evaluate("//error", document, XPathConstants.NODESET);
    List<RuleMatch> matches = new ArrayList<RuleMatch>();
    for (int i = 0; i < nodeSet.getLength(); i++) {
      Node errorNode = nodeSet.item(i);
      RuleMatch ruleMatch = getRuleMatch(text, errorNode);
      matches.add(ruleMatch);
    }
    return matches;
  }

  private RuleMatch getRuleMatch(TextWithMapping text, Node errorNode) {
    NamedNodeMap attributes = errorNode.getAttributes();
    String message = attributes.getNamedItem("msg").getNodeValue();
    Node replacementAttribute = attributes.getNamedItem("replacements");
    List<String> replacements = replacementAttribute != null ?
            Arrays.asList(replacementAttribute.getNodeValue().split("#")) : Collections.<String>emptyList();
    String offsetStr = attributes.getNamedItem("offset").getNodeValue();
    String lengthStr = attributes.getNamedItem("errorlength").getNodeValue();
    Node issueType = attributes.getNamedItem("locqualityissuetype");
    int offset = Integer.parseInt(offsetStr);
    int length = Integer.parseInt(lengthStr);
    RuleMatch ruleMatch = new RuleMatch(message, offset, offset + length, replacements, issueType != null ? issueType.getNodeValue() : null);
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
