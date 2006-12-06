// Copyright 2006 Google Inc. All Rights Reserved.
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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Represents an individual browser window and all of its controls.
 */
public abstract class BrowserWidget extends Composite {

  private class Toolbar extends HeaderBarBase implements SelectionListener {
    public Toolbar(Composite parent) {
      super(parent);

      fBackButton = newItem("back.gif", "   &Back   ", "Go back one state");
      fBackButton.addSelectionListener(this);

      fForwardButton = newItem("forward.gif", "&Forward",
        "Go forward one state");
      fForwardButton.addSelectionListener(this);

      fRefreshButton = newItem("refresh.gif", " &Refresh ", "Reload the page");
      fRefreshButton.addSelectionListener(this);

      fStopButton = newItem("stop.gif", "    &Stop    ",
        "Stop loading the page");
      fStopButton.addSelectionListener(this);

      newSeparator();

      fOpenWebModeButton = newItem("new-web-mode-window.gif",
        "&Compile/Browse",
        "Compiles and opens the current URL in the system browser");
      fOpenWebModeButton.addSelectionListener(this);
      fOpenWebModeButton.setEnabled(false);
    }

    public void widgetDefaultSelected(SelectionEvent e) {
    }

    public void widgetSelected(SelectionEvent evt) {
      if (evt.widget == fBackButton) {
        fBrowser.back();
      } else if (evt.widget == fForwardButton) {
        fBrowser.forward();
      } else if (evt.widget == fRefreshButton) {
        // we have to clean up old module spaces here b/c we don't get a
        // location changed event

        // lastHostPageLocation = null;
        fBrowser.refresh();
      } else if (evt.widget == fStopButton) {
        fBrowser.stop();
      } else if (evt.widget == fOpenWebModeButton) {
        // first, compile
        Set keySet = moduleSpacesByName.keySet();
        String[] moduleNames = Util.toStringArray(keySet);
        if (moduleNames.length == 0) {
          // A latent problem with a module.
          //
          fOpenWebModeButton.setEnabled(false);
          return;
        }
        TreeLogger logger = fLogger;
        try {
          Cursor waitCursor = getDisplay().getSystemCursor(SWT.CURSOR_WAIT);
          getShell().setCursor(waitCursor);
          getHost().compile(moduleNames);
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

        String location = fLocation.getText();

        launchExternalBrowser(logger, location);
      }
    }

    private final ToolItem fBackButton;
    private final ToolItem fForwardButton;

    private final ToolItem fOpenWebModeButton;

    private final ToolItem fRefreshButton;

    private final ToolItem fStopButton;
  }

  static void launchExternalBrowser(TreeLogger logger, String location) {

    // check GWT_EXTERNAL_BROWSER first, it overrides everything else
    LowLevel.init();
    String browserCmd = LowLevel.getEnv("GWT_EXTERNAL_BROWSER");
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
        logger.log(
          TreeLogger.ERROR,
          "Error launching gwt.browser.default executable '" + browserCmd + "'",
          e);
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

    logger.log(
      TreeLogger.WARN,
      "Try setting the environment varable GWT_EXTERNAL_BROWSER to your web browser executable before launching the GWT shell",
      null);
  }

  public BrowserWidget(Composite parent, BrowserWidgetHost host) {
    super(parent, SWT.NONE);

    fHost = host;
    fLogger = fHost.getLogger();

    fBgColor = new Color(null, 239, 237, 216);

    fToolbar = new Toolbar(this);
    Composite secondBar = buildLocationBar(this);

    fBrowser = new Browser(this, SWT.NONE);

    {
      fStatusBar = new Label(this, SWT.BORDER | SWT.SHADOW_IN);
      fStatusBar.setBackground(fBgColor);
      GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
      gridData.verticalAlignment = GridData.CENTER;
      gridData.verticalIndent = 0;
      gridData.horizontalIndent = 0;
      fStatusBar.setLayoutData(gridData);
    }

    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    layout.verticalSpacing = 1;
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    setLayout(layout);

    fToolbar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    secondBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    GridData data = new GridData(GridData.FILL_BOTH);
    data.grabExcessVerticalSpace = true;
    data.grabExcessHorizontalSpace = true;
    fBrowser.setLayoutData(data);

    // Hook up all appropriate event listeners.
    //
    hookBrowserListeners();
  }

  /**
   * Gets the browser object wrapped by this window.
   */
  public Browser getBrowser() {
    return fBrowser;
  }

  public BrowserWidgetHost getHost() {
    return fHost;
  }

  /**
   * Go to a given url, possibly rewriting it if it can be served from any
   * project's public directory.
   */
  public void go(String target) {
    String url = fHost.normalizeURL(target);
    fBrowser.setUrl(url);
  }

  public void onFirstShown() {
    String baseUrl = fHost.normalizeURL("/");
    setLocationText(baseUrl);
    fLocation.setFocus();
    fLocation.setSelection(baseUrl.length());
    fLocation.addFocusListener(new FocusListener() {
      public void focusGained(FocusEvent e) {
        int length = fLocation.getText().length();
        fLocation.setSelection(length, length);
      }

      public void focusLost(FocusEvent e) {
      }
    });
  }

