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
import com.google.gwt.user.client.ui.DockLayoutPanel.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link DockLayoutPanel}.
 */
public class DockLayoutPanelTest extends WidgetTestBase {

  public void testGetResolvedDirection() {
    DockLayoutPanel panel = createDockLayoutPanel();
    assertEquals(Direction.WEST,
        panel.getResolvedDirection(Direction.LINE_START));
    assertEquals(Direction.EAST, panel.getResolvedDirection(Direction.LINE_END));
  }

  public void testAddLineEnd() {
    DockLayoutPanel panel = createDockLayoutPanel();
    Widget widget = new Label();
    panel.addLineEnd(widget, 10);
    assertEquals(Direction.LINE_END, panel.getWidgetDirection(widget));
  }

  public void testAddLineStart() {
    DockLayoutPanel panel = createDockLayoutPanel();
    Widget widget = new Label();
    panel.addLineStart(widget, 10);
    assertEquals(Direction.LINE_START, panel.getWidgetDirection(widget));
  }

  /**
   * Tests {@link DockLayoutPanel#addEast(Widget, double)}.
   */
  public void testAddEast() {
    DockLayoutPanel panel = createDockLayoutPanel();
    Widget widget = new Label();

    panel.addEast(widget, 10);

    assertWidgetDirection(panel, widget, Direction.EAST);
    assertLogicalPaternity(panel, widget);
    assertPhysicalPaternity(panel, widget);
  }

  /**
   * Tests {@link DockLayoutPanel#addEast(IsWidget, double)}.
   */
  public void testAddEastAsIsWidget() {
    DockLayoutPanel panel = createDockLayoutPanel();
    Widget widget = new Label();

    // IsWidget cast to call the overloaded version
    panel.addEast((IsWidget) widget, 10);

    assertWidgetDirection(panel, widget, Direction.EAST);
    assertLogicalPaternity(panel, widget);
    assertPhysicalPaternity(panel, widget);
  }

  /**
   * Tests {@link DockLayoutPanel#addNorth(Widget, double)}.
   */
  public void testAddNorth() {
    DockLayoutPanel panel = createDockLayoutPanel();
    Widget widget = new Label();

    panel.addNorth(widget, 10);

    assertWidgetDirection(panel, widget, Direction.NORTH);
    assertLogicalPaternity(panel, widget);
    assertPhysicalPaternity(panel, widget);
  }

  /**
   * Tests {@link DockLayoutPanel#addNorth(IsWidget, double)}.
   */
  public void testAddNorthAsIsWidget() {
    DockLayoutPanel panel = createDockLayoutPanel();
    Widget widget = new Label();

    // IsWidget cast to call the overloaded version
    panel.addNorth((IsWidget) widget, 10);

    assertWidgetDirection(panel, widget, Direction.NORTH);
    assertLogicalPaternity(panel, widget);
    assertPhysicalPaternity(panel, widget);
  }

  /**
   * Tests {@link DockLayoutPanel#addSouth(Widget, double)}.
   */
  public void testAddSouth() {
    DockLayoutPanel panel = createDockLayoutPanel();
    Widget widget = new Label();

    panel.addSouth(widget, 10);

    assertWidgetDirection(panel, widget, Direction.SOUTH);
    assertLogicalPaternity(panel, widget);
    assertPhysicalPaternity(panel, widget);
  }

  /**
   * Tests {@link DockLayoutPanel#addSouth(IsWidget, double)}.
   */
  public void testAddSouthAsIsWidget() {
    DockLayoutPanel panel = createDockLayoutPanel();
    Widget widget = new Label();

    // IsWidget cast to call the overloaded version
    panel.addSouth((IsWidget) widget, 10);

    assertWidgetDirection(panel, widget, Direction.SOUTH);
    assertLogicalPaternity(panel, widget);
    assertPhysicalPaternity(panel, widget);
  }

