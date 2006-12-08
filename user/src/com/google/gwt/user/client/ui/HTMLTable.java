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
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
import com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant;

import java.util.Iterator;

/**
 * HTMLTable contains the common table algorithms for
 * {@link com.google.gwt.user.client.ui.Grid} and
 * {@link com.google.gwt.user.client.ui.FlexTable}.
 * <p>
 * <img class='gallery' src='Table.png'/>
 * </p>
 */
public abstract class HTMLTable extends Panel implements SourcesTableEvents {
  /**
   * This class contains methods used to format a table's cells.
   */
  public class CellFormatter {
    /**
     * Adds a style to the specified cell.
     * 
     * @param row the cell's row
     * @param column the cell's column
     * @param styleName the style name to be added
     * @see UIObject#addStyleName(String)
     */
    public void addStyleName(int row, int column, String styleName) {
      prepareCell(row, column);
      Element td = getCellElement(bodyElem, row, column);
      UIObject.setStyleName(td, styleName, true);
    }

    /**
     * Gets the TD element representing the specified cell.
     * 
     * @param row the row of the cell to be retrieved
     * @param column the column of the cell to be retrieved
     * @return the column's TD element
     * @throws IndexOutOfBoundsException
     */
    public Element getElement(int row, int column) {
      checkCellBounds(row, column);
      return getCellElement(bodyElem, row, column);
    }

    /**
     * Gets a style from a specified row.
     * 
     * @param row the row of the cell which the style while be added to
     * @param column the column of the cell which the style will be added to
     * @see UIObject#getStyleName()
     * @return returns the style name
     * @throws IndexOutOfBoundsException
     */
    public String getStyleName(int row, int column) {
      return DOM.getAttribute(getElement(row, column), "className");
    }

    /**
     * Determines whether or not this cell is visible.
     * 
     * @param row the row of the cell whose visibility is to be set
     * @param column the column of the cell whose visibility is to be set
     * @return <code>true</code> if the object is visible
     */
    public boolean isVisible(int row, int column) {
      Element e = getElement(row, column);
      return UIObject.isVisible(e);
    }

    /**
     * Removes a style from the specified cell.
     * 
     * @param row the cell's row
     * @param column the cell's column
     * @param styleName the style name to be removed
     * @see UIObject#removeStyleName(String)
     * @throws IndexOutOfBoundsException
     */
    public void removeStyleName(int row, int column, String styleName) {
      checkCellBounds(row, column);
      Element td = getCellElement(bodyElem, row, column);
      UIObject.setStyleName(td, styleName, false);
    }

    /**
     * Sets the horizontal and vertical alignment of the specified cell's
     * contents.
     * 
     * @param row the row of the cell whose alignment is to be set
     * @param column the cell whose alignment is to be set
     * @param hAlign the cell's new horizontal alignment as specified in
     *          {@link HasHorizontalAlignment}
     * @param vAlign the cell's new vertical alignment as specified in
     *          {@link HasVerticalAlignment}
     * @throws IndexOutOfBoundsException
     */
    public void setAlignment(int row, int column,
        HorizontalAlignmentConstant hAlign, VerticalAlignmentConstant vAlign) {
      setHorizontalAlignment(row, column, hAlign);
      setVerticalAlignment(row, column, vAlign);
    }

    /**
     * Sets the height of the specified cell.
     * 
     * @param row the row of the cell whose height is to be set
     * @param column the cell whose height is to be set
     * @param height the cell's new height, in CSS units
     * @throws IndexOutOfBoundsException
     */
    public void setHeight(int row, int column, String height) {
      prepareCell(row, column);
      Element elem = getCellElement(bodyElem, row, column);
      DOM.setAttribute(elem, "height", height);
    }

    /**
     * Sets the horizontal alignment of the specified cell.
     * 
     * @param row the row of the cell whose alignment is to be set
     * @param column the cell whose alignment is to be set
     * @param align the cell's new horizontal alignment as specified in
     *          {@link HasHorizontalAlignment}.
     * @throws IndexOutOfBoundsException
     */
    public void setHorizontalAlignment(int row, int column,
        HorizontalAlignmentConstant align) {
      prepareCell(row, column);
      Element elem = getCellElement(bodyElem, row, column);
      DOM.setAttribute(elem, "align", align.getTextAlignString());
    }

