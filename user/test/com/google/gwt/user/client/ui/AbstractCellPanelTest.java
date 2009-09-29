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
public abstract class AbstractCellPanelTest<T extends CellPanel> extends PanelTestBase<T> {

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.DebugTest";
  }

  public void testGetWidgetTd() {
    CellPanel panel = createCellPanel();
    Widget w = panel.getWidget(0);
    assertEquals(w.getElement().getParentElement(), panel.getWidgetTd(w));
  }

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

  public void testSetCellAlignmentForNonChildWidget() {
    CellPanel panel = createCellPanel();
    Widget w = new Label("Not a chid");

    // setCellVerticalAlignment should not throw an error
    panel.setCellVerticalAlignment(w, HasVerticalAlignment.ALIGN_BOTTOM);

    // setCellHorizontalAlignment should not throw an error
    panel.setCellHorizontalAlignment(w, HasHorizontalAlignment.ALIGN_RIGHT);
  }

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

  public void testSetCellSizeForNonChildWidget() {
    CellPanel panel = createCellPanel();
    Widget w = new Label("Not a chid");

    // setCellHeight should not throw an error
    panel.setCellHeight(w, "100px");

    // setCellWidth should not throw an error
    panel.setCellWidth(w, "200px");
  }

  /**
   * Create a populated {@link CellPanel}.
   * 
   * @return the {@link CellPanel}
   */
  protected abstract CellPanel createCellPanel();
}
