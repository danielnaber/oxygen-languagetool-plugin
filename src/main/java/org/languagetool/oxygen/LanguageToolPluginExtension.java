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

import org.jetbrains.annotations.Nullable;
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
import ro.sync.exml.workspace.api.editor.page.text.TextPopupMenuCustomizer;
import ro.sync.exml.workspace.api.editor.page.text.WSTextEditorPage;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ToolbarComponentsCustomizer;
import ro.sync.exml.workspace.api.standalone.ToolbarInfo;
import ro.sync.exml.workspace.api.standalone.ui.ToolbarButton;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

@SuppressWarnings("CallToPrintStackTrace")
public class LanguageToolPluginExtension implements WorkspaceAccessPluginExtension {

  private static final String LANGUAGETOOL_URL = "http://localhost:8081/";
  private static final int MIN_WAIT_MILLIS = 500;
  private static final double MAX_REPLACEMENTS = 5;  // maximum number of suggestion shown in the context menu
  private static final String PREFS_FILE = "oxyOptionsSa16.0.xml";
  
  private final LanguageToolClient client = new LanguageToolClient(LANGUAGETOOL_URL);
  private final HighlightData highlightData = new HighlightData();

  private StandalonePluginWorkspace pluginWorkspaceAccess;
  private AuthorPopupMenuCustomizer authorPopupMenuCustomizer;
  private TextPopupMenuCustomizer textPopupMenuCustomizer;
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
        // also see http://www.oxygenxml.com/forum/topic10704.html
        WSTextEditorPage currentPage = (WSTextEditorPage)editorAccess.getCurrentPage();
        Object textComponent = currentPage.getTextComponent();
        if (textComponent instanceof JTextArea) {
          JTextArea textArea = (JTextArea) textComponent;
          // TODO: activate if it's fast enough:
          /*textArea.addKeyListener(new KeyListener() {
              @Override
              public void keyTyped(KeyEvent e) {}
              @Override
              public void keyPressed(KeyEvent e) {}
              @Override
              public void keyReleased(KeyEvent e) {
                // see addAuthorListener() below
              }
          });*/
          checkText(textArea, editorAccess, currentPage, pluginWorkspaceAccess);
        }
      }
      
      private void setupHighlightingForAuthorMode(WSEditor editorAccess) {
        final WSAuthorEditorPage authorPageAccess = (WSAuthorEditorPage) editorAccess.getCurrentPage();
        if (authorPopupMenuCustomizer != null) {
          authorPageAccess.removePopUpMenuCustomizer(authorPopupMenuCustomizer);
        }
        authorPopupMenuCustomizer = new ApplyReplacementMenuCustomizerForAuthor();
        authorPageAccess.addPopUpMenuCustomizer(authorPopupMenuCustomizer);
        final AuthorDocumentController controller = authorPageAccess.getDocumentController();
        final AuthorHighlighter highlighter = authorPageAccess.getHighlighter();
        controller.addAuthorListener(new AuthorListenerAdapter() {
          @Override
          public void documentChanged(AuthorDocument authorDocument, AuthorDocument authorDocument2) {
            // "A new document has been set into the author page."
            checkTextInBackground(highlighter, authorPageAccess, pluginWorkspaceAccess);
          }
          @Override
          public void contentDeleted(DocumentContentDeletedEvent documentContentDeletedEvent) {
            checkTextInBackground(highlighter, authorPageAccess, pluginWorkspaceAccess);
          }
          @Override
          public void contentInserted(DocumentContentInsertedEvent documentContentInsertedEvent) {
            checkTextInBackground(highlighter, authorPageAccess, pluginWorkspaceAccess);
          }
        });
        checkTextInBackground(highlighter, authorPageAccess, pluginWorkspaceAccess);
      }
    };

    // we need this as editorAccess is null in applicationStarted() at application start:
    pluginWorkspaceAccess.addToolbarComponentsCustomizer(new ToolbarComponentsCustomizer() {
      @Override
      public void customizeToolbar(ToolbarInfo toolbarInfo) {
        if ("SampleWorkspaceAccessToolbarID".equals(toolbarInfo.getToolbarID())) {
          List<JComponent> components = new ArrayList<JComponent>();
          JComponent[] initialComponents = toolbarInfo.getComponents();
          if (initialComponents != null && initialComponents.length > 0) {
            Collections.addAll(components, initialComponents);
          }
          ToolbarButton button = new ToolbarButton(checkTextAction, true);
          button.setText("LanguageTool Check");
          components.add(button);
          toolbarInfo.setComponents(components.toArray(new JComponent[components.size()]));
        }
      }
    });

  }

  // We cannot access the global preferences via API it seems (http://www.oxygenxml.com/forum/topic9966.html#p29244),
  // so we access the file on disk:
  private String getDefaultLanguageCode(StandalonePluginWorkspace pluginWorkspaceAccess) {
    String preferencesDir = pluginWorkspaceAccess.getPreferencesDirectory();
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
    Scanner sc = new Scanner(file);
    try {
      while (sc.hasNextLine()) {
        sb.append(sc.nextLine());
      }
    } finally {
      sc.close();
    }
    return sb.toString();
  }

  private synchronized void checkTextInBackground(final AuthorHighlighter highlighter, final WSAuthorEditorPage authorEditorPage, final StandalonePluginWorkspace pluginWorkspaceAccess) {
    stopTimer();
    timer = new Timer(MIN_WAIT_MILLIS, new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        new Thread(new Runnable() {
          @Override
          public void run() {
            checkText(highlighter, authorEditorPage, pluginWorkspaceAccess);
          }
        }).start();
      }
    });
    timer.start();
  }
  
  private void checkText(AuthorHighlighter highlighter, WSAuthorEditorPage authorEditorPage, StandalonePluginWorkspace pluginWorkspaceAccess) {
    stopTimer();
    long startTime = System.currentTimeMillis();
    try {
      AuthorDocumentController docController = authorEditorPage.getDocumentController();
      AuthorDocument authorDocumentNode = docController.getAuthorDocumentNode();
      /* TODO: use this instead of TextCollector:
      //Why does this include some PI (processing instruction)?
      TextContentIterator textContentIterator = docController.getTextContentIterator(0, docController.getAuthorDocumentNode().getEndOffset());
      while (textContentIterator.hasNext()) {
        System.out.println("#"+textContentIterator.next().getText() + "#");
      }*/
      List<AuthorNode> contentNodes = authorDocumentNode.getContentNodes();
      TextCollector textCollector = new TextCollector();
      TextWithMapping textWithMapping = textCollector.collectTexts(contentNodes);
      try {
        String langCode = getDefaultLanguageCode(pluginWorkspaceAccess);
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

  private void checkText(JTextArea textArea, WSEditor editorAccess, WSTextEditorPage currentPage, StandalonePluginWorkspace pluginWorkspaceAccess) {
    stopTimer();
    long startTime = System.currentTimeMillis();
    try {
      String langCode = getDefaultLanguageCode(pluginWorkspaceAccess);
      Highlighter highlighter = textArea.getHighlighter();
      try {
        highlightData.clear(editorAccess);
        highlighter.removeAllHighlights();
        ro.sync.exml.view.graphics.Color painterColor = new ColorHighlightPainter().getColor();
        Color markerColor = new Color(painterColor.getRed(), painterColor.getGreen(), painterColor.getBlue());

        TextModeTextCollector textCollector = new TextModeTextCollector();
        TextWithMapping textWithMapping = textCollector.collectTexts(textArea.getText());
        List<RuleMatch> ruleMatches = client.checkText(textWithMapping, langCode);
        for (RuleMatch ruleMatch : ruleMatches) {
          int start = ruleMatch.getOxygenOffsetStart() - 1;
          int end = ruleMatch.getOxygenOffsetEnd();
          Object highlight = highlighter.addHighlight(start, end, new DefaultHighlighter.DefaultHighlightPainter(markerColor));
          highlightData.addInfo(new HighlightInfo(start, end, ruleMatch, highlight), editorAccess);
        }

        if (textPopupMenuCustomizer != null) {
          currentPage.removePopUpMenuCustomizer(textPopupMenuCustomizer);
        }
        textPopupMenuCustomizer = new ApplyReplacementMenuCustomizerForText(editorAccess);
        currentPage.addPopUpMenuCustomizer(textPopupMenuCustomizer);

        long endTime = System.currentTimeMillis();
        System.out.println("Check time: " + (endTime-startTime) + "ms for " + textWithMapping.getText().length() + " bytes, "
                + ruleMatches.size() + " matches, language: " + langCode);

      } catch (BadLocationException e) {
        e.printStackTrace();
      }
    } catch (Exception e) {
      showErrorDialog(e);
    }
  }

  private void stopTimer() {
    synchronized (this) {
      if (timer != null) {
        timer.stop();
      }
    }
  }

  private void showErrorDialog(Exception e) {
    e.printStackTrace();
    String msg = e.getMessage() + "\n(See console output for full stacktrace.)";
    JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.ERROR_MESSAGE);
  }

  private void addMenuItems(JPopupMenu popUp, RuleMatch match, Action action) {
    List<String> splitMessage = Helper.splitAtSpace(match.getMessage(), 40);
    int i = 0;
    for (String s : splitMessage) {
      JMenuItem menuItem = new JMenuItem(i == 0 ? s : "  " + s);
      popUp.add(menuItem);
      i++;
    }
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
  
  class ApplyReplacementMenuCustomizerForAuthor implements AuthorPopupMenuCustomizer {
    @Override
    public void customizePopUpMenu(Object popUp, AuthorAccess authorAccess) {
      Highlight[] highlights = authorAccess.getEditorAccess().getHighlighter().getHighlights();
      int caretOffset = authorAccess.getEditorAccess().getCaretOffset();
      for (Highlight highlight : highlights) {
        if (caretOffset >= highlight.getStartOffset() && caretOffset <= highlight.getEndOffset()) {
          RuleMatch match = (RuleMatch) highlight.getAdditionalData();
          addMenuItems((JPopupMenu) popUp, match, new AuthorModeApplyReplacementAction(match, highlight, authorAccess));
          break;
        }
      }
    }
  }

  class ApplyReplacementMenuCustomizerForText extends TextPopupMenuCustomizer {
    private final WSEditor editorAccess;
    ApplyReplacementMenuCustomizerForText(WSEditor editorAccess) {
      this.editorAccess = editorAccess;
    }
    @Override
    public void customizePopUpMenu(Object popUp, WSTextEditorPage textPage) {
      Object textComponent = textPage.getTextComponent();
      if (textComponent instanceof JTextArea) {
        int caretOffset = textPage.getCaretOffset();
        HighlightInfo hInfo = highlightData.getInfoForCaretOrNull(caretOffset, editorAccess);
        if (hInfo != null) {
          RuleMatch match = hInfo.ruleMatch;
          addMenuItems((JPopupMenu) popUp, match, new TextModeApplyReplacementAction(match, hInfo.highlight, textPage));
        }
      } else {
        System.err.println("textComponent not of type JTextArea: " + textComponent.getClass().getName());
      }
    }
  }

  /**
   * In text mode, we cannot add the additional info we need (the matching rule) to a highlight,
   * so we keep it here.
   */
  static class HighlightData {

    private final Map<String,HighlightData> highlightData = new HashMap<String,HighlightData>();  // editorAccess.getEditorLocation() -> HighlightData for that editor
    private final List<HighlightInfo> infos = new ArrayList<HighlightInfo>();

    void clear(WSEditor editor) {
      HighlightData data = highlightData.get(editor.getEditorLocation().toString());
      if (data != null) {
        data.infos.clear();
      }
    }

    void addInfo(HighlightInfo info, WSEditor editor) {
      String key = editor.getEditorLocation().toString();
      if (!highlightData.containsKey(key)) {
        highlightData.put(key, new HighlightData());
      }
      HighlightData data = highlightData.get(key);
      data.infos.add(info);
    }

    @Nullable
    HighlightInfo getInfoForCaretOrNull(int caretOffset, WSEditor editor) {
      HighlightData data = highlightData.get(editor.getEditorLocation().toString());
      if (data == null) {
        throw new NullPointerException("Could not find '" + editor.getEditorLocation() + "' in map keySet: " + highlightData.keySet());
      }
      for (HighlightInfo highlight : data.infos) {
        if (caretOffset >= highlight.startOffset && caretOffset <= highlight.endOffset) {
          return highlight;
        }
      }
      return null;
    }
  }

  static class HighlightInfo {

    private final int startOffset;
    private final int endOffset;
    private final RuleMatch ruleMatch;
    private final Object highlight;

    HighlightInfo(int startOffset, int endOffset, RuleMatch ruleMatch, Object highlight) {
      this.startOffset = startOffset;
      this.endOffset = endOffset;
      this.ruleMatch = Objects.requireNonNull(ruleMatch);
      this.highlight = Objects.requireNonNull(highlight);
    }
  }

  class AuthorModeApplyReplacementAction extends AbstractAction {

    private final RuleMatch match;
    private final Highlight highlight;
    private final AuthorAccess authorAccess;

    AuthorModeApplyReplacementAction(RuleMatch match, Highlight highlight, AuthorAccess authorAccess) {
      this.match = Objects.requireNonNull(match);
      this.highlight = Objects.requireNonNull(highlight);
      this.authorAccess = Objects.requireNonNull(authorAccess);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      WSEditor editorAccess = pluginWorkspaceAccess.getCurrentEditorAccess(StandalonePluginWorkspace.MAIN_EDITING_AREA);
      WSAuthorEditorPage authorPageAccess = (WSAuthorEditorPage) editorAccess.getCurrentPage();
      AuthorDocumentController controller = authorPageAccess.getDocumentController();
      controller.beginCompoundEdit();
      try {
        boolean deleted = controller.delete(match.getOxygenOffsetStart(), match.getOxygenOffsetEnd());
        if (!deleted) {
          System.err.println("Could not delete text for match " + match);
        } else {
          controller.insertText(match.getOxygenOffsetStart(), event.getActionCommand());
          authorAccess.getEditorAccess().getHighlighter().removeHighlight(highlight);
        }
      } finally {
        controller.endCompoundEdit();
      }
    }

  }

  static class TextModeApplyReplacementAction extends AbstractAction {

    private final RuleMatch match;
    private final Object textHighlight;
    private final WSTextEditorPage textPage;

    TextModeApplyReplacementAction(RuleMatch match, Object highlight, WSTextEditorPage textPage) {
      this.match = Objects.requireNonNull(match);
      this.textHighlight = Objects.requireNonNull(highlight);
      this.textPage = Objects.requireNonNull(textPage);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      //TODO:
      //controller.beginCompoundEdit();
      Object textComponent = textPage.getTextComponent();
      if (textComponent instanceof JTextArea) {
        JTextArea textArea = (JTextArea) textComponent;
        textArea.getHighlighter().removeHighlight(textHighlight);
        textArea.replaceRange(event.getActionCommand(), match.getOxygenOffsetStart() - 1, match.getOxygenOffsetEnd());
      }
    }

  }
}
