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

import ro.sync.ecss.extensions.api.AuthorDocumentController;
import ro.sync.ecss.extensions.api.filter.AuthorFilteredContent;
import ro.sync.ecss.extensions.api.filter.AuthorNodesFilter;
import ro.sync.ecss.extensions.api.node.AuthorNode;

import javax.swing.text.BadLocationException;

/**
 * Collects the text part of an Author view in Oxygen, taking
 * into account what's an inline element and what's not.
 * See https://github.com/danielnaber/oxygen-languagetool-plugin/issues/6.
 * Needs some debugging, sometimes the position seems to be off by one.
 */
class TextCollector2 {

  TextWithMapping collectTexts(AuthorDocumentController docController) throws BadLocationException {
    int endOffset = docController.getAuthorDocumentNode().getEndOffset();
    final AuthorFilteredContent filteredContent = docController.getFilteredContent(0, endOffset, new AuthorNodesFilter() {
      @Override
      public boolean shouldFilterNode(AuthorNode authorNode) {
        return false;
      }
    });
    TextWithMapping mapping = new TextWithMapping() {
      @Override
      int getOxygenPositionFor(int offset) {
        return filteredContent.getOriginalOffset(offset) - 1;
      }
      @Override
      String getText() {
        return String.valueOf(filteredContent);
      }
    };
    return mapping;
  }

}
