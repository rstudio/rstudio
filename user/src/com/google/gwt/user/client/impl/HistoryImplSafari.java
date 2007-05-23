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
 * Safari implementation of
 * {@link com.google.gwt.user.client.impl.HistoryImplFrame}.
 */
class HistoryImplSafari extends HistoryImplFrame {

  protected native String getTokenElementContent(Element tokenElement) /*-{
    return tokenElement.value;
  }-*/;

  protected native void initHistoryToken() /*-{
    // Get the initial token from the url's hash component.
    var hash = $wnd.location.hash;
    if (hash.length > 0)
      $wnd.__gwt_historyToken = decodeURIComponent(hash.substring(1));
    else
      $wnd.__gwt_historyToken = '';
  }-*/;

  protected native void injectGlobalHandler() /*-{
    $wnd.__gwt_onHistoryLoad = function(token) {
      token = decodeURIComponent(token);

      // Change the URL and notify the application that its history frame
      // is changing.
      if (token != $wnd.__gwt_historyToken) {
        $wnd.__gwt_historyToken = token;

// TODO(jgw): can't actually do this on Safari without screwing everything up.
//        $wnd.location.hash = encodeURIComponent(token);

        // Fire the event.
        @com.google.gwt.user.client.impl.HistoryImpl::onHistoryChanged(Ljava/lang/String;)(token);
      }
    };
  }-*/;

  protected native void newItemImpl(Element historyFrame, String historyToken,
      boolean forceAdd) /*-{
    // Ignore 'forceAdd'. It's only needed on IE.

    // The history frame's contentWindow can be null when backing into an
    // application. For some reason, the history frame will finish loading
    // *after* the application itself, which is a bit of a race condition.
    if (historyFrame.contentWindow) {
      historyToken = historyToken || "";

      var base = @com.google.gwt.core.client.GWT::getModuleBaseURL()();
      historyFrame.contentWindow.location.href = base + 'history.html?' + historyToken;
    }
  }-*/;
}
