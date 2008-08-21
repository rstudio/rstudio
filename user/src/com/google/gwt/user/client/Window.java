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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ScriptElement;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.impl.WindowImpl;
import com.google.gwt.user.client.ui.RootPanel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class provides access to the browser window's methods, properties, and
 * events.
 */
public class Window {

  /**
   * This class provides access to the browser's location's object. The location
   * object contains information about the current URL and methods to manipulate
   * it. <code>Location</code> is a very simple wrapper, so not all browser
   * quirks are hidden from the user.
   * 
   */
  public static class Location {
    private static Map<String, String> paramMap;
    private static Map<String, List<String>> listParamMap;

    /**
     * Assigns the window to a new URL. All GWT state will be lost.
     * 
     * @param newURL the new URL
     */
    public static native void assign(String newURL) /*-{
      $wnd.location.assign(newURL);
    }-*/;

    /**
     * Gets the string to the right of the URL's hash.
     * 
     * @return the string to the right of the URL's hash.
     */
    public static String getHash() {
      return impl.getHash();
    }

    /**
     * Gets the URL's host and port name.
     * 
     * @return the host and port name
     */
    public static native String getHost() /*-{
      return $wnd.location.host;
    }-*/;

    /**
     * Gets the URL's host name.
     * 
     * @return the host name
     */
    public static native String getHostName() /*-{
      return $wnd.location.hostname;
    }-*/;

    /**
     * Gets the entire URL.
     * 
     * @return the URL
     */
    public static native String getHref() /*-{
      return $wnd.location.href;
    }-*/;

    /**
     * Gets the URL's parameter of the specified name. Note that if multiple
     * parameters have been specified with the same name, the last one will
     * be returned.
     * 
     * @param name the name of the URL's parameter
     * @return the value of the URL's parameter
     */
    public static String getParameter(String name) {
      ensureParameterMap();
      return paramMap.get(name);
    }

    /**
     * Returns a Map of the URL query parameters for the host page; since
     * changing the map would not change the window's location, the map returned
     * is immutable.
     * 
     * @return a map from URL query parameter names to a list of values
     */
    public static Map<String, List<String>> getParameterMap() {
      if (listParamMap == null) {
        listParamMap = buildListParamMap(getQueryString());
      }
      return listParamMap;
    }

    /**
     * Gets the path to the URL.
     * 
     * @return the path to the URL.
     */
    public static native String getPath() /*-{
      return $wnd.location.pathname;
    }-*/;

    /**
     * Gets the URL's port.
     * 
     * @return the URL's port
     */
    public static native String getPort() /*-{
      return $wnd.location.port;
    }-*/;

    /**
     * Gets the URL's protocol.
     * 
     * @return the URL's protocol.
     */
    public static native String getProtocol() /*-{
      return $wnd.location.protocol;
    }-*/;

    /**
     * Gets the URL's query string.
     * 
     * @return the URL's query string
     */
    public static String getQueryString() {
      return impl.getQueryString();
    }

    /**
     * Reloads the current browser window. All GWT state will be lost.
     */
    public static native void reload() /*-{
      $wnd.location.reload();
    }-*/;

    /**
     * Replaces the current URL with a new one. All GWT state will be lost. In
     * the browser's history, the current URL will be replaced by the new URL.
     * 
     * @param newURL the new URL
     */
    public static native void replace(String newURL) /*-{
      $wnd.location.replace(newURL);
    }-*/;

    /**
     * Builds the immutable map from String to List<String> that we'll return
     * in getParameterMap(). Package-protected for testing.
     * @return a map from the 
     */
    static Map<String,List<String>> buildListParamMap(String queryString) {
      Map<String,List<String>> out = new HashMap<String, List<String>>();

      if (queryString != null && queryString.length() > 1) {
        String qs = queryString.substring(1);

        for (String kvPair : qs.split("&")) {
          String[] kv = kvPair.split("=", 2);
          if (kv[0].length() == 0) {
            continue;
          }

          List<String> values = out.get(kv[0]);
          if (values == null) {
            values = new ArrayList<String>();
            out.put(kv[0], values);
          }
          values.add(kv.length > 1 ? URL.decode(kv[1]) : "");
        }
      }

      for (Map.Entry<String, List<String>> entry : out.entrySet()) {
        entry.setValue(Collections.unmodifiableList(entry.getValue()));
      }

      out = Collections.unmodifiableMap(out);

      return out;
    }
    
