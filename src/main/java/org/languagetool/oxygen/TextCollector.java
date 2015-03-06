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

import ro.sync.ecss.extensions.api.node.AuthorElement;
import ro.sync.ecss.extensions.api.node.AuthorNode;

import javax.swing.text.BadLocationException;
import java.util.List;

/**
 * Collects the text part of an Author view in Oxygen.
 */
class TextCollector {

  TextWithMapping collectTexts(List<AuthorNode> contentNodes) throws BadLocationException {
    TextWithMapping mapping = new TextWithMapping();
    StringBuilder sb = new StringBuilder();
    doCollectTexts(contentNodes, mapping, sb, 0);
    mapping.setText(sb.toString());
    return mapping;
  }
  
  private int doCollectTexts(List<AuthorNode> contentNodes, TextWithMapping mapping, StringBuilder text, int checkTextPos) throws BadLocationException {
    for (AuthorNode contentNode : contentNodes) {
      boolean isTextLevel;
      if (contentNode.getType() == AuthorNode.NODE_TYPE_ELEMENT) {
        AuthorElement authorElement = (AuthorElement) contentNode;
        List<AuthorNode> subContentNodes = authorElement.getContentNodes();
        isTextLevel = subContentNodes.size() == 0;
        checkTextPos = doCollectTexts(subContentNodes, mapping, text, checkTextPos);
      } else {
        isTextLevel = false;
      }
      // TODO: this is wrong: it will skip over eg. "<s>this is <b>text</b></s>" -
      //   see LanguageToolPluginExtension.checkText() instead
      if (isTextLevel) {
        TextRange textCheckRange = new TextRange(checkTextPos, checkTextPos + contentNode.getTextContent().length() + 1);
        TextRange oxygenRange = new TextRange(contentNode.getStartOffset(), contentNode.getEndOffset());
        mapping.addMapping(textCheckRange, oxygenRange);
        text.append(contentNode.getTextContent()).append("\n");
        checkTextPos += contentNode.getTextContent().length() + 1;
      }
    }
    return checkTextPos;
  }


}
