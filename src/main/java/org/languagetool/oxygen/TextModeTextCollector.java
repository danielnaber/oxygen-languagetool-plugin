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

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.StringReader;

/**
 * Collects the text part of a Text view in Oxygen.
 */
class TextModeTextCollector {

  TextWithMapping collectTexts(String content) {
    TextWithMapping mapping = new TextWithMapping();
    XmlHandler handler = new XmlHandler(mapping);
    final SAXParserFactory factory = SAXParserFactory.newInstance();
    try {
      final SAXParser saxParser = factory.newSAXParser();
      saxParser.getXMLReader().setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      saxParser.parse(new InputSource(new StringReader(content)), handler);
      mapping.setText(handler.sb.toString());
    } catch (ParserConfigurationException e) {
      // TODO: ???
      e.printStackTrace();
    } catch (SAXException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return mapping;
  }

  class XmlHandler extends DefaultHandler {

    private final StringBuilder sb = new StringBuilder();
    private final TextWithMapping mapping;

    public XmlHandler(TextWithMapping mapping) {
      this.mapping = mapping;
    }

    @Override
    public void characters(final char[] buf, final int offset, final int len) {
      //System.out.println(offset + ", l:" + len + " -> " + new String(buf, offset, len));
      TextRange plainTextRange = new TextRange(sb.length(), sb.length() + len);
      TextRange xmlRange = new TextRange(offset, offset + len);
      //System.out.println("MAP: " + plainTextRange + " -> " + xmlRange);
      sb.append(new String(buf, offset, len));
      mapping.addMapping(plainTextRange, xmlRange);
      // TODO: where to add spaces?
    }
  }

}
