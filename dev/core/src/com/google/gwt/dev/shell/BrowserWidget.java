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
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.Util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.StatusTextEvent;
import org.eclipse.swt.browser.StatusTextListener;
import org.eclipse.swt.browser.TitleEvent;
import org.eclipse.swt.browser.TitleListener;
import org.eclipse.swt.browser.WindowEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolItem;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents an individual browser window and all of its controls.
 */
public abstract class BrowserWidget extends Composite {

  private class Toolbar extends HeaderBarBase implements SelectionListener {
    private final ToolItem backButton;

    private final ToolItem forwardButton;

    private final ToolItem openWebModeButton;

    private final ToolItem refreshButton;
    private final ToolItem stopButton;

    public Toolbar(Composite parent) {
      super(parent);

      backButton = newItem("back.gif", "   &Back   ", "Go back one state");
      backButton.addSelectionListener(this);

      forwardButton = newItem("forward.gif", "&Forward", "Go forward one state");
      forwardButton.addSelectionListener(this);

      refreshButton = newItem("refresh.gif", " &Refresh ", "Reload the page");
      refreshButton.addSelectionListener(this);

      stopButton = newItem("stop.gif", "    &Stop    ", "Stop loading the page");
      stopButton.addSelectionListener(this);

      newSeparator();

      openWebModeButton = newItem("new-web-mode-window.gif", "&Compile/Browse",
          "Compiles and opens the current URL in the system browser");
      openWebModeButton.addSelectionListener(this);
      updateWebMode();
    }

    @Deprecated
    public void updateWebMode() {
      if (!openWebModeButton.isDisposed()) {
        if (getHost().isLegacyMode()) {
          openWebModeButton.setEnabled(!loadedModules.isEmpty());
        } else {
          openWebModeButton.setEnabled(true);
        }
      }
    }

    public void widgetDefaultSelected(SelectionEvent e) {
    }

    @SuppressWarnings("deprecation")
    public void widgetSelected(SelectionEvent evt) {
      if (evt.widget == backButton) {
        browser.back();
      } else if (evt.widget == forwardButton) {
        browser.forward();
      } else if (evt.widget == refreshButton) {
        // we have to clean up old module spaces here b/c we don't get a
        // location changed event

        // lastHostPageLocation = null;
        browser.refresh();
      } else if (evt.widget == stopButton) {
        browser.stop();
      } else if (evt.widget == openWebModeButton) {
        // first, compile
        try {
          Cursor waitCursor = getDisplay().getSystemCursor(SWT.CURSOR_WAIT);
          getShell().setCursor(waitCursor);
          if (getHost().isLegacyMode()) {
            Set<String> keySet = new HashSet<String>();
            for (Map.Entry<?, ModuleSpace> entry : loadedModules.entrySet()) {
              ModuleSpace module = entry.getValue();
              keySet.add(module.getModuleName());
            }
            String[] moduleNames = Util.toStringArray(keySet);
            if (moduleNames.length == 0) {
              // A latent problem with a module.
              //
              openWebModeButton.setEnabled(false);
              return;
            }
            getHost().compile(moduleNames);
          } else {
            getHost().compile();
          }
        } catch (UnableToCompleteException e) {
          // Already logged by callee.
          //
          MessageBox msgBox = new MessageBox(getShell(), SWT.OK
              | SWT.ICON_ERROR);
          msgBox.setText("Compilation Failed");
          msgBox.setMessage("Compilation failed. Please see the log in the development shell for details.");
          msgBox.open();
          return;
        } finally {
          // Restore the cursor.
          //
          Cursor normalCursor = getDisplay().getSystemCursor(SWT.CURSOR_ARROW);
          getShell().setCursor(normalCursor);
        }

        String locationText = location.getText();

        launchExternalBrowser(logger, locationText);
      }
    }
  }

