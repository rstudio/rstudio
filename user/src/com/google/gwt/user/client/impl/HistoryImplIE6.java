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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * Internet Explorer 6 implementation of
 * {@link com.google.gwt.user.client.impl.HistoryImplFrame}.
 */
class HistoryImplIE6 extends HistoryImplFrame {
  
  /**
   * Sanitizes an untrusted string to be used in an HTML context. NOTE: This
   * method of escaping strings should only be used on Internet Explorer.
   * 
   * @param maybeHtml untrusted string that may contain html
   * @return sanitized string
   */
  private static String escapeHtml(String maybeHtml) {
    final Element div = DOM.createDiv();
    DOM.setInnerText(div, maybeHtml);
    return DOM.getInnerHTML(div);
  }
  
  private static native void initUrlCheckTimer() /*-{
    // This is the URL check timer.  It detects when an unexpected change
    // occurs in the document's URL (e.g. when the user enters one manually
    // or selects a 'favorite', but only the #hash part changes).  When this
    // occurs, we _must_ reload the page.  This is because IE has a really
    // nasty bug that totally mangles its history stack and causes the location
    // bar in the UI to stop working under these circumstances.
    var urlChecker = function() {
      var hash = $wnd.location.hash;
      if (hash.length > 0) {
        var token = '';
        try {
          token = decodeURIComponent(hash.substring(1));
        } catch (e) {
          // If there's a bad hash, always reload. This could only happen if
          // if someone entered or linked to a bad url.
          $wnd.location.reload();
        }

        if ($wnd.__gwt_historyToken && (token != $wnd.__gwt_historyToken)) {
          $wnd.location.reload();
        }
      }
      $wnd.setTimeout(urlChecker, 250);
    };
    urlChecker();
  }-*/;

  public boolean init() {
    if (!super.init()) {
      return false;
    }
    initUrlCheckTimer();
    return true;
  }

  protected native String getTokenElementContent(Element tokenElement) /*-{
    return tokenElement.innerText;
  }-*/;

  protected native void initHistoryToken() /*-{
    // Get the initial token from the url's hash component.
    var hash = $wnd.location.hash;
    if (hash.length > 0) {
      try {
        $wnd.__gwt_historyToken = decodeURIComponent(hash.substring(1));
      } catch (e) {
        // Clear the bad hash and __gwt_historyToken
        // (this can't have been a valid token).
        $wnd.location.hash = '';
        $wnd.__gwt_historyToken = '';
      }
      return;
    }

    // There was no hash. Just start off with an empty token.
    $wnd.__gwt_historyToken = '';
  }-*/;

  protected native void injectGlobalHandler() /*-{
    $wnd.__gwt_onHistoryLoad = function(token) {
      // Change the URL and notify the application that its history frame
      // is changing.
      if (token != $wnd.__gwt_historyToken) {
        $wnd.__gwt_historyToken = token;
        $wnd.location.hash = encodeURIComponent(token);
        @com.google.gwt.user.client.impl.HistoryImpl::onHistoryChanged(Ljava/lang/String;)(token);
      }
    };
  }-*/;

  protected native void newItemImpl(Element historyFrame, String historyToken, boolean forceAdd) /*-{
    historyToken = @com.google.gwt.user.client.impl.HistoryImplIE6::escapeHtml(Ljava/lang/String;)(historyToken || "");
    if (forceAdd || ($wnd.__gwt_historyToken != historyToken)) {
      var doc = historyFrame.contentWindow.document;
      doc.open();
      doc.write('<html><body onload="if(parent.__gwt_onHistoryLoad)parent.__gwt_onHistoryLoad(__gwt_historyToken.innerText)"><div id="__gwt_historyToken">' + historyToken + '</div></body></html>');
      doc.close();
    }
  }-*/;
}
