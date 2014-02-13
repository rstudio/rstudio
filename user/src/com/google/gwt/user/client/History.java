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
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.impl.Disposable;
import com.google.gwt.core.client.impl.Impl;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;

/**
 * This class allows you to interact with the browser's history stack. Each
 * "item" on the stack is represented by a single string, referred to as a
 * "token". You can create new history items (which have a token associated with
 * them when they are created), and you can programmatically force the current
 * history to move back or forward.
 *
 * <p>
 * In order to receive notification of user-directed changes to the current
 * history item, implement the {@link ValueChangeHandler} interface and attach
 * it via {@link #addValueChangeHandler(ValueChangeHandler)}.
 * </p>
 *
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.HistoryExample}
 * </p>
 *
 * <p>
 * <h3>URL Encoding</h3>
 * Any valid characters may be used in the history token and will survive
 * round-trips through {@link #newItem(String)} to {@link #getToken()}/
 * {@link ValueChangeHandler#onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)}
 * , but most will be encoded in the user-visible URL. The following US-ASCII
 * characters are not encoded on any currently supported browser (but may be in
 * the future due to future browser changes):
 * <ul>
 * <li>a-z
 * <li>A-Z
 * <li>0-9
 * <li>;,/?:@&=+$-_.!~*()
 * </ul>
 * </p>
 */
public class History {

  private static class HistoryEventSource implements HasValueChangeHandlers<String> {

    private HandlerManager handlers = new HandlerManager(null);

    @Override
    public void fireEvent(GwtEvent<?> event) {
      handlers.fireEvent(event);
    }

    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {
      return handlers.addHandler(ValueChangeEvent.getType(), handler);
    }

    public void fireValueChangedEvent(String newToken) {
      ValueChangeEvent.fire(this, newToken);
    }

    public HandlerManager getHandlers() {
      return handlers;
    }
  }

  private interface HistoryImpl {
    void attachListener(JavaScriptObject handler);

    void detachListener(JavaScriptObject handler);

    void newToken(String historyToken);

    void replaceToken(String historyToken);
  }

  /**
   * This is the standard implementation for HistoryImpl using HTML pushstate.
   */
  private static class HistoryImplPushState implements HistoryImpl {
    // List of browsers that do not support pushstate:
    // IE8-9, Android 3.x, Android 4.0, Android 4.1
    static native boolean isSupported() /*-{
      return $wnd.history.pushState != null && $wnd.history.replaceState != null;
    }-*/;

    @Override
    public native void attachListener(JavaScriptObject handler) /*-{
      $wnd.addEventListener('popstate', handler);
    }-*/;

    @Override
    public native void detachListener(JavaScriptObject handler) /*-{
      $wnd.removeEventListener('popstate', handler);
    }-*/;

    @Override
    public native void newToken(String historyToken) /*-{
      $wnd.history.pushState({}, "", '#' + historyToken);
    }-*/;

    @Override
    public native void replaceToken(String historyToken) /*-{
      $wnd.history.replaceState({}, "", '#' + historyToken);
    }-*/;
  }

  /**
   * History implementation using hash tokens.
   * <p>This is the fallback implementation for browsers that do not support HTML5
   * pushstate.
   */
  private static class HistoryImplHashToken implements HistoryImpl {

    @Override
    public native void attachListener(JavaScriptObject handler) /*-{
      $wnd.addEventListener('hashchange', handler);
    }-*/;

    @Override
    public native void detachListener(JavaScriptObject handler) /*-{
      $wnd.removeEventListener('hashchange', handler);
    }-*/;

    @Override
    public native void newToken(String historyToken) /*-{
      $wnd.location.hash = historyToken;
    }-*/;

    @Override
    public void replaceToken(String historyToken) {
      Window.Location.replace("#" + historyToken);
    }
  }

  /**
   * History implementation for IE8 using onhashchange.
   */
  @SuppressWarnings("unused")
  private static class HistoryImplIE8 extends HistoryImplHashToken {

    private JavaScriptObject oldHandler;

    @Override
    public native void attachListener(JavaScriptObject handler) /*-{
      var oldHandler = $wnd.onhashchange;
      $wnd.onhashchange = function() {
        var ex;

        try {
          handler();
        } catch(e) {
          ex = e;
        }

        if (oldHandler != null) {
          try {
            oldHandler();
          } catch(e) {
            ex = ex || e;
          }
        }

        if (ex != null) {
          throw ex;
        }
      };
      this.@com.google.gwt.user.client.History.HistoryImplIE8::oldHandler = oldHandler;
    }-*/;

