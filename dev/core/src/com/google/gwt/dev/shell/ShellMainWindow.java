/*
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
import java.awt.Color;
import java.awt.HeadlessException;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
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
   * Launches a URL by copying it to the clipboard.
   */
  private class CopyToClipboardLauncher extends LaunchMethod {

    public CopyToClipboardLauncher() {
      super("Copy URL to clipboard");
    }

    @Override
    public void launchUrl(URL url) {
      if (getLogger().isLoggable(TreeLogger.INFO)) {
        getLogger().log(TreeLogger.INFO, "Paste " + url.toExternalForm()
            + " into a browser");
      }
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
      TreeLogger branch = getLogger().branch(TreeLogger.ERROR,
          "Unable to launch default browser", caught);
      if (branch.isLoggable(TreeLogger.INFO)) {
        branch.log(TreeLogger.INFO, url.toExternalForm());
      }
    }
  }

  /**
   * A class for implementing different methods of launching a URL.
   * <p>
   * Note that this is retained despite the UI change because we plan to support
   * multiple launcher types in the future.
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
  private JComboBox urlCombo;
  private JButton defaultBrowserButton;
  private JButton copyToClipboardButton;
  private JLabel loadingMessage;
  private JPanel launchPanel;

  /**
   * Create the main window with the top-level logger and launch controls.
   * <p>
   * MUST BE CALLED FROM THE UI THREAD
   *
   * @param maxLevel
   * @param logFile
   */
  public ShellMainWindow(TreeLogger.Type maxLevel, File logFile) {
    super(new BorderLayout());
    launchPanel = new JPanel(new WrapLayout());
    launchPanel.setBorder(BorderFactory.createTitledBorder(
        "Launch GWT Module"));
    launchPanel.add(new JLabel("Startup URL:"));
    JPanel startupPanel = new JPanel();
    urlCombo = new JComboBox();
    urlCombo.addItem("Computing...");
    startupPanel.add(urlCombo);
    launchPanel.add(startupPanel);
    loadingMessage = new JLabel("Loading...");
    launchPanel.add(loadingMessage);
    defaultBrowserButton = new JButton("Launch Default Browser");
    defaultBrowserButton.setMnemonic(KeyEvent.VK_L);
    defaultBrowserButton.setEnabled(false);
    defaultBrowserButton.setVisible(false);
    defaultBrowserButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        launch(new DefaultBrowserLauncher());
      }
    });
    launchPanel.add(defaultBrowserButton);
    copyToClipboardButton = new JButton("Copy to Clipboard");
    copyToClipboardButton.setMnemonic(KeyEvent.VK_C);
    copyToClipboardButton.setEnabled(false);
    copyToClipboardButton.setVisible(false);
    copyToClipboardButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        launch(new CopyToClipboardLauncher());
      }
    });
    launchPanel.add(copyToClipboardButton);
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
   * Indicate that all modules have been loaded -- on success, URLs previously
   * specified in {@link #setStartupUrls(Map)} may be launched.
   * <p>
   * MUST BE CALLED FROM THE UI THREAD
   *
   * @param successfulLoad true if all modules were successfully loaded
   */
  public void moduleLoadComplete(boolean successfulLoad) {
    if (!successfulLoad) {
      loadingMessage.setText("Module Load Failure");
      loadingMessage.setForeground(Color.RED);
      return;
    }
    if (urlCombo.getItemCount() == 0) {
      loadingMessage.setText("No URLs to Launch");
      loadingMessage.setForeground(Color.RED);
      urlCombo.addItem("No startup URLs");
      urlCombo.setEnabled(false);
      return;
    }
    loadingMessage.setVisible(false);
    defaultBrowserButton.setVisible(true);
    defaultBrowserButton.setEnabled(true);
    copyToClipboardButton.setVisible(true);
    copyToClipboardButton.setEnabled(true);
    launchPanel.revalidate();
    launchPanel.repaint();
  }

  /**
   * Create the UI to show available startup URLs.  These should not be
   * launchable by the user until the {@link #moduleLoadComplete(boolean)} method is
   * called.
   * <p>
   * MUST BE CALLED FROM THE UI THREAD

   * @param urls map of user-specified URL fragments to final URLs
   */
  public void setStartupUrls(final Map<String, URL> urls) {
    urlCombo.removeAllItems();
    ArrayList<String> keys = new ArrayList<String>(urls.keySet());
    Collections.sort(keys);
    for (String url : keys) {
      urlCombo.addItem(new UrlComboEntry(url, urls.get(url)));
    }
    urlCombo.revalidate();
  }

  /**
   * Launch the selected URL with the selected launch method.
   * <p>
   * MUST BE CALLED FROM THE UI THREAD
   * @param launcher
   */
  protected void launch(LaunchMethod launcher) {
    UrlComboEntry selectedUrl = (UrlComboEntry) urlCombo.getSelectedItem();
    if (launcher == null || selectedUrl == null) {
      // Shouldn't happen - should we log anything?
      return;
    }
    URL url = selectedUrl.getUrl();
    launcher.launchUrl(url);
  }
}
