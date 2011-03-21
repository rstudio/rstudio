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
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
import com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant;

/**
 * A panel whose child widgets are contained within the cells of a table. Each
 * cell's size may be set independently. Each child widget can take up a subset
 * of its cell and can be aligned within it.
 * 
 * <p>
 * Note: This class is not related to the
 * {@link com.google.gwt.cell.client.Cell} based data presentation widgets such
 * as {@link com.google.gwt.user.cellview.client.CellList} and
 * {@link com.google.gwt.user.cellview.client.CellTable}.
 * 
 * <h3>Use in UiBinder Templates</h3>
 * <P>
 * When working with CellPanel subclasses in 
 * {@link com.google.gwt.uibinder.client.UiBinder UiBinder} templates, wrap
 * child widgets in <code>&lt;g:cell></code> elements. (Note the lower case
 * "c", meant to signal that the cell is not a runtime object, and so cannot
 * have a <code>ui:field</code> attribute.) Cell elements can have
 * attributes setting their height, width and alignment.
 * <h4>&lt;g:cell> attributes</h4>
 * <p>
 * <dl>
 * <dt>horizontalAlignment
 * <dd>Interpreted as a static member of {@link HorizontalAlignmentConstant}
 * and used as the <code>align</code> argument to {@link #setCellHorizontalAlignment}
 * <dt>verticalAlignment
 * <dd>Interpreted as a static member of {@link VerticalAlignmentConstant}
 * and used as the <code>align</code> argument to {@link #setCellVerticalAlignment}
 * <dt>width
 * <dd>Used as the <code>width</code> argument to {@link #setCellWidth} 
 * <dt>height
 * <dd>Used as the <code>height</code> argument to {@link #setCellHeight} 
 * </dl>
 * <p>
 * For example:<pre>
 * &lt;g:HorizontalPanel>
 *   &lt;g:cell width='5em' horizontalAlignment='ALIGN_RIGHT'>
 *     &lt;g:Label ui:field='leftSide' />
 *   &lt;/g:cell>
 *   &lt;g:cell width='15em' horizontalAlignment='ALIGN_LEFT'>
 *     &lt;g:Label ui:field='rightSide' />
 *   &lt;/g:cell>
 * &lt;/g:HorizontalPanel>
 * </pre>
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
    DOM.setElementProperty(table, "border", "" + width);
  }

  /**
   * Sets the height of the cell associated with the given widget, related to
   * the panel as a whole.
   * 
   * @param w the widget whose cell height is to be set
   * @param height the cell's height, in CSS units
   */
  public void setCellHeight(Widget w, String height) {
    Element td = getWidgetTd(w);
    if (td != null) {
      td.setPropertyString("height", height);
    }
  }
  
  /**
   * Overloaded version for IsWidget.
   * 
   * @see #setCellHeight(Widget,String)
   */
  public void setCellHeight(IsWidget w, String height) {
    this.setCellHeight(w.asWidget(), height);
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
      setCellHorizontalAlignment(td, align);
    }
  }
  
  /**
   * Overloaded version for IsWidget.
   * 
   * @see #setCellHorizontalAlignment(Widget,HasHorizontalAlignment.HorizontalAlignmentConstant)
   */
  public void setCellHorizontalAlignment(IsWidget w,
      HorizontalAlignmentConstant align) {
    this.setCellHorizontalAlignment(w.asWidget(), align);
  }

  /**
   * Sets the vertical alignment of the given widget within its cell.
   * 
   * @param w the widget whose vertical alignment is to be set
   * @param align the widget's vertical alignment, as defined in
   *          {@link HasVerticalAlignment}.
   */
  public void setCellVerticalAlignment(Widget w, HasVerticalAlignment.VerticalAlignmentConstant align) {
    Element td = getWidgetTd(w);
    if (td != null) {
      setCellVerticalAlignment(td, align);
    }
  }
  
  /**
   * Overloaded version for IsWidget.
   * 
   * @see #setCellVerticalAlignment(Widget,HasVerticalAlignment.VerticalAlignmentConstant)
   */
  public void setCellVerticalAlignment(IsWidget w, VerticalAlignmentConstant align) {
    this.setCellVerticalAlignment(w.asWidget(),align);
  }

  /**
   * Sets the width of the cell associated with the given widget, related to the
   * panel as a whole.
   * 
   * @param w the widget whose cell width is to be set
   * @param width the cell's width, in CSS units
   */
  public void setCellWidth(Widget w, String width) {
    Element td = getWidgetTd(w);
    if (td != null) {
      td.setPropertyString("width", width);
    }
  }
  
  /**
   * Overloaded version for IsWidget.
   * 
   * @see #setCellWidth(Widget,String)
   */
  public void setCellWidth(IsWidget w, String width) {
    this.setCellWidth(w.asWidget(), width);
  }

  /**
   * Sets the amount of spacing between this panel's cells.
   * 
   * @param spacing the inter-cell spacing, in pixels
   */
  public void setSpacing(int spacing) {
    this.spacing = spacing;
    DOM.setElementPropertyInt(table, "cellSpacing", spacing);
  }

  protected Element getBody() {
    return body;
  }

  protected Element getTable() {
    return table;
  }

  protected void setCellHorizontalAlignment(Element td,
      HorizontalAlignmentConstant align) {
    DOM.setElementProperty(td, "align", align.getTextAlignString());
  }

  protected void setCellVerticalAlignment(Element td,
      VerticalAlignmentConstant align) {
    DOM.setStyleAttribute(td, "verticalAlign", align.getVerticalAlignString());
  }

  Element getWidgetTd(Widget w) {
    if (w.getParent() != this) {
      return null;
    }
    return DOM.getParent(w.getElement());
  }
}