  /**
   * Tests {@link DockLayoutPanel#addWest(Widget, double)}.
   */
  public void testAddWest() {
    DockLayoutPanel panel = createDockLayoutPanel();
    Widget widget = new Label();

    panel.addWest(widget, 10);

    assertWidgetDirection(panel, widget, Direction.WEST);
    assertLogicalPaternity(panel, widget);
    assertPhysicalPaternity(panel, widget);
  }

  /**
   * Tests {@link DockLayoutPanel#addWest(IsWidget, double)}.
   */
  public void testAddWestAsIsWidget() {
    DockLayoutPanel panel = createDockLayoutPanel();
    Widget widget = new Label();

    // IsWidget cast to call the overloaded version
    panel.addWest((IsWidget) widget, 10);

    assertWidgetDirection(panel, widget, Direction.WEST);
    assertLogicalPaternity(panel, widget);
    assertPhysicalPaternity(panel, widget);
  }

  public void testGetWidgetSize() {
    DockLayoutPanel panel = createDockLayoutPanel();
    Widget widget = new Label();
    panel.addEast(widget, 123.4);
    assertEquals(123.4, panel.getWidgetSize(widget), 0.1);
  }

  public void testInsertLineEnd() {
    DockLayoutPanel panel = createDockLayoutPanel();
    Widget widget = new Label();
    panel.insertLineEnd(widget, 10, null);
    assertEquals(Direction.LINE_END, panel.getWidgetDirection(widget));
  }

  public void testInsertLineStart() {
    DockLayoutPanel panel = createDockLayoutPanel();
    Widget widget = new Label();
    panel.insertLineStart(widget, 10, null);
    assertEquals(Direction.LINE_START, panel.getWidgetDirection(widget));
  }

  /**
   * Test that forcing layout will call onResize only once.
   */
  public void testForceLayoutNoRedundantOnResize() {
    final List<Boolean> called = new ArrayList<>();
    DockLayoutPanel panel = createDockLayoutPanel();
    SimpleLayoutPanel child = new SimpleLayoutPanel() {
      @Override
      public void onResize() {
        super.onResize();
        called.add(true);
      }
    };
    panel.addWest(child,123.4);
    panel.forceLayout();
    assertEquals(1,called.size());
  }

  protected DockLayoutPanel createDockLayoutPanel() {
    return new DockLayoutPanel(Unit.PX);
  }

  /**
   * Asserts that <b>panel</b> is the logical parent of <b>expectedChild</b>.
   * 
   * @param panel the parent panel
   * @param expectedChild the expected child of <b>panel</b>
   */
  private void assertLogicalPaternity(ComplexPanel panel, Widget expectedChild) {
    assertSame("The parent and the panel must be the same", panel,
        expectedChild.getParent());
    assertTrue("The child must be in the childen collection of the panel",
        panel.getChildren().contains(expectedChild));
  }

  /**
   * Asserts that <b>expectedFirstChild</b> is the first physical child of
   * <b>panel</b>.
   * 
   * @param panel the parent panel
   * @param expectedFirstChild the expected first child of <b>panel</b>
   */
  private void assertPhysicalPaternity(DockLayoutPanel panel,
      Widget expectedFirstChild) {
    assertSame(
        "The parent's Element of the child must be the panel's container Element",
        expectedFirstChild.getElement().getParentElement(),
        panel.getWidgetContainerElement(expectedFirstChild));
    assertSame(
        "The child's Element must be first child of the panel's container Element",
        panel.getWidgetContainerElement(expectedFirstChild).getFirstChildElement(),
        expectedFirstChild.getElement());
  }

  /**
   * Asserts that the {@link DockLayoutPanel.Direction} of <b>widget</b> in
   * <b>panel</b> is <b>expectedDirection</b>.
   * 
   * @param panel the panel containing <b>widget</b>
   * @param widget the widget being tested
   * @param expectedDirection the expected direction
   */
  private void assertWidgetDirection(DockLayoutPanel panel, Widget widget,
      DockLayoutPanel.Direction expectedDirection) {
    assertEquals("The direction of the widget in the panel should be "
        + expectedDirection, expectedDirection,
        panel.getWidgetDirection(widget));
  }

}
