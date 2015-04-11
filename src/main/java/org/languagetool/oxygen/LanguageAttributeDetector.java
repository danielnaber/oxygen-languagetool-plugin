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

import org.jetbrains.annotations.Nullable;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Parses the XML looking for the first {@code lang} or {@code xml:lang} attribute.
 */
class LanguageAttributeDetector {

  @Nullable
  String getDocumentLanguage(String xml) {
    try {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      SAXParser saxParser = factory.newSAXParser();
      saxParser.getXMLReader().setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      InputStream is = new ByteArrayInputStream(xml.getBytes());
      LangAttributeHandler handler = new LangAttributeHandler();
      saxParser.parse(is, handler);
      return handler.getLanguageCode();
    } catch (SAXParseException e) {
      // users may try to check a document that's not well-formed, so don't crash:
      System.err.println("Could not parse document to get document language: " + e.getMessage());
      return null;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static class LangAttributeHandler extends DefaultHandler {

    private String languageCode;

    @Override
    public void startElement(String namespaceURI, String lName,
                             String qName, Attributes attrs) throws SAXException {
      if (languageCode == null) {
        String lang = attrs.getValue("lang");
        String nsLang = attrs.getValue("xml:lang");
        if (lang != null) {
          languageCode = lang;
        } else if (nsLang != null) {
          languageCode = nsLang;
        }
      }
    }

    @Nullable
    public String getLanguageCode() {
      return languageCode;
    }
  }

}
