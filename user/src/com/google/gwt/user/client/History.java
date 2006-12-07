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
import com.google.gwt.user.client.impl.HistoryImpl;

import java.util.Iterator;
import java.util.Vector;

/**
 * This class allows you to interact with the browser's history stack. Each
 * "item" on the stack is represented by a single string, referred to as a
 * "token". You can create new history items (which have a token associated with
 * them when they are created), and you can programmatically force the current
 * history to move back or forward.
 * 
 * <p>
 * In order to receive notification of user-directed changes to the current
 * history item, implement the
 * {@link com.google.gwt.user.client.HistoryListener} interface and attach it
 * via {@link #addHistoryListener}.
 * </p>
 * 
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.HistoryExample}
 * </p>
 */
public class History {

  private static Vector historyListeners = new Vector();
  private static HistoryImpl impl;

  static {
    impl = (HistoryImpl) GWT.create(HistoryImpl.class);
    if (!impl.init()) {
      // Set impl to null as a flag to no-op future calls.
      impl = null;

      // Tell the user.
      GWT.log(
          "Unable to initialize the history subsystem; did you "
              + "include the history frame in your host page? Try "
              + "<iframe id='__gwt_historyFrame' style='width:0;height:0;border:0'>"
              + "</iframe>", null);
    }
  }

  /**
   * Adds a listener to be informed of changes to the browser's history stack.
   * 
   * @param listener the listener to be added
   */
  public static void addHistoryListener(HistoryListener listener) {
    historyListeners.add(listener);
  }

  /**
   * Programmatic equivalent to the user pressing the browser's 'back' button.
   */
  public static native void back() /*-{
    $wnd.history.back();
  }-*/;

  /**
   * Programmatic equivalent to the user pressing the browser's 'forward'
   * button.
   */
  public static native void forward() /*-{
    $wnd.history.forward();
  }-*/;

  /**
   * Gets the current history token. The listener will not receive an
   * onHistoryChanged() event for the initial token; requiring that an
   * application request the token explicitly on startup gives it an opportunity
   * to run different initialization code in the presence or absence of an
   * initial token.
   * 
   * @return the initial token, or the empty string if none is present.
   */
  public static String getToken() {
    return impl != null ? impl.getToken() : "";
  }

  /**
   * Adds a new browser history entry. In hosted mode, the 'back' and 'forward'
   * actions are accessible via the standard Alt-Left and Alt-Right keystrokes.
   * Calling this method will cause {@link #onHistoryChanged} to be called as
   * well.
   * 
   * @param historyToken the token to associate with the new history item
   */
  public static void newItem(String historyToken) {
    if (impl != null) {
      impl.newItem(historyToken);
    }
  }

  public static void onHistoryChanged(String historyToken) {
    UncaughtExceptionHandler handler = GWT.getUncaughtExceptionHandler();
    if (handler != null) {
      fireHistoryChangedAndCatch(historyToken, handler);
    } else {
      fireHistoryChangedImpl(historyToken);
    }
  }

  /**
   * Removes a history listener.
   * 
   * @param listener the listener to be removed
   */
  public static void removeHistoryListener(HistoryListener listener) {
    historyListeners.remove(listener);
  }

  private static void fireHistoryChangedAndCatch(String historyToken,
      UncaughtExceptionHandler handler) {
    try {
      fireHistoryChangedImpl(historyToken);
    } catch (Throwable e) {
      handler.onUncaughtException(e);
    }
  }

  private static void fireHistoryChangedImpl(String historyToken) {
    for (Iterator it = historyListeners.iterator(); it.hasNext();) {
      HistoryListener listener = (HistoryListener) it.next();
      listener.onHistoryChanged(historyToken);
    }
  }
}
