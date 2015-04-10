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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class LanguageToolOptionPagePluginExtension extends OptionPagePluginExtension {

  static final String SERVER_URL_KEY = "serverUrl";
  static final String DEFAULT_URL = "http://localhost:8081";

  private final JTextField serverUrlField = new JTextField();

  @Override
  public String getTitle() {
    return "LanguageTool Plugin";
  }

  @Override
  public JComponent init(final PluginWorkspace pluginWorkspace) {
    JPanel panel = new JPanel(new GridBagLayout());

    GridBagConstraints cons = new GridBagConstraints();
    cons.insets = new Insets(0, 4, 0, 0);
    cons.gridx = 0;
    cons.gridy = 0;
    cons.anchor = GridBagConstraints.NORTH;
    cons.fill = GridBagConstraints.HORIZONTAL;

    panel.add(new JLabel("LanguageTool server URL:"), cons);

    cons.gridx = 1;
    cons.fill = GridBagConstraints.HORIZONTAL;
    cons.weightx = 1.0;
    serverUrlField.setText(getServerUrl(pluginWorkspace));
    serverUrlField.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        setServerUrl(pluginWorkspace);
      }
    });
    panel.add(serverUrlField, cons);

    return panel;
  }

  @Override
  public void apply(PluginWorkspace pluginWorkspace) {
    setServerUrl(pluginWorkspace);
  }

  @Override
  public void restoreDefaults() {
    serverUrlField.setText(DEFAULT_URL);
  }

  private String getServerUrl(PluginWorkspace pluginWorkspace) {
    return pluginWorkspace.getOptionsStorage().getOption(SERVER_URL_KEY, DEFAULT_URL);
  }

  private void setServerUrl(PluginWorkspace pluginWorkspace) {
    pluginWorkspace.getOptionsStorage().setOption(SERVER_URL_KEY, serverUrlField.getText());
  }

}