    private static void ensureParameterMap() {
      if (paramMap == null) {
        paramMap = new HashMap<String, String>();
        String queryString = getQueryString();
        if (queryString != null && queryString.length() > 1) {
          String qs = queryString.substring(1);
          for (String kvPair : qs.split("&")) {
            String[] kv = kvPair.split("=", 2);
            if (kv.length > 1) {
              paramMap.put(kv[0], URL.decode(kv[1]));
            } else {
              paramMap.put(kv[0], "");
            }
          }
        }
      }
    }

    private Location() {
    }
  }

  private static boolean handlersAreInitialized;
  private static final WindowImpl impl = GWT.create(WindowImpl.class);

  private static ArrayList<WindowCloseListener> closingListeners;
  private static ArrayList<WindowResizeListener> resizeListeners;
  private static ArrayList<WindowScrollListener> scrollListeners;

  /**
   * Adds a listener to receive window closing events.
   * 
   * @param listener the listener to be informed when the window is closing
   */
  public static void addWindowCloseListener(WindowCloseListener listener) {
    maybeInitializeHandlers();
    if (closingListeners == null) {
      closingListeners = new ArrayList<WindowCloseListener>();
    }
    closingListeners.add(listener);
  }

  /**
   * Adds a listener to receive window resize events.
   * 
   * @param listener the listener to be informed when the window is resized
   */
  public static void addWindowResizeListener(WindowResizeListener listener) {
    maybeInitializeHandlers();
    if (resizeListeners == null) {
      resizeListeners = new ArrayList<WindowResizeListener>();
    }
    resizeListeners.add(listener);
  }

  /**
   * Adds a listener to receive window scroll events.
   * 
   * @param listener the listener to be informed when the window is scrolled
   */
  public static void addWindowScrollListener(WindowScrollListener listener) {
    maybeInitializeHandlers();
    if (scrollListeners == null) {
      scrollListeners = new ArrayList<WindowScrollListener>();
    }
    scrollListeners.add(listener);
  }

  /**
   * Displays a message in a modal dialog box.
   * 
   * @param msg the message to be displayed.
   */
  public static native void alert(String msg) /*-{
    $wnd.alert(msg);
  }-*/;

  /**
   * Displays a message in a modal dialog box, along with the standard 'OK' and
   * 'Cancel' buttons.
   * 
   * @param msg the message to be displayed.
   * @return <code>true</code> if 'OK' is clicked, <code>false</code> if
   *         'Cancel' is clicked.
   */
  public static native boolean confirm(String msg) /*-{
    return $wnd.confirm(msg);
  }-*/;

  /**
   * Use this method to explicitly disable the window's scrollbars. Applications
   * that choose to resize their user-interfaces to fit within the window's
   * client area will normally want to disable window scrolling.
   * 
   * @param enable <code>false</code> to disable window scrolling
   */
  public static void enableScrolling(boolean enable) {
    impl.enableScrolling(enable);
  }

  /**
   * Gets the height of the browser window's client area excluding the scroll
   * bar.
   * 
   * @return the window's client height
   */
  public static int getClientHeight() {
    return impl.getClientHeight();
  }

  /**
   * Gets the width of the browser window's client area excluding the vertical
   * scroll bar.
   * 
   * @return the window's client width
   */
  public static int getClientWidth() {
    return impl.getClientWidth();
  }

  /**
   * Gets the window's scroll left.
   * 
   * @return window's scroll left
   */
  public static int getScrollLeft() {
    return impl.getScrollLeft();
  }

  /**
   * Get the window's scroll top.
   * 
   * @return the window's scroll top
   */
  public static int getScrollTop() {
    return impl.getScrollTop();
  }

  /**
   * Gets the browser window's current title.
   * 
   * @return the window's title.
   */
  public static native String getTitle() /*-{
    return $doc.title;
  }-*/;

  /**
   * Opens a new browser window. The "name" and "features" arguments are
   * specified <a href=
   * 'http://www.mozilla.org/docs/dom/domref/dom_window_ref76.html'>here</a>.
   * 
   * @param url the URL that the new window will display
   * @param name the name of the window (e.g. "_blank")
   * @param features the features to be enabled/disabled on this window
   */
  public static native void open(String url, String name, String features) /*-{
    $wnd.open(url, name, features);
  }-*/;