  public static void launchExternalBrowser(TreeLogger logger, String location) {
    String browserCmd = System.getenv("GWT_EXTERNAL_BROWSER");
    if (browserCmd != null) {
      browserCmd += " " + location;
      try {
        Runtime.getRuntime().exec(browserCmd);
        return;
      } catch (IOException e) {
        logger.log(TreeLogger.ERROR,
            "Error launching GWT_EXTERNAL_BROWSER executable '" + browserCmd
                + "'", e);
        return;
      }
    }

    // legacy: gwt.browser.default
    browserCmd = System.getProperty("gwt.browser.default");
    if (browserCmd != null) {
      browserCmd += " " + location;
      try {
        Runtime.getRuntime().exec(browserCmd);
        return;
      } catch (IOException e) {
        logger.log(TreeLogger.ERROR,
            "Error launching gwt.browser.default executable '" + browserCmd
                + "'", e);
        return;
      }
    }

    // Programmatically try to find something that can handle html files
    Program browserProgram = Program.findProgram("html");
    if (browserProgram != null) {
      if (browserProgram.execute(location)) {
        return;
      } else {
        logger.log(TreeLogger.ERROR, "Error launching external HTML program '"
            + browserProgram.getName() + "'", null);
        return;
      }
    }

    // We're out of options, so fail.
    logger.log(TreeLogger.ERROR,
        "Unable to find a default external web browser", null);

    logger.log(TreeLogger.WARN, "Try setting the environment variable "
        + "GWT_EXTERNAL_BROWSER to your web browser executable before "
        + "launching the GWT shell", null);
  }

  protected Browser browser;

  private Color bgColor = new Color(null, 239, 237, 216);

  private Button goButton;

  private final BrowserWidgetHost host;

  private final Map<Object, ModuleSpace> loadedModules = new HashMap<Object, ModuleSpace>();

  private Text location;

  private final TreeLogger logger;

  private Label statusBar;

  private Toolbar toolbar;

  public BrowserWidget(Composite parent, BrowserWidgetHost host) {
    super(parent, SWT.NONE);

    this.host = host;
    logger = this.host.getLogger();

    bgColor = new Color(null, 239, 237, 216);

    toolbar = new Toolbar(this);
    Composite secondBar = buildLocationBar(this);

    browser = new Browser(this, SWT.NONE);

    {
      statusBar = new Label(this, SWT.BORDER | SWT.SHADOW_IN);
      statusBar.setBackground(bgColor);
      GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
      gridData.verticalAlignment = GridData.CENTER;
      gridData.verticalIndent = 0;
      gridData.horizontalIndent = 0;
      statusBar.setLayoutData(gridData);
    }

    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    layout.verticalSpacing = 1;
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    setLayout(layout);

    toolbar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    secondBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    GridData data = new GridData(GridData.FILL_BOTH);
    data.grabExcessVerticalSpace = true;
    data.grabExcessHorizontalSpace = true;
    browser.setLayoutData(data);

    // Hook up all appropriate event listeners.
    //
    hookBrowserListeners();
  }

  /**
   * Gets the browser object wrapped by this window.
   */
  public Browser getBrowser() {
    return browser;
  }

  public BrowserWidgetHost getHost() {
    return host;
  }

  public abstract String getUserAgent();

  /**
   * Go to a given url, possibly rewriting it if it can be served from any
   * project's public directory.
   */
  public void go(String target) {
    String url = host.normalizeURL(target);
    browser.setUrl(url);
  }

  public void onFirstShown() {
    String baseUrl = host.normalizeURL("/");
    setLocationText(baseUrl);
    location.setFocus();
    location.setSelection(baseUrl.length());
    location.addFocusListener(new FocusListener() {
      public void focusGained(FocusEvent e) {
        int length = location.getText().length();
        location.setSelection(length, length);
      }

      public void focusLost(FocusEvent e) {
      }
    });
  }

  /**
   * Initializes and attaches module space to this browser widget. Called by
   * subclasses in response to calls from JavaScript.
   * 
   * @param moduleSpaceLogger logger to use for attaching the ModuleSpace
   * @param space ModuleSpace instance to initialize
   */
  protected final void attachModuleSpace(TreeLogger moduleSpaceLogger,
      ModuleSpace space) throws UnableToCompleteException {
    Object key = space.getKey();
    loadedModules.put(key, space);

    moduleSpaceLogger.log(TreeLogger.SPAM, "Loading module "
        + space.getModuleName() + " (id " + key.toString() + ")", null);

    // Let the space do its thing.
    //
    space.onLoad(moduleSpaceLogger);

    // Enable the compile button since we successfully loaded.
    //
    toolbar.updateWebMode();
  }

