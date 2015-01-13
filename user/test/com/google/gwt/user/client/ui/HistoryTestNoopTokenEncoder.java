/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.user.client.ui;

import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;

/**
 * Tests for the history system without encoding of history tokens.
 */
public class HistoryTestNoopTokenEncoder extends HistoryTest {

  private static native boolean isFirefox() /*-{
    var ua = navigator.userAgent.toLowerCase();
    var docMode = $doc.documentMode;
    return (ua.indexOf('gecko') != -1 && typeof(docMode) == 'undefined');
  }-*/;

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.HistoryTestNoopTokenEncoder";
  }

  @Override
  protected String getHistoryToken2() {
    return isFirefox() ? "token2" : "token 2";
  }

  @Override
  protected String getHistoryToken2_encoded() {
    return isFirefox() ? "token2" : "token%202";
  }

  @Override
  public void testTokenEscaping() {
    if (isFirefox()) {
      return; // encoding is broken for Firefox.
    }
    super.testTokenEscaping();
  }

  @DoNotRunWith(Platform.HtmlUnitUnknown)
  @Override
  public void testEmptyHistoryTokens() {
    super.testEmptyHistoryTokens();
  }
}