    /**
     * Sets the style name associated with the specified cell.
     * 
     * @param row the row of the cell whose style name is to be set
     * @param column the column of the cell whose style name is to be set
     * @param styleName the new style name
     * @see UIObject#setStyleName(String)
     * @throws IndexOutOfBoundsException
     */
    public void setStyleName(int row, int column, String styleName) {
      prepareCell(row, column);
      setAttr(row, column, "className", styleName);
    }

    /**
     * Sets the vertical alignment of the specified cell.
     * 
     * @param row the row of the cell whose alignment is to be set
     * @param column the cell whose alignment is to be set
     * @param align the cell's new vertical alignment as specified in
     *          {@link HasVerticalAlignment}.
     * @throws IndexOutOfBoundsException
     */
    public void setVerticalAlignment(int row, int column,
        VerticalAlignmentConstant align) {
      prepareCell(row, column);
      DOM.setStyleAttribute(getCellElement(bodyElem, row, column),
          "verticalAlign", align.getVerticalAlignString());
    }

    /**
     * Sets whether this cell is visible via the display style property. The
     * other cells in the row will all shift left to fill the cell's space. So,
     * for example a table with (0,1,2) will become (1,2) if cell 1 is hidden.
     * 
     * @param row the row of the cell whose visibility is to be set
     * @param column the column of the cell whose visibility is to be set
     * @param visible <code>true</code> to show the cell, <code>false</code>
     *          to hide it
     */
    public void setVisible(int row, int column, boolean visible) {
      Element e = ensureElement(row, column);
      UIObject.setVisible(e, visible);
    }

    /**
     * Sets the width of the specified cell.
     * 
     * @param row the row of the cell whose width is to be set
     * @param column the cell whose width is to be set
     * @param width the cell's new width, in CSS units
     * @throws IndexOutOfBoundsException
     */
    public void setWidth(int row, int column, String width) {
      // Give the subclass a chance to prepare the cell.
      prepareCell(row, column);
      DOM.setAttribute(getCellElement(bodyElem, row, column), "width", width);
    }

    /**
     * Sets whether the specified cell will allow word wrapping of its contents.
     * 
     * @param row the row of the cell whose word-wrap is to be set
     * @param column the cell whose word-wrap is to be set
     * @param wrap <code>false </code> to disable word wrapping in this cell
     * @throws IndexOutOfBoundsException
     */
    public void setWordWrap(int row, int column, boolean wrap) {
      prepareCell(row, column);
      String wrapValue = wrap ? "" : "nowrap";
      DOM.setStyleAttribute(getElement(row, column), "whiteSpace", wrapValue);
    }

    /**
     * Gets the element associated with a cell. If it does not exist and the
     * subtype allows creation of elements, creates it.
     * 
     * @param row the cell's row
     * @param column the cell's column
     * @return the cell's element
     * @throws IndexOutOfBoundsException
     */
    protected Element ensureElement(int row, int column) {
      prepareCell(row, column);
      return getCellElement(bodyElem, row, column);
    }

    /**
     * Convenience methods to get an attribute on a cell.
     * 
     * @param row cell's row
     * @param column cell's column
     * @param attr attribute to get
     * @return the attribute's value
     * @throws IndexOutOfBoundsException
     */
    protected String getAttr(int row, int column, String attr) {
      Element elem = getElement(row, column);
      return DOM.getAttribute(elem, attr);
    }

    /**
     * Convenience methods to set an attribute on a cell.
     * 
     * @param row cell's row
     * @param column cell's column
     * @param attrName attribute to set
     * @param value value to set
     * @throws IndexOutOfBoundsException
     */
    protected void setAttr(int row, int column, String attrName, String value) {
      Element elem = ensureElement(row, column);
      DOM.setAttribute(elem, attrName, value);
    }

    /**
     * Native method to get a cell's element.
     * 
     * @param table the table element
     * @param row the row of the cell
     * @param col the column of the cell
     * @return the element
     */

    private native Element getCellElement(Element table, int row, int col) /*-{
      var out = table.rows[row].cells[col];
      return (out == null ? null : out);
     }-*/;