  /**
   * Unload one or more modules. If key is null, emulate old behavior by
   * unloading all loaded modules.
   * 
   * @param key unique key to identify module to unload or null for all
   */
  protected void doUnload(Object key) {
    if (key == null) {
      // BEGIN BACKWARD COMPATIBILITY
      // remove all modules
      for (Map.Entry<?, ModuleSpace> entry : loadedModules.entrySet()) {
        unloadModule(entry.getValue());
      }
      loadedModules.clear();
      // END BACKWARD COMPATIBILITY
    } else {
      ModuleSpace moduleSpace = loadedModules.get(key);
      if (moduleSpace != null) {
        // If the module failed to load at all, it may not be in the map.
        unloadModule(moduleSpace);
        loadedModules.remove(key);
      }
    }
    toolbar.updateWebMode();
  }

  /**
   * Report that an external method was called with the wrong number of
   * arguments.
   */
  protected void reportIncorrectInvocation(String name, int expectedArgs,
      int actualArgs) {
    getHost().getLogger().log(
        TreeLogger.ERROR,
        "Not enough arguments (" + actualArgs + ") passed to external." + name
            + "(), expected (" + expectedArgs + "); "
            + "your hosted mode bootstrap file may be out of date; "
            + "if you are using -noserver try recompiling and redeploying "
            + "your app");
  }

  /**
   * Unload the specified module.
   * 
   * @param moduleSpace a ModuleSpace instance to unload.
   */
  protected void unloadModule(ModuleSpace moduleSpace) {
    String moduleName = moduleSpace.getModuleName();
    Object key = moduleSpace.getKey();
    moduleSpace.dispose();
    logger.log(TreeLogger.SPAM, "Unloading module " + moduleName + " (id "
        + key.toString() + ")", null);
  }

  private Composite buildLocationBar(Composite parent) {
    Color white = new Color(null, 255, 255, 255);

    Composite bar = new Composite(parent, SWT.BORDER);
    bar.setBackground(white);

    location = new Text(bar, SWT.FLAT);

    goButton = new Button(bar, SWT.NONE);
    goButton.setBackground(bgColor);
    goButton.setText("Go");
    goButton.setImage(LowLevel.loadImage("go.gif"));

    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginWidth = layout.marginHeight = 0;
    layout.marginLeft = 2;
    layout.verticalSpacing = layout.horizontalSpacing = 0;
    bar.setLayout(layout);

    GridData data = new GridData(GridData.FILL_HORIZONTAL);
    data.grabExcessHorizontalSpace = true;
    data.verticalAlignment = GridData.CENTER;
    location.setLayoutData(data);

    return bar;
  }

