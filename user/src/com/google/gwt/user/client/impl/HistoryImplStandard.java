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

/**
 * Standard history implementation, currently used only on Opera browsers.
 */
class HistoryImplStandard extends HistoryImpl {

  @Override
  public native boolean init() /*-{
    var token = '';

    // Get the initial token from the url's hash component.
    var hash = $wnd.location.hash;
    if (hash.length > 0) {
      token = this.@com.google.gwt.user.client.impl.HistoryImpl::decodeFragment(Ljava/lang/String;)(hash.substring(1));
    }

    @com.google.gwt.user.client.impl.HistoryImpl::setToken(Ljava/lang/String;)(token);

    // Create the timer that checks the browser's url hash every 1/4 s.
    var historyImpl = this;
    $wnd.__checkHistory = function() {
      $wnd.setTimeout($wnd.__checkHistory, 250);

      var token = '', hash = $wnd.location.hash;
      if (hash.length > 0) {
        token = historyImpl.@com.google.gwt.user.client.impl.HistoryImpl::decodeFragment(Ljava/lang/String;)(hash.substring(1));
      }

      historyImpl.@com.google.gwt.user.client.impl.HistoryImpl::newItemOnEvent(Ljava/lang/String;)(token);
    };

    // Kick off the timer.
    $wnd.__checkHistory();
    return true;
  }-*/;

  /**
   * The standard updateHash implementation assigns to location.hash() with an
   * encoded history token.
   */
  protected native void nativeUpdate(String historyToken) /*-{
    $wnd.location.hash = this.@com.google.gwt.user.client.impl.HistoryImpl::encodeFragment(Ljava/lang/String;)(historyToken);
  }-*/;

  @Override
  protected void nativeUpdateOnEvent(String historyToken) {
    // Do nothing, the hash is already updated.
  }
}
