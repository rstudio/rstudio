/*
 * Copyright 2008 Google Inc.
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
 * <img class='gallery' src='doc-files/HorizontalPanel.png'/>
 * </p>
 */
public class HorizontalPanel extends CellPanel implements HasAlignment,
    InsertPanel.ForIsWidget {

  private HorizontalAlignmentConstant horzAlign = ALIGN_DEFAULT;
  private Element tableRow;
  private VerticalAlignmentConstant vertAlign = ALIGN_TOP;
  /**
   * Creates an empty horizontal panel.
   */
  public HorizontalPanel() {
    tableRow = DOM.createTR();
    DOM.appendChild(getBody(), tableRow);

    DOM.setElementProperty(getTable(), "cellSpacing", "0");
    DOM.setElementProperty(getTable(), "cellPadding", "0");
  }

  @Override
  public void add(Widget w) {
    Element td = createAlignedTd();
    DOM.appendChild(tableRow, td);
    add(w, td);
  }

  public HorizontalAlignmentConstant getHorizontalAlignment() {
    return horzAlign;
  }

  public VerticalAlignmentConstant getVerticalAlignment() {
    return vertAlign;
  }

  public void insert(IsWidget w, int beforeIndex) {
    insert(asWidgetOrNull(w), beforeIndex);
  }

  public void insert(Widget w, int beforeIndex) {
    checkIndexBoundsForInsertion(beforeIndex);

    /*
     * The case where we reinsert an already existing child is tricky.
     * 
     * For the WIDGET, it ultimately removes first and inserts second, so we
     * have to adjust the index within ComplexPanel.insert(). But for the DOM,
     * we insert first and remove second, which means we DON'T need to adjust
     * the index.
     */
    Element td = createAlignedTd();
    DOM.insertChild(tableRow, td, beforeIndex);
    insert(w, td, beforeIndex, false);
  }

  @Override
  public boolean remove(Widget w) {
    // Get the TD to be removed, before calling super.remove(), because
    // super.remove() will detach the child widget's element from its parent.
    Element td = DOM.getParent(w.getElement());
    boolean removed = super.remove(w);
    if (removed) {
      DOM.removeChild(tableRow, td);
    }
    return removed;
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

  /**
   * <b>Affected Elements:</b>
   * <ul>
   * <li>-# = the cell at the given index.</li>
   * </ul>
   * 
   * @see UIObject#onEnsureDebugId(String)
   */
  @Override
  protected void onEnsureDebugId(String baseID) {
    super.onEnsureDebugId(baseID);
    int numChildren = getWidgetCount();
    for (int i = 0; i < numChildren; i++) {
      ensureDebugId(getWidgetTd(getWidget(i)), baseID, "" + i);
    }
  }

  private Element createAlignedTd() {
    Element td = DOM.createTD();
    setCellHorizontalAlignment(td, horzAlign);
    setCellVerticalAlignment(td, vertAlign);
    return td;
  }
}