  /**
   * Prints the document in the window, as if the user had issued a "Print"
   * command.
   */
  public static native void print() /*-{
    $wnd.print();
  }-*/;

  /**
   * Displays a request for information in a modal dialog box, along with the
   * standard 'OK' and 'Cancel' buttons.
   * 
   * @param msg the message to be displayed
   * @param initialValue the initial value in the dialog's text field
   * @return the value entered by the user if 'OK' was pressed, or
   *         <code>null</code> if 'Cancel' was pressed
   */
  public static native String prompt(String msg, String initialValue) /*-{
    return $wnd.prompt(msg, initialValue);
  }-*/;

  /**
   * Removes a window closing listener.
   * 
   * @param listener the listener to be removed
   */
  public static void removeWindowCloseListener(WindowCloseListener listener) {
    if (closingListeners != null) {
      closingListeners.remove(listener);
    }
  }

  /**
   * Removes a window resize listener.
   * 
   * @param listener the listener to be removed
   */
  public static void removeWindowResizeListener(WindowResizeListener listener) {
    if (resizeListeners != null) {
      resizeListeners.remove(listener);
    }
  }

  /**
   * Removes a window scroll listener.
   * 
   * @param listener the listener to be removed
   */
  public static void removeWindowScrollListener(WindowScrollListener listener) {
    if (scrollListeners != null) {
      scrollListeners.remove(listener);
    }
  }

  /**
   * Scroll the window to the specified position.
   * 
   * @param left the left scroll position
   * @param top the top scroll position
   */
  public static native void scrollTo(int left, int top) /*-{
    $wnd.scrollTo(left, top);
  }-*/;

  /**
   * Sets the size of the margins used within the window's client area. It is
   * sometimes necessary to do this because some browsers, such as Internet
   * Explorer, add margins by default, which can confound attempts to resize
   * panels to fit exactly within the window.
   * 
   * @param size the window's new margin size, in CSS units.
   */
  public static native void setMargin(String size) /*-{
    $doc.body.style.margin = size;
  }-*/;

  /**
   * Sets the status text for the window. Calling this method in Firefox has no
   * effect.
   * 
   * @param status the new message to display.
   */
  public static native void setStatus(String status) /*-{
    $wnd.status = status;
  }-*/;

  /**
   * Sets the browser window's title.
   * 
   * @param title the new window title.
   */
  public static native void setTitle(String title) /*-{
    $doc.title = title;
  }-*/;

  static void onClosed() {
    UncaughtExceptionHandler handler = GWT.getUncaughtExceptionHandler();
    if (handler != null) {
      fireClosedAndCatch(handler);
    } else {
      fireClosedImpl();
    }
  }

  static String onClosing() {
    UncaughtExceptionHandler handler = GWT.getUncaughtExceptionHandler();
    if (handler != null) {
      return fireClosingAndCatch(handler);
    } else {
      return fireClosingImpl();
    }
  }

  static void onResize() {
    UncaughtExceptionHandler handler = GWT.getUncaughtExceptionHandler();
    if (handler != null) {
      fireResizedAndCatch(handler);
    } else {
      fireResizedImpl();
    }
  }

  static void onScroll() {
    UncaughtExceptionHandler handler = GWT.getUncaughtExceptionHandler();
    if (handler != null) {
      fireScrollAndCatch(handler);
    } else {
      fireScrollImpl();
    }
  }

  private static void fireClosedAndCatch(UncaughtExceptionHandler handler) {
    try {
      fireClosedImpl();
    } catch (Throwable e) {
      handler.onUncaughtException(e);
    }
  }

  private static void fireClosedImpl() {
    if (closingListeners != null) {
      for (WindowCloseListener listener : closingListeners) {
        listener.onWindowClosed();
      }
    }
  }

  private static String fireClosingAndCatch(UncaughtExceptionHandler handler) {
    try {
      return fireClosingImpl();
    } catch (Throwable e) {
      handler.onUncaughtException(e);
      return null;
    }
  }

