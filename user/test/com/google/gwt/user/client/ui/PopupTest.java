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
package com.google.gwt.user.client.ui;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Window;

/**
 * TODO: document me.
 */
public class PopupTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testPopup() {
    // Get rid of window margins so we can test absolute position.
    Window.setMargin("0px");

    PopupPanel popup = new PopupPanel();
    Label lbl = new Label("foo");

    // Make sure that setting the popup's size & position works _before_
    // setting its widget.
    popup.setSize("384px", "128px");
    popup.setPopupPosition(128, 64);
    popup.setWidget(lbl);
    popup.show();

    assertEquals(popup.getOffsetWidth(), 384);
    assertEquals(popup.getOffsetHeight(), 128);
    assertEquals(popup.getPopupLeft(), 128);
    assertEquals(popup.getPopupTop(), 64);

    // Make sure that setting the popup's size & position works _after_
    // setting its widget (and that clearing its size properly resizes it to
    // its widget's size).
    popup.setSize("", "");
    popup.setPopupPosition(16, 16);

    assertEquals(popup.getOffsetWidth(), lbl.getOffsetWidth());
    assertEquals(popup.getOffsetHeight(), lbl.getOffsetHeight());
    assertEquals(popup.getAbsoluteLeft(), 16);
    assertEquals(popup.getAbsoluteTop(), 16);

    // Ensure that hiding the popup fires the appropriate events.
    delayTestFinish(1000);
    popup.addPopupListener(new PopupListener() {
      public void onPopupClosed(PopupPanel sender, boolean autoClosed) {
        finishTest();
      }
    });
    popup.hide();
  }
}
