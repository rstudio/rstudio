// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.user.client.ui;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Window;

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
