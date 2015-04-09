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
import ro.sync.ecss.extensions.api.structure.AuthorPopupMenuCustomizer;
import ro.sync.exml.editor.EditorPageConstants;
import ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.editor.page.author.WSAuthorEditorPage;
import ro.sync.exml.workspace.api.editor.page.text.TextPopupMenuCustomizer;
import ro.sync.exml.workspace.api.editor.page.text.WSTextEditorPage;
import ro.sync.exml.workspace.api.standalone.MenuBarCustomizer;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ToolbarComponentsCustomizer;
import ro.sync.exml.workspace.api.standalone.ToolbarInfo;
import ro.sync.exml.workspace.api.standalone.ui.ToolbarButton;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.List;

@SuppressWarnings("CallToPrintStackTrace")
public class LanguageToolPluginExtension implements WorkspaceAccessPluginExtension {

  private static final String LANGUAGETOOL_URL = "http://localhost:8081/";
  private static final double MAX_REPLACEMENTS = 5;  // maximum number of suggestion shown in the context menu
  private static final String PREFS_FILE = "oxyOptionsSa16.0.xml";
  private static final Color DEFAULT_COLOR = new Color(255, 199, 66);

  private final LanguageToolClient client = new LanguageToolClient(LANGUAGETOOL_URL);
  private final PerEditorHighlightData perEditorHighlightData = new PerEditorHighlightData();

  private StandalonePluginWorkspace pluginWorkspaceAccess;
  private AuthorPopupMenuCustomizer authorPopupMenuCustomizer;
  private TextPopupMenuCustomizer textPopupMenuCustomizer;
  private Map<String, Color> errorTypeToColor = new HashMap<String, Color>();

  @Override
  public void applicationStarted(final StandalonePluginWorkspace pluginWorkspaceAccess) {
    pluginWorkspaceAccess.setGlobalObjectProperty("can.edit.read.only.files", Boolean.FALSE);
    this.pluginWorkspaceAccess = pluginWorkspaceAccess;
    try {
      ColorConfiguration colorConfiguration = new ColorConfiguration();
      errorTypeToColor = colorConfiguration.getTypeToColorMap();
    } catch (IOException e) {
      // The configuration file may not exist, e.g. because a remote server is used
      // for checking and LT has never been installed on this machine, so don't crash.
      System.err.println("Could not load color configuration. Stacktrace follows:");
      e.printStackTrace();
    }

    final Action checkTextAction = new AbstractAction("LanguageTool Check") {
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
          // activate for on-the-fly checking:
          /*textArea.addKeyListener(new KeyListener() { ... });*/
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
        final AuthorHighlighter highlighter = authorPageAccess.getHighlighter();
        checkTextInBackground(highlighter, authorPageAccess);
      }
    };