    /**
     * Gets the TD element representing the specified cell unsafely (meaning
     * that it doesn't ensure that the row and column are valid).
     * 
     * @param row the row of the cell to be retrieved
     * @param column the column of the cell to be retrieved
     * @return the column's TD element
     */
    private Element getRawElement(int row, int column) {
      return getCellElement(bodyElem, row, column);
    }
  }

  /**
   * This class contains methods used to format a table's columns. It is limited
   * by the support cross-browser HTML support for column formatting.
   */
  public class ColumnFormatter {
    protected Element columnGroup;

    /**
     * Adds a style to the specified column.
     * 
     * @param col the col to which the style while be added
     * @param styleName the style name to be added
     * @see UIObject#addStyleName(String)
     * @throws IndexOutOfBoundsException
     */
    public void addStyleName(int col, String styleName) {
      UIObject.setStyleName(ensureColumn(col), styleName, true);
    }

    /**
     * Gets a style from a specified column.
     * 
     * @param column the column to which the style while be added
     * @see UIObject#getStyleName()
     * @throws IndexOutOfBoundsException
     * @return the style name
     */
    public String getStyleName(int column) {
      return DOM.getAttribute(ensureColumn(column), "className");
    }

    /**
     * Removes a style from the specified column.
     * 
     * @param column the column to which the style while be removed
     * @param styleName the style name to be removed
     * @see UIObject#removeStyleName(String)
     * @throws IndexOutOfBoundsException
     */
    public void removeStyleName(int column, String styleName) {
      UIObject.setStyleName(ensureColumn(column), styleName, false);
    }

    /**
     * Sets the style name associated with the specified column.
     * 
     * @param column the column whose style name is to be set
     * @param styleName the new style name
     * @see UIObject#setStyleName(String)
     * @throws IndexOutOfBoundsException
     */
    public void setStyleName(int column, String styleName) {
      Element elem = ensureColumn(column);
      DOM.setAttribute(elem, "className", styleName);
    }

    /**
     * Sets the width of the specified column.
     * 
     * @param column the column of the cell whose width is to be set
     * @param width the cell's new width, in percentage or pixel units
     * @throws IndexOutOfBoundsException
     */
    public void setWidth(int column, String width) {
      DOM.setAttribute(ensureColumn(column), "width", width);
    }

    private Element ensureColumn(int col) {
      prepareColumn(col);

      if (columnGroup == null) {
        columnGroup = DOM.createElement("colgroup");
        DOM.insertChild(getElement(), columnGroup, 0);
      }

      int num = DOM.getChildCount(columnGroup);
      if (num <= col) {
        Element colElement = null;
        for (int i = num; i <= col; i++) {
          colElement = DOM.createElement("col");
          DOM.appendChild(columnGroup, colElement);
        }
        return colElement;
      }
      return DOM.getChild(columnGroup, col);
    }
  }

  /**
   * This class contains methods used to format a table's rows.
   */
  public class RowFormatter {

    /**
     * Adds a style to the specified row.
     * 
     * @param row the row to which the style while be added
     * @param styleName the style name to be added
     * @see UIObject#addStyleName(String)
     * @throws IndexOutOfBoundsException
     */
    public void addStyleName(int row, String styleName) {
      UIObject.setStyleName(ensureElement(row), styleName, true);
    }

    /**
     * Gets the TR element representing the specified row.
     * 
     * @param row the row whose TR element is to be retrieved
     * @return the row's TR element
     * @throws IndexOutOfBoundsException
     */
    public Element getElement(int row) {
      checkRowBounds(row);
      return getRow(bodyElem, row);
    }

    /**
     * Gets a style from a specified row.
     * 
     * @param row the row to which the style while be added
     * @see UIObject#getStyleName()
     * @throws IndexOutOfBoundsException
     * @return the style name
     */
    public String getStyleName(int row) {
      return DOM.getAttribute(getElement(row), "className");
    }

    /**
     * Determines whether or not this row is visible via the display style
     * attribute.
     * 
     * @param row the row whose visibility is to be set
     * @return <code>true</code> if the row is visible
     */
    public boolean isVisible(int row) {
      Element e = getElement(row);
      return UIObject.isVisible(e);
    }

    /**
     * Removes a style from the specified row.
     * 
     * @param row the row to which the style while be removed
     * @param styleName the style name to be removed
     * @see UIObject#removeStyleName(String)
     * @throws IndexOutOfBoundsException
     */
    public void removeStyleName(int row, String styleName) {
      UIObject.setStyleName(getElement(row), styleName, false);
    }

