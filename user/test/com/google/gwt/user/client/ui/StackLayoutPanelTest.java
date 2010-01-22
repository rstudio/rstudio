/*
 * Copyright 2010 Google Inc.
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

import com.google.gwt.dom.client.Style.Unit;

/**
 * Tests for {@link StackLayoutPanel}.
 */
public class StackLayoutPanelTest extends WidgetTestBase {

  static class Adder implements HasWidgetsTester.WidgetAdder {
    public void addChild(HasWidgets container, Widget child) {
      ((StackLayoutPanel) container).add(child, new Label("Header"), 1);
    }
  }

  public void testAttachDetachOrder() {
    HasWidgetsTester.testAll(new StackLayoutPanel(Unit.EM), new Adder(), true);
  }

  public void testEmptyAdd() {
    // Issue 4414: Attaching an empty StackLayoutPanel caused an exception.
    StackLayoutPanel p = new StackLayoutPanel(Unit.EM);
    RootPanel.get().add(p);
  }

  public void testVisibleWidget() {
    StackLayoutPanel p = new StackLayoutPanel(Unit.EM);
    assertNull(p.getVisibleWidget());

    Label l0 = new Label("foo");
    Label l1 = new Label("bar");
    Label l2 = new Label("baz");

    // Add one widget, and ensure that it's visible.
    p.add(l0, new Label("header"), 1);
    assertEquals(l0, p.getVisibleWidget());

    // Remove the same, and ensure that there's no visible widget.
    p.remove(l0);
    assertNull(p.getVisibleWidget());

    // Add all three.
    p.add(l0, new Label("header"), 1);
    p.add(l1, new Label("header"), 1);
    p.add(l2, new Label("header"), 1);

    // l0 should be visible by default.
    assertEquals(l0, p.getVisibleWidget());

    // Removing l0 (currently visible) should cause l1 to be shown.
    p.remove(l0);
    assertEquals(l1, p.getVisibleWidget());

    // Now ensure that showing l2 works properly.
    p.showWidget(l2);
    assertEquals(l2, p.getVisibleWidget());
  }
}
