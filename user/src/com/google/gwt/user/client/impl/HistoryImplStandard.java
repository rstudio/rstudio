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
 * Standard history implementation, currently used only on Mozilla browsers.
 */
class HistoryImplStandard extends HistoryImpl {

  public native String getToken() /*-{
    return $wnd.__historyToken;
  }-*/;

  public native boolean init() /*-{
    $wnd.__historyToken = '';

    // Get the initial token from the url's hash component.
    var hash = $wnd.location.hash;
    if (hash.length > 0)
      $wnd.__historyToken = decodeURIComponent(hash.substring(1));

    // Create the timer that checks the browser's url hash every 1/4 s.
    $wnd.__checkHistory = function() {
      var token = '', hash = $wnd.location.hash;
      if (hash.length > 0)
        token = decodeURIComponent(hash.substring(1));

      if (token != $wnd.__historyToken) {
        $wnd.__historyToken = token;
        @com.google.gwt.user.client.impl.HistoryImpl::onHistoryChanged(Ljava/lang/String;)(token);
      }

      $wnd.setTimeout('__checkHistory()', 250);
    };

    // Kick off the timer.
    $wnd.__checkHistory();

    return true;
  }-*/;

  public native void newItem(String historyToken) /*-{
    if(historyToken == "" || historyToken == null){
      historyToken = "#";
    }
    $wnd.location.hash = encodeURIComponent(historyToken);
   }-*/;
}
