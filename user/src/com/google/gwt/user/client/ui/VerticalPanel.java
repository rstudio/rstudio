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
 * A panel that lays all of its widgets out in a single vertical column.
 * 
 * <p>
 * <img class='gallery' src='VerticalPanel.png'/>
 * </p>
 */
public class VerticalPanel extends CellPanel implements IndexedPanel,
    HasAlignment {

  private HorizontalAlignmentConstant horzAlign = ALIGN_LEFT;
  private VerticalAlignmentConstant vertAlign = ALIGN_TOP;

  /**
   * Creates an empty vertical panel.
   */
  public VerticalPanel() {
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

    Element tr = DOM.createTR();
    Element td = DOM.createTD();

    DOM.insertChild(getBody(), tr, beforeIndex);
    DOM.appendChild(tr, td);

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
    Element tr = DOM.getParent(td);
    DOM.removeChild(getBody(), tr);

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
