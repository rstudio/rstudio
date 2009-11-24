/**
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.shell.log.SwingLoggerPanel;
import com.google.gwt.dev.util.BrowserLauncher;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Top-level window for the Swing DevMode UI.
 */
public class ShellMainWindow extends JPanel {

  /**
   * A class for implementing different methods of launching a URL.
   */
  private abstract static class LaunchMethod {

    private final String displayName;

    /**
     * Construct the launch method.
     * 
     * @param displayName the name that will display in the UI for this launch
     *     method
     */
    public LaunchMethod(String displayName) {
      this.displayName = displayName;
    }

    /**
     * Launch the specified URL.
     * 
     * @param url
     */
    public abstract void launchUrl(URL url);

    @Override
    public String toString() {
      return displayName;
    }
  }

  /**
   * Launches a URL using the default browser, as defined by
   * {@link BrowserLauncher}.
   */
  private class DefaultBrowserLauncher extends LaunchMethod {

    public DefaultBrowserLauncher() {
      super("Default browser");
    }

    @Override
    public void launchUrl(URL url) {
      Throwable caught = null;
      try {
        BrowserLauncher.browse(url.toExternalForm());
        return;
      } catch (IOException e) {
        caught = e;
      } catch (URISyntaxException e) {
        caught = e;
      }
      getLogger().log(TreeLogger.ERROR, "Unable to launch default browser",
          caught);
    }
  }

  /**
   * Launches a URL by copying it to the clipboard.
   */
  private class CopyToClipboardLauncher extends LaunchMethod {

    public CopyToClipboardLauncher() {
      super("Copy URL to clipboard");
    }

    @Override
    public void launchUrl(URL url) {
      // is it better to use SwingUtilities2.canAccessSystemClipboard() here?
      Throwable caught = null;
      try {
        Clipboard clipboard = logWindow.getToolkit().getSystemClipboard();
        StringSelection selection = new StringSelection(url.toExternalForm());
        clipboard.setContents(selection, selection);
        return;
      } catch (SecurityException e) {
        caught = e;
      } catch (HeadlessException e) {
        caught = e;
      }
      getLogger().log(TreeLogger.ERROR, "Unable to copy URL to clipboard",
          caught);
    }
  }

  /**
   * User-visible URL and complete URL for use in combo box.
   */
  private static class UrlComboEntry {
    
    private final String urlFragment;
    private final URL url;

    public UrlComboEntry(String urlFragment, URL url) {
      this.urlFragment = urlFragment;
      this.url = url;
    }
    
    public URL getUrl() {
      return url;
    }

    @Override
    public String toString() {
      return urlFragment;
    }
  }
  private SwingLoggerPanel logWindow;
  private JComboBox launchCombo;
  private JButton launchButton;

  private JComboBox urlCombo;

  /**
   * @param maxLevel
   * @param logFile
   */
  public ShellMainWindow(TreeLogger.Type maxLevel, File logFile) {
    super(new BorderLayout());
    // TODO(jat): add back when we have real options
    if (false) {
      JPanel panel = new JPanel(new GridLayout(2, 1));
      JPanel optionPanel = new JPanel();
      optionPanel.setBorder(BorderFactory.createTitledBorder("Options"));
      optionPanel.add(new JLabel("Miscellaneous options here"));
      panel.add(optionPanel);
      add(panel, BorderLayout.NORTH);
    }
    JPanel launchPanel = new JPanel();
    launchPanel.setBorder(BorderFactory.createTitledBorder(
        "Launch GWT Module"));
    launchPanel.add(new JLabel("Startup URL:"));
    JPanel startupPanel = new JPanel();
    urlCombo = new JComboBox();
    urlCombo.addItem("Computing...");
    startupPanel.add(urlCombo);
    launchPanel.add(startupPanel);
    launchPanel.add(new JLabel("Launch Method:"));
    launchCombo = new JComboBox();
    populateLaunchComboBox();
    launchPanel.add(launchCombo);
    launchButton = new JButton("Loading...");
    launchButton.setEnabled(false);
    launchButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        launch();
      }
    });
    launchPanel.add(launchButton);
    add(launchPanel, BorderLayout.NORTH);
    logWindow = new SwingLoggerPanel(maxLevel, logFile);
    add(logWindow);
  }

  /**
   * @return TreeLogger instance
   */
  public TreeLogger getLogger() {
    return logWindow.getLogger();
  }

  /**
   * Indicate that URLs specified in {@link #setStartupUrls(Map)} are now
   * launchable.
   */
  public void setLaunchable() {
    if (urlCombo.getItemCount() == 0) {
      launchButton.setText("No URLs to Launch");
      urlCombo.addItem("No startup URLs");
      urlCombo.setEnabled(false);
      return;
    }
    launchButton.setText("Launch");
    launchButton.setEnabled(true);
  }

  /**
   * Create the UI to show available startup URLs.  These should not be
   * launchable by the user until the {@link #setLaunchable()} method is called.
   * 
   * @param urls map of user-specified URL fragments to final URLs
   */
  public void setStartupUrls(Map<String, URL> urls) {
    urlCombo.removeAllItems();
    ArrayList<String> keys = new ArrayList<String>(urls.keySet());
    Collections.sort(keys);
    for (String url : keys) {
      urlCombo.addItem(new UrlComboEntry(url, urls.get(url)));
    }
    urlCombo.revalidate();
  }

  protected void launch() {
    LaunchMethod launcher = (LaunchMethod) launchCombo.getSelectedItem();
    UrlComboEntry selectedUrl = (UrlComboEntry) urlCombo.getSelectedItem();
    URL url = selectedUrl.getUrl();
    launcher.launchUrl(url);
  }

  private void populateLaunchComboBox() {
    // TODO(jat): support scanning for other browsers and launching them
    launchCombo.addItem(new DefaultBrowserLauncher());
    launchCombo.addItem(new CopyToClipboardLauncher());
  }
}
