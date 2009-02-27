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

import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the FlowPanel widget.
 */
public class FlowPanelTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testAttachDetachOrder() {
    HasWidgetsTester.testAll(new FlowPanel());
  }

  public void testClear() {
    int size = 10;
    FlowPanel target;
    List<Widget> children = new ArrayList<Widget>();

    target = new FlowPanel();

    for (int i = 0; i < size; i++) {
      Widget w = new Label("widget-" + i);
      target.add(w);
      children.add(w);
    }

    RootPanel.get().add(target);
    for (Widget child : target) {
      assertNotNull(child.getElement().getPropertyString("__listener"));
    }
    assertEquals(10, target.getWidgetCount());
    target.clear();
    assertEquals(0, target.getWidgetCount());

    for (Widget child : target) {
      assertNull(child.getElement().getPropertyString("__listener") == null);
    }
  }

  public void testClearWithNestedChildren() {
    FlowPanel target = new FlowPanel();
    FlowPanel child0 = new FlowPanel();
    HTML child1 = new HTML();
    target.add(child0);
    child0.add(child1);

    Element child0Elem = child0.getElement();
    Element child1Elem = child1.getElement();

    assertEquals(child0Elem, target.getElement().getFirstChild());
    assertEquals(child1Elem, child0Elem.getFirstChild());

    target.clear();

    assertEquals(child1Elem, child0Elem.getFirstChildElement());
  }
}
