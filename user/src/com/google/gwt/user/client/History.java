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

  /**
   * HistoryTokenEncoder is responsible for encoding and decoding history token,
   * thus ensuring that tokens are safe to use in the browsers URL.
   */
  private static class HistoryTokenEncoder {
    public native String encode(String toEncode) /*-{
      // encodeURI() does *not* encode the '#' character.
      return $wnd.encodeURI(toEncode).replace("#", "%23");
    }-*/;

    public native String decode(String toDecode) /*-{
      return $wnd.decodeURI(toDecode.replace("%23", "#"));
    }-*/;
  }

  /**
   * NoopHistoryTokenEncoder does not perform any encoding.
   */
  // Used from rebinding
  @SuppressWarnings("unused")
  private static class NoopHistoryTokenEncoder extends HistoryTokenEncoder {
    @Override
    public String encode(String toEncode) {
      return toEncode;
    }

    @Override
    public String decode(String toDecode) {
      return toDecode;
    }
  }

  /**
   * History implementation using hash tokens.
   * <p>This is the default implementation for all browsers except IE8.
   */
  private static class HistoryImpl {

    public HistoryImpl() {
      attachListener();
    }

    protected native void attachListener() /*-{
      // We explicitly use the third parameter for capture, since Firefox before version 6
      // throws an exception if the parameter is missing.
      // See: https://developer.mozilla.org/es/docs/DOM/elemento.addEventListener#Gecko_notes
      var handler = $entry(@History::onHashChanged());
      $wnd.addEventListener('hashchange', handler, false);
    }-*/;

    public native void newToken(String historyToken) /*-{
      $wnd.location.hash = historyToken;
    }-*/;

    public void replaceToken(String historyToken) {
      Window.Location.replace("#" + historyToken);
    }
  }

  /**
   * History implementation for IE8 using onhashchange.
   */
  @SuppressWarnings("unused")
  private static class HistoryImplIE8 extends HistoryImpl {
    @Override
    protected native void attachListener() /*-{
      var handler = $entry(@History::onHashChanged());
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

  private static HistoryImpl impl = GWT.create(HistoryImpl.class);
  private static HistoryEventSource historyEventSource = new HistoryEventSource();
  private static HistoryTokenEncoder tokenEncoder = GWT.create(HistoryTokenEncoder.class);
  private static String token = getDecodedHash();

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
  public static String encodeHistoryToken(String historyToken) {
    return tokenEncoder.encode(historyToken);
  }

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
   * <p>Note: This method has problems. The URL is updated with window.location.replace,
   * this unfortunately has side effects when using the deprecated iframe linker
   * (ie. "std" linker). Make sure you are using the cross site iframe linker when using
   * this method in your code.
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
   * <p>Note: This method has problems. The URL is updated with window.location.replace,
   * this unfortunately has side effects when using the deprecated iframe linker
   * (ie. "std" linker). Make sure you are using the cross site iframe linker when using
   * this method in your code.
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

  private static String getDecodedHash() {
    String hashToken = Window.Location.getHash();
    if (hashToken == null || hashToken.isEmpty()) {
      return "";
    }
    return tokenEncoder.decode(hashToken.substring(1));
  }

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