  private static String fireClosingImpl() {
    String ret = null;
    if (closingListeners != null) {
      for (WindowCloseListener listener : closingListeners) {
        // If any listener wants to suppress the window closing event, then do
        // so.
        String msg = listener.onWindowClosing();
        if (ret == null) {
          ret = msg;
        }
      }
    }

    return ret;
  }

  private static void fireResizedAndCatch(UncaughtExceptionHandler handler) {
    try {
      fireResizedImpl();
    } catch (Throwable e) {
      handler.onUncaughtException(e);
    }
  }

  private static void fireResizedImpl() {
    if (resizeListeners != null) {
      for (WindowResizeListener listener : resizeListeners) {
        listener.onWindowResized(getClientWidth(), getClientHeight());
      }
    }
  }

  private static void fireScrollAndCatch(UncaughtExceptionHandler handler) {
    try {
      fireScrollImpl();
    } catch (Throwable e) {
      handler.onUncaughtException(e);
    }
  }

  private static void fireScrollImpl() {
    if (scrollListeners != null) {
      for (WindowScrollListener listener : scrollListeners) {
        listener.onScroll(getScrollLeft(), getScrollTop());
      }
    }
  }

  /**
   * This method defines a function that will in turn define the
   * __gwt_initWindowHandlers method that will be used to initialize the event
   * handlers used by the {@link Window}. However, this method returns the
   * function as a String so the __gwt_initWindowHandlers method can be added to
   * the outer window.
   * 
   * We need to declare __gwt_initWindowHandlers on the outer window because you
   * cannot attach Window listeners from within an iframe on IE6.
   * 
   * Per ECMAScript 262 spec 15.3.4.2, Function.prototype.toString() returns a
   * string representation of the function that has the syntax of the function.
   */
  private static native String getInitHandlerMethodString() /*-{
    return function __gwt_initWindowHandlers(resize, scroll, beforeunload, unload) {
      var wnd = window
      , oldOnResize = wnd.onresize
      , oldOnBeforeUnload = wnd.onbeforeunload
      , oldOnUnload = wnd.onunload
      , oldOnScroll = wnd.onscroll
      ;

      wnd.onresize = function(evt) {
        try {
          resize();
        } finally {
          oldOnResize && oldOnResize(evt);
        }
      };

      wnd.onscroll = function(evt) {
        try {
          scroll();
        } finally {
          oldOnScroll && oldOnScroll(evt);
        }
      };

      wnd.onbeforeunload = function(evt) {
        var ret, oldRet;
        try {
          ret = beforeunload();
        } finally {
          oldRet = oldOnBeforeUnload && oldOnBeforeUnload(evt);
        }
        // Avoid returning null as IE6 will coerce it into a string.
        // Ensure that "" gets returned properly.
        if (ret != null) {
          return ret;
        }
        if (oldRet != null) {
          return oldRet;
        }
        // returns undefined.
      };
    
      wnd.onunload = function(evt) {
        try {
          unload();
        } finally {
          oldOnUnload && oldOnUnload(evt);
          wnd.onresize = null;
          wnd.onscroll = null;
          wnd.onbeforeunload = null;
          wnd.onunload = null;
        }
      };
      
      // Remove the reference once we've initialize the handlers
      wnd.__gwt_initWindowHandlers = undefined;
    }.toString();
  }-*/;

  private static native void init() /*-{
    $wnd.__gwt_initWindowHandlers(
      function() {
        @com.google.gwt.user.client.Window::onResize()();
      },
      function() {
        @com.google.gwt.user.client.Window::onScroll()();
      },
      function() {
        return @com.google.gwt.user.client.Window::onClosing()();
      },
      function() {
        @com.google.gwt.user.client.Window::onClosed()();
      }
    );
  }-*/;

  private static void maybeInitializeHandlers() {
    if (GWT.isClient() && !handlersAreInitialized) {
      // Embed the init script on the page
      ScriptElement scriptElem = Document.get().createScriptElement();
      scriptElem.setText(getInitHandlerMethodString());
      Document.get().getBody().appendChild(scriptElem);
      
      // Initialize the handlers
      init();

      // Remove the init script from the page
      RootPanel.getBodyElement().removeChild(scriptElem);
      handlersAreInitialized = true;
    }
  }

  private Window() {
  }
}