    @Override
    public native void detachListener(JavaScriptObject handler) /*-{
      $wnd.onhashchange = this.@com.google.gwt.user.client.History.HistoryImplIE8::oldHandler;
      this.@com.google.gwt.user.client.History.HistoryImplIE8::oldHandler = null;
    }-*/;
  }

  @SuppressWarnings("deprecation")
  private static class WrapHistory extends BaseListenerWrapper<HistoryListener>
      implements ValueChangeHandler<String> {
    @Deprecated
    public static void add(HistoryListener listener) {
      addValueChangeHandler(new WrapHistory(listener));
    }

    public static void remove(HandlerManager manager, HistoryListener listener) {
      baseRemove(manager, listener, ValueChangeEvent.getType());
    }

    private WrapHistory(HistoryListener listener) {
      super(listener);
    }

    @Override
    public void onValueChange(ValueChangeEvent<String> event) {
      listener.onHistoryChanged(event.getValue());
    }
  }

  private static HistoryEventSource historyEventSource;
  private static String token;
  private static HistoryImpl impl;

  static {
    impl = createHistoryImpl();
    historyEventSource = new HistoryEventSource();
    token = getDecodedHash();
    final JavaScriptObject handler = getHistoryChangeHandler();
    impl.attachListener(handler);
    Impl.scheduleDispose(new Disposable() {
      @Override
      public void dispose() {
        impl.detachListener(handler);
      }
    });
  }

  private static HistoryImpl createHistoryImpl() {
    HistoryImpl historyImpl = GWT.create(HistoryImpl.class);
    // Feature test for HTML5 pushstate, otherwise use hashtoken version.
    if (historyImpl instanceof HistoryImplPushState && !HistoryImplPushState.isSupported()) {
      return new HistoryImplHashToken();
    }
    return historyImpl;
  }

  /**
   * Adds a listener to be informed of changes to the browser's history stack.
   *
   * @param listener the listener to be added
   * @deprecated use {@link History#addValueChangeHandler(ValueChangeHandler)} instead
   */
  @Deprecated
  public static void addHistoryListener(HistoryListener listener) {
    WrapHistory.add(listener);
  }

  /**
   * Adds a {@link com.google.gwt.event.logical.shared.ValueChangeEvent} handler
   * to be informed of changes to the browser's history stack.
   *
   * @param handler the handler
   * @return the registration used to remove this value change handler
   */
  public static HandlerRegistration addValueChangeHandler(
      ValueChangeHandler<String> handler) {
    return historyEventSource.addValueChangeHandler(handler);
  }

  /**
   * Programmatic equivalent to the user pressing the browser's 'back' button.
   */
  public static native void back() /*-{
    $wnd.history.back();
  }-*/;

  /**
   * Encode a history token for use as part of a URI.
   *
   * @param historyToken the token to encode
   * @return the encoded token, suitable for use as part of a URI
   */
  public static native String encodeHistoryToken(String historyToken) /*-{
    // encodeURI() does *not* encode the '#' character.
    return $wnd.encodeURI(historyToken).replace("#", "%23");
  }-*/;

  /**
   * Fire
   * {@link ValueChangeHandler#onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)}
   * events with the current history state. This is most often called at the end
   * of an application's
   * {@link com.google.gwt.core.client.EntryPoint#onModuleLoad()} to inform
   * history handlers of the initial application state.
   */
  public static void fireCurrentHistoryState() {
    String currentToken = getToken();
    historyEventSource.fireValueChangedEvent(currentToken);
  }

  /**
   * Programmatic equivalent to the user pressing the browser's 'forward'
   * button.
   */
  public static native void forward() /*-{
    $wnd.history.forward();
  }-*/;

  /**
   * Gets the current history token. The handler will not receive a
   * {@link ValueChangeHandler#onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)}
   * event for the initial token; requiring that an application request the
   * token explicitly on startup gives it an opportunity to run different
   * initialization code in the presence or absence of an initial token.
   *
   * @return the initial token, or the empty string if none is present.
   */
  public static String getToken() {
    return token;
  }