    pluginWorkspaceAccess.addMenuBarCustomizer(new MenuBarCustomizer() {
      @Override
      public void customizeMainMenu(JMenuBar mainMenu) {
        JMenuItem menuItem = new JMenuItem();
        menuItem.setAction(checkTextAction);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK));
        mainMenu.getMenu(6).add(menuItem);  // assume 6 is the "Tools" menu
      }
    });

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

  private synchronized void checkTextInBackground(final AuthorHighlighter highlighter, final WSAuthorEditorPage authorEditorPage) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        checkText(highlighter, authorEditorPage, pluginWorkspaceAccess);
      }
    }).start();
  }
  
  private synchronized void checkTextInBackground(final JTextArea textArea, final WSEditor editorAccess, final WSTextEditorPage currentPage) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        // comment in to fake slow server response:
        /*try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }*/
        checkText(textArea, editorAccess, currentPage, pluginWorkspaceAccess);
      }
    }).start();
  }

  private void checkText(AuthorHighlighter highlighter, WSAuthorEditorPage authorEditorPage, StandalonePluginWorkspace pluginWorkspaceAccess) {
    long startTime = System.currentTimeMillis();
    try {
      AuthorDocumentController docController = authorEditorPage.getDocumentController();
      TextCollector textCollector = new TextCollector();
      TextWithMapping textWithMapping = textCollector.collectTexts(docController);
      try {
        String langCode = getDefaultLanguageCode(pluginWorkspaceAccess);
        // TODO: also consider document language ('xml:lang' or 'lang' attributes)
        List<RuleMatch> ruleMatches = client.checkText(textWithMapping, langCode);
        highlighter.removeAllHighlights();
        for (RuleMatch match : ruleMatches) {
          int start = match.getOxygenOffsetStart();
          int end = match.getOxygenOffsetEnd();
          Color markerColor = getMarkerColor(match);
          ro.sync.exml.view.graphics.Color col =
                  new ro.sync.exml.view.graphics.Color(markerColor.getRed(), markerColor.getGreen(), markerColor.getBlue(), 255);
          ColorHighlightPainter painter = new ColorHighlightPainter();
          painter.setBgColor(col);
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
    long startTime = System.currentTimeMillis();
    try {
      String langCode = getDefaultLanguageCode(pluginWorkspaceAccess);
      Highlighter highlighter = textArea.getHighlighter();
      try {
        perEditorHighlightData.get(editorAccess).clear();
        highlighter.removeAllHighlights();

        TextModeTextCollector textCollector = new TextModeTextCollector();
        TextWithMapping textWithMapping = textCollector.collectTexts(textArea.getText());
        List<RuleMatch> ruleMatches = client.checkText(textWithMapping, langCode);
        for (RuleMatch ruleMatch : ruleMatches) {
          Color markerColor = getMarkerColor(ruleMatch);
          int start = ruleMatch.getOxygenOffsetStart() - 1;
          int end = ruleMatch.getOxygenOffsetEnd();
          highlighter.addHighlight(start, end, new DefaultHighlighter.DefaultHighlightPainter(markerColor));
          perEditorHighlightData.get(editorAccess).addInfo(new HighlightInfo(start, end, ruleMatch));
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
    } catch (MappingException e) {
      showErrorDialog("Your text could not be checked with LanguageTool.\nPlease make sure your XML is well-formed when you check it.", e);
    } catch (Exception e) {
      showErrorDialog(e);
    }
  }

  private Color getMarkerColor(RuleMatch ruleMatch) {
    Color colorOrNull = errorTypeToColor.get(ruleMatch.getIssueType());
    return colorOrNull != null ? colorOrNull : DEFAULT_COLOR;
  }

  private void showErrorDialog(String message, Exception e) {
    e.printStackTrace();
    JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
  }

  private void showErrorDialog(Exception e) {
    String msg = e.getMessage() + "\n(See console output for full stacktrace.)";
    showErrorDialog(msg, e);
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
        HighlightData highlightData = perEditorHighlightData.get(editorAccess);
        HighlightInfo hInfo = highlightData.getInfoForCaretOrNull(caretOffset);
        if (hInfo != null) {
          RuleMatch match = hInfo.ruleMatch;
          addMenuItems((JPopupMenu) popUp, match, new TextModeApplyReplacementAction(match, textPage, highlightData, editorAccess));
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
  static class PerEditorHighlightData {

    private final Map<String,HighlightData> highlightData = new HashMap<String,HighlightData>();  // editorAccess.getEditorLocation() -> HighlightData for that editor

    HighlightData get(WSEditor editor) {
      String key = editor.getEditorLocation().toString();
      if (!highlightData.containsKey(key)) {
        highlightData.put(key, new HighlightData());
      }
      return highlightData.get(key);
    }
  }

  /**
   * In text mode, we cannot add the additional info we need (the matching rule) to a highlight,
   * so we keep it here.
   */
  static class HighlightData {

    private final List<HighlightInfo> infos = new ArrayList<HighlightInfo>();

    void clear() {
      infos.clear();
    }

    void addInfo(HighlightInfo info) {
      infos.add(info);
    }

    @Nullable
    HighlightInfo getInfoForCaretOrNull(int caretOffset) {
      for (HighlightInfo highlight : infos) {
        if (caretOffset >= highlight.startOffset && caretOffset <= highlight.endOffset) {
          return highlight;
        }
      }
      return null;
    }

    @Override
    public String toString() {
      return infos.toString();
    }
  }

  static class HighlightInfo {

    private final int startOffset;
    private final int endOffset;
    private final RuleMatch ruleMatch;

    HighlightInfo(int startOffset, int endOffset, RuleMatch ruleMatch) {
      this.startOffset = startOffset;
      this.endOffset = endOffset;
      this.ruleMatch = Objects.requireNonNull(ruleMatch);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      HighlightInfo other = (HighlightInfo) o;
      if (endOffset != other.endOffset) return false;
      if (startOffset != other.startOffset) return false;
      return true;
    }

    @Override
    public int hashCode() {
      int result = startOffset;
      result = 31 * result + endOffset;
      return result;
    }

    @Override
    public String toString() {
      return startOffset + "-" + endOffset + ":" + ruleMatch.getMessage();
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
          AuthorHighlighter highlighter = authorAccess.getEditorAccess().getHighlighter();
          highlighter.removeAllHighlights();
          controller.insertText(match.getOxygenOffsetStart(), event.getActionCommand());
          checkTextInBackground(highlighter, authorPageAccess);
        }
      } finally {
        controller.endCompoundEdit();
      }
    }

  }

  class TextModeApplyReplacementAction extends AbstractAction {

    private final RuleMatch match;
    private final WSTextEditorPage textPage;
    private final HighlightData highlightData;
    private final WSEditor editorAccess;

    TextModeApplyReplacementAction(RuleMatch match, WSTextEditorPage textPage, HighlightData highlightData, WSEditor editorAccess) {
      this.match = Objects.requireNonNull(match);
      this.textPage = Objects.requireNonNull(textPage);
      this.highlightData = highlightData;
      this.editorAccess = editorAccess;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      Object textComponent = textPage.getTextComponent();
      if (textComponent instanceof JTextArea) {
        JTextArea textArea = (JTextArea) textComponent;
        textArea.getHighlighter().removeAllHighlights();
        highlightData.clear();
        textArea.replaceRange(event.getActionCommand(), match.getOxygenOffsetStart() - 1, match.getOxygenOffsetEnd());
        // If the replacement is longer or shorter than the original text, we would need to
        // adapt positions in the rule matches and highlights. As this is fragile, we simply
        // run a new check (using a background check so UI doesn't freeze):
        checkTextInBackground(textArea, editorAccess, textPage);
      }
    }

  }
}
