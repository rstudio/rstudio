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
package com.google.gwt.junit;

import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.JS_WINDOW_ONERROR_COLUMN_ERROR_ARGUMENT;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.shell.HostedModePluginObject;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSet;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

import com.gargoylesoftware.htmlunit.AlertHandler;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.IncorrectnessListener;
import com.gargoylesoftware.htmlunit.InteractivePage;
import com.gargoylesoftware.htmlunit.OnbeforeunloadHandler;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptEngine;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptErrorListener;
import com.gargoylesoftware.htmlunit.javascript.host.Window;
import com.gargoylesoftware.htmlunit.util.WebClientUtils;

import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.Function;
import net.sourceforge.htmlunit.corejs.javascript.JavaScriptException;
import net.sourceforge.htmlunit.corejs.javascript.ScriptableObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Launches a web-mode test via HTMLUnit.
 */
public class RunStyleHtmlUnit extends RunStyle {

  /**
   * Runs HTMLUnit in a separate thread.
   */
  protected static class HtmlUnitThread extends Thread implements AlertHandler,
      IncorrectnessListener, OnbeforeunloadHandler {

    private final BrowserVersion browser;
    private final boolean developmentMode;
    private final TreeLogger treeLogger;
    private final String url;
    private Object waitForUnload = new Object();

    public HtmlUnitThread(BrowserVersion browser, String url,
        TreeLogger treeLogger, boolean developmentMode) {
      this.browser = browser;
      this.url = url;
      this.treeLogger = treeLogger;
      this.setName("htmlUnit client thread");
      this.developmentMode = developmentMode;
    }

    public void handleAlert(Page page, String message) {
      treeLogger.log(TreeLogger.ERROR, "Alert: " + message);
    }

    public boolean handleEvent(Page page, String returnValue) {
      synchronized (waitForUnload) {
        waitForUnload.notifyAll();
      }
      return true;
    }

    public void notify(String message, Object origin) {
      if ("Obsolete content type encountered: 'text/javascript'.".equals(message) ||
          "Obsolete content type encountered: 'application/x-javascript'.".equals(message)) {
        // silently eat warning about text/javascript MIME type and application/x-javascript
        return;
      }
      treeLogger.log(TreeLogger.WARN, message);
    }

    @Override
    public void run() {
      WebClient webClient = new WebClient(browser);
      webClient.setAlertHandler(this);
      webClient.setIncorrectnessListener(this);
      webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
      // To receive exceptions from js side in the development mode, we need set this to 'true'.
      // However, as htmlunit dies after throwing the exception, we still want it to be 'false'
      // for web mode.
      webClient.getOptions().setThrowExceptionOnScriptError(developmentMode);
      webClient.setOnbeforeunloadHandler(this);
      webClient.setJavaScriptErrorListener(new JavaScriptErrorListener() {

        @Override
        public void loadScriptError(InteractivePage htmlPage, URL scriptUrl,
            Exception exception) {
            treeLogger.log(TreeLogger.ERROR,
              "Load Script Error: " + exception, exception);
        }

        @Override
        public void malformedScriptURL(InteractivePage htmlPage, String url,
            MalformedURLException malformedURLException) {
          treeLogger.log(TreeLogger.ERROR,
              "Malformed Script URL: " + malformedURLException.getLocalizedMessage());
        }

        @Override
        public void scriptException(InteractivePage htmlPage,
            ScriptException scriptException) {
          treeLogger.log(TreeLogger.DEBUG,
              "Script Exception: " + scriptException.getLocalizedMessage() +
               ", line " + scriptException.getFailingLine());
        }

        @Override
        public void timeoutError(InteractivePage htmlPage, long allowedTime,
            long executionTime) {
          treeLogger.log(TreeLogger.ERROR,
              "Script Timeout Error " + executionTime + " > " + allowedTime);
        }
      });
      setupWebClient(webClient);
      try {
        Page page = webClient.getPage(url);
        webClient.waitForBackgroundJavaScriptStartingBefore(2000);
        if (treeLogger.isLoggable(TreeLogger.SPAM)) {
          treeLogger.log(TreeLogger.SPAM, "getPage returned "
              + ((HtmlPage) page).asXml());
        }
        // TODO(amitmanjhi): call webClient.closeAllWindows()
      } catch (FailingHttpStatusCodeException e) {
        treeLogger.log(TreeLogger.ERROR, "HTTP request failed", e);
        return;
      } catch (MalformedURLException e) {
        treeLogger.log(TreeLogger.ERROR, "Bad URL", e);
        return;
      } catch (IOException e) {
        treeLogger.log(TreeLogger.ERROR, "I/O error on HTTP request", e);
        return;
      }
    }

    protected void setupWebClient(WebClient webClient) {
      if (developmentMode) {
        JavaScriptEngine hostedEngine = new HostedJavaScriptEngine(webClient,
            treeLogger);
        webClient.setJavaScriptEngine(hostedEngine);
      } else {
        JavaScriptEngine webEngine = new WebJavaScriptEngine(webClient);
        webClient.setJavaScriptEngine(webEngine);
      }
      if (System.getProperty("gwt.htmlunit.debug") != null) {
        WebClientUtils.attachVisualDebugger(webClient);
      }
    }
  }

