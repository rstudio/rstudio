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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.shell.HostedModePluginObject;

import com.gargoylesoftware.htmlunit.AlertHandler;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.IncorrectnessListener;
import com.gargoylesoftware.htmlunit.OnbeforeunloadHandler;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptEngine;
import com.gargoylesoftware.htmlunit.javascript.host.Window;

import net.sourceforge.htmlunit.corejs.javascript.ScriptableObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
    private final String url;
    private Object waitForUnload = new Object();
    private final TreeLogger treeLogger;
    private final boolean developmentMode;

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
      if ("Obsolete content type encountered: 'text/javascript'.".equals(message)) {
        // silently eat warning about text/javascript MIME type
        return;
      }
      treeLogger.log(TreeLogger.WARN, message);
    }

    @Override
    public void run() {
      WebClient webClient = new WebClient(browser);
      webClient.setAlertHandler(this);
      webClient.setIncorrectnessListener(this);
      webClient.setThrowExceptionOnFailingStatusCode(false);
      webClient.setThrowExceptionOnScriptError(true);
      webClient.setOnbeforeunloadHandler(this);
      setupWebClient(webClient);
      try {
        Page page = webClient.getPage(url);
        // TODO(jat): is this necessary?
        webClient.waitForBackgroundJavaScriptStartingBefore(2000);
        page.getEnclosingWindow().getJobManager().waitForJobs(60000);
        treeLogger.log(TreeLogger.SPAM, "getPage returned "
            + ((HtmlPage) page).asXml());
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
        JavaScriptEngine hostedEngine = new HostedJavaScriptEngine(webClient);
        webClient.setJavaScriptEngine(hostedEngine);
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

    public HostedJavaScriptEngine(WebClient webClient) {
      super(webClient);
    }

    @Override
    public void initialize(WebWindow webWindow) {
      // Hook in the hosted-mode plugin after initializing the JS engine.
      super.initialize(webWindow);
      Window window = (Window) webWindow.getScriptObject();
      window.defineProperty("__gwt_HostedModePlugin",
          new HostedModePluginObject(this), ScriptableObject.READONLY);
    }
  }

  private static final Map<String, BrowserVersion> BROWSER_MAP = createBrowserMap();

  /**
   * Returns the list of browsers Htmlunit emulates as a comma separated string.
   */
  static String getBrowserList() {
    StringBuffer sb = new StringBuffer();
    for (String str : BROWSER_MAP.keySet()) {
      sb.append(str);
      sb.append(",");
    }
    if (sb.length() > 1) {
      return sb.substring(0, sb.length() - 1);
    }
    return sb.toString();
  }

  private static Map<String, BrowserVersion> createBrowserMap() {
    Map<String, BrowserVersion> browserMap = new HashMap<String, BrowserVersion>();
    for (BrowserVersion browser : new BrowserVersion[] {
        BrowserVersion.FIREFOX_2, BrowserVersion.FIREFOX_3,
        BrowserVersion.INTERNET_EXPLORER_6, BrowserVersion.INTERNET_EXPLORER_7}) {
      browserMap.put(browser.getNickname(), browser);
    }
    return Collections.unmodifiableMap(browserMap);
  }

  private Set<BrowserVersion> browsers = new HashSet<BrowserVersion>();
  private final List<Thread> threads = new ArrayList<Thread>();
  private boolean developmentMode;

  /**
   * Create a RunStyle instance with the passed-in browser targets.
   */
  public RunStyleHtmlUnit(JUnitShell shell) {
    super(shell);
  }

  @Override
  public boolean initialize(String args) {
    if (args == null || args.length() == 0) {
      // If no browsers specified, default to Firefox 3.
      args = "FF3";
    }
    Set<BrowserVersion> browserSet = new HashSet<BrowserVersion>();
    for (String browserName : args.split(",")) {
      BrowserVersion browser = BROWSER_MAP.get(browserName);
      if (browser == null) {
        getLogger().log(TreeLogger.ERROR, "RunStyleHtmlUnit: Unknown browser "
            + "name " + browserName + ", expected browser name: one of "
            + BROWSER_MAP.keySet());
        return false;
      } else {
        browserSet.add(browser);
      }
    }
    browsers = Collections.unmodifiableSet(browserSet);
    return true;
  }

  @Override
  public void launchModule(String moduleName) {
    for (BrowserVersion browser : browsers) {
      String url = shell.getModuleUrl(moduleName);
      HtmlUnitThread hut = createHtmlUnitThread(browser, url);
      shell.getTopLogger().log(TreeLogger.INFO,
          "Starting " + url + " on browser " + browser.getNickname());
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
