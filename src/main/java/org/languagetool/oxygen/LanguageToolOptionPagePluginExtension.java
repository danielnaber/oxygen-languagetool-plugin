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

import ro.sync.exml.plugin.option.OptionPagePluginExtension;
import ro.sync.exml.workspace.api.PluginWorkspace;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

public class LanguageToolOptionPagePluginExtension extends OptionPagePluginExtension {

  static final String USE_INTERNET_SERVER_KEY = "useInternetServer";
  static final String SERVER_URL_KEY = "serverUrl";
  static final String DEFAULT_URL = "http://localhost:8081";
  static final String IGNORE_SPELLING_ERRORS_KEY = "ignoreSpellingErrors";
  static final String IGNORE_WHITESPACE_ERRORS_KEY = "ignoreWhitespaceErrors";

  private final JTextField serverUrlField = new JTextField();
  private final JCheckBox useInternetServer = new JCheckBox("Use internet server (slower; see http://languagetool.org for privacy policy)");
  private final JCheckBox ignoreSpellingErrors = new JCheckBox("Ignore spelling errors (as oXygen has its own spell checker)");
  private final JCheckBox ignoreWhitespaceErrors = new JCheckBox("Ignore whitespace errors (cause false alarms in text mode)");

  @Override
  public String getTitle() {
    return "LanguageTool Plugin";
  }

  @Override
  public JComponent init(final PluginWorkspace pluginWorkspace) {
    JPanel panel = new JPanel(new GridBagLayout());

    GridBagConstraints cons = new GridBagConstraints();
    cons.insets = new Insets(0, 4, 5, 0);
    cons.gridx = 0;
    cons.gridy = 0;
    cons.anchor = GridBagConstraints.NORTH;
    cons.fill = GridBagConstraints.HORIZONTAL;

    useInternetServer.setSelected(getUseInternetServer(pluginWorkspace));
    useInternetServer.addItemListener(e -> serverUrlField.setEnabled(e.getStateChange() == ItemEvent.DESELECTED));
    cons.gridwidth = 2;
    panel.add(useInternetServer, cons);

    cons.gridy = 1;
    cons.gridwidth = 1;
    panel.add(new JLabel("LanguageTool server URL:"), cons);

    cons.gridx = 1;
    cons.weightx = 1.0;
    serverUrlField.setText(getServerUrl(pluginWorkspace));
    serverUrlField.setEnabled(!getUseInternetServer(pluginWorkspace));
    panel.add(serverUrlField, cons);

    cons.gridx = 0;
    cons.gridy = 2;
    cons.gridwidth = 2;
    ignoreSpellingErrors.setSelected(getIgnoreSpellingErrors(pluginWorkspace));
    panel.add(ignoreSpellingErrors, cons);

    cons.gridx = 0;
    cons.gridy = 3;
    ignoreWhitespaceErrors.setSelected(getIgnoreWhitespaceErrors(pluginWorkspace));
    panel.add(ignoreWhitespaceErrors, cons);

    return panel;
  }

  @Override
  public void apply(PluginWorkspace pluginWorkspace) {
    setUseInternetServer(pluginWorkspace);
    setServerUrl(pluginWorkspace);
    setIgnoreSpellingErrors(pluginWorkspace);
    setIgnoreWhitespaceErrors(pluginWorkspace);
  }

  @Override
  public void restoreDefaults() {
    useInternetServer.setEnabled(false);
    serverUrlField.setText(DEFAULT_URL);
    ignoreSpellingErrors.setEnabled(true);
    ignoreWhitespaceErrors.setEnabled(true);
  }

  private boolean getUseInternetServer(PluginWorkspace pluginWorkspace) {
    // set the default to 'false' for privacy reasons:
    return pluginWorkspace.getOptionsStorage().getOption(USE_INTERNET_SERVER_KEY, "false").equals("true");
  }

  private void setUseInternetServer(PluginWorkspace pluginWorkspace) {
    pluginWorkspace.getOptionsStorage().setOption(USE_INTERNET_SERVER_KEY, String.valueOf(useInternetServer.isSelected()));
  }

  private String getServerUrl(PluginWorkspace pluginWorkspace) {
    return pluginWorkspace.getOptionsStorage().getOption(SERVER_URL_KEY, DEFAULT_URL);
  }

  private void setServerUrl(PluginWorkspace pluginWorkspace) {
    pluginWorkspace.getOptionsStorage().setOption(SERVER_URL_KEY, serverUrlField.getText());
  }

  private boolean getIgnoreSpellingErrors(PluginWorkspace pluginWorkspace) {
    return pluginWorkspace.getOptionsStorage().getOption(IGNORE_SPELLING_ERRORS_KEY, "true").equals("true");
  }

  private void setIgnoreSpellingErrors(PluginWorkspace pluginWorkspace) {
    pluginWorkspace.getOptionsStorage().setOption(IGNORE_SPELLING_ERRORS_KEY, String.valueOf(ignoreSpellingErrors.isSelected()));
  }

  private boolean getIgnoreWhitespaceErrors(PluginWorkspace pluginWorkspace) {
    return pluginWorkspace.getOptionsStorage().getOption(IGNORE_WHITESPACE_ERRORS_KEY, "true").equals("true");
  }

  private void setIgnoreWhitespaceErrors(PluginWorkspace pluginWorkspace) {
    pluginWorkspace.getOptionsStorage().setOption(IGNORE_WHITESPACE_ERRORS_KEY, String.valueOf(ignoreWhitespaceErrors.isSelected()));
  }

}
