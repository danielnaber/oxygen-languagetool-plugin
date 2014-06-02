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
import org.w3c.dom.Node;
import ro.sync.ecss.extensions.api.*;
import ro.sync.ecss.extensions.api.highlights.AuthorHighlighter;
import ro.sync.ecss.extensions.api.highlights.ColorHighlightPainter;
import ro.sync.ecss.extensions.api.highlights.Highlight;
import ro.sync.ecss.extensions.api.node.AuthorDocument;
import ro.sync.ecss.extensions.api.node.AuthorNode;
import ro.sync.ecss.extensions.api.structure.AuthorPopupMenuCustomizer;
import ro.sync.exml.editor.EditorPageConstants;
import ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.editor.page.author.WSAuthorEditorPage;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ToolbarComponentsCustomizer;
import ro.sync.exml.workspace.api.standalone.ToolbarInfo;
import ro.sync.exml.workspace.api.standalone.ui.ToolbarButton;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

@SuppressWarnings("CallToPrintStackTrace")
public class LanguageToolPluginExtension implements WorkspaceAccessPluginExtension {

  private static final String LANGUAGETOOL_URL = "http://localhost:8081/";
  private static final int MIN_WAIT_MILLIS = 500;
  private static final String PREFS_FILE = "oxyOptionsSa16.0.xml";
  private static final double MAX_REPLACEMENTS = 5;  // maximum number of suggestion shown in the context menu
  
  private final LanguageToolClient client = new LanguageToolClient(LANGUAGETOOL_URL);

  private StandalonePluginWorkspace pluginWorkspaceAccess;
  private AuthorPopupMenuCustomizer authorPopupMenuCustomizer;
  private Timer timer;
  
  @Override
  public void applicationStarted(final StandalonePluginWorkspace pluginWorkspaceAccess) {
    pluginWorkspaceAccess.setGlobalObjectProperty("can.edit.read.only.files", Boolean.FALSE);
    this.pluginWorkspaceAccess = pluginWorkspaceAccess;

    final Action checkTextAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent actionevent) {
        WSEditor editorAccess = pluginWorkspaceAccess.getCurrentEditorAccess(StandalonePluginWorkspace.MAIN_EDITING_AREA);
        if (editorAccess != null && EditorPageConstants.PAGE_TEXT.equals(editorAccess.getCurrentPageID())) {
          setupHighlightingForTextMode(editorAccess);
        }
        if (editorAccess != null && EditorPageConstants.PAGE_AUTHOR.equals(editorAccess.getCurrentPageID())) {
          setupHighlightingForAuthorMode(editorAccess);
        }
      }

      private void setupHighlightingForTextMode(WSEditor editorAccess) {
        //TODO: highlight in text mode
        //see http://www.oxygenxml.com/forum/topic10704.html
        /*WSTextEditorPage currentPage = (WSTextEditorPage)editorAccess.getCurrentPage();
        Object textComponent = currentPage.getTextComponent();
        if (textComponent instanceof JTextArea) {
           JTextArea textArea = (JTextArea) textComponent;
           Highlighter highlighter = textArea.getHighlighter();
          try {
            highlighter.addHighlight(from, to, new DefaultHighlighter.DefaultHighlightPainter(Color.RED));
          } catch (BadLocationException e) {
            e.printStackTrace();
          }
        }*/
      }
      