    /**
     * Sets the style name associated with the specified row.
     * 
     * @param row the row whose style name is to be set
     * @param styleName the new style name
     * @see UIObject#setStyleName(String)
     * @throws IndexOutOfBoundsException
     */
    public void setStyleName(int row, String styleName) {
      Element elem = ensureElement(row);
      DOM.setAttribute(elem, "className", styleName);
    }

    /**
     * Sets the vertical alignment of the specified row.
     * 
     * @param row the row whose alignment is to be set
     * @param align the row's new vertical alignment as specified in
     *          {@link HasVerticalAlignment}
     * @throws IndexOutOfBoundsException
     */
    public void setVerticalAlign(int row, VerticalAlignmentConstant align) {
      DOM.setStyleAttribute(ensureElement(row), "verticalAlign",
          align.getVerticalAlignString());
    }

    /**
     * Sets whether this row is visible.
     * 
     * @param row the row whose visibility is to be set
     * @param visible <code>true</code> to show the row, <code>false</code>
     *          to hide it
     */
    public void setVisible(int row, boolean visible) {
      Element e = ensureElement(row);
      UIObject.setVisible(e, visible);
    }

    /**
     * Ensure the TR element representing the specified row exists for
     * subclasses that allow dynamic addition of elements.
     * 
     * @param row the row whose TR element is to be retrieved
     * @return the row's TR element
     * @throws IndexOutOfBoundsException
     */
    protected Element ensureElement(int row) {
      prepareRow(row);
      return getRow(bodyElem, row);
    }

    protected native Element getRow(Element elem, int row)/*-{
      return elem.rows[row];
     }-*/;

    /**
     * Convenience methods to set an attribute on a row.
     * 
     * @param row cell's row
     * @param attrName attribute to set
     * @param value value to set
     * @throws IndexOutOfBoundsException
     */
    protected void setAttr(int row, String attrName, String value) {
      Element elem = ensureElement(row);
      DOM.setAttribute(elem, attrName, value);
    }
  }

  /**
   * Attribute name to store hash.
   */
  private static final String HASH_ATTR = "__hash";

  /**
   * Table's body.
   */
  private final Element bodyElem;

  /**
   * Current cell formatter.
   */
  private CellFormatter cellFormatter;

  /**
   * Column Formatter.
   */
  private ColumnFormatter columnFormatter;

  /**
   * Current row formatter.
   */
  private RowFormatter rowFormatter;

  /**
   * Table element.
   */
  private final Element tableElem;

  /**
   * Current table listener.
   */
  private TableListenerCollection tableListeners;

  /**
   * The element map, used to quickly look up the Widget in a particular cell.
   * We have to use a map here, because hanging references to Widgets from
   * Elements would cause memory leaks.
   */
  private final FastStringMap widgetMap = new FastStringMap();

  /**
   * Create a new empty HTML Table.
   */
  public HTMLTable() {
    tableElem = DOM.createTable();
    bodyElem = DOM.createTBody();
    DOM.appendChild(tableElem, bodyElem);
    setElement(tableElem);
    sinkEvents(Event.ONCLICK);
  }

  /**
   * Adds a listener to the current table.
   * 
   * @param listener listener to add
   */
  public void addTableListener(TableListener listener) {
    if (tableListeners == null) {
      tableListeners = new TableListenerCollection();
    }
    tableListeners.add(listener);
  }

  /**
   * Removes all widgets from this table, but does not remove other HTML or text
   * contents of cells.
   */
  public void clear() {
    for (int row = 0; row < getRowCount(); ++row) {
      for (int col = 0; col < getCellCount(row); ++col) {
        Widget child = getWidget(row, col);
        if (child != null) {
          removeWidget(child);
        }
      }
    }
    assert (widgetMap.size() == 0);
  }

  /**
   * Clears the given row and column. If it contains a Widget, it will be
   * removed from the table. If not, its contents will simply be cleared.
   * 
   * @param row the widget's column
   * @param column the widget's column
   * @return true if a widget was removed
   * @throws IndexOutOfBoundsException
   */
  public boolean clearCell(int row, int column) {
    Element td = getCellFormatter().getElement(row, column);
    return internalClearCell(td, true);
  }

