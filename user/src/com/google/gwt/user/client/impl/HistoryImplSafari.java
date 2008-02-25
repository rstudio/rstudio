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
 * Safari implementation of
 * {@link com.google.gwt.user.client.impl.HistoryImplStandard}.
 * 
 * This implementation works on both Safari 2 and 3, by detecting the version
 * and reverting to a stub implementation for Safari 2.
 */
class HistoryImplSafari extends HistoryImplStandard {

  private static boolean isOldSafari = detectOldSafari();

  private static native boolean detectOldSafari() /*-{
    var exp = / AppleWebKit\/([\d]+)/;
    var result = exp.exec(navigator.userAgent);
    if (result) {
      // The standard history implementation works fine on WebKit >= 522
      // (Safari 3 beta).
      if (parseInt(result[1]) >= 522) {
        return false;
      }
    }

    // The standard history implementation works just fine on the iPhone, which
    // unfortunately reports itself as WebKit/420+.
    if (navigator.userAgent.indexOf('iPhone') != -1) {
      return false;
    }

    return true;
  }-*/;

  @Override
  public boolean init() {
    if (isOldSafari) {
      initImpl();
      return true;
    }

    return super.init();
  }

  @Override
  public void newItem(String historyToken) {
    if (isOldSafari) {
      newItemImpl(historyToken);
      return;
    }

    super.newItem(historyToken);
  }

  protected native String decode(String historyToken) /*-{
    try {
      // Unlike most other browsers, Safari does not unescape location.hash.
      return decodeURIComponent(historyToken);
    } catch (e) {
      // This is very unlikely to be malformed, since it comes directly from
      // location.hash.
      return "";
    }
  }-*/;

  private native void initImpl() /*-{
    $wnd.__gwt_historyToken = '';

    // Get the initial token from the url's hash component.
    var hash = $wnd.location.hash;
    if (hash.length > 0) {
      $wnd.__gwt_historyToken =
        this.@com.google.gwt.user.client.impl.HistoryImplStandard::decode(Ljava/lang/String;)(hash.substring(1));
    }

    @com.google.gwt.user.client.impl.HistoryImpl::onHistoryChanged(Ljava/lang/String;)($wnd.__gwt_historyToken);
  }-*/;

  private native void newItemImpl(String historyToken) /*-{
    // Use a bizarre meta refresh trick to update the url's hash, without
    // creating a history entry.
    var meta = $doc.createElement('meta');
    meta.setAttribute('http-equiv','refresh');

    var newUrl = $wnd.location.href.split('#')[0] + '#' + encodeURIComponent(historyToken);
    meta.setAttribute('content','0.01;url=' + newUrl);

    $doc.body.appendChild(meta);
    window.setTimeout(function() {
      $doc.body.removeChild(meta);
    }, 1);

    // Update the global history token and fire the history event.
    $wnd.__gwt_historyToken = historyToken;
    @com.google.gwt.user.client.impl.HistoryImpl::onHistoryChanged(Ljava/lang/String;)($wnd.__gwt_historyToken);
  }-*/;
}
