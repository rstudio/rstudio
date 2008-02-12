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
 * Tests the HTMLPanel widget.
 */
public class HTMLPanelTest extends GWTTestCase {

  static class Adder implements HasWidgetsTester.WidgetAdder {
    public void addChild(HasWidgets container, Widget child) {
      ((HTMLPanel) container).add(child, "w00t");
    }
  }

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  /**
   * Tests {@link HTMLPanel#add(Widget, String)}.
   */
  public void testAddToElementWithId() {
    Label labelA = new Label("A"), labelB = new Label("B");
    HTMLPanel p = new HTMLPanel("<div id=\"a\"></div><div id=\"b\"></div>");
    p.add(labelA, "a");
    p.add(labelB, "b");
    // Ensure that both Label's have the correct parent.
    assertEquals("a", DOM.getElementAttribute(
        DOM.getParent(labelA.getElement()), "id"));
    assertEquals("b", DOM.getElementAttribute(
        DOM.getParent(labelB.getElement()), "id"));
  }

  public void testAttachDetachOrder() {
    HTMLPanel p = new HTMLPanel("<div id='w00t'></div>");
    HasWidgetsTester.testAll(p, new Adder());
  }
}
