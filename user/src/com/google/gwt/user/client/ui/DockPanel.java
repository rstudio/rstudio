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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

import java.util.Iterator;

/**
 * A panel that lays its child widgets out "docked" at its outer edges, and
 * allows its last widget to take up the remaining space in its center.
 * 
 * <p>
 * <img class='gallery' src='DockPanel.png'/>
 * </p>
 */
public class DockPanel extends CellPanel implements HasAlignment {

  /**
   * DockPanel layout constant, used in
   * {@link DockPanel#add(Widget, DockPanel.DockLayoutConstant)}.
   */
  public static class DockLayoutConstant {
    private DockLayoutConstant() {
    }
  }

  /*
   * This class is package-protected for use with DockPanelTest.
   */
  static class LayoutData {
    public DockLayoutConstant direction;
    public String hAlign = "left";
    public String height = "";
    public Element td;
    public String vAlign = "top";
    public String width = "";

    public LayoutData(DockLayoutConstant dir) {
      direction = dir;
    }
  }

  private static class TmpRow {
    public int center;
    public Element tr;
  }

  /**
   * Specifies that a widget be added at the center of the dock.
   */
  public static final DockLayoutConstant CENTER = new DockLayoutConstant();

  /**
   * Specifies that a widget be added at the east edge of the dock.
   */
  public static final DockLayoutConstant EAST = new DockLayoutConstant();

  /**
   * Specifies that a widget be added at the north edge of the dock.
   */
  public static final DockLayoutConstant NORTH = new DockLayoutConstant();

  /**
   * Specifies that a widget be added at the south edge of the dock.
   */
  public static final DockLayoutConstant SOUTH = new DockLayoutConstant();

  /**
   * Specifies that a widget be added at the west edge of the dock.
   */
  public static final DockLayoutConstant WEST = new DockLayoutConstant();

  private HorizontalAlignmentConstant horzAlign = ALIGN_LEFT;
  private VerticalAlignmentConstant vertAlign = ALIGN_TOP;
  private Widget center;

  /**
   * Creates an empty dock panel.
   */
  public DockPanel() {
    DOM.setElementPropertyInt(getTable(), "cellSpacing", 0);
    DOM.setElementPropertyInt(getTable(), "cellPadding", 0);
  }

  /**
   * Adds a widget to the specified edge of the dock. If the widget is already a
   * child of this panel, this method behaves as though {@link #remove(Widget)}
   * had already been called.
   * 
   * @param widget the widget to be added
   * @param direction the widget's direction in the dock
   * 
   * @throws IllegalArgumentException when adding to the {@link #CENTER} and
   *           there is already a different widget there
   */
  public void add(Widget widget, DockLayoutConstant direction) {
    // Validate
    if (direction == CENTER) {
      // Early out on the case of reinserting the center at the center.
      if (widget == center) {
        return;
      } else if (center != null) {
        // Ensure a second 'center' widget is not being added.
        throw new IllegalArgumentException(
            "Only one CENTER widget may be added");
      }
    }

    // Detach new child.
    widget.removeFromParent();

    // Logical attach.
    getChildren().add(widget);
    if (direction == CENTER) {
      center = widget;
    }

    // Physical attach.
    LayoutData layout = new LayoutData(direction);
    widget.setLayoutData(layout);
    setCellHorizontalAlignment(widget, horzAlign);
    setCellVerticalAlignment(widget, vertAlign);
    realizeTable();

    // Adopt.
    adopt(widget);
  }

  public HorizontalAlignmentConstant getHorizontalAlignment() {
    return horzAlign;
  }

  public VerticalAlignmentConstant getVerticalAlignment() {
    return vertAlign;
  }

  /**
   * Gets the layout direction of the given child widget.
   * 
   * @param w the widget to be queried
   * @return the widget's layout direction, or <code>null</code> if it is not
   *         a child of this panel
   */
  public DockLayoutConstant getWidgetDirection(Widget w) {
    if (w.getParent() != this) {
      return null;
    }
    return ((LayoutData) w.getLayoutData()).direction;
  }

  @Override
  public boolean remove(Widget w) {
    boolean removed = super.remove(w);
    if (removed) {
      // Clear the center widget.
      if (w == center) {
        center = null;
      }
      realizeTable();
    }
    return removed;
  }

  @Override
  public void setCellHeight(Widget w, String height) {
    LayoutData data = (LayoutData) w.getLayoutData();
    data.height = height;
    if (data.td != null) {
      DOM.setStyleAttribute(data.td, "height", data.height);
    }
  }

