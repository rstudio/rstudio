/*
 * Copyright 2006 Google Inc.
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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
import com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant;

/**
 * A panel whose child widgets are contained within the cells of a table. Each
 * cell's size may be set independently. Each child widget can take up a subset
 * of its cell and can be aligned within it.
 */
public abstract class CellPanel extends ComplexPanel {

  private int spacing;
  private Element table, body;

  public CellPanel() {
    table = DOM.createTable();
    body = DOM.createTBody();
    DOM.appendChild(table, body);
    setElement(table);
  }

  /**
   * Gets the amount of spacing between this panel's cells.
   * 
   * @return the inter-cell spacing, in pixels
   */
  public int getSpacing() {
    return spacing;
  }

  /**
   * Sets the width of the border to be applied to all cells in this panel. This
   * is particularly useful when debugging layouts, in that it allows you to see
   * explicitly the cells that contain this panel's children.
   * 
   * @param width the width of the panel's cell borders, in pixels
   */
  public void setBorderWidth(int width) {
    DOM.setAttribute(table, "border", "" + width);
  }

  /**
   * Sets the height of the cell associated with the given widget, related to
   * the panel as a whole.
   * 
   * @param w the widget whose cell height is to be set
   * @param height the cell's height, in CSS units
   */
  public void setCellHeight(Widget w, String height) {
    Element td = DOM.getParent(w.getElement());
    DOM.setAttribute(td, "height", height);
  }

  /**
   * Sets the horizontal alignment of the given widget within its cell.
   * 
   * @param w the widget whose horizontal alignment is to be set
   * @param align the widget's horizontal alignment, as defined in
   *          {@link HasHorizontalAlignment}.
   */
  public void setCellHorizontalAlignment(Widget w,
      HorizontalAlignmentConstant align) {
    Element td = getWidgetTd(w);
    if (td != null) {
      DOM.setAttribute(td, "align", align.getTextAlignString());
    }
  }

  /**
   * Sets the vertical alignment of the given widget within its cell.
   * 
   * @param w the widget whose vertical alignment is to be set
   * @param align the widget's vertical alignment, as defined in
   *          {@link HasVerticalAlignment}.
   */
  public void setCellVerticalAlignment(Widget w, VerticalAlignmentConstant align) {
    Element td = getWidgetTd(w);
    if (td != null) {
      DOM.setStyleAttribute(td, "verticalAlign", align.getVerticalAlignString());
    }
  }

  /**
   * Sets the width of the cell associated with the given widget, related to the
   * panel as a whole.
   * 
   * @param w the widget whose cell width is to be set
   * @param width the cell's width, in CSS units
   */
  public void setCellWidth(Widget w, String width) {
    Element td = DOM.getParent(w.getElement());
    DOM.setAttribute(td, "width", width);
  }

  /**
   * Sets the amount of spacing between this panel's cells.
   * 
   * @param spacing the inter-cell spacing, in pixels
   */
  public void setSpacing(int spacing) {
    this.spacing = spacing;
    DOM.setIntAttribute(table, "cellSpacing", spacing);
  }

  protected Element getBody() {
    return body;
  }

  protected Element getTable() {
    return table;
  }

  private Element getWidgetTd(Widget w) {
    if (w.getParent() != this) {
      return null;
    }
    return DOM.getParent(w.getElement());
  }
}
