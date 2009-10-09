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
package com.google.gwt.user.client;

import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Test Case for {@link Cookies}.
 */
public class WindowTest extends GWTTestCase {

  private static native String getNodeName(Element elem) /*-{
     return (elem.nodeName || "").toLowerCase();
   }-*/;

  /**
   * Removes all elements in the body, except scripts and iframes.
   */
  private static void clearBodyContent() {
    Element bodyElem = RootPanel.getBodyElement();

    List<Element> toRemove = new ArrayList<Element>();
    for (int i = 0, n = DOM.getChildCount(bodyElem); i < n; ++i) {
      Element elem = DOM.getChild(bodyElem, i);
      String nodeName = getNodeName(elem);
      if (!"script".equals(nodeName) && !"iframe".equals(nodeName)) {
        toRemove.add(elem);
      }
    }

    for (int i = 0, n = toRemove.size(); i < n; ++i) {
      DOM.removeChild(bodyElem, toRemove.get(i));
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testLocation() {
    // testing reload, replace, and assign seemed to hang our junit harness.
    // Therefore only testing subset of Location that is testable.

    // Use History to get the #hash part of the url into a known state (if the
    // url has somehow been set to http://host/#, location.hash returns the
    // empty string, but location.href includes the trailing hash).
    History.newItem("foo");

    // As we have no control over these values we cannot assert much about them.
    String hash = Window.Location.getHash();
    String host = Window.Location.getHost();
    String hostName = Window.Location.getHostName();
    String href = Window.Location.getHref();
    assertNull(Window.Location.getParameter("fuzzy bunny"));
    String path = Window.Location.getPath();
    String port = Window.Location.getPort();
    String protocol = Window.Location.getProtocol();
    String query = Window.Location.getQueryString();

    // Check that the sum is equal to its parts.
    assertEquals(host, hostName + ":" + port);
    assertEquals(href, protocol + "//" + host + path + query + hash);
  }

  public void testLocationParsing() {
    Map<String, List<String>> map;

    // typical case
    map = Window.Location.buildListParamMap("?fuzzy=bunnies&foo=bar&num=42");
    assertEquals(map.size(), 3);
    assertEquals(map.get("foo").get(0), "bar");
    assertEquals(map.get("fuzzy").get(0), "bunnies");
    
    // multiple values for the same parameter
    map = Window.Location.buildListParamMap(
        "?fuzzy=bunnies&foo=bar&num=42&foo=baz");
    assertEquals(map.size(), 3);
    assertEquals(map.get("foo").get(0), "bar");
    assertEquals(map.get("foo").get(1), "baz");
    
    // no query parameters.
    map = Window.Location.buildListParamMap("");
    assertEquals(map.size(), 0);
    
    // blank keys should be ignored, but blank values are OK. Also,
    // keys can contain whitespace. (but the browser may give whitespace
    // back as escaped).
    map = Window.Location.buildListParamMap(
        "?&& &a&b=&c=c&d=d=d&=e&f=2&f=1&");
    assertEquals(map.size(), 6);
    assertEquals(map.get(" ").get(0), "");
    assertEquals(map.get("a").get(0), "");
    assertEquals(map.get("b").get(0), "");
    assertEquals(map.get("c").get(0), "c");
    assertEquals(map.get("d").get(0), "d=d");
    assertEquals(map.get("f").get(0), "2");
    assertEquals(map.get("f").get(1), "1");

    // Values escaped with hex codes should work too.
    map = Window.Location.buildListParamMap(
    "?foo=bar%20baz%3aqux");
    assertEquals(map.get("foo").get(0), "bar baz:qux");
  }

  /**
   * Tests the ability of the Window to get the client size correctly with and
   * without visible scroll bars.
   */
  @DoNotRunWith({Platform.Htmlunit})
  public void testGetClientSize() {

    // NOTE: We must clear the DOM here so that previous tests do not pollute
    // our results.
    clearBodyContent();

    // Get the dimensions without any scroll bars
    Window.enableScrolling(false);
    final int oldClientHeight = Window.getClientHeight();
    final int oldClientWidth = Window.getClientWidth();
    assertTrue("Expect positive oldClientHeight. "
        + "This will fail in WebKit if run headless", oldClientHeight > 0);
    assertTrue(oldClientWidth > 0);

    // Compare to the dimensions with scroll bars
    Window.enableScrolling(true);
    final Label largeDOM = new Label();
    largeDOM.setPixelSize(oldClientWidth + 100, oldClientHeight + 100);
    RootPanel.get().add(largeDOM);
    delayTestFinish(200);
    DeferredCommand.addCommand(new Command() {
      public void execute() {
        int newClientHeight = Window.getClientHeight();
        int newClientWidth = Window.getClientWidth();
        assertTrue(newClientHeight < oldClientHeight);
        assertTrue(newClientWidth < oldClientWidth);
        RootPanel.get().remove(largeDOM);
        finishTest();
      }
    });
  }

  /**
   * Tests the ability of scroll the Window and catch scroll events.
   */
  @DoNotRunWith({Platform.Htmlunit})
  public void testScrolling() {
    // Force scroll bars to appear
    Window.enableScrolling(true);
    int clientHeight = Window.getClientHeight();
    int clientWidth = Window.getClientWidth();
    final Label largeDOM = new Label();
    largeDOM.setPixelSize(clientWidth + 500, clientHeight + 500);
    RootPanel.get().add(largeDOM);

    // Listener for scroll events
    Window.scrollTo(100, 200);
    assertEquals(100, Window.getScrollLeft());
    assertEquals(200, Window.getScrollTop());
    Window.scrollTo(0, 0);
    assertEquals(0, Window.getScrollLeft());
    assertEquals(0, Window.getScrollTop());

    // Cleanup the window
    RootPanel.get().remove(largeDOM);
  }

  @SuppressWarnings("deprecation")
  static class ListenerTester implements WindowResizeListener {
    static int resize = 0;
 
    public void onWindowResized(int width, int height) {
      ++resize;
    }

    public static void fire() {
      resize = 0;
      ResizeEvent.fire(Window.handlers, 0, 0);
    }
  }

  @SuppressWarnings("deprecation")
  public void testListenerRemoval() {

    WindowResizeListener r1 = new ListenerTester();
    WindowResizeListener r2 = new ListenerTester();

    Window.addWindowResizeListener(r1);
    Window.addWindowResizeListener(r2);

    ListenerTester.fire();
    assertEquals(ListenerTester.resize, 2);

    Window.removeWindowResizeListener(r1);
    ListenerTester.fire();
    assertEquals(ListenerTester.resize, 1);
    
    Window.removeWindowResizeListener(r2);
    ListenerTester.fire();
    assertEquals(ListenerTester.resize, 0);
  }

}
