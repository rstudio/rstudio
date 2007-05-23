/*
 * Copyright 2007 Google Inc.
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

import com.google.gwt.user.client.Element;

/**
 * An IFRAME implementation of
 * {@link com.google.gwt.user.client.impl.HistoryImpl}.
 */
abstract class HistoryImplFrame extends HistoryImpl {

  private static native Element findHistoryFrame() /*-{
    var historyFrame = $doc.getElementById('__gwt_historyFrame');
    return historyFrame || null;
  }-*/;

  private static native Element getTokenElement(Element historyFrame) /*-{
    // Initialize the history iframe.  If '__gwt_historyToken' already exists, then
    // we're probably backing into the app, so _don't_ set the iframe's location.
    var tokenElement = null;
    if (historyFrame.contentWindow) {
      var doc = historyFrame.contentWindow.document;
      tokenElement = doc.getElementById('__gwt_historyToken') || null;
    }
    return tokenElement;
  }-*/;

  private Element historyFrame;

  public boolean init() {
    historyFrame = findHistoryFrame();
    if (historyFrame == null) {
      return false;
    }

    initHistoryToken();

    // Initialize the history iframe. If a token element already exists, then
    // we're probably backing into the app, so _don't_ create a new item.
    Element tokenElement = getTokenElement(historyFrame);
    if (tokenElement != null) {
      setToken(getTokenElementContent(tokenElement));
    } else {
      newItemImpl(historyFrame, getToken(), true);
    }

    injectGlobalHandler();
    return true;
  }

  public void newItem(String historyToken) {
    newItemImpl(historyFrame, historyToken, false);
  }

  protected abstract String getTokenElementContent(Element tokenElement);

  protected abstract void initHistoryToken();

  protected abstract void injectGlobalHandler();

  protected abstract void newItemImpl(Element historyFrame,
      String historyToken, boolean forceAdd);
}