  @Override
  public void setCellHorizontalAlignment(Widget w,
      HorizontalAlignmentConstant align) {
    LayoutData data = (LayoutData) w.getLayoutData();
    data.hAlign = align.getTextAlignString();
    if (data.td != null) {
      DOM.setElementProperty(data.td, "align", data.hAlign);
    }
  }

  @Override
  public void setCellVerticalAlignment(Widget w, VerticalAlignmentConstant align) {
    LayoutData data = (LayoutData) w.getLayoutData();
    data.vAlign = align.getVerticalAlignString();
    if (data.td != null) {
      DOM.setStyleAttribute(data.td, "verticalAlign", data.vAlign);
    }
  }

  @Override
  public void setCellWidth(Widget w, String width) {
    LayoutData data = (LayoutData) w.getLayoutData();
    data.width = width;
    if (data.td != null) {
      DOM.setStyleAttribute(data.td, "width", data.width);
    }
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
   * (Re)creates the DOM structure of the table representing the DockPanel,
   * based on the order and layout of the children.
   */
  private void realizeTable() {
    Element bodyElem = getBody();
    while (DOM.getChildCount(bodyElem) > 0) {
      DOM.removeChild(bodyElem, DOM.getChild(bodyElem, 0));
    }

    int rowCount = 1, colCount = 1;
    for (Iterator<Widget> it = getChildren().iterator(); it.hasNext();) {
      Widget child = it.next();
      DockLayoutConstant dir = ((LayoutData) child.getLayoutData()).direction;
      if ((dir == NORTH) || (dir == SOUTH)) {
        ++rowCount;
      } else if ((dir == EAST) || (dir == WEST)) {
        ++colCount;
      }
    }

    TmpRow[] rows = new TmpRow[rowCount];
    for (int i = 0; i < rowCount; ++i) {
      rows[i] = new TmpRow();
      rows[i].tr = DOM.createTR();
      DOM.appendChild(bodyElem, rows[i].tr);
    }

    int westCol = 0, eastCol = colCount - 1;
    int northRow = 0, southRow = rowCount - 1;
    Element centerTd = null;

    for (Iterator<Widget> it = getChildren().iterator(); it.hasNext();) {
      Widget child = it.next();
      LayoutData layout = (LayoutData) child.getLayoutData();

      Element td = DOM.createTD();
      layout.td = td;
      DOM.setElementProperty(layout.td, "align", layout.hAlign);
      DOM.setStyleAttribute(layout.td, "verticalAlign", layout.vAlign);
      DOM.setElementProperty(layout.td, "width", layout.width);
      DOM.setElementProperty(layout.td, "height", layout.height);

      if (layout.direction == NORTH) {
        DOM.insertChild(rows[northRow].tr, td, rows[northRow].center);
        DOM.appendChild(td, child.getElement());
        DOM.setElementPropertyInt(td, "colSpan", eastCol - westCol + 1);
        ++northRow;
      } else if (layout.direction == SOUTH) {
        DOM.insertChild(rows[southRow].tr, td, rows[southRow].center);
        DOM.appendChild(td, child.getElement());
        DOM.setElementPropertyInt(td, "colSpan", eastCol - westCol + 1);
        --southRow;
      } else if (layout.direction == WEST) {
        TmpRow row = rows[northRow];
        DOM.insertChild(row.tr, td, row.center++);
        DOM.appendChild(td, child.getElement());
        DOM.setElementPropertyInt(td, "rowSpan", southRow - northRow + 1);
        ++westCol;
      } else if (layout.direction == EAST) {
        TmpRow row = rows[northRow];
        DOM.insertChild(row.tr, td, row.center);
        DOM.appendChild(td, child.getElement());
        DOM.setElementPropertyInt(td, "rowSpan", southRow - northRow + 1);
        --eastCol;
      } else if (layout.direction == CENTER) {
        // Defer adding the center widget, so that it can be added after all
        // the others are complete.
        centerTd = td;
      }
    }

    // If there is a center widget, add it at the end (centerTd is guaranteed
    // to be initialized because it will have been set in the CENTER case in
    // the above loop).
    if (center != null) {
      TmpRow row = rows[northRow];
      DOM.insertChild(row.tr, centerTd, row.center);
      DOM.appendChild(centerTd, center.getElement());
    }
  }
}
