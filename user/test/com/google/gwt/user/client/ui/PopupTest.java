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
package com.google.gwt.user.client.ui;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Window;

/**
 * TODO: document me.
 */
public class PopupTest extends GWTTestCase {

  /**
   * Expose otherwise private or protected methods.
   */
  private class TestablePopupPanel extends PopupPanel {
    @Override
    public Element getContainerElement() {
      return super.getContainerElement();
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  /**
   * Test the basic accessors.
   */
  public void testAccessors() {
    PopupPanel popup = new PopupPanel();
    
    // Animation enabled
    assertTrue(popup.isAnimationEnabled());
    popup.setAnimationEnabled(false);
    assertFalse(popup.isAnimationEnabled());
  }

  public void testPopup() {
    // Get rid of window margins so we can test absolute position.
    Window.setMargin("0px");

    PopupPanel popup = new PopupPanel();
    popup.setAnimationEnabled(false);
    Label lbl = new Label("foo");

    // Make sure that setting the popup's size & position works _before_
    // setting its widget.
    popup.setSize("384px", "128px");
    popup.setPopupPosition(128, 64);
    popup.setWidget(lbl);
    popup.show();

    // DecoratorPanel adds width and height because it wraps the content in a
    // 3x3 table.
    assertTrue(popup.getOffsetWidth() >= 384);
    assertTrue(popup.getOffsetHeight() >= 128);
    assertEquals(128, popup.getPopupLeft());
    assertEquals(64, popup.getPopupTop());

    // Make sure that the popup returns to the correct position
    // after hiding and showing it.
    popup.hide();
    popup.show();
    assertEquals(128, popup.getPopupLeft());
    assertEquals(64, popup.getPopupTop());

    // Make sure that setting the popup's size & position works _after_
    // setting its widget (and that clearing its size properly resizes it to
    // its widget's size).
    popup.setSize("", "");
    popup.setPopupPosition(16, 16);

    // DecoratorPanel adds width and height because it wraps the content in a
    // 3x3 table.
    assertTrue(popup.getOffsetWidth() >= lbl.getOffsetWidth());
    assertTrue(popup.getOffsetWidth() >= lbl.getOffsetHeight());
    assertEquals(16, popup.getAbsoluteLeft());
    assertEquals(16, popup.getAbsoluteTop());

    // Ensure that hiding the popup fires the appropriate events.
    delayTestFinish(1000);
    popup.addPopupListener(new PopupListener() {
      public void onPopupClosed(PopupPanel sender, boolean autoClosed) {
        finishTest();
      }
    });
    popup.hide();
  }
  
  public void testSeparateContainers() {
    TestablePopupPanel p1 = new TestablePopupPanel();
    TestablePopupPanel p2 = new TestablePopupPanel();
    assertTrue(p1.getContainerElement() != null);
    assertTrue(p2.getContainerElement() != null);
    assertFalse(p1.getContainerElement() == p2.getContainerElement());
  }
}