  /**
   * Hooks up all necessary event listeners.
   */
  private void hookBrowserListeners() {

    this.addDisposeListener(new DisposeListener() {
      public void widgetDisposed(DisposeEvent e) {
        bgColor.dispose();
      }
    });

    goButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        go(location.getText());
      }
    });

    // Hook up the return key in the location bar.
    //
    location.addKeyListener(new KeyListener() {
      public void keyPressed(KeyEvent e) {
        if (e.character == '\r') {
          go(location.getText());
        }
      }

      public void keyReleased(KeyEvent e) {
      }
    });

    // Tie the status label to the browser's status.
    //
    browser.addStatusTextListener(new StatusTextListener() {
      public void changed(StatusTextEvent evt) {
        // Add a little space so it doesn't look so crowded.
        statusBar.setText(" " + evt.text);
      }
    });

    browser.addTitleListener(new TitleListener() {
      public void changed(TitleEvent evt) {
        browser.getShell().setText(evt.title);
      }
    });

    // Tie the location text box to the browser's location.
    //
    browser.addLocationListener(new LocationListener() {

      public void changed(LocationEvent evt) {
        if (evt.top) {
          setLocationText(evt.location);
        }
      }

      public void changing(LocationEvent evt) {
        String whitelistRuleFound = null;
        String blacklistRuleFound = null;
        if (evt.location.indexOf(":") == -1) {
          evt.location = "file://" + evt.location;
        }
        String url = evt.location;
        evt.doit = false;

        // Ensure that the request is 'safe', meaning it targets the user's
        // local machine or a host that has been whitelisted.
        //
        if (BrowserWidgetHostChecker.isAlwaysWhitelisted(url)) {
          // if the URL is 'always whitelisted', i.e. localhost
          // we load the page without regard to blacklisting
          evt.doit = true;
          return;
        }
        whitelistRuleFound = BrowserWidgetHostChecker.matchWhitelisted(url);
        blacklistRuleFound = BrowserWidgetHostChecker.matchBlacklisted(url);

        // If a host is blacklisted and whitelisted, disallow
        evt.doit = whitelistRuleFound != null && blacklistRuleFound == null;
        // We need these if we show a dialog box, so we declare them here and
        // initialize them inside the dialog box case before we change the
        // [in]valid hosts
        // no opinion either way
        if (whitelistRuleFound == null && blacklistRuleFound == null) {
          if (DialogBase.confirmAction(
              (Shell) getParent(),
              "Browsing to remote sites is a security risk!  A malicious site could\r\n"
                  + "execute Java code though this browser window.  Only click \"Yes\" if you\r\n"
                  + "are sure you trust the remote site.  See the log for details and\r\n"
                  + "configuration instructions.\r\n" + "\r\n" + "\r\n"
                  + "Allow access to '" + url
                  + "' for the rest of this session?\r\n", "Security Warning")) {
            evt.doit = true;
            BrowserWidgetHostChecker.whitelistURL(url);
          } else {
            evt.doit = false;
            BrowserWidgetHostChecker.blacklistURL(url);
          }
        }

        // Check for file system.
        //
        if (!evt.doit) {
          // Rip off the query string part. When launching files directly from
          // the filesystem, the existence of a query string when doing the
          // lookup below causes problems (e.g. we don't want to look up a file
          // called "C:\www\myapp.html?gwt.hybrid").
          //
          int lastQues = url.lastIndexOf('?');
          int lastSlash = url.lastIndexOf(File.pathSeparatorChar);
          if (lastQues != -1 && lastQues > lastSlash) {
            url = url.substring(0, lastQues);
          }

          // If any part of the path exists, it is at least a valid attempt.
          // This avoids the misleading security message when a file simply
          // cannot be found.
          //
          if (!url.startsWith("http:") && !url.startsWith("https:")) {
            File file = new File(url);
            while (file != null) {
              if (file.exists()) {
                evt.doit = true;
                break;
              } else {
                logger.log(TreeLogger.ERROR, "Cannot find file '"
                    + file.getAbsolutePath() + "'");
              }
              file = file.getParentFile();
            }
          }
        }
        // if it wasn't whitelisted or we were blocked we want to say something
        if (whitelistRuleFound == null || !evt.doit) {
          // Restore the URL.
          String typeStr = "untrusted";
          if (blacklistRuleFound != null) {
            typeStr = "blocked";
          }
          TreeLogger header;
          TreeLogger.Type msgType = TreeLogger.ERROR;
          if (!evt.doit) {
            header = logger.branch(msgType, "Unable to visit " + typeStr
                + " URL: '" + url, null);
          } else {
            msgType = TreeLogger.WARN;
            header = logger.branch(TreeLogger.WARN,
                "Confirmation was required to visit " + typeStr + " URL: '"
                    + url, null);
          }
          if (blacklistRuleFound == null) {
            BrowserWidgetHostChecker.notifyUntrustedHost(url, header, msgType);
          } else {
            BrowserWidgetHostChecker.notifyBlacklistedHost(blacklistRuleFound,
                url, header, msgType);
          }
          setLocationText(browser.getUrl());
        }
      }

    });

    // Handle new window requests.
    //
    browser.addOpenWindowListener(new OpenWindowListener() {
      public void open(WindowEvent event) {
        try {
          event.browser = host.openNewBrowserWindow().getBrowser();
          event.browser.getShell().open();
        } catch (UnableToCompleteException e) {
          logger.log(TreeLogger.ERROR, "Unable to open new browser window", e);
        }
      }
    });
  }

  private void setLocationText(String text) {
    location.setText(text);
    int length = text.length();
    location.setSelection(length, length);
  }
}
