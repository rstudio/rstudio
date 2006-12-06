/*
 * Copyright 2006 Google Inc.
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

import java.util.Iterator;
import java.util.Vector;

/**
 * This class provides access to the browser window's methods, properties, and
 * events.
 */
public class Window {

  private static Vector closingListeners = new Vector();
  private static Vector resizeListeners = new Vector();

  static {
    init();
  }

  /**
   * Adds a listener to receive window closing events.
   * 
   * @param listener the listener to be informed when the window is closing
   */
  public static void addWindowCloseListener(WindowCloseListener listener) {
    closingListeners.add(listener);
  }

  /**
   * Adds a listener to receive window resize events.
   * 
   * @param listener the listener to be informed when the window is resized
   */
  public static void addWindowResizeListener(WindowResizeListener listener) {
    resizeListeners.add(listener);
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
   * Use this method to explicitly disable the window's scrollbars.
   * Applications that choose to resize their user-interfaces to fit within the
   * window's client area will normally want to disable window scrolling.
   * 
   * @param enable <code>false</code> to disable window scrolling
   */
  public static native void enableScrolling(boolean enable) /*-{
    $doc.body.style.overflow = enable ? 'auto' : 'hidden';
  }-*/;

  /**
   * Gets the height of the browser window's client area.
   * 
   * @return the window's client height
   */
  public static native int getClientHeight() /*-{
    if ($wnd.innerHeight)
      return $wnd.innerHeight;
    return $doc.body.clientHeight;
  }-*/;

  /**
   * Gets the width of the browser window's client area.
   * 
   * @return the window's client width
   */
  public static native int getClientWidth() /*-{
    if ($wnd.innerWidth)
      return $wnd.innerWidth;
    return $doc.body.clientWidth;
  }-*/;

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
   * Removes a window closing listener.
   * 
   * @param listener the listener to be removed
   */
  public static void removeWindowCloseListener(WindowCloseListener listener) {
    closingListeners.remove(listener);
  }

  /**
   * Removes a window resize listener.
   * 
   * @param listener the listener to be removed
   */
  public static void removeWindowResizeListener(WindowResizeListener listener) {
    resizeListeners.remove(listener);
  }

  /**
   * Sets the size of the margins used within the window's client area.  It is
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

  private static void fireClosedAndCatch(UncaughtExceptionHandler handler) {
    try {
      fireClosedImpl();
    } catch (Throwable e) {
      handler.onUncaughtException(e);
    }
  }

  private static void fireClosedImpl() {
    for (Iterator it = closingListeners.iterator(); it.hasNext();) {
      WindowCloseListener listener = (WindowCloseListener) it.next();
      listener.onWindowClosed();
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
    for (Iterator it = closingListeners.iterator(); it.hasNext();) {
      WindowCloseListener listener = (WindowCloseListener) it.next();

      // If any listener wants to suppress the window closing event, then do so.
      String msg = listener.onWindowClosing();
      if (ret == null) {
        ret = msg;
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
    for (Iterator it = resizeListeners.iterator(); it.hasNext();) {
      WindowResizeListener listener = (WindowResizeListener) it.next();
      listener.onWindowResized(getClientWidth(), getClientHeight());
    }
  }

  private static native void init() /*-{
    $wnd.__gwt_initHandlers(
      function() {
        @com.google.gwt.user.client.Window::onResize()();
      },
      function() {
        return @com.google.gwt.user.client.Window::onClosing()();
      },
      function() {
        @com.google.gwt.user.client.Window::onClosed()();
        $wnd.onresize = null;
        $wnd.onbeforeclose = null;
        $wnd.onclose = null;
      }
    );
  }-*/;

  private Window() {
  }
}
