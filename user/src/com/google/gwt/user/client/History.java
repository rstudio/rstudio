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

  private static class HistoryImpl {

    private JavaScriptObject handler;

    public void attachListener(JavaScriptObject handler) {
      this.handler = handler;
      addDomListener(handler);
    }

    public void dispose() {
      removeDomListener(handler);
    }

    public void maybeRunOtherListeners() {
    }

    private native void addDomListener(JavaScriptObject handler) /*-{
      $wnd.addEventListener('hashchange', handler);
    }-*/;

    private native void removeDomListener(JavaScriptObject handler) /*-{
      $wnd.removeEventListener('hashchange', handler);
    }-*/;
  }

  // used for rebinding
  @SuppressWarnings("unused")
  private static class HistoryImplIE8 extends HistoryImpl {

    private JavaScriptObject oldHandler;

    @Override
    public void attachListener(JavaScriptObject handler) {
      oldHandler = getHashChangeHandler();
      setListener(handler);
    }

    @Override
    public void dispose() {
      setListener(oldHandler);
    }

    @Override
    public void maybeRunOtherListeners() {
      // run the listener we removed if any
      try {
        if (oldHandler != null) {
          executeJavaScriptFunction(oldHandler);
        }
      } catch (Exception e) {
        // we do not care about errors from another module or JS
      }
    }

    private native void setListener(JavaScriptObject handler) /*-{
      $wnd.onhashchange = handler;
    }-*/;

    private native JavaScriptObject getHashChangeHandler() /*-{
      return $wnd.onhashchange;
    }-*/;

    private native void executeJavaScriptFunction(JavaScriptObject f) /*-{
      f();
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
    impl = GWT.create(HistoryImpl.class);
    historyEventSource = new HistoryEventSource();
    token = getDecodedHash();
    impl.attachListener(getHistoryChangeHandler());
    Impl.scheduleDispose(new Disposable() {
      @Override
      public void dispose() {
        impl.dispose();
      }
    });
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
      nativeUpdate(updateToken);
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

  /**
   * The standard updateHash implementation assigns to location.hash() with an
   * encoded history token.
   */
  private static native void nativeUpdate(String historyToken) /*-{
    $wnd.location.hash = historyToken;
  }-*/;

  // this is called from JS when the native onhashchange occurs
  private static void onHashChanged() {
    String hashToken = getDecodedHash();
    if (!hashToken.equals(getToken())) {
      token = hashToken;
      historyEventSource.fireValueChangedEvent(hashToken);
    }
    impl.maybeRunOtherListeners();
  }
}
