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
package com.google.gwt.dev.shell.test;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Tests unloading individual modules when more than one are loaded on a page,
 * including in nested frames.
 * 
 * The test will load up the initial configuration, then when all frames are
 * done loading will toggle the first frame, then the second frame, then both at
 * the same time. Buttons are provided for manual testing.
 * 
 * Currently, there isn't much it can do to actually verify the proper behavior
 * other than not crashing (which does not verify that the removed module isn't
 * holding a lot of memory), but it is hard to do more than that without adding
 * a lot of hooks that would only be used for this test. When tobyr's profiling
 * changes are merged in, we will have to create some of the hooks to allow
 * calls into the external object which could be used for the hooks for this
 * test.
 */
public class MultiModuleTest extends GWTTestCase {

  /**
   * Used to setup the variable to keep track of things to be loaded, plus the
   * JavaScript function used to communicate with the main module (the one that
   * sets up the frames) from other modules.
   * 
   * @param javaThis frameTest instance to use for callback
   */
  private static native void setupDoneLoading(MultiModuleTest javaThis) /*-{
    $wnd.__count_to_be_loaded = 0;
    $wnd.__done_loading = function() {
      javaThis.@com.google.gwt.dev.shell.test.MultiModuleTest::doneLoading()();
    };
  }-*/;

  /**
   * Used to setup the JavaScript function used to communicate with the main
   * module (the one that sets up the frames) from other modules.
   * 
   * @param javaThis frameTest instance to use for callback
   */
  private static native void setupTestComplete(MultiModuleTest javaThis) /*-{
    $wnd.__test_complete = function() {
      javaThis.@com.google.gwt.dev.shell.test.MultiModuleTest::completedTest()();
    };
  }-*/;

  /**
   * Child frames, which are unused in nested modules.
   */
  private Frame[] frame = new Frame[2];

  /**
   * Flags indicating the "B" version of frame i is displayed, used to toggle
   * the individual frames.
   */
  private boolean[] frameB = new boolean[2];

  /**
   * The top-level panel for callbacks.
   */
  private VerticalPanel mainPanel = null;

  /**
   * The state for automated frame toggles.
   */
  private int state;

  /**
   * Get the name of the GWT module to use for this test.
   * 
   * @return the fully-qualified module name
   */
  public String getModuleName() {
    return "com.google.gwt.dev.shell.MultiModuleTest";
  }

  /**
   * Create the DOM elements for the module, based on the query string. The top
   * level (query parameter frame=top) drives the process and sets up the
   * automated state transition hooks.
   * 
   * This function returns with no effect if gwt.junit.testfuncname is not
   * passed as a query parameter, which means it is being run as a real test
   * rather than as a "submodule" of testMultipleModules.
   */
  public void testInnerModules() {
    String url = getURL();
    Map params = getURLParams(url);
    if (!params.containsKey("gwt.junit.testfuncname")) {
      // if this test is being run as a normal JUnit test, return success
      return;
    }

    // we were invoked by testMultipleModules, get the frame to load
    String frameName = (String) params.get("frame");
    
    VerticalPanel panel = new VerticalPanel();
    RootPanel.get().add(panel);
    if (frameName.equals("top")) {
      // initial load
      setupDoneLoading(this);
      mainPanel = panel;
      panel.add(new Label("Top level frame"));
      state = 0;
      params.put("frame", "1a");
      frame[0] = new Frame(buildURL(url, params));
      panel.add(frame[0]);
      params.put("frame", "2a");
      frame[1] = new Frame(buildURL(url, params));
      panel.add(frame[1]);
      addToBeLoaded(0, 2);
    } else if (frameName.equals("1a")) {
      panel.add(new Label("Frame 1a"));
      markLoaded(1);
    } else if (frameName.equals("1b")) {
      panel.add(new Label("Frame 1b"));
      markLoaded(1);
    } else if (frameName.equals("2a")) {
      panel.add(new Label("Frame 2a"));
      params.put("frame", "2suba");
      Frame sub = new Frame(buildURL(url, params));
      panel.add(sub);
    } else if (frameName.equals("2b")) {
      panel.add(new Label("Frame 2b"));
      params.put("frame", "2subb");
      Frame sub = new Frame(buildURL(url, params));
      panel.add(sub);
    } else if (frameName.equals("2suba")) {
      panel.add(new Label("Frame 2a inner"));
      markLoaded(2);
    } else if (frameName.equals("2subb")) {
      panel.add(new Label("Frame 2b inner"));
      markLoaded(2);
    } else {
      GWT.log("Unexpected frame name " + frameName);
    }
  }

  public void testMultipleModules() {
    setupTestComplete(this);
    
    // build new URL from current one
    String url = getURL();
    Map params = getURLParams(url);
    params.put("frame", "top");
    params.put("gwt.junit.testclassname", MultiModuleTest.class.getName());
    params.put("gwt.junit.testfuncname", "testInnerModules");
    
    // open a new frame containing the module that drives the actual test
    Frame frame = new Frame(buildURL(url, params));
    frame.setHeight("100%");
    frame.setWidth("100%");
    RootPanel.get().add(frame);
    // wait up to 60 seconds for inner frames module to do its job
    delayTestFinish(60000);
  }