  /**
   * Gets the number of cells in a given row.
   * 
   * @param row the row whose cells are to be counted
   * @return the number of cells present in the row
   */
  public abstract int getCellCount(int row);

  /**
   * Gets the {@link CellFormatter} associated with this table. Use casting to
   * get subclass-specific functionality
   * 
   * @return this table's cell formatter
   */
  public CellFormatter getCellFormatter() {
    return cellFormatter;
  }

  /**
   * Gets the amount of padding that is added around all cells.
   * 
   * @return the cell padding, in pixels
   */
  public int getCellPadding() {
    return DOM.getIntAttribute(tableElem, "cellPadding");
  }

  /**
   * Gets the amount of spacing that is added around all cells.
   * 
   * @return the cell spacing, in pixels
   */
  public int getCellSpacing() {
    return DOM.getIntAttribute(tableElem, "cellSpacing");
  }

  public ColumnFormatter getColumnFormatter() {
    return columnFormatter;
  }

  /**
   * Gets the HTML contents of the specified cell.
   * 
   * @param row the cell's row
   * @param column the cell's column
   * @return the cell's HTML contents
   * @throws IndexOutOfBoundsException
   */
  public String getHTML(int row, int column) {
    return DOM.getInnerHTML(cellFormatter.getElement(row, column));
  }

  /**
   * Gets the number of rows present in this table.
   * 
   * @return the table's row count
   */
  public abstract int getRowCount();

  /**
   * Gets the RowFormatter associated with this table.
   * 
   * @return the table's row formatter
   */
  public RowFormatter getRowFormatter() {
    return rowFormatter;
  }

  /**
   * Gets the text within the specified cell.
   * 
   * @param row the cell's row
   * @param column the cell's column
   * @return the cell's text contents
   * @throws IndexOutOfBoundsException
   */
  public String getText(int row, int column) {
    checkCellBounds(row, column);
    Element e = cellFormatter.getElement(row, column);
    return DOM.getInnerText(e);
  }

  /**
   * Gets the widget in the specified cell.
   * 
   * @param row the cell's row
   * @param column the cell's column
   * @return the widget in the specified cell, or <code>null</code> if none is
   *         present
   * @throws IndexOutOfBoundsException
   */
  public Widget getWidget(int row, int column) {
    checkCellBounds(row, column);
    Object key = computeKey(row, column);
    if (key == null) {
      return null;
    } else {
      return (Widget) widgetMap.get(key);
    }
  }

  /**
   * Determines whether the specified cell exists.
   * 
   * @param row the cell's row
   * @param column the cell's column
   * @return <code>true</code> if the specified cell exists
   */
  public boolean isCellPresent(int row, int column) {
    if ((row >= getRowCount()) && (row < 0)) {
      return false;
    }
    if ((column < 0) || (column >= getCellCount(row))) {
      return false;
    } else {
      return true;
    }
  }

  /**
   * Returns an iterator containing all the widgets in this table.
   * 
   * @return the iterator
   */
  public Iterator iterator() {
    return widgetMap.values().iterator();
  }

  /**
   * Method to process events generated from the browser.
   * 
   * @param event the generated event
   */
  public void onBrowserEvent(Event event) {
    switch (DOM.eventGetType(event)) {
      case Event.ONCLICK: {
        if (tableListeners != null) {
          // Find out which cell was actually clicked.
          Element td = getEventTargetCell(event);
          if (td == null) {
            return;
          }
          Element tr = DOM.getParent(td);
          Element body = DOM.getParent(tr);
          int row = DOM.getChildIndex(body, tr);
          int column = DOM.getChildIndex(tr, td);
          // Fire the event.
          tableListeners.fireCellClicked(this, row, column);
        }
        break;
      }
      default: {
        // Do nothing
      }
    }
  }

  /**
   * Remove the specified widget from the table.
   * 
   * @param widget widget to remove
   * @return was the widget removed from the table.
   */
  public boolean remove(Widget widget) {
    // Make sure the Widget is actually contained in this table.
    if (widget.getParent() != this) {
      return false;
    }
    // Get the row and column of the cell containing this widget.
    removeWidget(widget);
    return true;
  }

  /**
   * Removes the specified table listener.
   * 
   * @param listener listener to remove
   */
  public void removeTableListener(TableListener listener) {
    if (tableListeners != null) {
      tableListeners.remove(listener);
    }
  }

