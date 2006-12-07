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
 * Internet Explorer 6 implementation of
 * {@link com.google.gwt.user.client.impl.HistoryImpl}.
 */
class HistoryImplIE6 extends HistoryImpl {

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
      historyFrame.src = 'history.html?' + $wnd.__historyToken;

    // Expose the '__onHistoryChanged' function, which will be called by
    // the history frame when it loads.
    $wnd.__onHistoryChanged = function(token) {
      // Change the URL and notify the application that its history frame
      // is changing.  Note that setting location.hash does _not_ add a history
      // frame on IE, so we don't have to do a 'location.replace()'.
      if (token != $wnd.__historyToken) {
        $wnd.__historyToken = token;
        $wnd.location.hash = encodeURIComponent(token);
        @com.google.gwt.user.client.impl.HistoryImpl::onHistoryChanged(Ljava/lang/String;)(token);
      }
    };

    // This is the URL check timer.  It detects when an unexpected change
    // occurs in the document's URL (e.g. when the user enters one manually
    // or selects a 'favorite', but only the #hash part changes).  When this
    // occurs, we _must_ reload the page.  This is because IE has a really
    // nasty bug that totally mangles its history stack and causes the location
    // bar in the UI to stop working under these circumstances.
    var urlChecker = function() {
      var hash = $wnd.location.hash;
      if (hash.length > 0) {
        var token = decodeURIComponent(hash.substring(1));
        if ($wnd.__historyToken && (token != $wnd.__historyToken))
          $wnd.location.reload();
      }
      $wnd.setTimeout(urlChecker, 250);
    };
    urlChecker();

    return true;
  }-*/;

  public native void newItem(String historyToken) /*-{
    var iframe = $doc.getElementById('__gwt_historyFrame');
    iframe.contentWindow.location.href = 'history.html?' + historyToken;
  }-*/;
}
