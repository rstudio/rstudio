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
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.event.logical.shared.HasResizeHandlers;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.impl.WindowImpl;

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
   * Fired just before the browser window closes or navigates to a different
   * site.
   */
  public static class ClosingEvent extends GwtEvent<Window.ClosingHandler> {
    /**
     * The event type.
     */
    private static final Type<ClosingHandler> TYPE = new Type<ClosingHandler>();

    static Type<ClosingHandler> getType() {
      return TYPE;
    }

    /**
     * The message to display to the user to see whether they really want to
     * leave the page.
     */
    private String message = null;

    @Override
    public final Type<ClosingHandler> getAssociatedType() {
      return TYPE;
    }

    /**
     * Get the message that will be presented to the user in a confirmation
     * dialog that asks the user whether or not she wishes to navigate away from
     * the page.
     * 
     * @return the message to display to the user, or null
     */
    public String getMessage() {
      return message;
    }

    /**
     * Set the message to a <code>non-null</code> value to present a
     * confirmation dialog that asks the user whether or not she wishes to
     * navigate away from the page. If multiple handlers set the message, the
     * last message will be displayed; all others will be ignored.
     * 
     * @param message the message to display to the user, or null
     */
    public void setMessage(String message) {
      this.message = message;
    }

    @Override
    protected void dispatch(ClosingHandler handler) {
      handler.onWindowClosing(this);
    }
  }

  /**
   * Handler for {@link Window.ClosingEvent} events.
   */
  public interface ClosingHandler extends EventHandler {
    /**
     * Fired just before the browser window closes or navigates to a different
     * site. No user-interface may be displayed during shutdown.
     * 
     * @param event the event
     */
    void onWindowClosing(Window.ClosingEvent event);
  }

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
     * parameters have been specified with the same name, the last one will be
     * returned.
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
     * Builds the immutable map from String to List<String> that we'll return in
     * getParameterMap(). Package-protected for testing.
     * 
     * @return a map from the
     */
    static Map<String, List<String>> buildListParamMap(String queryString) {
      Map<String, List<String>> out = new HashMap<String, List<String>>();

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
          values.add(kv.length > 1 ? URL.decodeComponent(kv[1]) : "");
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
              paramMap.put(kv[0], URL.decodeComponent(kv[1]));
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

  /**
   * Fired when the browser window is scrolled.
   */
  public static class ScrollEvent extends GwtEvent<Window.ScrollHandler> {
    /**
     * The event type.
     */
    static final Type<Window.ScrollHandler> TYPE = new Type<Window.ScrollHandler>();

    static Type<Window.ScrollHandler> getType() {
      return TYPE;
    }

    private int scrollLeft;
    private int scrollTop;

    /**
     * Construct a new {@link Window.ScrollEvent}.
     * 
     * @param scrollLeft the left scroll position
     * @param scrollTop the top scroll position
     */
    private ScrollEvent(int scrollLeft, int scrollTop) {
      this.scrollLeft = scrollLeft;
      this.scrollTop = scrollTop;
    }

    @Override
    public final Type<ScrollHandler> getAssociatedType() {
      return TYPE;
    }

    /**
     * Gets the window's scroll left.
     * 
     * @return window's scroll left
     */
    public int getScrollLeft() {
      return scrollLeft;
    }

    /**
     * Get the window's scroll top.
     * 
     * @return the window's scroll top
     */
    public int getScrollTop() {
      return scrollTop;
    }

    @Override
    protected void dispatch(ScrollHandler handler) {
      handler.onWindowScroll(this);
    }
  }

  /**
   * Handler for {@link Window.ScrollEvent} events.
   */
  public interface ScrollHandler extends EventHandler {
    /**
     * Fired when the browser window is scrolled.
     * 
     * @param event the event
     */
    void onWindowScroll(Window.ScrollEvent event);
  }

  private static class WindowHandlers extends HandlerManager implements
      HasCloseHandlers<Window>, HasResizeHandlers, HasHandlers {

    public WindowHandlers() {
      super(null);
    }

    public HandlerRegistration addCloseHandler(CloseHandler<Window> handler) {
      return addHandler(CloseEvent.getType(), handler);
    }

    public HandlerRegistration addResizeHandler(ResizeHandler handler) {
      return addHandler(ResizeEvent.getType(), handler);
    }

    public HandlerManager getHandlers() {
      return this;
    }
  }

  // Package protected for testing.
  static WindowHandlers handlers;
  private static boolean closeHandlersInitialized;
  private static boolean scrollHandlersInitialized;
  private static boolean resizeHandlersInitialized;
  private static int lastResizeWidth;
  private static int lastResizeHeight;
  
  private static final WindowImpl impl = GWT.create(WindowImpl.class);

  /**
   * Adds a {@link CloseEvent} handler.
   * 
   * @param handler the handler
   * @return returns the handler registration
   */
  public static HandlerRegistration addCloseHandler(CloseHandler<Window> handler) {
    maybeInitializeCloseHandlers();
    return addHandler(CloseEvent.getType(), handler);
  }

  /**
   * Adds a {@link ResizeEvent} handler.
   * 
   * @param handler the handler
   * @return returns the handler registration
   */
  public static HandlerRegistration addResizeHandler(ResizeHandler handler) {
    maybeInitializeCloseHandlers();
    maybeInitializeResizeHandlers();
    return addHandler(ResizeEvent.getType(), handler);
  }

  /**
   * Adds a listener to receive window closing events.
   * 
   * @deprecated use {@link Window#addWindowClosingHandler(ClosingHandler)} and
   *             {@link Window#addCloseHandler(CloseHandler)} instead
   * @param listener the listener to be informed when the window is closing
   */
  @Deprecated
  public static void addWindowCloseListener(WindowCloseListener listener) {
    BaseListenerWrapper.WrapWindowClose.add(listener);
  }

  /**
   * Adds a {@link Window.ClosingEvent} handler.
   * 
   * @param handler the handler
   * @return returns the handler registration
   */
  public static HandlerRegistration addWindowClosingHandler(
      ClosingHandler handler) {
    maybeInitializeCloseHandlers();
    return addHandler(Window.ClosingEvent.getType(), handler);
  }

  /**
   * Adds a listener to receive window resize events.
   * 
   * @param listener the listener to be informed when the window is resized
   * @deprecated use {@link Window#addResizeHandler(ResizeHandler)} instead
   */
  @Deprecated
  public static void addWindowResizeListener(WindowResizeListener listener) {
    BaseListenerWrapper.WrapWindowResize.add(listener);
  }

  /**
   * Adds a {@link Window.ScrollEvent} handler.
   * 
   * @param handler the handler
   * @return returns the handler registration
   */
  public static HandlerRegistration addWindowScrollHandler(
      Window.ScrollHandler handler) {
    maybeInitializeCloseHandlers();
    maybeInitializeScrollHandlers();
    return addHandler(Window.ScrollEvent.getType(), handler);
  }

  /**
   * Adds a listener to receive window scroll events.
   * 
   * @param listener the listener to be informed when the window is scrolled
   * @deprecated use {@link Window#addWindowScrollHandler(ScrollHandler)} instead
   */
  @Deprecated
  public static void addWindowScrollListener(WindowScrollListener listener) {
    BaseListenerWrapper.WrapWindowScroll.add(listener);
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
    Document.get().enableScrolling(enable);
  }

  /**
   * Gets the height of the browser window's client area excluding the scroll
   * bar.
   * 
   * @return the window's client height
   */
  public static int getClientHeight() {
    return Document.get().getClientHeight();
  }

  /**
   * Gets the width of the browser window's client area excluding the vertical
   * scroll bar.
   * 
   * @return the window's client width
   */
  public static int getClientWidth() {
    return Document.get().getClientWidth();
  }

  /**
   * Gets the window's scroll left.
   * 
   * @return window's scroll left
   */
  public static int getScrollLeft() {
    return Document.get().getScrollLeft();
  }

  /**
   * Get the window's scroll top.
   * 
   * @return the window's scroll top
   */
  public static int getScrollTop() {
    return Document.get().getScrollTop();
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
   * 'http://developer.mozilla.org/en/docs/DOM:window.open'>here</a>.
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
  @Deprecated
  public static void removeWindowCloseListener(WindowCloseListener listener) {
    BaseListenerWrapper.WrapWindowClose.remove(handlers, listener);
  }

  /**
   * Removes a window resize listener.
   * 
   * @param listener the listener to be removed
   */
  @Deprecated
  public static void removeWindowResizeListener(WindowResizeListener listener) {
    BaseListenerWrapper.WrapWindowResize.remove(handlers, listener);
  }

  /**
   * Removes a window scroll listener.
   * 
   * @param listener the listener to be removed
   */
  @Deprecated
  public static void removeWindowScrollListener(WindowScrollListener listener) {
    BaseListenerWrapper.WrapWindowScroll.remove(handlers, listener);
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

  /**
   * Adds this handler to the Window.
   * 
   * @param <H> the type of handler to add
   * @param type the event type
   * @param handler the handler
   * @return {@link HandlerRegistration} used to remove the handler
   */
  private static <H extends EventHandler> HandlerRegistration addHandler(
      GwtEvent.Type<H> type, final H handler) {
    return getHandlers().addHandler(type, handler);
  }

  private static void fireClosedAndCatch(UncaughtExceptionHandler handler) {
    try {
      fireClosedImpl();
    } catch (Throwable e) {
      handler.onUncaughtException(e);
    }
  }

  private static void fireClosedImpl() {
    if (closeHandlersInitialized) {
      CloseEvent.fire(getHandlers(), null);
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
    if (closeHandlersInitialized) {
      Window.ClosingEvent event = new Window.ClosingEvent();
      fireEvent(event);
      return event.getMessage();
    }
    return null;
  }

  /**
   * Fires an event.
   * 
   * @param event the event
   */
  private static void fireEvent(GwtEvent<?> event) {
    if (handlers != null) {
      handlers.fireEvent(event);
    }
  }

  private static void fireResizedAndCatch(UncaughtExceptionHandler handler) {
    try {
      fireResizedImpl();
    } catch (Throwable e) {
      handler.onUncaughtException(e);
    }
  }

  private static void fireResizedImpl() {
    if (resizeHandlersInitialized) {
      // On webkit and IE we sometimes get duplicate window resize events.
      // Here, we manually filter them.
      int width = getClientWidth();
      int height = getClientHeight();
      if (lastResizeWidth != width || lastResizeHeight != height) {
        lastResizeWidth = width;
        lastResizeHeight = height;
        ResizeEvent.fire(getHandlers(), width, height);
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
    if (scrollHandlersInitialized) {
      fireEvent(new Window.ScrollEvent(getScrollLeft(), getScrollTop()));
    }
  }

  private static WindowHandlers getHandlers() {
    if (handlers == null) {
      handlers = new WindowHandlers();
    }
    return handlers;
  }

  private static void maybeInitializeCloseHandlers() {
    if (GWT.isClient() && !closeHandlersInitialized) {
      impl.initWindowCloseHandler();
      closeHandlersInitialized = true;
    }
  }

  private static void maybeInitializeResizeHandlers() {
    if (GWT.isClient() && !resizeHandlersInitialized) {
      impl.initWindowResizeHandler();
      resizeHandlersInitialized = true;
    }
  }

  private static void maybeInitializeScrollHandlers() {
    if (GWT.isClient() && !scrollHandlersInitialized) {
      impl.initWindowScrollHandler();
      scrollHandlersInitialized = true;
    }
  }

  private Window() {
  }
}
