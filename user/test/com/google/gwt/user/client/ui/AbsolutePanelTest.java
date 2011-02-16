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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.user.client.Window;

/**
 * Tests for {@link AbsolutePanel}.
 */
public class AbsolutePanelTest extends PanelTestBase<AbsolutePanel> {

  /**
   * AbsolutePanel once had a bug where calling
   * {@link AbsolutePanel#add(Widget, int, int)} twice on the same child widget
   * would throw an {@link IndexOutOfBoundsException}.
   */
  public void testDoubleAdd() {
    AbsolutePanel absolutePanel = createPanel();
    Label label = new Label("label");

    absolutePanel.add(label, 10, 10);
    absolutePanel.add(label, 10, 10);
  }

  /**
   * Ensures that add(Widget, int, int) adds the Widget as its child.
   */
  public void testAdd() {
    AbsolutePanel absolutePanel = createPanel();
    Label label = new Label("foo");

    absolutePanel.add(label, 10, 10);

    assertLogicalPaternity(absolutePanel, label);
    assertPhysicalPaternity(absolutePanel, label);
  }

  /**
   * Ensures that add(IsWidget, int, int) adds the Widget as its child.
   */
  public void testAddAsIsWidget() {
    AbsolutePanel absolutePanel = createPanel();
    Label label = new Label("foo");

    // IsWidget cast to call the overloaded version
    absolutePanel.add((IsWidget) label, 10, 10);

    assertLogicalPaternity(absolutePanel, label);
    assertPhysicalPaternity(absolutePanel, label);
  }

  /**
   * Failed in all modes with absolute positioning. TODO: (flin) File a new
   * HtmlUnit bug.
   */
  @DoNotRunWith(Platform.HtmlUnitBug)
  public void testPositioning() {
    // Make an absolute panel with a label at (3, 7).
    AbsolutePanel abs = createPanel();
    abs.setSize("128px", "128px");
    Label lbl = new Label("foo");
    abs.add(lbl, 3, 7);

    // Put the panel in a grid that will place it at (100, 200) within the grid.
    Grid g = new Grid(2, 2);
    g.setBorderWidth(0);
    g.setCellPadding(0);
    g.setCellSpacing(0);
    g.getCellFormatter().setWidth(0, 0, "100px");
    g.getCellFormatter().setHeight(0, 0, "200px");
    g.setWidget(1, 1, abs);

    // Clear the margin so that absolute position is predictable.
    Window.setMargin("0px");
    RootPanel.get().add(g);

    // Make sure that the label's position, both relative to the absolute panel
    // and relative to the page, is correct. It is important to test both of
    // these, because an incorrectly constructed AbsolutePanel will lead to
    // wacky positioning of its children.
    int x = abs.getWidgetLeft(lbl);
    int y = abs.getWidgetTop(lbl);
    int absX = lbl.getAbsoluteLeft() - Document.get().getBodyOffsetLeft();
    int absY = lbl.getAbsoluteTop() - Document.get().getBodyOffsetTop();
    assertEquals(3, x);
    assertEquals(7, y);
    assertEquals(
        "absX should be 103. This will fail in WebKit if run headless",
        3 + 100, absX);
    assertEquals(7 + 200, absY);
  }

  @Override
  protected AbsolutePanel createPanel() {
    return new AbsolutePanel();
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
  private void assertPhysicalPaternity(ComplexPanel panel,
      Widget expectedFirstChild) {
    Element panelElement = panel.getElement();
    Element childElement = expectedFirstChild.getElement();
    assertSame("The parent's Element of the child must be the panel's Element",
        panelElement, childElement.getParentElement());
    assertSame(
        "The child's Element must be first child of the panel's Element",
        childElement, panelElement.getFirstChildElement());
  }
}