  /**
   * JavaScriptEngine subclass that provides a hook of initializing the
   * __gwt_HostedModePlugin property on any new window, so it acts just like
   * Firefox with the XPCOM plugin installed.
   */
  private static class HostedJavaScriptEngine extends JavaScriptEngine {

    private static final long serialVersionUID = 3594816610842448691L;
    private final TreeLogger logger;

    public HostedJavaScriptEngine(WebClient webClient, TreeLogger logger) {
      super(webClient);
      this.logger = logger;
    }

    @Override
    public void initialize(WebWindow webWindow) {
      // Hook in the hosted-mode plugin after initializing the JS engine.
      super.initialize(webWindow);
      Window window = (Window) webWindow.getScriptableObject();
      window.defineProperty("__gwt_HostedModePlugin",
          new HostedModePluginObject(this, logger), ScriptableObject.READONLY);
    }
  }

  /**
   * JavaScriptEngine subclass that fixes a bug when calling {@code window.onerror}.
   * Make sure to remove when updating HtmlUnit.
   *
   * @see <a href="https://sourceforge.net/p/htmlunit/bugs/1924/">HtmlUnit bug #1924</a>
   */
  private static class WebJavaScriptEngine extends JavaScriptEngine {
    private static final Log LOG = LogFactory.getLog(JavaScriptEngine.class);

    public WebJavaScriptEngine(WebClient webClient) {
      super(webClient);
    }

    @Override
    protected void handleJavaScriptException(ScriptException scriptException,
        boolean triggerOnError) {
      // XXX(tbroyer): copied from JavaScriptEngine to call below triggerOnError
      // instead of Window's triggerOnError.

      // Trigger window.onerror, if it has been set.
      final InteractivePage page = scriptException.getPage();
      if (triggerOnError && page != null) {
        final WebWindow window = page.getEnclosingWindow();
        if (window != null) {
          final Window w = (Window) window.getScriptableObject();
          if (w != null) {
            try {
              triggerOnError(w, scriptException);
            } catch (final Exception e) {
              handleJavaScriptException(new ScriptException(page, e, null), false);
            }
          }
        }
      }
      final JavaScriptErrorListener javaScriptErrorListener =
          getWebClient().getJavaScriptErrorListener();
      if (javaScriptErrorListener != null) {
        javaScriptErrorListener.scriptException(page, scriptException);
      }
      // Throw a Java exception if the user wants us to.
      if (getWebClient().getOptions().isThrowExceptionOnScriptError()) {
        throw scriptException;
      }
      // Log the error; ScriptException instances provide good debug info.
      LOG.info("Caught script exception", scriptException);
    }

    private void triggerOnError(Window w, ScriptException e) {
      // XXX(tbroyer): copied from HtmlUnit's javascript.host.Window
      // with fix unwrapping the JS exception before passing it back to JS.
      final Object o = w.getOnerror();
      if (o instanceof Function) {
        final Function f = (Function) o;
        final String msg = e.getMessage();
        final String url = e.getPage().getUrl().toExternalForm();
        final int line = e.getFailingLineNumber();

        Object[] args;
        if (w.getBrowserVersion().hasFeature(JS_WINDOW_ONERROR_COLUMN_ERROR_ARGUMENT)) {
          final int column = e.getFailingColumnNumber();

          Object jsError = null;
          if (e.getCause() instanceof JavaScriptException) {
            jsError = ((JavaScriptException) e.getCause()).getValue();
          }

          args = new Object[] {msg, url, Integer.valueOf(line), Integer.valueOf(column), jsError};
        } else {
          args = new Object[] {msg, url, Integer.valueOf(line)};
        }
        f.call(Context.getCurrentContext(), w, w, args);
      }
    }
  }