  /**
   * Sets the width of the table's border. This border is displayed around all
   * cells in the table.
   * 
   * @param width the width of the border, in pixels
   */
  public void setBorderWidth(int width) {
    DOM.setAttribute(tableElem, "border", "" + width);
  }

  /**
   * Sets the amount of padding to be added around all cells.
   * 
   * @param padding the cell padding, in pixels
   */
  public void setCellPadding(int padding) {
    DOM.setIntAttribute(tableElem, "cellPadding", padding);
  }

  /**
   * Sets the amount of spacing to be added around all cells.
   * 
   * @param spacing the cell spacing, in pixels
   */
  public void setCellSpacing(int spacing) {
    DOM.setIntAttribute(tableElem, "cellSpacing", spacing);
  }

  /**
   * Sets the HTML contents of the specified cell.
   * 
   * @param row the cell's row
   * @param column the cell's column
   * @param html the cell's HTML contents
   * @throws IndexOutOfBoundsException
   */
  public void setHTML(int row, int column, String html) {
    prepareCell(row, column);
    Element td = cleanCell(row, column, html == null);
    if (html != null) {
      DOM.setInnerHTML(td, html);
    }
  }

  /**
   * Sets the text within the specified cell.
   * 
   * @param row the cell's row
   * @param column cell's column
   * @param text the cell's text contents
   * @throws IndexOutOfBoundsException
   */
  public void setText(int row, int column, String text) {
    prepareCell(row, column);
    Element td;
    td = cleanCell(row, column, text == null);
    if (text != null) {
      DOM.setInnerText(td, text);
    }
  }

  /**
   * Sets the widget within the specified cell.
   * <p>
   * Inherited implementations may either throw IndexOutOfBounds exception if
   * the cell does not exist, or allocate a new cell to store the content.
   * </p>
   * <p>
   * FlexTable will automatically allocate the cell at the correct location and
   * then set the widget. Grid will set the widget if and only if the cell is
   * within the Grid's bounding box.
   * </p>
   * 
   * @param widget The widget to be added
   * @param row the cell's row
   * @param column the cell's column
   * @throws IndexOutOfBoundsException
   */
  public void setWidget(int row, int column, Widget widget) {
    prepareCell(row, column);
    if (widget != null) {
      // Call this early to ensure that the table doesn't end up partially
      // constructed when an exception is thrown from adopt().
      widget.removeFromParent();

      // Attach it to the cell's TD.
      Element td = cleanCell(row, column, true);

      // Add the widget to the map.
      String hash = Integer.toString(widget.hashCode());
      Element e = widget.getElement();
      DOM.setAttribute(e, HASH_ATTR, hash);
      widgetMap.put(hash, widget);

      // Set the widget's parent.
      adopt(widget, td);
    }
  }

  /**
   * Bounds checks that the cell exists at the specified location.
   * 
   * @param row cell's row
   * @param column cell's column
   * @throws IndexOutOfBoundsException
   */
  protected void checkCellBounds(int row, int column) {
    checkRowBounds(row);
    if (column < 0) {
      throw new IndexOutOfBoundsException("Column " + column
          + " must be non-negative: " + column);
    }
    int cellSize = getCellCount(row);
    if (cellSize <= column) {
      throw new IndexOutOfBoundsException("Column index: " + column
          + ", Column size: " + getCellCount(row));
    }
  }

  /**
   * Checks that the row is within the correct bounds.
   * 
   * @param row row index to check
   * @throws IndexOutOfBoundsException
   */
  protected void checkRowBounds(int row) {
    int rowSize = getRowCount();
    if ((row >= rowSize) || (row < 0)) {
      throw new IndexOutOfBoundsException("Row index: " + row + ", Row size: "
          + rowSize);
    }
  }

  /**
   * Creates a new cell. Override this method if the cell should have initial
   * contents.
   * 
   * @return the newly created TD
   */
  protected Element createCell() {
    return DOM.createTD();
  }

  /**
   * Gets the table's TBODY element.
   * 
   * @return the TBODY element
   */
  protected Element getBodyElement() {
    return bodyElem;
  }

