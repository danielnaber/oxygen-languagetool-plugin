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

import ro.sync.ecss.extensions.api.AuthorDocumentController;
import ro.sync.ecss.extensions.api.content.TextContentIterator;
import ro.sync.ecss.extensions.api.content.TextContext;
import ro.sync.ecss.extensions.api.node.AuthorNode;

import javax.swing.text.BadLocationException;

/**
 * Collects the text part of an Author view in Oxygen.
 */
class TextCollector {

  TextWithMapping collectTexts(AuthorDocumentController docController) throws BadLocationException {
    TextWithMapping mapping = new TextWithMapping();
    StringBuilder sb = new StringBuilder();
    TextContentIterator textContentIterator = docController.getTextContentIterator(0, docController.getAuthorDocumentNode().getEndOffset());
    while (textContentIterator.hasNext()) {
      TextContext content = textContentIterator.next();
      CharSequence text = content.getText();
      AuthorNode nodeAtOffset = docController.getNodeAtOffset(content.getTextEndOffset());
      if (nodeAtOffset.getType() != AuthorNode.NODE_TYPE_PI) {
        TextRange textCheckRange = new TextRange(sb.length(), sb.length() + text.length());
        TextRange oxygenRange = new TextRange(content.getTextStartOffset()-1, content.getTextEndOffset()-1);
        mapping.addMapping(textCheckRange, oxygenRange);
        sb.append(text);
        // We cannot easily tell where whitespace belongs as this requires some kind of "rendering"
        // of the text, so we guess that this is a place where adding whitespace makes sense:
        //sb.append(" ");
        // However, this would introduce a whitespace here, producing "Nautilus , leaving":
        // "This marvelous <i>Nautilus</i>, leaving."
      }
    }
    mapping.setText(sb.toString());
    return mapping;
  }

}