  /**
   * Adds a new browser history entry. Calling this method will cause
   * {@link ValueChangeHandler#onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)}
   * to be called as well.
   *
   * @param historyToken the token to associate with the new history item
   */
  public static void newItem(String historyToken) {
    newItem(historyToken, true);
  }

  /**
   * Adds a new browser history entry. Calling this method will cause
   * {@link ValueChangeHandler#onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)}
   * to be called as well if and only if issueEvent is true.
   *
   * @param historyToken the token to associate with the new history item
   * @param issueEvent true if a
   *          {@link ValueChangeHandler#onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)}
   *          event should be issued
   */
  public static void newItem(String historyToken, boolean issueEvent) {
    historyToken = (historyToken == null) ? "" : historyToken;
    if (!historyToken.equals(getToken())) {
      token = historyToken;
      String updateToken = encodeHistoryToken(historyToken);
      impl.newToken(updateToken);
      if (issueEvent) {
        historyEventSource.fireValueChangedEvent(historyToken);
      }
    }
  }

  /**
   * Call all history handlers with the specified token. Note that this does not
   * change the history system's idea of the current state and is only kept for
   * backward compatibility. To fire history events for the initial state of the
   * application, instead call {@link #fireCurrentHistoryState()} from the
   * application {@link com.google.gwt.core.client.EntryPoint#onModuleLoad()}
   * method.
   *
   * @param historyToken history token to fire events for
   * @deprecated Use {@link #fireCurrentHistoryState()} instead.
   */
  @Deprecated
  public static void onHistoryChanged(String historyToken) {
    historyEventSource.fireValueChangedEvent(historyToken);
  }

  /**
   * Removes a history listener.
   *
   * @param listener the listener to be removed
   */
  @Deprecated
  public static void removeHistoryListener(HistoryListener listener) {
    WrapHistory.remove(historyEventSource.getHandlers(), listener);
  }

  /**
   * Replace the current history token on top of the browsers history stack.
   *
   * <p>Note: This method has problems on older browsers (IE8 and IE9). Since these
   * do not support HTML5 pushState we update the URL with window.location.replace,
   * this unfortunately has side effects when using the iframe linker. Make sure you are
   * using the cross site iframe linker when using this method in your code.
   *
   * <p>Calling this method will cause
   * {@link ValueChangeHandler#onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)}
   * to be called as well.
   *
   * @param historyToken history token to replace current top entry
   */
  public static void replaceItem(String historyToken) {
    replaceItem(historyToken, true);
  }

  /**
   * Replace the current history token on top of the browsers history stack.
   *
   * <p>Note: This method has problems on older browsers (IE8 and IE9). Since these
   * do not support HTML5 pushState we update the URL with window.location.replace,
   * this unfortunately has side effects when using the iframe linker. Make sure you are
   * using the cross site iframe linker when using this method in your code.
   *
   * <p>Calling this method will cause
   * {@link ValueChangeHandler#onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)}
   * to be called as well if and only if issueEvent is true.
   *
   * @param historyToken history token to replace current top entry
   * @param issueEvent issueEvent true if a
   *          {@link ValueChangeHandler#onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)}
   *          event should be issued
   */
  public static void replaceItem(String historyToken, boolean issueEvent) {
    token = historyToken;
    impl.replaceToken(encodeHistoryToken(historyToken));
    if (issueEvent) {
      fireCurrentHistoryState();
    }
  }

  private static native String decodeURI(String s) /*-{
    return $wnd.decodeURI(s.replace("%23", "#"));
  }-*/;

  private static String getDecodedHash() {
    String hashToken = Window.Location.getHash();
    return hashToken.isEmpty() ? "" : decodeURI(hashToken.substring(1));
  }

  private static native JavaScriptObject getHistoryChangeHandler() /*-{
    return $entry(@com.google.gwt.user.client.History::onHashChanged());
  }-*/;

  // this is called from JS when the native onhashchange occurs
  private static void onHashChanged() {
    /*
     * We guard against firing events twice, some browser (e.g. safari) tend to
     * fire events on startup if HTML5 pushstate is used.
     */
    String hashToken = getDecodedHash();
    if (!hashToken.equals(getToken())) {
      token = hashToken;
      historyEventSource.fireValueChangedEvent(hashToken);
    }
  }
}
