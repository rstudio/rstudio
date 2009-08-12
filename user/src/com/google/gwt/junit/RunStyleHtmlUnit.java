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
import com.google.gwt.core.ext.UnableToCompleteException;

import com.gargoylesoftware.htmlunit.AlertHandler;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.IncorrectnessListener;
import com.gargoylesoftware.htmlunit.OnbeforeunloadHandler;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

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
public class RunStyleHtmlUnit extends RunStyleRemote {

  /**
   * Runs HTMLUnit in a separate thread.
   */
  protected static class HtmlUnitThread extends Thread implements AlertHandler,
      IncorrectnessListener, OnbeforeunloadHandler {

    private final BrowserVersion browser;
    private final String url;
    private Object waitForUnload = new Object();
    private final TreeLogger treeLogger;

    public HtmlUnitThread(BrowserVersion browser, String url,
        TreeLogger treeLogger) {
      this.browser = browser;
      this.url = url;
      this.treeLogger = treeLogger;
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
      try {
        webClient.setAlertHandler(this);
        webClient.setIncorrectnessListener(this);
        webClient.setThrowExceptionOnFailingStatusCode(false);
        webClient.setThrowExceptionOnScriptError(true);
        webClient.setOnbeforeunloadHandler(this);
        setupWebClient(webClient);
        Page page = webClient.getPage(url);
        // TODO(jat): is this necessary?
        webClient.waitForBackgroundJavaScriptStartingBefore(2000);
        page.getEnclosingWindow().getJobManager().waitForJobs(60000);
        treeLogger.log(TreeLogger.DEBUG, "getPage returned "
            + ((HtmlPage) page).asXml());
      } catch (FailingHttpStatusCodeException e) {
        treeLogger.log(TreeLogger.ERROR, "HTTP request failed", e);
      } catch (MalformedURLException e) {
        treeLogger.log(TreeLogger.ERROR, "Bad URL", e);
      } catch (IOException e) {
        treeLogger.log(TreeLogger.ERROR, "I/O error on HTTP request", e);
      } finally {
        webClient.closeAllWindows();
      }
    }

    /**
     * Additional setup of the WebClient before starting test. Hook necessary
     * for plugging in HtmlUnitHosted.
     */
    protected void setupWebClient(WebClient webClient) {
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

  private final Set<BrowserVersion> browsers;
  private final List<Thread> threads = new ArrayList<Thread>();

  /**
   * Create a RunStyle instance with the passed-in browser targets.
   */
  public RunStyleHtmlUnit(JUnitShell shell, String[] targetsIn) {
    super(shell);
    this.browsers = getBrowserSet(targetsIn);
  }

  @Override
  public void launchModule(String moduleName) {
    for (BrowserVersion browser : browsers) {
      String url = getMyUrl(moduleName);
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

  @Override
  public void maybeCompileModule(String moduleName)
      throws UnableToCompleteException {
    shell.compileForWebMode(moduleName, getUserAgents());
  }

  public int numBrowsers() {
    return browsers.size();
  }

  protected HtmlUnitThread createHtmlUnitThread(BrowserVersion browser,
      String url) {
    return new HtmlUnitThread(browser, url, shell.getTopLogger());
  }

  private Set<BrowserVersion> getBrowserSet(String[] targetsIn) {
    Set<BrowserVersion> browserSet = new HashSet<BrowserVersion>();
    for (String browserName : targetsIn) {
      BrowserVersion browser = BROWSER_MAP.get(browserName);
      if (browser == null) {
        throw new IllegalArgumentException("Expected browser name: one of "
            + BROWSER_MAP.keySet() + ", actual name: " + browserName);
      }
      browserSet.add(browser);
    }
    return Collections.unmodifiableSet(browserSet);
  }

  private String[] getUserAgents() {
    Map<BrowserVersion, String> userAgentMap = new HashMap<BrowserVersion, String>();
    userAgentMap.put(BrowserVersion.FIREFOX_2, "gecko1_8");
    userAgentMap.put(BrowserVersion.FIREFOX_3, "gecko");
    userAgentMap.put(BrowserVersion.INTERNET_EXPLORER_6, "ie6");
    userAgentMap.put(BrowserVersion.INTERNET_EXPLORER_7, "ie6");

    String userAgents[] = new String[numBrowsers()];
    int index = 0;
    for (BrowserVersion browser : browsers) {
      userAgents[index++] = userAgentMap.get(browser);
    }
    return userAgents;
  }
}
