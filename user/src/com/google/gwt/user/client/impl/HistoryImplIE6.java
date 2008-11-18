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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * Internet Explorer 6 implementation HistoryImplFrame.
 */
class HistoryImplIE6 extends HistoryImplFrame {

  /**
   * Sanitizes an untrusted string to be used in an HTML context. NOTE: This
   * method of escaping strings should only be used on Internet Explorer.
   * 
   * @param maybeHtml untrusted string that may contain html
   * @return sanitized string
   */
  @SuppressWarnings("unused")
  private static String escapeHtml(String maybeHtml) {
    final Element div = DOM.createDiv();
    DOM.setInnerText(div, maybeHtml);
    return DOM.getInnerHTML(div);
  }

  /**
   * For IE6, reading from $wnd.location.hash drops part of the fragment if the
   * fragment contains a '?'. To avoid this bug, we use location.href instead.
   */
  @SuppressWarnings("unused")
  private static native String getLocationHash() /*-{
    var href = $wnd.location.href;
    var hashLoc = href.lastIndexOf("#");
    return (hashLoc > 0) ? href.substring(hashLoc) : "";
  }-*/;

  @Override
  public boolean init() {
    if (!super.init()) {
      return false;
    }
    initUrlCheckTimer();
    return true;
  }

  @Override
  protected native String getTokenElementContent(Element tokenElement) /*-{
    return tokenElement.innerText;
  }-*/;

  @Override
  protected native void initHistoryToken() /*-{
    // Assume an empty token.
    var token = '';
    // Get the initial token from the url's hash component.
    var hash = @com.google.gwt.user.client.impl.HistoryImplIE6::getLocationHash()();
    if (hash.length > 0) {
      try {
        token = this.@com.google.gwt.user.client.impl.HistoryImpl::decodeFragment(Ljava/lang/String;)(hash.substring(1));
      } catch (e) {
        // Clear the bad hash (this can't have been a valid token).
        $wnd.location.hash = '';
      }
    }
    @com.google.gwt.user.client.impl.HistoryImpl::setToken(Ljava/lang/String;)(token);
  }-*/;

  @Override
  protected native void injectGlobalHandler() /*-{
    var historyImplRef = this;

    $wnd.__gwt_onHistoryLoad = function(token) {
      historyImplRef.@com.google.gwt.user.client.impl.HistoryImpl::newItemOnEvent(Ljava/lang/String;)(token);
    };
  }-*/;

  @Override
  protected native void navigateFrame(String token) /*-{
    var escaped = @com.google.gwt.user.client.impl.HistoryImplIE6::escapeHtml(Ljava/lang/String;)(token);
    var doc = this.@com.google.gwt.user.client.impl.HistoryImplFrame::historyFrame.contentWindow.document;
    doc.open();
    doc.write('<html><body onload="if(parent.__gwt_onHistoryLoad)parent.__gwt_onHistoryLoad(__gwt_historyToken.innerText)"><div id="__gwt_historyToken">' + escaped + '</div></body></html>');
    doc.close();
  }-*/;

  @Override
  protected native void updateHash(String token) /*-{
    $wnd.location.hash = this.@com.google.gwt.user.client.impl.HistoryImpl::encodeFragment(Ljava/lang/String;)(token);
  }-*/;

  private native void initUrlCheckTimer() /*-{
    // This is the URL check timer.  It detects when an unexpected change
    // occurs in the document's URL (e.g. when the user enters one manually
    // or selects a 'favorite', but only the #hash part changes).  When this
    // occurs, we _must_ reload the page.  This is because IE has a really
    // nasty bug that totally mangles its history stack and causes the location
    // bar in the UI to stop working under these circumstances.
    var historyImplRef = this;
    var urlChecker = function() {
      $wnd.setTimeout(urlChecker, 250);
      var hash = @com.google.gwt.user.client.impl.HistoryImplIE6::getLocationHash()();
      if (hash.length > 0) {
        var token = '';
        try {
          token = historyImplRef.@com.google.gwt.user.client.impl.HistoryImpl::decodeFragment(Ljava/lang/String;)(hash.substring(1));
        } catch (e) {
          // If there's a bad hash, always reload. This could only happen if
          // if someone entered or linked to a bad url.
          $wnd.location.reload();
        }

        var historyToken = @com.google.gwt.user.client.impl.HistoryImpl::getToken()();
        if (historyToken && (token != historyToken)) {
          $wnd.location.reload();
        }
      }
    };
    urlChecker();
  }-*/;
}
