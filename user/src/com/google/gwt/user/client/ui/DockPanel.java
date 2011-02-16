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
import com.google.gwt.i18n.client.LocaleInfo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A panel that lays its child widgets out "docked" at its outer edges, and
 * allows its last widget to take up the remaining space in its center.
 * 
 * <p>
 * This widget has limitations in standards mode that did not exist in quirks
 * mode. The child Widgets contained within a DockPanel cannot be sized using
 * percentages. Setting a child widget's height to <code>100%</code> will
 * <em>NOT</em> cause the child to fill the available height.
 * </p>
 * 
 * <p>
 * If you need to work around these limitations, use {@link DockLayoutPanel}
 * instead, but understand that it is not a drop in replacement for this class.
 * It requires standards mode, and is most easily used under a
 * {@link RootLayoutPanel} (as opposed to a {@link RootPanel}).
 * </p>
 * 
 * <p>
 * <img class='gallery' src='doc-files/DockPanel.png'/>
 * </p>
 * 
 * @see DockLayoutPanel
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
    public String hAlign = ALIGN_DEFAULT.getTextAlignString();
    public String height = "";
    public Element td;
    public String vAlign = ALIGN_TOP.getVerticalAlignString();
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
   * Specifies that a widget be added at the beginning of the line direction
   * for the layout.
   */
  public static final DockLayoutConstant LINE_START = new DockLayoutConstant();

  /**
   * Specifies that a widget be added at the end of the line direction
   * for the layout.
   */
  public static final DockLayoutConstant LINE_END = new DockLayoutConstant();

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

  /**
   * Generate a debug ID for the {@link Widget} given the direction and number
   * of occurrences of the direction.
   * 
   * @param direction the direction of the widget
   * @param count the number of widgets in that direction
   */
  private static String generateDebugId(DockLayoutConstant direction, int count) {
    if (direction == NORTH) {
      return "north" + count;
    } else if (direction == SOUTH) {
      return "south" + count;
    } else if (direction == WEST) {
      return "west" + count;
    } else if (direction == EAST) {
      return "east" + count;
    } else if (direction == LINE_START) {
        return "linestart" + count;      
    } else if (direction == LINE_END) {
        return "lineend" + count;      
    } else {    
      return "center";
    }
  }
  
  private HorizontalAlignmentConstant horzAlign = ALIGN_DEFAULT;
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
  
  /**
   * Overloaded version for IsWidget.
   * 
   * @see #add(Widget,DockLayoutConstant)
   */
  public void add(IsWidget widget, DockLayoutConstant direction) {
   this.add(widget.asWidget(), direction);
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
      setCellHorizontalAlignment(data.td, align);
    }
  }

  @Override
  public void setCellVerticalAlignment(Widget w, VerticalAlignmentConstant align) {
    LayoutData data = (LayoutData) w.getLayoutData();
    data.vAlign = align.getVerticalAlignString();
    if (data.td != null) {
      setCellVerticalAlignment(data.td, align);
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
   * {@link DockPanel} supports adding more than one cell in a direction, so an
   * integer will be appended to the end of the debug id. For example, the first
   * north cell is labeled "north1", the second is "north2", and the third is
   * "north3".
   * 
   * This widget recreates its structure every time a {@link Widget} is added,
   * so you must call this method after adding a new {@link Widget} or all debug
   * IDs will be lost.
   * 
   * <p>
   * <b>Affected Elements:</b>
   * <ul>
   * <li>-center = the center cell.</li>
   * <li>-north# = the northern cell.</li>
   * <li>-south# = the southern cell.</li>
   * <li>-east# = the eastern cell.</li>
   * <li>-west# = the western cell.</li>
   * </ul>
   * </p>
   * 
   * @see UIObject#onEnsureDebugId(String)
   */
  @Override
  protected void onEnsureDebugId(String baseID) {
    super.onEnsureDebugId(baseID);

    Map<DockLayoutConstant, Integer> dirCount = new HashMap<DockLayoutConstant, Integer>();
    Iterator<Widget> it = getChildren().iterator();
    while (it.hasNext()) {
      Widget child = it.next();
      DockLayoutConstant dir = ((LayoutData) child.getLayoutData()).direction;

      // Get a debug id
      Integer countObj = dirCount.get(dir);
      int count = countObj == null ? 1 : countObj.intValue();
      String debugID = generateDebugId(dir, count);
      ensureDebugId(DOM.getParent(child.getElement()), baseID, debugID);

      // Increment the count
      dirCount.put(dir, count + 1);
    }
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
      } else if ((dir == EAST) || (dir == WEST) || (dir == LINE_START) || (dir == LINE_END)) {
        ++colCount;
      }
    }

    TmpRow[] rows = new TmpRow[rowCount];
    for (int i = 0; i < rowCount; ++i) {
      rows[i] = new TmpRow();
      rows[i].tr = DOM.createTR();
      DOM.appendChild(bodyElem, rows[i].tr);
    }

    int logicalLeftCol = 0, logicalRightCol = colCount - 1;
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
        DOM.setElementPropertyInt(td, "colSpan", logicalRightCol - logicalLeftCol + 1);
        ++northRow;
      } else if (layout.direction == SOUTH) {
        DOM.insertChild(rows[southRow].tr, td, rows[southRow].center);
        DOM.appendChild(td, child.getElement());
        DOM.setElementPropertyInt(td, "colSpan", logicalRightCol - logicalLeftCol + 1);
        --southRow;
      } else if (layout.direction == CENTER) {
        // Defer adding the center widget, so that it can be added after all
        // the others are complete.
        centerTd = td; 
      } else if (shouldAddToLogicalLeftOfTable(layout.direction)) {
        TmpRow row = rows[northRow];
        DOM.insertChild(row.tr, td, row.center++);
        DOM.appendChild(td, child.getElement());
        DOM.setElementPropertyInt(td, "rowSpan", southRow - northRow + 1);
        ++logicalLeftCol;
      } else if (shouldAddToLogicalRightOfTable(layout.direction)) {
        TmpRow row = rows[northRow];
        DOM.insertChild(row.tr, td, row.center);
        DOM.appendChild(td, child.getElement());
        DOM.setElementPropertyInt(td, "rowSpan", southRow - northRow + 1);
        --logicalRightCol;
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
  
  private boolean shouldAddToLogicalLeftOfTable(DockLayoutConstant widgetDirection) {
    
    assert (widgetDirection == LINE_START || widgetDirection == LINE_END || 
        widgetDirection == EAST || widgetDirection == WEST);
    
    // In a bidi-sensitive environment, adding a widget to the logical left
    // column (think DOM order) means that it will be displayed at the start
    // of the line direction for the current layout. This is because HTML
    // tables are bidi-sensitive; the column order switches depending on 
    // the line direction.
    if (widgetDirection == LINE_START) {  
      return true;
    }
    
    if (LocaleInfo.getCurrentLocale().isRTL()) {
      // In an RTL layout, the logical left columns will be displayed on the right hand 
      // side. When the direction for the widget is EAST, adding the widget to the logical 
      // left columns will have the desired effect of displaying the widget on the 'eastern' 
      // side of the screen.
      return (widgetDirection == EAST);          
    }
    
    // In an LTR layout, the logical left columns are displayed on the left hand
    // side. When the direction for the widget is WEST, adding the widget to the
    // logical left columns will have the desired effect of displaying the widget on the
    // 'western' side of the screen.
    return (widgetDirection == WEST);
  }

  private boolean shouldAddToLogicalRightOfTable(DockLayoutConstant widgetDirection) {
    
    // See comments for shouldAddToLogicalLeftOfTable for clarification
    
    assert (widgetDirection == LINE_START || widgetDirection == LINE_END ||
        widgetDirection == EAST || widgetDirection == WEST);

    if (widgetDirection == LINE_END) {
      return true;
    }

    if (LocaleInfo.getCurrentLocale().isRTL()) {   
      return (widgetDirection == WEST);
    }
    
    return (widgetDirection == EAST);
  }
}