  /**
   * Increments the number of pages to be loaded. This count is kept in the
   * context of the top-level module, so the depth parameter is provided to find
   * it.
   * 
   * @param depth nesting depth of this module, 0 = top level
   * @param count number of pages due to be loaded
   */
  private native void addToBeLoaded(int depth, int count) /*-{
    var frame = $wnd;
    while (depth-- > 0) {
      frame = frame.parent;
    }
    frame.__count_to_be_loaded += count;
  }-*/;

  /**
   * Create a URL given an old URL and a map of query parameters. The search
   * portion of the original URL will be discarded and replaced with a string of
   * the form ?param1&param2=value2 etc., where param1 has a null value in the
   * map.
   * 
   * @param url the original URL to rewrite
   * @param params a map of parameter names to values
   * @return the revised URL
   */
  private String buildURL(String url, Map params) {

    // strip off the query string if present
    int pos = url.indexOf("?");
    if (pos >= 0) {
      url = url.substring(0, pos);
    }

    // flag if we are generating the first parameter in the URL
    boolean firstParam = true;

    // gwt.hybrid must be first if present
    if (params.containsKey("gwt.hybrid")) {
      url += "?gwt.hybrid";
      firstParam = false;
    }

    // now add the rest of the parameters, excluding gwt.hybrid
    for (Iterator it = params.entrySet().iterator(); it.hasNext();) {
      Map.Entry entry = (Map.Entry) it.next();
      String param = (String) entry.getKey();

      if (param.equals("gwt.hybrid")) {
        // we already included gwt.hybrid if it was present
        continue;
      }

      // add the parameter name to the URL
      if (firstParam) {
        url += "?";
        firstParam = false;
      } else {
        url += "&";
      }
      url += param;

      // add the value if necessary
      String value = (String) entry.getValue();
      if (value != null) {
        url += "=" + value;
      }
    }
    return url;
  }

  /**
   * Called via JSNI by testInnerModules when it successfully goes through
   * all its iterations.
   */
  private void completedTest() {
    // tell JUnit that we completed successfully
    finishTest();
  }

  /**
   * Proceed to the next automatic state change if any. This is called in the
   * context of the top-level module via JSNI calls when all modules being
   * waited on are loaded.
   */
  private void doneLoading() {
    String url = getURL();
    Map params = getURLParams(url);
    mainPanel.add(new Label("done loading"));
    if (++state == 4) {
      // all tests complete, notify parent
      notifyParent();
    }
    if (state >= 4) {
      return;
    }
    StringBuffer buf = new StringBuffer();
    buf.append("Toggling frame(s)");
    if ((state & 1) != 0) {
      buf.append(" 0");
      toggleFrame(0, url, params);
    }
    if ((state & 2) != 0) {
      buf.append(" 1");
      toggleFrame(1, url, params);
    }
    mainPanel.add(new Label(buf.toString()));
  }

  /**
   * Get the query string from the URL, including the question mark if present.
   * 
   * @return the query string
   */
  private native String getURL() /*-{
    return $wnd.location.href || '';  
  }-*/;

  /**
   * Parse a URL and return a map of query parameters. If a parameter is
   * supplied without =value, it will be defined as null.
   * 
   * @param url the full or partial (ie, only location.search) URL to parse
   * @return the map of parameter names to values
   */
  private Map getURLParams(String url) {
    HashMap map = new HashMap();
    int pos = url.indexOf("?");
    
    // loop precondition: pos is the index of the next ? or & character in url
    while (pos >= 0) {
      // skip over the separator character
      url = url.substring(pos + 1);

      // find the end of this parameter, which is the next ? or &
      pos = url.indexOf("?");
      int posAlt = url.indexOf("&");
      if (pos < 0 || (posAlt >= 0 && posAlt < pos)) {
        pos = posAlt;
      }
      String param;
      if (pos >= 0) {
        // trim this parameter if there is a terminator
        param = url.substring(0, pos);
      } else {
        param = url;
      }

      // split value from parameter name if present
      int equals = param.indexOf("=");
      String value = null;
      if (equals >= 0) {
        value = param.substring(equals + 1);
        param = param.substring(0, equals);
      }

      map.put(param, value);
    }
    return map;
  }

  /**
   * Mark this page as loaded, using JSNI to mark it in the context of the
   * top-level module space. If all outstanding modules have loaded, call the
   * doneLoading method in the top-level module space (using JSNI and the depth
   * to find it).
   * 
   * @param depth nesting depth of this module, 0 = top level
   */
  private native void markLoaded(int depth) /*-{
    var frame = $wnd;
    while (depth-- > 0) {
      frame = frame.parent;
    }
    if (!--frame.__count_to_be_loaded) {
      frame.__done_loading();
    }
  }-*/;

  /**
   * Notify our parent frame that the test is complete.
   */
  private native void notifyParent() /*-{
    $wnd.parent.__test_complete();
  }-*/;

  /**
   * Replace the specified frame with its alternate version.
   * 
   * @param frameNumber the number of the frame to replace, starting with 0
   */
  private void toggleFrame(int frameNumber, String url, Map params) {
    params.put("frame", (frameNumber + 1) + (frameB[frameNumber] ? "a" : "b"));
    frame[frameNumber].setUrl(buildURL(url, params));
    frameB[frameNumber] = !frameB[frameNumber];
    addToBeLoaded(0, 1);
  }
}
