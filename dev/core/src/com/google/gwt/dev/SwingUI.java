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
import com.google.gwt.dev.DevModeBase.HostedModeBaseOptions;
import com.google.gwt.dev.WebServerPanel.RestartAction;
import com.google.gwt.dev.shell.ShellMainWindow;
import com.google.gwt.dev.ui.DevModeUI;
import com.google.gwt.dev.ui.DoneCallback;
import com.google.gwt.dev.ui.DoneEvent;
import com.google.gwt.dev.ui.RestartServerCallback;
import com.google.gwt.dev.ui.RestartServerEvent;
import com.google.gwt.dev.util.collect.HashMap;

import java.awt.EventQueue;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

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
  protected class SwingModuleHandle implements ModuleHandle {

    private final ModulePanel tab;

    /**
     * Create an immutable module handle.
     * @param tab the module panel associated with this instance
     */
    public SwingModuleHandle(ModulePanel tab) {
      this.tab = tab;
    }

    @Override
    public TreeLogger getLogger() {
      return tab.getLogger();
    }

    /**
     * @return the ModulePanel associated with this module instance.
     */
    public ModulePanel getTab() {
      return tab;
    }

    @Override
    public void unload() {
      if (tab != null) {
        tab.disconnect();
      }
    }
  }

  /**
   * Interface to group activities related to adding and deleting tabs.
   */
  protected interface TabPanelCollection {

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
  public ModuleHandle getModuleLogger(final String userAgent,
      final String remoteSocket, final String url, final String tabKey,
      final String moduleName, final String sessionKey, final String agentTag,
      final byte[] agentIcon, final TreeLogger.Type logLevel) {
    // TODO(jat): add support for closing an active module
    ModuleHandle handle = invokeAndGet(new Callable<ModuleHandle>() {
          @Override
          public ModuleHandle call() throws Exception {
            ModuleTabPanel tabPanel = findModuleTab(userAgent, remoteSocket,
                url, tabKey, moduleName, agentIcon);
            ModulePanel tab = tabPanel.addModuleSession(logLevel, moduleName,
                sessionKey, options.getLogFile(String.format("%s-%s-%d.log",
                    moduleName, agentTag, getNextSessionCounter(
                        options.getLogDir()))));
            // TODO: Switch to a wait cursor?
            return new SwingModuleHandle(tab);
          }
        });
    TreeLogger logger = handle.getLogger();
    TreeLogger branch = logger.branch(TreeLogger.INFO, "Loading module "
        + moduleName);
    if (logger.isLoggable(TreeLogger.INFO)) {
      if (url != null) {
        branch.log(TreeLogger.INFO, "Top URL: " + url);
      }
      branch.log(TreeLogger.INFO, "User agent: " + userAgent);
    }
    if (logger.isLoggable(TreeLogger.TRACE)) {
      branch.log(TreeLogger.TRACE, "Remote socket: " + remoteSocket);
    }
    if (branch.isLoggable(TreeLogger.DEBUG)) {
      if (tabKey != null) {
        branch.log(TreeLogger.DEBUG, "Tab key: " + tabKey);
      }
      if (sessionKey != null) {
        branch.log(TreeLogger.DEBUG, "Session key: " + sessionKey);
      }
    }
    return handle;
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
          @Override
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
  public void initialize(final Type logLevel) {
    super.initialize(logLevel);
    invokeAndWait(new Runnable() {
      @Override
      public void run() {
        ImageIcon gwtIcon16 = loadImageIcon("icon16.png");
        ImageIcon gwtIcon24 = loadImageIcon("icon24.png");
        ImageIcon gwtIcon32 = loadImageIcon("icon32.png");
        ImageIcon gwtIcon48 = loadImageIcon("icon48.png");
        ImageIcon gwtIcon64 = loadImageIcon("icon64.png");
        ImageIcon gwtIcon128 = loadImageIcon("icon128.png");
        frame = new JFrame("GWT Development Mode");
        tabs = new JTabbedPane();
        if (options.alsoLogToFile()) {
          options.getLogDir().mkdirs();
        }
        mainWnd = new ShellMainWindow(logLevel, options.getLogFile("main.log"));
        topLogger = mainWnd.getLogger();
        tabs.addTab("Development Mode", gwtIcon24, mainWnd,
            "GWT Development Mode");
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
        setIconImages(topLogger, gwtIcon48, gwtIcon32, gwtIcon64, gwtIcon128,
            gwtIcon16);
        frame.setVisible(true);
      }
    });
    maybeInitializeOsXApplication();
  }

  @Override
  public void moduleLoadComplete(final boolean success) {
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        mainWnd.moduleLoadComplete(success);
      }
    });
  }

  @Override
  public void setStartupUrls(final Map<String, URL> urls) {
    invokeAndWait(new Runnable() {
      @Override
      public void run() {
        mainWnd.setStartupUrls(urls);
      }
    });
  }

  @Override
  public void setWebServerSecure(TreeLogger serverLogger) {
    if (webServerLog != null && serverLogger == webServerLog.getLogger()) {
      EventQueue.invokeLater(new Runnable() {
        @Override
        public void run() {
          // TODO(jat): if the web server has an icon, should combine with the
          // secure icon or perhaps switch to a different one.
          ImageIcon secureIcon = loadImageIcon("secure24.png");
          tabs.setIconAt(1, secureIcon);
        }
      });
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
            @Override
            public void addTab(ModuleTabPanel tabPanel, ImageIcon icon,
                String title, String tooltip) {
              synchronized (tabs) {
                tabs.addTab(title, icon, tabPanel, tooltip);
                tabPanels.put(key, tabPanel);
              }
            }

            @Override
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

  /**
   * Invoke a Callable on the UI thread, wait for it to finish, and return the
   * result.
   *
   * @param <T> return type
   * @param callable wrapper of the method to run on the UI thread
   * @return the return value of callable.call()
   * @throws RuntimeException if an error occurs
   */
  private <T> T invokeAndGet(Callable<T> callable) {
    FutureTask<T> task = new FutureTask<T>(callable);
    try {
      EventQueue.invokeAndWait(task);
      return task.get();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Invoke a Runnable on the UI thread and wait for it to finish.
   *
   * @param runnable
   * @throws RuntimeException if an error occurs
   */
  private void invokeAndWait(Runnable runnable) {
    try {
      EventQueue.invokeAndWait(runnable);
    } catch (InterruptedException e) {
      throw new RuntimeException("Error running on Swing UI thread", e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException("Error running on Swing UI thread", e);
    }
  }

  /**
   * This method contains code that will call certain Apple extensions to make
   * the DevMode app integrate into the system UI a bit better. These methods
   * are called reflectively so that we don't have to build gwt-dev against a
   * no-op stub library.
   */
  private void maybeInitializeOsXApplication() {
    Throwable ex;
    try {
      Class<?> applicationClass = Class.forName("com.apple.eawt.Application");

      topLogger.log(TreeLogger.SPAM, "Got Application class, on OS X");

      Object application = applicationClass.getMethod("getApplication").invoke(
          null);
      assert application != null : "application";

      // Remove the about menu entry
      applicationClass.getMethod("removeAboutMenuItem").invoke(application);

      // Remove the preferences menu entry
      applicationClass.getMethod("removePreferencesMenuItem").invoke(
          application);

      // Make the Dock icon pretty
      applicationClass.getMethod("setDockIconImage", Image.class).invoke(
          application, loadImageIcon("icon128.png").getImage());

      return;
    } catch (ClassNotFoundException e) {
      // Nothing to do here, this is expected on non-Apple JVMs.
      return;
    } catch (RuntimeException e) {
      ex = e;
    } catch (IllegalAccessException e) {
      ex = e;
    } catch (InvocationTargetException e) {
      ex = e;
    } catch (NoSuchMethodException e) {
      ex = e;
    }

    topLogger.log(TreeLogger.WARN, "Unable to initialize some OS X UI support",
        ex);
  }

  /**
   * Set the images for the frame.  On JDK 1.5, only the last icon supplied is
   * used for all needs.
   *
   * @param logger logger to use for warnings
   * @param icons one or more icons
   */
  private void setIconImages(TreeLogger logger, ImageIcon... icons) {
    if (icons.length == 0) {
      return;
    }
    Exception caught = null;
    try {
      // if this fails, we fall back to the JDK 1.5 method
      Method method = frame.getClass().getMethod("setIconImages", List.class);
      List<Image> imageList = new ArrayList<Image>();
      for (ImageIcon icon : icons) {
        Image image = icon.getImage();
        if (image != null) {
          imageList.add(image);
        }
      }
      method.invoke(frame, imageList);
      return;
    } catch (SecurityException e) {
      caught = e;
    } catch (IllegalArgumentException e) {
      caught = e;
    } catch (NoSuchMethodException e) {
      // ignore, expected on JDK 1.5
    } catch (IllegalAccessException e) {
      caught = e;
    } catch (InvocationTargetException e) {
      caught = e;
    }
    if (caught != null) {
      logger.log(TreeLogger.WARN, "Unexpected exception setting icon images",
          caught);
    }
    frame.setIconImage(icons[icons.length - 1].getImage());
  }
}