  private static final Map<String, BrowserVersion> BROWSER_MAP = Maps.newHashMap();
  private static final Map<BrowserVersion, String> USER_AGENT_MAP  = Maps.newHashMap();

  static {
    // “Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36 Edge/12.0″
    addBrowser(BrowserVersion.EDGE, "safari");
    addBrowser(BrowserVersion.FIREFOX_38, "gecko1_8");
    addBrowser(BrowserVersion.CHROME, "safari");
    addBrowser(BrowserVersion.INTERNET_EXPLORER_8, "ie8");
    addBrowser(BrowserVersion.INTERNET_EXPLORER_11, "gecko1_8");
  }

  private static void addBrowser(BrowserVersion browser, String userAgent) {
    BROWSER_MAP.put(browser.getNickname(), browser);
    USER_AGENT_MAP.put(browser, userAgent);
  }

  /*
   * as long as this number is greater than 1, GWTTestCaseTest::testRetry will
   * pass
   */
  private static final int DEFAULT_TRIES = 1;

  private static final Set<Platform> PLATFORMS = ImmutableSet.of(Platform.HtmlUnitBug,
      Platform.HtmlUnitLayout, Platform.HtmlUnitUnknown);

  private Set<BrowserVersion> browsers = new HashSet<BrowserVersion>();
  private boolean developmentMode;
  private final List<Thread> threads = new ArrayList<Thread>();

  /**
   * Create a RunStyle instance with the passed-in browser targets.
   */
  public RunStyleHtmlUnit(JUnitShell shell) {
    super(shell);
  }

  @Override
  public Set<Platform> getPlatforms() {
    return PLATFORMS;
  }

  @Override
  public int initialize(String args) {
    if (args == null || args.length() == 0) {
      // If no browsers specified, default to Firefox 38.
      args = "FF38";
    }
    Set<BrowserVersion> browserSet = new HashSet<BrowserVersion>();
    Set<String> userAgentSet = new HashSet<String>();
    for (String browserName : args.split(",")) {
      BrowserVersion browser = BROWSER_MAP.get(browserName);
      if (browser == null) {
        getLogger().log(
            TreeLogger.ERROR,
            "RunStyleHtmlUnit: Unknown browser " + "name " + browserName
                + ", expected browser name: one of " + BROWSER_MAP.keySet());
        return -1;
      }
      browserSet.add(browser);
      userAgentSet.add(USER_AGENT_MAP.get(browser));
    }
    browsers = Collections.unmodifiableSet(browserSet);
    setUserAgents(Collections.unmodifiableSet(userAgentSet));
    setTries(DEFAULT_TRIES); // set to the default value for this RunStyle
    return browsers.size();
  }

  @Override
  public void launchModule(String moduleName) {
    for (BrowserVersion browser : browsers) {
      String url = shell.getModuleUrl(moduleName);
      HtmlUnitThread hut = createHtmlUnitThread(browser, url);
      TreeLogger logger = shell.getTopLogger();
      if (logger.isLoggable(TreeLogger.INFO)) {
        logger.log(TreeLogger.INFO,
            "Starting " + url + " on browser " + browser.getNickname());
      }
      /*
       * TODO (amitmanjhi): Is it worth pausing here and waiting for the main
       * test thread to get to an "okay" state.
       */
      hut.start();
      threads.add(hut);
    }
  }

  public int numBrowsers() {
    return browsers.size();
  }

  @Override
  public boolean setupMode(TreeLogger logger, boolean developmentMode) {
    this.developmentMode = developmentMode;
    return true;
  }

  protected HtmlUnitThread createHtmlUnitThread(BrowserVersion browser,
      String url) {
    return new HtmlUnitThread(browser, url, shell.getTopLogger().branch(
        TreeLogger.SPAM, "logging for HtmlUnit thread"), developmentMode);
  }
}