  /**
   * Directly ask the underlying DOM what the cell count on the given row is.
   * 
   * @param row the row
   * @return number of columns in the row
   */
  protected native int getDOMCellCount(Element elem, int row) /*-{
    return elem.rows[row].cells.length;
   }-*/;

  /**
   * Directly ask the underlying DOM what the cell count on the given row is.
   * 
   * @param row the row
   * @return number of columns in the row
   */
  protected int getDOMCellCount(int row) {
    return getDOMCellCount(bodyElem, row);
  }

  /**
   * Directly ask the underlying DOM what the row count is.
   * 
   * @return Returns the number of rows in the table
   */
  protected int getDOMRowCount() {
    return getDOMRowCount(bodyElem);
  }

  protected native int getDOMRowCount(Element elem) /*-{
    return elem.rows.length;
   }-*/;

  /**
   * Determines the TD associated with the specified event.
   * 
   * @param event the event to be queried
   * @return the TD associated with the event, or <code>null</code> if none is
   *         found.
   */
  protected Element getEventTargetCell(Event event) {
    Element td = DOM.eventGetTarget(event);
    for (; td != null; td = DOM.getParent(td)) {
      // If it's a TD, it might be the one we're looking for.
      if (DOM.getAttribute(td, "tagName").equalsIgnoreCase("td")) {
        // Make sure it's directly a part of this table before returning it.
        Element tr = DOM.getParent(td);
        Element body = DOM.getParent(tr);
        if (DOM.compare(body, bodyElem)) {
          return td;
        }
      }
      // If we run into this table's body, we're out of options.
      if (DOM.compare(td, bodyElem)) {
        return null;
      }
    }
    return null;
  }

  /**
   * Inserts a new cell into the specified row.
   * 
   * @param row the row into which the new cell will be inserted
   * @param column the column before which the cell will be inserted
   * @throws IndexOutOfBoundsException
   */
  protected void insertCell(int row, int column) {
    Element tr = rowFormatter.getRow(bodyElem, row);
    Element td = createCell();
    DOM.insertChild(tr, td, column);
  }

  /**
   * Inserts a number of cells before the specified cell.
   * 
   * @param row the row into which the new cells will be inserted
   * @param column the column before which the new cells will be inserted
   * @param count number of cells to be inserted
   * @throws IndexOutOfBoundsException
   */
  protected void insertCells(int row, int column, int count) {
    Element tr = rowFormatter.getRow(bodyElem, row);
    for (int i = column; i < column + count; i++) {
      Element td = createCell();
      DOM.insertChild(tr, td, i);
    }
  }

  /**
   * Inserts a new row into the table.
   * 
   * @param beforeRow the index before which the new row will be inserted
   * @return the index of the newly-created row
   * @throws IndexOutOfBoundsException
   */
  protected int insertRow(int beforeRow) {
    // Specifically allow the row count as an insert position.
    if (beforeRow != getRowCount()) {
      checkRowBounds(beforeRow);
    }
    Element tr = DOM.createTR();
    DOM.insertChild(bodyElem, tr, beforeRow);
    return beforeRow;
  }

  /**
   * Does actual clearing, used by clearCell and cleanCell. All HTMLTable
   * methods should use internalClearCell rather than clearCell, as clearCell
   * may be overridden in subclasses to format an empty cell.
   * 
   * @param td element to clear
   * @param clearInnerHTML should the cell's inner html be cleared?
   * @return returns whether a widget was cleared
   */
  protected boolean internalClearCell(Element td, boolean clearInnerHTML) {
    Element maybeChild = DOM.getFirstChild(td);
    Widget widget = null;
    if (maybeChild != null) {
      widget = getWidget(maybeChild);
    }
    if (widget != null) {
      // If there is a widget, remove it.
      removeWidget(widget);
      return true;
    } else {
      // Otherwise, simply clear whatever text and/or HTML may be there.
      if (clearInnerHTML) {
        DOM.setInnerHTML(td, "");
      }
      return false;
    }
  }

  /**
   * Subclasses must implement this method. It allows them to decide what to do
   * just before a cell is accessed. If the cell already exists, this method
   * must do nothing. Otherwise, a subclass must either ensure that the cell
   * exists or throw an {@link IndexOutOfBoundsException}.
   * 
   * @param row the cell's row
   * @param column the cell's column
   */
  protected abstract void prepareCell(int row, int column);

