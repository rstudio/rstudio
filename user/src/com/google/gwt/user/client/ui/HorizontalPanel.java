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

/**
 * A panel that lays all of its widgets out in a single horizontal column.
 * 
 * <p>
 * <img class='gallery' src='HorizontalPanel.png'/>
 * </p>
 */
public class HorizontalPanel extends CellPanel implements IndexedPanel,
    HasAlignment {

  private HorizontalAlignmentConstant horzAlign = ALIGN_LEFT;
  private Element tableRow;
  private VerticalAlignmentConstant vertAlign = ALIGN_TOP;

  /**
   * Creates an empty horizontal panel.
   */
  public HorizontalPanel() {
    tableRow = DOM.createTR();
    DOM.appendChild(getBody(), tableRow);

    DOM.setAttribute(getTable(), "cellSpacing", "0");
    DOM.setAttribute(getTable(), "cellPadding", "0");
  }

  /**
   * Adds a child widget to the panel.
   * 
   * @param w the widget to be added
   */
  public void add(Widget w) {
    insert(w, getWidgetCount());
  }

  public HorizontalAlignmentConstant getHorizontalAlignment() {
    return horzAlign;
  }

  public VerticalAlignmentConstant getVerticalAlignment() {
    return vertAlign;
  }

  public Widget getWidget(int index) {
    return getChildren().get(index);
  }

  public int getWidgetCount() {
    return getChildren().size();
  }

  public int getWidgetIndex(Widget child) {
    return getChildren().indexOf(child);
  }

  /**
   * Inserts a widget before the specified index.
   * 
   * @param w the widget to be inserted
   * @param beforeIndex the index before which it will be inserted
   * @throws IndexOutOfBoundsException if <code>beforeIndex</code> is out of
   *           range
   */
  public void insert(Widget w, int beforeIndex) {
    // Call this early to ensure that the table doesn't end up partially
    // constructed when an exception is thrown from adopt().
    w.removeFromParent();

    Element td = DOM.createTD();
    DOM.insertChild(tableRow, td, beforeIndex);

    super.insert(w, td, beforeIndex);

    setCellHorizontalAlignment(w, horzAlign);
    setCellVerticalAlignment(w, vertAlign);
  }

  public boolean remove(int index) {
    return remove(getWidget(index));
  }

  public boolean remove(Widget w) {
    if (w.getParent() != this) {
      return false;
    }

    Element td = DOM.getParent(w.getElement());
    DOM.removeChild(tableRow, td);

    super.remove(w);
    return true;
  }

  /**
   * Sets the default horizontal alignment to be used for widgets added to this
   * panel. It only applies to widgets added after this property is set.
   * 
   * @see HasHorizontalAlignment#setHorizontalAlignment(HasHorizontalAlignment.HorizontalAlignmentConstant)
   */
  public void setHorizontalAlignment(HorizontalAlignmentConstant align) {
    horzAlign = align;
  }

  /**
   * Sets the default vertical alignment to be used for widgets added to this
   * panel. It only applies to widgets added after this property is set.
   * 
   * @see HasVerticalAlignment#setVerticalAlignment(HasVerticalAlignment.VerticalAlignmentConstant)
   */
  public void setVerticalAlignment(VerticalAlignmentConstant align) {
    vertAlign = align;
  }
}
