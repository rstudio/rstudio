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
import com.google.gwt.user.client.DOM;

/**
 * Tests {@link CaptionPanel}.
 */
public class CaptionPanelTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }
  
  /**
   * Test the basic setting and getting of the caption.
   */
  public void testSetCaption() {
    // Null caption
    CaptionPanel panel1 = new CaptionPanel(null);
    assertNull(panel1.getCaption());
    assertNull(DOM.getFirstChild(panel1.getElement()));
    panel1.setCaption("");
    assertEquals("", panel1.getCaption());
    assertNull(DOM.getFirstChild(panel1.getElement()));
    
    // Actual caption
    CaptionPanel panel2 = new CaptionPanel("my panel2");
    assertEquals("my panel2", panel2.getCaption());
    assertNotNull(DOM.getFirstChild(panel2.getElement()));
    panel2.setCaption("my cool panel 2");
    assertEquals("my cool panel 2", panel2.getCaption());
    assertNotNull(DOM.getFirstChild(panel2.getElement()));
  }
  
  /**
   * Test the setting and removal of a widget.
   */
  public void testSetWidget() {
    // No Widget constructor
    CaptionPanel panel1 = new CaptionPanel("no widget");
    assertNull(panel1.getWidget());
    
    // Widget constructor
    HTML widget2 = new HTML("widget 2");
    CaptionPanel panel2 = new CaptionPanel("has widget", widget2);
    assertEquals(widget2, panel2.getWidget());
    
    // Set widget
    HTML widget3 = new HTML("widget 3");
    CaptionPanel panel3 = new CaptionPanel("set widget", null);
    assertNull(panel3.getWidget());
    panel3.setWidget(widget3);
    assertEquals(widget3, panel3.getWidget());
    panel3.remove(widget3);
    assertNull(panel3.getWidget());
  }
}