  /**
   * Subclasses can implement this method. It allows them to decide what to do
   * just before a column is accessed. For classes, such as
   * <code>FlexTable</code>, that do not have a concept of a global column
   * length can ignore this method.
   * 
   * @param column the cell's column
   * @throws IndexOutOfBoundsException
   */
  protected void prepareColumn(int column) {
    // By default, do nothing.
  }

  /**
   * Subclasses must implemea whaccessed. If the row already exists, this method
   * must do nothing. Otherwise, a subclass must either ensure that the row
   * exists or throw an {@link IndexOutOfBoundsException}.
   * 
   * @param row the cell's row
   */
  protected abstract void prepareRow(int row);

  /**
   * Removes the specified cell from the table.
   * 
   * @param row the row of the cell to remove
   * @param column the column of cell to remove
   * @throws IndexOutOfBoundsException
   */
  protected void removeCell(int row, int column) {
    checkCellBounds(row, column);
    Element td = cleanCell(row, column, false);
    Element tr = rowFormatter.getRow(bodyElem, row);
    DOM.removeChild(tr, td);
  }

  /**
   * Removes the specified row from the table.
   * 
   * @param row the index of the row to be removed
   * @throws IndexOutOfBoundsException
   */
  protected void removeRow(int row) {
    int columnCount = getCellCount(row);
    for (int column = 0; column < columnCount; ++column) {
      cleanCell(row, column, false);
    }
    DOM.removeChild(bodyElem, rowFormatter.getRow(bodyElem, row));
  }

  /**
   * Sets the table's CellFormatter.
   * 
   * @param cellFormatter the table's cell formatter
   */
  protected void setCellFormatter(CellFormatter cellFormatter) {
    this.cellFormatter = cellFormatter;
  }

  protected void setColumnFormatter(ColumnFormatter formatter) {
    columnFormatter = formatter;
  }

  /**
   * Sets the table's RowFormatter.
   * 
   * @param rowFormatter the table's row formatter
   */
  protected void setRowFormatter(RowFormatter rowFormatter) {
    this.rowFormatter = rowFormatter;
  }

  /**
   * Gets the widget map, only should be used by testing code.
   * 
   * @return the internal widget map
   */
  FastStringMap getWidgetMap() {
    return widgetMap;
  }

  /**
   * Removes any widgets, text, and HTML within the cell. This method assumes
   * that the requested cell already exists.
   * 
   * @param row the cell's row
   * @param column the cell's column
   * @param clearInnerHTML should the cell's inner html be cleared?
   * @return element that has been cleaned
   */
  private Element cleanCell(int row, int column, boolean clearInnerHTML) {
    // Clear whatever is in the cell.
    Element td = getCellFormatter().getRawElement(row, column);
    internalClearCell(td, clearInnerHTML);
    return td;
  }

  /**
   * Gets the key associated with the cell. This key is used within the widget
   * map.
   * 
   * @param row the cell's row
   * @param column the cell's column
   * @return the associated key
   */
  private Object computeKey(int row, int column) {
    Element e = cellFormatter.getRawElement(row, column);
    Element child = DOM.getFirstChild(e);
    if (child == null) {
      return null;
    } else {
      return computeKeyForElement(child);
    }
  }

  /**
   * Computes the key to lookup the Widget.
   * 
   * @param widgetElement
   * @return returns the key
   */
  private String computeKeyForElement(Element widgetElement) {
    return DOM.getAttribute(widgetElement, HASH_ATTR);
  }

  /**
   * Gets the Widget associated with the element.
   * 
   * @param widgetElement widget's element
   * @return the widget
   */
  private Widget getWidget(Element widgetElement) {
    Object key = computeKeyForElement(widgetElement);
    if (key != null) {
      Widget widget = (Widget) widgetMap.get(key);
      assert (widget != null);
      return widget;
    } else {
      return null;
    }
  }

  /**
   * Removes the given widget from a cell. The widget must not be
   * <code>null</code>.
   * 
   * @param widget widget to be removed
   * @return always return true
   */
  private boolean removeWidget(Widget widget) {
    // Clear the widget's parent.
    disown(widget);

    // Remove the widget from the map.
    Object x = widgetMap.remove(computeKeyForElement(widget.getElement()));
    assert (x != null);
    return true;
  }
}