/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.junit.client;


import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.dom.client.ButtonElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;

/**
 * A base class for tests GWTTestCase.
 */
abstract class GWTTestCaseTestBase extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.junit.JUnit";
  }

  protected void failNow(String failMsg) {
    fail("Expected failure (" + failMsg + ")");
  }

  protected void failViaUncaughtException(final String failMsg) {
    ButtonElement btn = Document.get().createPushButtonElement();
    Document.get().getBody().appendChild(btn);
    Event.sinkEvents(btn, Event.ONCLICK);

    EventListener listener = new EventListener() {
      public void onBrowserEvent(Event event) {
        failNow(failMsg);
      }
    };

    DOM.setEventListener(btn.<com.google.gwt.user.client.Element>cast(), listener);
    try {
      btn.click();
    } catch (JavaScriptException ignored) {
      // In HtmlUnit exception thrown by event handlers are propagated to #click() call.
    }
  }
}