      private void setupHighlightingForAuthorMode(WSEditor editorAccess) {
        final WSAuthorEditorPage authorPageAccess = (WSAuthorEditorPage) editorAccess.getCurrentPage();
        if(authorPopupMenuCustomizer != null) {
          authorPageAccess.removePopUpMenuCustomizer(authorPopupMenuCustomizer);
        }
        authorPopupMenuCustomizer = new ApplyReplacementMenuCustomizer();
        authorPageAccess.addPopUpMenuCustomizer(authorPopupMenuCustomizer);
        final AuthorDocumentController controller = authorPageAccess.getDocumentController();
        final AuthorHighlighter highlighter = authorPageAccess.getHighlighter();
        controller.addAuthorListener(new AuthorListenerAdapter() {
          @Override
          public void documentChanged(AuthorDocument authorDocument, AuthorDocument authorDocument2) {
            // "A new document has been set into the author page."
            checkTextInBackground(highlighter, authorPageAccess);
          }
          @Override
          public void contentDeleted(DocumentContentDeletedEvent documentContentDeletedEvent) {
            checkTextInBackground(highlighter, authorPageAccess);
          }
          @Override
          public void contentInserted(DocumentContentInsertedEvent documentContentInsertedEvent) {
            checkTextInBackground(highlighter, authorPageAccess);
          }
        });
        checkTextInBackground(highlighter, authorPageAccess);
      }
    };

    // we need this as editorAccess is null in applicationStarted() at application start:
    pluginWorkspaceAccess.addToolbarComponentsCustomizer(new ToolbarComponentsCustomizer() {
      @Override
      public void customizeToolbar(ToolbarInfo toolbarInfo) {
        if ("SampleWorkspaceAccessToolbarID".equals(toolbarInfo.getToolbarID())) {
          List<JComponent> components = new ArrayList<>();
          JComponent[] initialComponents = toolbarInfo.getComponents();
          if (initialComponents != null && initialComponents.length > 0) {
            Collections.addAll(components, initialComponents);
          }
          ToolbarButton button = new ToolbarButton(checkTextAction, true);
          button.setText("Check Text");
          components.add(button);
          toolbarInfo.setComponents(components.toArray(new JComponent[components.size()]));
        }
      }
    });

  }

  // We cannot access the global preferences via API it seems (http://www.oxygenxml.com/forum/topic9966.html#p29244),
  // so we access the file on disk:
  private String getDefaultLanguageCode(WSAuthorEditorPage authorEditorPage) {
    String preferencesDir = authorEditorPage.getAuthorAccess().getWorkspaceAccess().getPreferencesDirectory();
    File preferencesFile = new File(preferencesDir, PREFS_FILE);
    if (preferencesFile.exists()) {
      try {
        String fileContent = loadFile(preferencesFile);
        Document preferencesDoc = XmlTools.getDocument(fileContent);
        XPath xPath = XPathFactory.newInstance().newXPath();
        Node node = (Node) xPath.evaluate("//field[@name='language']/String/text()", preferencesDoc, XPathConstants.NODE);
        return node.getNodeValue();
      } catch (Exception e) {
        System.err.println("Could not load language from " + preferencesFile + ": " + e.getMessage() + ", will use English for LanguageTool check");
        e.printStackTrace();
        return "en";
      }
    } else {
      System.err.println("Warning: No preference file found at " + preferencesFile + ", will use English for LanguageTool check");
      return "en";
    }
  }

  private String loadFile(File file) throws FileNotFoundException {
    StringBuilder sb = new StringBuilder();
    try (Scanner sc = new Scanner(file)) {
      while (sc.hasNextLine()) {
        sb.append(sc.nextLine());
      }
    }
    return sb.toString();
  }

  private synchronized void checkTextInBackground(final AuthorHighlighter highlighter, final WSAuthorEditorPage authorEditorPage) {
    if (timer != null) {
      timer.stop();
    }
    timer = new Timer(MIN_WAIT_MILLIS, new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        new Thread(new Runnable() {
          @Override
          public void run() {
            checkText(highlighter, authorEditorPage);
          }
        }).start();
      }
    });
    timer.start();
  }
  
  private void checkText(final AuthorHighlighter highlighter, final WSAuthorEditorPage authorEditorPage) {
    synchronized (this) {
      if (timer != null) {
        timer.stop();
      }
    }
    
    long startTime = System.currentTimeMillis();
    try {
      AuthorDocument authorDocumentNode = authorEditorPage.getDocumentController().getAuthorDocumentNode();
      List<AuthorNode> contentNodes = authorDocumentNode.getContentNodes();
      TextCollector textCollector = new TextCollector();
      TextWithMapping textWithMapping = textCollector.collectTexts(contentNodes);
      try {
        String langCode = getDefaultLanguageCode(authorEditorPage);
        // TODO: also consider document language ('xml:lang' or 'lang' attributes)
        List<RuleMatch> ruleMatches = client.checkText(textWithMapping, langCode);
        highlighter.removeAllHighlights();
        ColorHighlightPainter painter = new ColorHighlightPainter();
        for (RuleMatch match : ruleMatches) {
          int start = match.getOxygenOffsetStart();
          int end = match.getOxygenOffsetEnd();
          //System.out.println("Match: " + match.getOffsetStart() + "-" + match.getOffsetEnd() +
          //        " (Oxygen: " + start + "-"+ end + "): " + match.getMessage());
          highlighter.addHighlight(start, end, painter, match);
          // TODO: underlining a single char doesn't seem possible, see http://www.oxygenxml.com/forum/viewtopic.php?f=1&t=10702
          //highlighter.addHighlight(61, 61, painter, match);  -> doesn't underline anything. bug?
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Check time: " + (endTime-startTime) + "ms for " + textWithMapping.getText().length() + " bytes, "
                + ruleMatches.size() + " matches, language: " + langCode);
      } catch (Exception e) {
        showErrorDialog(e);
      }
    } catch (BadLocationException e) {
      e.printStackTrace();
    }
  }

  private void showErrorDialog(Exception e) {
    e.printStackTrace();
    String msg = e.getMessage() + "\n(See console output for full stacktrace.)";
    JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.ERROR_MESSAGE);
  }

  private void addMenuItems(JPopupMenu popUp, Highlight highlight, Action action) {
    RuleMatch match = (RuleMatch)highlight.getAdditionalData();
    JMenuItem menuItem = new JMenuItem(match.getMessage());
    popUp.add(menuItem);
    int replacementCount = 1;
    for (String replacement : match.getReplacements()) {
      JMenuItem replacementItem = new JMenuItem(action);
      replacementItem.setText(replacement);
      popUp.add(replacementItem);
      if (replacementCount++ >= MAX_REPLACEMENTS) {
        break;
      }
    }
  }

  @Override
  public boolean applicationClosing() {
    return true;
  }
  
  class ApplyReplacementMenuCustomizer implements AuthorPopupMenuCustomizer {
    @Override
    public void customizePopUpMenu(Object popUp, AuthorAccess authorAccess) {
      Highlight[] highlights = authorAccess.getEditorAccess().getHighlighter().getHighlights();
      int caretOffset = authorAccess.getEditorAccess().getCaretOffset();
      for (Highlight highlight : highlights) {
        if (caretOffset >= highlight.getStartOffset() && caretOffset <= highlight.getEndOffset()) {
          RuleMatch match = (RuleMatch) highlight.getAdditionalData();
          addMenuItems((JPopupMenu) popUp, highlight, new ApplyReplacementAction(match, highlight, authorAccess));
          break;
        }
      }
    }
  }

  class ApplyReplacementAction extends AbstractAction {
    
    private final RuleMatch match;
    private final Highlight highlight;
    private final AuthorAccess authorAccess;

    ApplyReplacementAction(RuleMatch match, Highlight highlight, AuthorAccess authorAccess) {
      this.match = match;
      this.highlight = highlight;
      this.authorAccess = authorAccess;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      WSEditor editorAccess = pluginWorkspaceAccess.getCurrentEditorAccess(StandalonePluginWorkspace.MAIN_EDITING_AREA);
      WSAuthorEditorPage authorPageAccess = (WSAuthorEditorPage) editorAccess.getCurrentPage();
      AuthorDocumentController controller = authorPageAccess.getDocumentController();
      boolean deleted = controller.delete(match.getOxygenOffsetStart(), match.getOxygenOffsetEnd());
      if (!deleted) {
        System.err.println("Could not delete text for match " + match);
      } else {
        controller.insertText(match.getOxygenOffsetStart(), event.getActionCommand());
        authorAccess.getEditorAccess().getHighlighter().removeHighlight(highlight);
      }
    }

  }
}
