/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.HostedModeBase.HostedModeBaseOptions;
import com.google.gwt.dev.WebServerPanel.RestartAction;
import com.google.gwt.dev.shell.ShellMainWindow;
import com.google.gwt.dev.ui.DevModeUI;
import com.google.gwt.dev.ui.DoneCallback;
import com.google.gwt.dev.ui.DoneEvent;
import com.google.gwt.dev.ui.RestartServerCallback;
import com.google.gwt.dev.ui.RestartServerEvent;
import com.google.gwt.dev.util.collect.HashMap;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URL;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

/**
 * Implements the Swing UI for development mode.
 */
public class SwingUI extends DevModeUI {

  /**
   * Module handle for Swing UI.
   */
  public class SwingModuleHandle implements ModuleHandle {

    private final TreeLogger logger;
    private final ModulePanel tab;

    public SwingModuleHandle(TreeLogger logger, ModulePanel tab) {
      this.logger = logger;
      this.tab = tab;
    }

    public TreeLogger getLogger() {
      return logger;
    }
    
    public ModulePanel getTab() {
      return tab;
    }
  }

  /**
   * Interface to group activities related to adding and deleting tabs.
   */
  public interface TabPanelCollection {
    
    /**
     * Add a new tab containing a ModuleTabPanel.
     * 
     * @param tabPanel
     * @param icon
     * @param title
     * @param tooltip
     */
    void addTab(ModuleTabPanel tabPanel, ImageIcon icon, String title,
        String tooltip);
    
    /**
     * Remove the tab containing a ModuleTabpanel.
     * 
     * @param tabPanel
     */
    void removeTab(ModuleTabPanel tabPanel);
  }

  protected static final String PACKAGE_PATH = SwingUI.class.getPackage(
      ).getName().replace('.', '/').concat("/shell/");

  private static final Object sessionCounterLock = new Object();

  private static int sessionCounter = 0;

  /**
   * Loads an image from the classpath in this package.
   */
  static ImageIcon loadImageIcon(String name) {
    return loadImageIcon(name, true);
  }

  /**
   * Loads an image from the classpath, optionally prepending this package.
   * 
   * @param name name of an image file.
   * @param prependPackage true if {@link #PACKAGE_PATH} should be prepended to
   *     this name.
   * @return an ImageIcon instance to use -- in the event of an error loading
   *     the requested image, a blank ImageIcon is returned
   */
  static ImageIcon loadImageIcon(String name, boolean prependPackage) {
    ClassLoader cl = SwingUI.class.getClassLoader();
    if (prependPackage) {
      name = PACKAGE_PATH + name;
    }
    
    URL url = (name == null) ? null : cl.getResource(name);
    if (url != null) {
      ImageIcon image = new ImageIcon(url);
      return image;
    } else {
      // Bad image.
      return new ImageIcon();
    }
  }

  private final HostedModeBaseOptions options;
  private final Map<DevelModeTabKey, ModuleTabPanel> tabPanels = new HashMap<
      DevelModeTabKey, ModuleTabPanel>();

  private ShellMainWindow mainWnd;
  private JFrame frame;
  private JTabbedPane tabs;
  private WebServerPanel webServerLog;

  private TreeLogger topLogger;

  /**
   * Create a Swing UI instance.
   * 
   * @param options parsed command-line options
   */
  public SwingUI(HostedModeBaseOptions options) {
    this.options = options;
  }

  @Override
  public TreeLogger getTopLogger() {
    return topLogger;
  }

  @Override
  public TreeLogger getWebServerLogger(String serverName, byte[] serverIcon) {
    if (webServerLog == null) {
      RestartAction restartAction = null;
      final RestartServerCallback callback = getCallback(
          RestartServerEvent.getType());
      if (callback != null) {
        restartAction = new RestartAction() {
          public void restartServer(TreeLogger logger) {
            callback.onRestartServer(logger);
          }
        };
      }
      webServerLog = new WebServerPanel(options.getPort(), getLogLevel(),
          options.getLogFile("webserver.log"), restartAction);
      Icon serverIconImage = null;
      if (serverIcon != null) {
        serverIconImage = new ImageIcon(serverIcon);
      }
      tabs.insertTab(serverName, serverIconImage, webServerLog, null, 1);
    }
    return webServerLog.getLogger();
  }

