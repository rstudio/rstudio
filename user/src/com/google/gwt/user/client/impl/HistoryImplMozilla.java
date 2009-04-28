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
 * History implementation for Mozilla-based browsers.
 */
class HistoryImplMozilla extends HistoryImplTimer {

  @Override
  protected String decodeFragment(String encodedFragment) {
    // Mozilla browsers pre-decode the result of location.hash, so there's no
    // need to decode it again (which would in fact be incorrect).
    return encodedFragment;
  }

  /**
   * When the historyToken is blank or null, we are not able to set
   * $wnd.location.hash to the empty string, due to a bug in Mozilla. Every time
   * $wnd.location.hash is set to the empty string, one of the characters at the
   * end of the URL stored in $wnd.location is 'eaten'. To get around this bug,
   * we generate the module's URL, and we append a '#' character onto the end.
   * Without the '#' character at the end of the URL, Mozilla would reload the
   * page from the server.
   */
  @Override
  protected native void nativeUpdate(String historyToken) /*-{
    if (historyToken.length == 0) {
      var s = $wnd.location.href;
      // Pull off any hash.
      var i = s.indexOf('#');
      if (i != -1)
        s = s.substring(0, i);

      $wnd.location = s + '#';
    } else {
      $wnd.location.hash = this.@com.google.gwt.user.client.impl.HistoryImpl::encodeFragment(Ljava/lang/String;)(historyToken);
    }
  }-*/;
}