  /**
   * Initializes and attaches module space to this browser widget. Called by
   * subclasses in response to calls from JavaScript.
   */
  protected final void attachModuleSpace(String moduleName, ModuleSpace space)
      throws UnableToCompleteException {

    // Let the space do its thing.
    //
    space.onLoad(fLogger);

    // Remember this new module space so that we can dispose of it later.
    //
    moduleSpacesByName.put(moduleName, space);

    // Enable the compile button since we successfully loaded.
    //
    fToolbar.fOpenWebModeButton.setEnabled(true);
  }

  /**
   * Disposes all the attached module spaces from the prior page (not the one
   * that just loaded). Called when this widget is disposed but, more
   * interestingly, whenever the browser's page changes.
   */
  protected void onPageUnload() {
    for (Iterator iter = moduleSpacesByName.entrySet().iterator(); iter.hasNext();) {
      Map.Entry entry = (Map.Entry) iter.next();
      String moduleName = (String) entry.getKey();
      ModuleSpace space = (ModuleSpace) entry.getValue();

      space.dispose();
      fLogger.log(TreeLogger.SPAM, "Cleaning up resources for module "
        + moduleName, null);
    }
    moduleSpacesByName.clear();

    if (!fToolbar.fOpenWebModeButton.isDisposed()) {
      // Disable the compile buton.
      //
      fToolbar.fOpenWebModeButton.setEnabled(false);
    }
  }

  private Composite buildLocationBar(Composite parent) {
    Color white = new Color(null, 255, 255, 255);

    Composite bar = new Composite(parent, SWT.BORDER);
    bar.setBackground(white);

    fLocation = new Text(bar, SWT.FLAT);

    fGoButton = new Button(bar, SWT.NONE);
    fGoButton.setBackground(fBgColor);
    fGoButton.setText("Go");
    fGoButton.setImage(LowLevel.loadImage("go.gif"));

    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginWidth = layout.marginHeight = 0;
    layout.marginLeft = 2;
    layout.verticalSpacing = layout.horizontalSpacing = 0;
    bar.setLayout(layout);

    GridData data = new GridData(GridData.FILL_HORIZONTAL);
    data.grabExcessHorizontalSpace = true;
    data.verticalAlignment = GridData.CENTER;
    fLocation.setLayoutData(data);

    return bar;
  }

  /**
   * Hooks up all necessary event listeners.
   */
  private void hookBrowserListeners() {

    this.addDisposeListener(new DisposeListener() {
      public void widgetDisposed(DisposeEvent e) {
        fBgColor.dispose();
      }
    });

    fGoButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        go(fLocation.getText());
      }
    });

    // Hook up the return key in the location bar.
    //
    fLocation.addKeyListener(new KeyListener() {
      public void keyPressed(KeyEvent e) {
        if (e.character == '\r') {
          go(fLocation.getText());
        }
      }

      public void keyReleased(KeyEvent e) {
      }
    });

    // Tie the status label to the browser's status.
    //
    fBrowser.addStatusTextListener(new StatusTextListener() {
      public void changed(StatusTextEvent evt) {
        // Add a little space so it doesn't look so crowded.
        fStatusBar.setText(" " + evt.text);
      }
    });

    fBrowser.addTitleListener(new TitleListener() {
      public void changed(TitleEvent evt) {
        fBrowser.getShell().setText(evt.title);
      }
    });

    // Tie the location text box to the browser's location.
    //
    fBrowser.addLocationListener(new LocationListener() {

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
                String msg = "Cannot find file '" + file.getAbsolutePath()
                  + "'";
                TreeLogger branch = fLogger.branch(TreeLogger.ERROR, msg, null);
                if ("gwt-hosted.html".equalsIgnoreCase(file.getName())) {
                  branch.log(
                    TreeLogger.ERROR,
                    "If you want to open compiled output within this hosted browser, add '?gwt.hybrid' to the end of the URL",
                    null);
                }
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
            header = fLogger.branch(msgType, "Unable to visit " + typeStr
              + " URL: '" + url, null);
          } else {
            msgType = TreeLogger.WARN;
            header = fLogger.branch(
              TreeLogger.WARN,
              "Confirmation was required to visit " + typeStr + " URL: '" + url,
              null);
          }
          if (blacklistRuleFound == null) {
            BrowserWidgetHostChecker.notifyUntrustedHost(url, header, msgType);
          } else {
            BrowserWidgetHostChecker.notifyBlacklistedHost(blacklistRuleFound,
              url, header, msgType);
          }
          setLocationText(fBrowser.getUrl());
        }
      }

    });

    // Handle new window requests.
    //
    fBrowser.addOpenWindowListener(new OpenWindowListener() {
      public void open(WindowEvent event) {
        try {
          event.browser = fHost.openNewBrowserWindow().getBrowser();
          event.browser.getShell().open();
        } catch (UnableToCompleteException e) {
          fLogger.log(TreeLogger.ERROR, "Unable to open new browser window", e);
        }
      }
    });
  }

  private void setLocationText(String text) {
    fLocation.setText(text);
    int length = text.length();
    fLocation.setSelection(length, length);
  }

  protected Browser fBrowser;
  private Color fBgColor = new Color(null, 239, 237, 216);
  private Button fGoButton;
  private final BrowserWidgetHost fHost;
  private Text fLocation;
  private final TreeLogger fLogger;
  private Label fStatusBar;
  private Toolbar fToolbar;
  private Map moduleSpacesByName = new HashMap();
}
