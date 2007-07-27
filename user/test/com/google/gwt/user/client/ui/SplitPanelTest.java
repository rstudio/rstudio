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
import com.google.gwt.user.client.DOM;

/**
 * Tests both {@link HorizontalSplitPanel} and {@link VerticalSplitPanel}.
 */
public class SplitPanelTest extends GWTTestCase {

  private static Widget createMockWidget() {
    final Label label = new Label();
    label.setText("Testing 1, 2, 3");
    DOM.setStyleAttribute(label.getElement(), "fontSize", "72pt");
    return label;
  }

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testHorizontalAttachDetachOrder() {
    HasWidgetsTester.testAttachDetachOrder(new HorizontalSplitPanel());
  }

  /**
   * Tests creation, widget assignment, null assignment for
   * {@link HorizontalSplitPanel}.
   */
  public void testHorizontalSplitPanelCreate() {
    final HorizontalSplitPanel panel = new HorizontalSplitPanel();
    final Widget widgetA = createMockWidget();
    final Widget widgetB = createMockWidget();

    // Intentionally add before setting widgets.

    RootPanel.get().add(panel);

    panel.setHeight("100px");
    panel.setWidth("100px");

    // Ensure position can be set before widgets are added.
    panel.setSplitPosition("20px");

    panel.setRightWidget(widgetB);
    panel.setLeftWidget(widgetA);

    assertTrue(panel.getRightWidget() == widgetB);
    assertTrue(panel.getLeftWidget() == widgetA);

    panel.setLeftWidget(null);
    panel.setRightWidget(null);

    assertTrue(panel.getRightWidget() == null);
    assertTrue(panel.getLeftWidget() == null);

    panel.setLeftWidget(widgetB);
    panel.setRightWidget(widgetA);

    assertTrue(panel.getLeftWidget() == widgetB);
    assertTrue(panel.getRightWidget() == widgetA);

    // Ensure we ended up at the right size.
    assertEquals(100, panel.getOffsetWidth());
    assertEquals(100, panel.getOffsetHeight());
  }

  public void testVerticalAttachDetachOrder() {
    HasWidgetsTester.testAttachDetachOrder(new VerticalSplitPanel());
  }

  /**
   * Tests creation, widget assignment, null assignment for
   * {@link VerticalSplitPanel}.
   */
  public void testVerticalSplitPanelCreate() {

    final VerticalSplitPanel panel = new VerticalSplitPanel();
    final Widget widgetA = createMockWidget();
    final Widget widgetB = createMockWidget();

    // Intentionally add before setting widgets.
    RootPanel.get().add(panel);

    panel.setHeight("100px");
    panel.setWidth("100px");
    // Ensure position can be set before widgets are added.
    panel.setSplitPosition("20px");

    panel.setBottomWidget(widgetB);
    panel.setTopWidget(widgetA);

    assertTrue(panel.getBottomWidget() == widgetB);
    assertTrue(panel.getTopWidget() == widgetA);

    panel.setTopWidget(null);
    panel.setBottomWidget(null);

    assertTrue(panel.getTopWidget() == null);
    assertTrue(panel.getBottomWidget() == null);

    panel.setTopWidget(widgetB);
    panel.setBottomWidget(widgetA);

    assertTrue(panel.getTopWidget() == widgetB);
    assertTrue(panel.getBottomWidget() == widgetA);

    // Ensure we ended up at the right size.
    assertEquals(100, panel.getOffsetWidth());
    assertEquals(100, panel.getOffsetHeight());
  }
}