  @Override
  public void initialize(Type logLevel) {
    super.initialize(logLevel);
    ImageIcon gwtIcon = loadImageIcon("icon24.png");
    frame = new JFrame("GWT Development Mode");
    tabs = new JTabbedPane();
    if (options.alsoLogToFile()) {
      options.getLogDir().mkdirs();
    }
    mainWnd = new ShellMainWindow(logLevel, options.getLogFile("main.log"));
    topLogger = mainWnd.getLogger();
    tabs.addTab("Development Mode", gwtIcon, mainWnd, "GWT Development Mode");
    frame.getContentPane().add(tabs);
    frame.setSize(950, 700);
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosed(WindowEvent e) {
        DoneCallback callback = getCallback(DoneEvent.getType());
        if (callback != null) {
          callback.onDone();
        }
      }
    });
    frame.setIconImage(loadImageIcon("icon16.png").getImage());
    frame.setVisible(true);
  }

  @Override
  public ModuleHandle loadModule(String userAgent, String remoteSocket,
      String url, String tabKey, String moduleName, String sessionKey,
      String agentTag, byte[] agentIcon, TreeLogger.Type logLevel) {
    // TODO(jat): add support for closing an active module
    ModuleTabPanel tabPanel = null;
    ModulePanel tab = null;
    tabPanel = findModuleTab(userAgent, remoteSocket, url, tabKey,
        moduleName, agentIcon);
    tab = tabPanel.addModuleSession(logLevel, moduleName, sessionKey,
        options.getLogFile(String.format("%s-%s-%d.log", moduleName, agentTag,
            getNextSessionCounter(options.getLogDir()))));
    TreeLogger logger = tab.getLogger();
    TreeLogger branch = logger.branch(TreeLogger.INFO, "Loading module "
        + moduleName);
    if (url != null) {
      branch.log(TreeLogger.INFO, "Top URL: " + url);
    }
    branch.log(TreeLogger.INFO, "User agent: " + userAgent);
    branch.log(TreeLogger.TRACE, "Remote socket: " + remoteSocket);
    if (tabKey != null) {
      branch.log(TreeLogger.DEBUG, "Tab key: " + tabKey);
    }
    if (sessionKey != null) {
      branch.log(TreeLogger.DEBUG, "Session key: " + sessionKey);
    }
    // TODO: Switch to a wait cursor?
    return new SwingModuleHandle(logger, tab);
  }

  @Override
  public void unloadModule(ModuleHandle module) {
    SwingModuleHandle handle = (SwingModuleHandle) module;
    Disconnectable tab = handle.getTab();
    if (tab != null) {
      tab.disconnect();
    }
  }

  protected int getNextSessionCounter(File logdir) {
    synchronized (sessionCounterLock) {
      if (sessionCounter == 0 && logdir != null) {
        // first time only, figure out the "last" session count already in use
        for (String filename : logdir.list()) {
          if (filename.matches("^[A-Za-z0-9_$]*-[a-z]*-[0-9]*.log$")) {
            String substring = filename.substring(filename.lastIndexOf('-') + 1,
                filename.length() - 4);
            int number = Integer.parseInt(substring);
            if (number > sessionCounter) {
              sessionCounter = number;
            }
          }
        }
      }
      // 
      return ++sessionCounter;
    }
  }

  private ModuleTabPanel findModuleTab(String userAgent, String remoteSocket,
      String url, String tabKey, String moduleName, byte[] agentIcon) {
    int hostEnd = remoteSocket.indexOf(':');
    if (hostEnd < 0) {
      hostEnd = remoteSocket.length();
    }
    String remoteHost = remoteSocket.substring(0, hostEnd);
    final DevelModeTabKey key = new DevelModeTabKey(userAgent, url, tabKey,
        remoteHost);
    ModuleTabPanel moduleTabPanel = tabPanels.get(key);
    if (moduleTabPanel == null) {
      moduleTabPanel = new ModuleTabPanel(userAgent, remoteSocket, url,
          agentIcon, new TabPanelCollection() {
            public void addTab(ModuleTabPanel tabPanel, ImageIcon icon,
                String title, String tooltip) {
              synchronized (tabs) {
                tabs.addTab(title, icon, tabPanel, tooltip);
                tabPanels.put(key, tabPanel);
              }
            }
  
            public void removeTab(ModuleTabPanel tabPanel) {
              synchronized (tabs) {
                tabs.remove(tabPanel);
                tabPanels.remove(key);
              }
            }
          }, moduleName);
    }
    return moduleTabPanel;
  }
}
