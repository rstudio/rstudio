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
 * {@link com.google.gwt.user.client.impl.HistoryImplTimer}.
 * 
 * This implementation works on both Safari 2 and 3, by detecting the version
 * and reverting to a stub implementation for Safari 2.
 */
class HistoryImplSafari extends HistoryImplTimer {

  @Override
  protected native void nativeUpdate(String historyToken) /*-{
    // Safari gets into a weird state (issue 2905) when setting the hash
    // component of the url to an empty string, but works fine as long as you
    // at least add a '#' to the end of the url. So we get around this by
    // recreating the url, rather than just setting location.hash.
    $wnd.location = $wnd.location.href.split('#')[0] + '#' +
      this.@com.google.gwt.user.client.impl.HistoryImpl::encodeFragment(Ljava/lang/String;)(historyToken);
  }-*/;
}
