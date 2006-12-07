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
package com.google.gwt.user.client.impl;

/**
 * Safari implementation of {@link com.google.gwt.user.client.impl.HistoryImpl}.
 */
class HistoryImplSafari extends HistoryImpl {

  public native String getToken() /*-{
    return $wnd.__historyToken;
  }-*/;

  public native boolean init() /*-{
    // Check for existence of the history frame.
    var historyFrame = $doc.getElementById('__gwt_historyFrame');
    if (!historyFrame)
      return false;

    // Get the initial token from the url's hash component.
    var hash = $wnd.location.hash;
    if (hash.length > 0)
      $wnd.__historyToken = decodeURIComponent(hash.substring(1));
    else
      $wnd.__historyToken = '';

    // Initialize the history iframe.  If '__historyToken' already exists, then
    // we're probably backing into the app, so _don't_ set the iframe's location.
    var tokenElement = null;
    if (historyFrame.contentWindow) {
      var doc = historyFrame.contentWindow.document;
      tokenElement = doc ? doc.getElementById('__historyToken') : null;
    }

    if (tokenElement)
      $wnd.__historyToken = tokenElement.value;
    else
      historyFrame.src = 'history.html?' + encodeURIComponent($wnd.__historyToken);

    // Expose the '__onHistoryChanged' function, which will be called by
    // the history frame when it loads.
    $wnd.__onHistoryChanged = function(token) {
      // Change the URL and notify the application that its history frame
      // is changing.
      if (token != $wnd.__historyToken) {
        $wnd.__historyToken = token;

        // TODO(jgw): fix the bookmark update, if possible.  The following code
        // screws up the browser by (a) making it pretend that it's loading the
        // page indefinitely, and (b) causing all text to disappear (!)
//        var base = $wnd.location.href;
//        var hashIdx = base.indexOf('#');
//        if (hashIdx != -1)
//          base = base.substring(0, hashIdx);
//        $wnd.location.replace(base + '#' + token);

        @com.google.gwt.user.client.impl.HistoryImpl::onHistoryChanged(Ljava/lang/String;)(token);
      }
    };

    return true;
  }-*/;

  public native void newItem(String historyToken) /*-{
    var iframe = $doc.getElementById('__gwt_historyFrame');
    iframe.contentWindow.location.href = 'history.html?' + historyToken;
  }-*/;
}
