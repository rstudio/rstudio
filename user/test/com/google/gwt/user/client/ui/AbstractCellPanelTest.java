/*
 * Copyright 2009 Google Inc.
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

/**
 * Base tests for {@link CellPanel}.
 * 
 * @param <T> the panel type
 */
public abstract class AbstractCellPanelTest<T extends CellPanel> extends
    PanelTestBase<T> {

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.DebugTest";
  }

  /**
   * Test {@link CellPanel#getWidgetTd(Widget)}.
   */
  public void testGetWidgetTd() {
    CellPanel panel = createCellPanel();
    Widget w = panel.getWidget(0);
    assertEquals(w.getElement().getParentElement(), panel.getWidgetTd(w));
  }

  /**
   * Tests
   * {@link CellPanel#setCellVerticalAlignment(Widget, com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant)}
   * and
   * {@link CellPanel#setCellHorizontalAlignment(Widget, com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant)}
   * .
   */
  public void testSetCellAlignment() {
    CellPanel panel = createCellPanel();
    Widget w = panel.getWidget(0);
    Element td = panel.getWidgetTd(w);

    // setCellVerticalAlignment
    panel.setCellVerticalAlignment(w, HasVerticalAlignment.ALIGN_BOTTOM);
    assertEquals("bottom", td.getStyle().getProperty("verticalAlign"));

    // setCellHorizontalAlignment
    panel.setCellHorizontalAlignment(w, HasHorizontalAlignment.ALIGN_RIGHT);
    assertEquals("right", td.getPropertyString("align"));
  }

  /**
   * Tests
   * {@link CellPanel#setCellVerticalAlignment(IsWidget, com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant)}
   * and
   * {@link CellPanel#setCellHorizontalAlignment(IsWidget, com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant)}
   * .
   */
  public void testSetCellAlignmentAsIsWidget() {
    CellPanel panel = createCellPanel();
    Widget w = panel.getWidget(0);
    Element td = panel.getWidgetTd(w);

    // setCellVerticalAlignment
    // IsWidget cast to call the overloaded version
    panel.setCellVerticalAlignment((IsWidget) w,
        HasVerticalAlignment.ALIGN_BOTTOM);
    assertEquals("bottom", td.getStyle().getProperty("verticalAlign"));

    // setCellHorizontalAlignment
    // IsWidget reference to call the overloaded version
    panel.setCellHorizontalAlignment((IsWidget) w,
        HasHorizontalAlignment.ALIGN_RIGHT);
    assertEquals("right", td.getPropertyString("align"));
  }

  /**
   * Ensures that
   * {@link CellPanel#setCellVerticalAlignment(Widget, com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant)}
   * and
   * {@link CellPanel#setCellHorizontalAlignment(Widget, com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant)}
   * don't throw an Exception when the Widget argument is not a child of the
   * panel.
   */
  public void testSetCellAlignmentForNonChildWidget() {
    CellPanel panel = createCellPanel();
    Widget w = new Label("Not a chid");

    // setCellVerticalAlignment should not throw an error
    panel.setCellVerticalAlignment(w, HasVerticalAlignment.ALIGN_BOTTOM);

    // setCellHorizontalAlignment should not throw an error
    panel.setCellHorizontalAlignment(w, HasHorizontalAlignment.ALIGN_RIGHT);
  }

  /**
   * Ensures that
   * {@link CellPanel#setCellVerticalAlignment(IsWidget, com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant)}
   * and
   * {@link CellPanel#setCellHorizontalAlignment(IsWidget, com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant)}
   * don't throw an Exception when the IsWidget argument is not a child of the
   * panel.
   */
  public void testSetCellAlignmentForNonChildWidgetAsIsWidget() {
    CellPanel panel = createCellPanel();
    // IsWidget reference to call the overloaded version
    IsWidget w = new Label("Not a chid");

    // setCellVerticalAlignment should not throw an error
    panel.setCellVerticalAlignment(w, HasVerticalAlignment.ALIGN_BOTTOM);

    // setCellHorizontalAlignment should not throw an error
    panel.setCellHorizontalAlignment(w, HasHorizontalAlignment.ALIGN_RIGHT);
  }

  /**
   * Tests {@link CellPanel#setCellHeight(Widget, String)} and
   * {@link CellPanel#setCellWidth(Widget, String)}.
   */
  public void testSetCellSize() {
    CellPanel panel = createCellPanel();
    Widget w = panel.getWidget(0);
    Element td = panel.getWidgetTd(w);

    // setCellHeight
    panel.setCellHeight(w, "100px");
    assertEquals(100, td.getPropertyInt("height"));

    // setCellWidth
    panel.setCellWidth(w, "200px");
    assertEquals(200, td.getPropertyInt("width"));
  }

  /**
   * Tests {@link CellPanel#setCellHeight(IsWidget, String)} and
   * {@link CellPanel#setCellWidth(IsWidget, String)}.
   */
  public void testSetCellSizeAsIsWidget() {
    CellPanel panel = createCellPanel();
    Widget w = panel.getWidget(0);
    Element td = panel.getWidgetTd(w);

    // setCellHeight
    // IsWidget cast to call the overloaded version
    panel.setCellHeight((IsWidget) w, "100px");
    assertEquals(100, td.getPropertyInt("height"));

    // setCellWidth
    // IsWidget cast to call the overloaded version
    panel.setCellWidth((IsWidget) w, "200px");
    assertEquals(200, td.getPropertyInt("width"));
  }

  /**
   * Ensures that {@link CellPanel#setCellHeight(Widget, String)} and
   * {@link CellPanel#setCellWidth(Widget, String)} don't throw an Exception
   * when the Widget argument is not a child of the panel.
   */
  public void testSetCellSizeForNonChildWidget() {
    CellPanel panel = createCellPanel();
    Widget w = new Label("Not a chid");

    // setCellHeight should not throw an error
    panel.setCellHeight(w, "100px");

    // setCellWidth should not throw an error
    panel.setCellWidth(w, "200px");
  }

  /**
   * Ensures that {@link CellPanel#setCellHeight(IsWidget, String)} and
   * {@link CellPanel#setCellWidth(IsWidget, String)} don't throw an Exception
   * when the IsWidget argument is not a child of the panel.
   */
  public void testSetCellSizeForNonChildWidgetAsIsWidget() {
    CellPanel panel = createCellPanel();
    Widget w = new Label("Not a chid");

    // setCellHeight should not throw an error
    // IsWidget cast to call the overloaded version
    panel.setCellHeight((IsWidget) w, "100px");

    // setCellWidth should not throw an error
    // IsWidget cast to call the overloaded version
    panel.setCellWidth((IsWidget) w, "200px");
  }

  /**
   * Create a populated {@link CellPanel}.
   * 
   * @return the {@link CellPanel}
   */
  protected abstract CellPanel createCellPanel();
}
