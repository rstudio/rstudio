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
package com.google.gwt.user.client.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.user.client.HistoryListener;

import java.util.ArrayList;

/**
 * Native implementation associated with
 * {@link com.google.gwt.user.client.History}.
 * 
 * User classes should not use this class directly.
 */
public abstract class HistoryImpl {

  private static ArrayList<HistoryListener> historyListeners = new ArrayList<HistoryListener>();

  /**
   * Adds a listener to be informed of changes to the browser's history stack.
   * 
   * @param listener the listener to be added
   */
  public static void addHistoryListener(HistoryListener listener) {
    historyListeners.add(listener);
  }

  public static native String getToken() /*-{
    return $wnd.__gwt_historyToken || "";
  }-*/;

  /**
   * Removes a history listener.
   * 
   * @param listener the listener to be removed
   */
  public static void removeHistoryListener(HistoryListener listener) {
    historyListeners.remove(listener);
  }

  protected static native void setToken(String token) /*-{
    $wnd.__gwt_historyToken = token;
  }-*/;

  private static void fireHistoryChanged(String historyToken) {
    UncaughtExceptionHandler handler = GWT.getUncaughtExceptionHandler();
    if (handler != null) {
      fireHistoryChangedAndCatch(historyToken, handler);
    } else {
      fireHistoryChangedImpl(historyToken);
    }
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
    // TODO: replace this copy when a more general solution to event handlers
    // wanting to remove themselves from the listener list is implemented.

    // This is necessary to avoid a CurrentModificationException in hosted
    // mode, as the listeners may try to remove themselves from the list while
    // it is being iterated, such as in HistoryTest.
    HistoryListener[] listenersToInvoke = historyListeners.toArray(new HistoryListener[historyListeners.size()]);
    for (HistoryListener listener : listenersToInvoke) {
      listener.onHistoryChanged(historyToken);
    }
  }

  public abstract boolean init();

  public final void newItem(String historyToken, boolean issueEvent) {
    historyToken = (historyToken == null) ? "" : historyToken;
    if (!historyToken.equals(getToken())) {
      setToken(historyToken);
      nativeUpdate(historyToken);
      if (issueEvent) {
        fireHistoryChangedImpl(historyToken);
      }
    }
  }

  public final void newItemOnEvent(String historyToken) {
    historyToken = (historyToken == null) ? "" : historyToken;
    if (!historyToken.equals(getToken())) {
      setToken(historyToken);
      nativeUpdateOnEvent(historyToken);
      fireHistoryChanged(historyToken);
    }
  }

  protected native String decodeFragment(String encodedFragment) /*-{
    // decodeURI() does *not* decode the '#' character.
    return decodeURI(encodedFragment.replace("%23", "#"));
  }-*/;

  protected native String encodeFragment(String fragment) /*-{
    // encodeURI() does *not* encode the '#' character.
    return encodeURI(fragment).replace("#", "%23");
  }-*/;

  protected abstract void nativeUpdate(String historyToken);

  protected abstract void nativeUpdateOnEvent(String historyToken);
}
