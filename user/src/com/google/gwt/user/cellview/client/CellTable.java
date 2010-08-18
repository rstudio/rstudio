/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.user.cellview.client;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableColElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.user.cellview.client.HasDataPresenter.LoadingState;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A list view that supports paging and columns.
 *
 * @param <T> the data type of each row
 */
public class CellTable<T> extends AbstractHasData<T> {

  /**
   * A cleaner version of the table that uses less graphics.
   */
  public static interface CleanResources extends Resources {

    @Source("CellTableClean.css")
    CleanStyle cellTableStyle();
  }

  /**
   * A cleaner version of the table that uses less graphics.
   */
  public static interface CleanStyle extends Style {
    String footer();

    String header();
  }

  /**
   * A ClientBundle that provides images for this widget.
   */
  public static interface Resources extends ClientBundle {

    /**
     * The background used for footer cells.
     */
    @Source("cellTableHeaderBackground.png")
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    ImageResource cellTableFooterBackground();

    /**
     * The background used for header cells.
     */
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    ImageResource cellTableHeaderBackground();

    /**
     * The loading indicator used while the table is waiting for data.
     */
    ImageResource cellTableLoading();

    /**
     * The background used for selected cells.
     */
    @Source("cellListSelectedBackground.png")
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    ImageResource cellTableSelectedBackground();

    /**
     * The styles used in this widget.
     */
    @Source("CellTable.css")
    Style cellTableStyle();
  }

  /**
   * Styles used by this widget.
   */
  public static interface Style extends CssResource {

    /**
     * Applied to every cell.
     */
    String cell();

    /**
     * Applied to the table.
     */
    String cellTable();

    /**
     * Applied to even rows.
     */
    String evenRow();

    /**
     * Applied to the first column.
     */
    String firstColumn();

    /**
     * Applied to the first column footers.
     */
    String firstColumnFooter();

    /**
     * Applied to the first column headers.
     */
    String firstColumnHeader();

    /**
     * Applied to footers cells.
     */
    String footer();

    /**
     * Applied to headers cells.
     */
    String header();

    /**
     * Applied to the hovered row.
     */
    String hoveredRow();

    /**
     * Applied to the keyboard selected cell.
     */
    String keyboardSelectedCell();

    /**
     * Applied to the keyboard selected row.
     */
    String keyboardSelectedRow();

    /**
     * Applied to the last column.
     */
    String lastColumn();

    /**
     * Applied to the last column footers.
     */
    String lastColumnFooter();

    /**
     * Applied to the last column headers.
     */
    String lastColumnHeader();

    /**
     * Applied to the loading indicator.
     */
    String loading();

    /**
     * Applied to odd rows.
     */
    String oddRow();

    /**
     * Applied to selected rows.
     */
    String selectedRow();
  }

  /**
   * Implementation of {@link CellTable}.
   */
  private static class Impl {

    private final com.google.gwt.user.client.Element tmpElem =
        Document.get().createDivElement().cast();

    /**
     * Convert the rowHtml into Elements wrapped by the specified table section.
     *
     * @param table the {@link CellTable}
     * @param sectionTag the table section tag
     * @param rowHtml the Html for the rows
     * @return the section element
     */
    protected TableSectionElement convertToSectionElement(
        CellTable<?> table, String sectionTag, String rowHtml) {
      // Attach an event listener so we can catch synchronous load events from
      // cached images.
      DOM.setEventListener(tmpElem, table);

      // Render the rows into a table.
      // IE doesn't support innerHtml on a TableSection or Table element, so we
      // generate the entire table.
      sectionTag = sectionTag.toLowerCase();
      String innerHtml = "<table><" + sectionTag + ">" + rowHtml + "</"
          + sectionTag + "></table>";
      tmpElem.setInnerHTML(innerHtml);
      TableElement tableElem = tmpElem.getFirstChildElement().cast();

      // Detach the event listener.
      DOM.setEventListener(tmpElem, null);

      // Get the section out of the table.
      if ("tbody".equals(sectionTag)) {
        return tableElem.getTBodies().getItem(0);
      } else if ("thead".equals(sectionTag)) {
        return tableElem.getTHead();
      } else if ("tfoot".equals(sectionTag)) {
        return tableElem.getTFoot();
      }
      throw new IllegalArgumentException(
          "Invalid table section tag: " + sectionTag);
    }

    /**
     * Render a table section in the table.
     *
     * @param table the {@link CellTable}
     * @param section the {@link TableSectionElement} to replace
     * @param html the html to render
     */
    protected void replaceAllRows(
        CellTable<?> table, TableSectionElement section, String html) {
      // If the widget is not attached, attach an event listener so we can catch
      // synchronous load events from cached images.
      if (!table.isAttached()) {
        DOM.setEventListener(table.getElement(), table);
      }

      // Render the html.
      section.setInnerHTML(html);

      // Detach the event listener.
      if (!table.isAttached()) {
        DOM.setEventListener(table.getElement(), null);
      }
    }
  }

  /**
   * Implementation of {@link CellTable} used by IE.
   */
  @SuppressWarnings("unused")
  private static class ImplTrident extends Impl {

    /**
     * IE doesn't support innerHTML on tbody, nor does it support removing or
     * replacing a tbody. The only solution is to remove and replace the rows
     * themselves.
     */
    @Override
    protected void replaceAllRows(
        CellTable<?> table, TableSectionElement section, String html) {
      // Remove all children.
      Element child = section.getFirstChildElement();
      while (child != null) {
        Element next = child.getNextSiblingElement();
        section.removeChild(child);
        child = next;
      }

      // Add new child elements.
      TableSectionElement newSection = convertToSectionElement(
          table, section.getTagName(), html);
      child = newSection.getFirstChildElement();
      while (child != null) {
        Element next = child.getNextSiblingElement();
        section.appendChild(child);
        child = next;
      }
    }
  }

  /**
   * The default page size.
   */
  private static final int DEFAULT_PAGESIZE = 15;

  private static Resources DEFAULT_RESOURCES;

  /**
   * The table specific {@link Impl}.
   */
  private static Impl TABLE_IMPL;

  private static Resources getDefaultResources() {
    if (DEFAULT_RESOURCES == null) {
      DEFAULT_RESOURCES = GWT.create(CleanResources.class);
    }
    return DEFAULT_RESOURCES;
  }

  private boolean cellIsEditing;
  private final TableColElement colgroup;

  private final List<Column<T, ?>> columns = new ArrayList<Column<T, ?>>();

  private boolean dependsOnSelection;

  private final List<Header<?>> footers = new ArrayList<Header<?>>();

  private boolean handlesSelection;

  private final List<Header<?>> headers = new ArrayList<Header<?>>();

  /**
   * Set to true when the footer is stale.
   */
  private boolean headersStale;

  private TableRowElement hoveringRow;

  private int keyboardSelectedColumn = 0;

  private int keyboardSelectedRow = 0;

  /**
   * Indicates whether or not the scheduled redraw has been cancelled.
   */
  private boolean redrawCancelled;

  /**
   * The command used to redraw the table after adding columns.
   */
  private final Scheduler.ScheduledCommand redrawCommand =
      new Scheduler.ScheduledCommand() {
        public void execute() {
          redrawScheduled = false;
          if (redrawCancelled) {
            redrawCancelled = false;
            return;
          }
          redraw();
        }
      };

  /**
   * Indicates whether or not a redraw is scheduled.
   */
  private boolean redrawScheduled;

  private final Style style;
  private final TableElement table;
  private final TableSectionElement tbody;
  private final TableSectionElement tbodyLoading;
  private final TableSectionElement tfoot;
  private final TableSectionElement thead;

  /**
   * Constructs a table with a default page size of 15.
   */
  public CellTable() {
    this(DEFAULT_PAGESIZE);
  }

  /**
   * Constructs a table with the given page size.
   *
   * @param pageSize the page size
   */
  public CellTable(final int pageSize) {
    this(pageSize, getDefaultResources());
  }

  /**
   * Constructs a table with the given page size with the specified
   * {@link Resources}.
   *
   * @param pageSize the page size
   * @param resources the resources to use for this widget
   */
  public CellTable(final int pageSize, Resources resources) {
    super(Document.get().createTableElement(), pageSize);
    if (TABLE_IMPL == null) {
      TABLE_IMPL = GWT.create(Impl.class);
    }
    this.style = resources.cellTableStyle();
    this.style.ensureInjected();

    table = getElement().cast();
    table.setCellSpacing(0);
    colgroup = Document.get().createColGroupElement();
    table.appendChild(colgroup);
    thead = table.createTHead();
    table.appendChild(tbody = Document.get().createTBodyElement());
    table.appendChild(tbodyLoading = Document.get().createTBodyElement());
    tfoot = table.createTFoot();
    setStyleName(this.style.cellTable());

    // Create the loading indicator.
    {
      TableCellElement td = Document.get().createTDElement();
      TableRowElement tr = Document.get().createTRElement();
      tbodyLoading.appendChild(tr);
      tr.appendChild(td);
      td.setAlign("center");
      td.setInnerHTML("<div class='" + style.loading() + "'></div>");
      setLoadingIconVisible(false);
    }

    // Sink events.
    sinkEvents(
        Event.ONCLICK | Event.ONMOUSEOVER | Event.ONMOUSEOUT | Event.ONKEYUP
            | Event.ONKEYDOWN);
  }

  /**
   * Adds a column to the table.
   */
  public void addColumn(Column<T, ?> col) {
    addColumn(col, (Header<?>) null, (Header<?>) null);
  }

  /**
   * Adds a column to the table with an associated header.
   */
  public void addColumn(Column<T, ?> col, Header<?> header) {
    addColumn(col, header, null);
  }

  /**
   * Adds a column to the table with an associated header and footer.
   */
  public void addColumn(Column<T, ?> col, Header<?> header, Header<?> footer) {
    headers.add(header);
    footers.add(footer);
    columns.add(col);
    updateDependsOnSelection();

    // Sink events used by the new column.
    Set<String> consumedEvents = new HashSet<String>();
    {
      Set<String> cellEvents = col.getCell().getConsumedEvents();
      if (cellEvents != null) {
        consumedEvents.addAll(cellEvents);
      }
    }
    if (header != null) {
      Set<String> headerEvents = header.getCell().getConsumedEvents();
      if (headerEvents != null) {
        consumedEvents.addAll(headerEvents);
      }
    }
    if (footer != null) {
      Set<String> footerEvents = footer.getCell().getConsumedEvents();
      if (footerEvents != null) {
        consumedEvents.addAll(footerEvents);
      }
    }
    CellBasedWidgetImpl.get().sinkEvents(this, consumedEvents);

    headersStale = true;
    scheduleRedraw();
  }

  /**
   * Adds a column to the table with an associated String header.
   */
  public void addColumn(Column<T, ?> col, String headerString) {
    addColumn(col, new TextHeader(headerString), null);
  }

  /**
   * Adds a column to the table with an associated String header and footer.
   */
  public void addColumn(
      Column<T, ?> col, String headerString, String footerString) {
    addColumn(col, new TextHeader(headerString), new TextHeader(footerString));
  }

  /**
   * Add a style name to the {@link TableColElement} at the specified index,
   * creating it if necessary.
   *
   * @param index the column index
   * @param styleName the style name to add
   */
  public void addColumnStyleName(int index, String styleName) {
    ensureTableColElement(index).addClassName(styleName);
  }

  public int getBodyHeight() {
    int height = getClientHeight(tbody);
    return height;
  }

  public int getHeaderHeight() {
    int height = getClientHeight(thead);
    return height;
  }

  /**
   * Get the {@link TableRowElement} for the specified row. If the row element
   * has not been created, null is returned.
   *
   * @param row the row index
   * @return the row element, or null if it doesn't exists
   * @throws IndexOutOfBoundsException if the row index is outside of the
   *           current page
   */
  public TableRowElement getRowElement(int row) {
    checkRowBounds(row);
    NodeList<TableRowElement> rows = tbody.getRows();
    return rows.getLength() > row ? rows.getItem(row) : null;
  }

  @Override
  public void onBrowserEvent(Event event) {
    CellBasedWidgetImpl.get().onBrowserEvent(this, event);
    super.onBrowserEvent(event);

    String eventType = event.getType();
    boolean keyUp = "keyup".equals(eventType);
    boolean keyDown = "keydown".equals(eventType);

    // Ignore keydown events unless the cell is in edit mode
    if (keyDown && !cellIsEditing) {
      return;
    }
    if (keyUp && !cellIsEditing) {
      if (handleKey(event)) {
        return;
      }
    }

    // Find the cell where the event occurred.
    EventTarget eventTarget = event.getEventTarget();
    TableCellElement tableCell = null;
    if (eventTarget != null && Element.is(eventTarget)) {
      tableCell = findNearestParentCell(Element.as(eventTarget));
    }
    if (tableCell == null) {
      return;
    }

    // Determine if we are in the header, footer, or body. Its possible that
    // the table has been refreshed before the current event fired (ex. change
    // event refreshes before mouseup fires), so we need to check each parent
    // element.
    Element trElem = tableCell.getParentElement();
    if (trElem == null) {
      return;
    }
    TableRowElement tr = TableRowElement.as(trElem);
    Element sectionElem = tr.getParentElement();
    if (sectionElem == null) {
      return;
    }
    TableSectionElement section = TableSectionElement.as(sectionElem);

    // Forward the event to the associated header, footer, or column.
    int col = tableCell.getCellIndex();
    if (section == thead) {
      Header<?> header = headers.get(col);
      if (header != null
          && cellConsumesEventType(header.getCell(), eventType)) {
        header.onBrowserEvent(tableCell, event);
      }
    } else if (section == tfoot) {
      Header<?> footer = footers.get(col);
      if (footer != null
          && cellConsumesEventType(footer.getCell(), eventType)) {
        footer.onBrowserEvent(tableCell, event);
      }
    } else if (section == tbody) {
      // Update the hover state.
      int row = tr.getSectionRowIndex();
      if ("mouseover".equals(eventType)) {
        if (hoveringRow != null) {
          hoveringRow.removeClassName(style.hoveredRow());
        }
        hoveringRow = tr;
        tr.addClassName(style.hoveredRow());
      } else if ("mouseout".equals(eventType)) {
        hoveringRow = null;
        tr.removeClassName(style.hoveredRow());
      }

      // Update selection. Selection occurs before firing the event to the cell
      // in case the cell operates on the currently selected item.
      T value = getDisplayedItem(row);
      SelectionModel<? super T> selectionModel = getSelectionModel();
      if (selectionModel != null && "click".equals(eventType)
          && !handlesSelection) {
        selectionModel.setSelected(value, true);
      }

      fireEventToCell(event, eventType, tableCell, value, col, row);
    }
  }

  public void redrawFooters() {
    createHeaders(true);
  }

  public void redrawHeaders() {
    createHeaders(false);
  }

  /**
   * Remove a column.
   *
   * @param col the column to remove
   */
  public void removeColumn(Column<T, ?> col) {
    int index = columns.indexOf(col);
    if (index < 0) {
      throw new IllegalArgumentException(
          "The specified column is not part of this table.");
    }
    removeColumn(index);
  }

  /**
   * Remove a column.
   *
   * @param index the column index
   */
  public void removeColumn(int index) {
    if (index < 0 || index >= columns.size()) {
      throw new IndexOutOfBoundsException(
          "The specified column index is out of bounds.");
    }
    columns.remove(index);
    headers.remove(index);
    footers.remove(index);
    updateDependsOnSelection();
    headersStale = true;
    scheduleRedraw();

    // We don't unsink events because other handlers or user code may have sunk
    // them intentionally.
  }

  /**
   * Remove a style from the {@link TableColElement} at the specified index.
   *
   * @param index the column index
   * @param styleName the style name to remove
   */
  public void removeColumnStyleName(int index, String styleName) {
    if (index >= colgroup.getChildCount()) {
      return;
    }
    ensureTableColElement(index).removeClassName(styleName);
  }

  @Override
  protected void renderRowValues(StringBuilder sb, List<T> values, int start,
      SelectionModel<? super T> selectionModel) {
    createHeadersAndFooters();

    ProvidesKey<T> keyProvider = getKeyProvider();
    String evenRowStyle = style.evenRow();
    String oddRowStyle = style.oddRow();
    String cellStyle = style.cell();
    String firstColumnStyle = " " + style.firstColumn();
    String lastColumnStyle = " " + style.lastColumn();
    String selectedRowStyle = " " + style.selectedRow();
    int columnCount = columns.size();
    int length = values.size();
    int end = start + length;
    for (int i = start; i < end; i++) {
      T value = values.get(i - start);
      boolean isSelected = (selectionModel == null || value == null)
          ? false : selectionModel.isSelected(value);
      sb.append("<tr onclick='' class='");
      sb.append(i % 2 == 0 ? evenRowStyle : oddRowStyle);
      if (isSelected) {
        sb.append(selectedRowStyle);
      }
      sb.append("'>");
      int curColumn = 0;
      for (Column<T, ?> column : columns) {
        sb.append("<td class='").append(cellStyle);
        if (curColumn == 0) {
          sb.append(firstColumnStyle);
        }
        // The first and last column could be the same column.
        if (curColumn == columnCount - 1) {
          sb.append(lastColumnStyle);
        }
        sb.append("'><div>");
        if (value != null) {
          column.render(value, keyProvider, sb);
        }
        sb.append("</div></td>");
        curColumn++;
      }
      sb.append("</tr>");
    }
  }

  @Override
  Element convertToElements(String html) {
    return TABLE_IMPL.convertToSectionElement(CellTable.this, "tbody", html);
  }

  @Override
  boolean dependsOnSelection() {
    return dependsOnSelection;
  }

  @Override
  Element getChildContainer() {
    return tbody;
  }

  @Override
  void onUpdateSelection() {
    // Refresh headers.
    for (Header<?> header : headers) {
      if (header != null && header.getCell().dependsOnSelection()) {
        createHeaders(false);
        break;
      }
    }

    // Refresh footers.
    for (Header<?> footer : footers) {
      if (footer != null && footer.getCell().dependsOnSelection()) {
        createHeaders(true);
        break;
      }
    }
  }

  @Override
  void replaceAllChildren(List<T> values, String html) {
    // Cancel any pending redraw.
    if (redrawScheduled) {
      redrawCancelled = true;
    }
    TABLE_IMPL.replaceAllRows(
        CellTable.this, tbody, CellBasedWidgetImpl.get().processHtml(html));
  }

  @Override
  void resetFocus() {
    int pageStart = getPageStart();
    int offset = keyboardSelectedRow - pageStart;
    // Check that both the row and column still exist. The column may not exist
    // if a column is removed.
    if (offset >= 0 && offset < getChildCount()
        && keyboardSelectedColumn < columns.size()) {
      TableRowElement tr = getRowElement(offset);
      TableCellElement td = tr.getCells().getItem(keyboardSelectedColumn);
      tr.addClassName(style.keyboardSelectedRow());
      td.addClassName(style.keyboardSelectedCell());
      td.setTabIndex(0);
      td.focus(); // TODO (rice) only focus if we were focused previously
    }
  }

  @Override
  void setLoadingState(LoadingState state) {
    setLoadingIconVisible(state == LoadingState.LOADING);
  }

  @Override
  void setSelected(Element elem, boolean selected) {
    setStyleName(elem, style.selectedRow(), selected);
  }

  /**
   * Check if a cell consumes the specified event type.
   *
   * @param cell the cell
   * @param eventType the event type to check
   * @return true if consumed, false if not
   */
  private boolean cellConsumesEventType(Cell<?> cell, String eventType) {
    Set<String> consumedEvents = cell.getConsumedEvents();
    return consumedEvents != null && consumedEvents.contains(eventType);
  }

  /**
   * Render the header or footer.
   *
   * @param isFooter true if this is the footer table, false if the header table
   */
  private void createHeaders(boolean isFooter) {
    List<Header<?>> theHeaders = isFooter ? footers : headers;
    TableSectionElement section = isFooter ? tfoot : thead;
    String className = isFooter ? style.footer() : style.header();

    boolean hasHeader = false;
    StringBuilder sb = new StringBuilder();
    sb.append("<tr>");
    int columnCount = columns.size();
    int curColumn = 0;
    for (Header<?> header : theHeaders) {
      sb.append("<th class='").append(className);
      if (curColumn == 0) {
        sb.append(" ");
        sb.append(
            isFooter ? style.firstColumnFooter() : style.firstColumnHeader());
      }
      // The first and last columns could be the same column.
      if (curColumn == columnCount - 1) {
        sb.append(" ");
        sb.append(
            isFooter ? style.lastColumnFooter() : style.lastColumnHeader());
      }
      sb.append("'>");
      if (header != null) {
        hasHeader = true;
        header.render(sb);
      }
      sb.append("</th>");
      curColumn++;
    }
    sb.append("</tr>");

    // Render the section contents.
    TABLE_IMPL.replaceAllRows(this, section, sb.toString());

    // If the section isn't used, hide it.
    setVisible(section, hasHeader);
  }

  private void createHeadersAndFooters() {
    if (headersStale) {
      headersStale = false;
      createHeaders(false);
      createHeaders(true);
    }
  }

  /**
   * Get the {@link TableColElement} at the specified index, creating it if
   * necessary.
   *
   * @param index the column index
   * @return the {@link TableColElement}
   */
  private TableColElement ensureTableColElement(int index) {
    // Ensure that we have enough columns.
    for (int i = colgroup.getChildCount(); i <= index; i++) {
      colgroup.appendChild(Document.get().createColElement());
    }
    return colgroup.getChild(index).cast();
  }

  /**
   * Find the cell that contains the element. Note that the TD element is not
   * the parent. The parent is the div inside the TD cell.
   *
   * @param elem the element
   * @return the parent cell
   */
  private TableCellElement findNearestParentCell(Element elem) {
    while ((elem != null) && (elem != table)) {
      // TODO: We need is() implementations in all Element subclasses.
      // This would allow us to use TableCellElement.is() -- much cleaner.
      String tagName = elem.getTagName();
      if ("td".equalsIgnoreCase(tagName) || "th".equalsIgnoreCase(tagName)) {
        return elem.cast();
      }
      elem = elem.getParentElement();
    }
    return null;
  }

  /**
   * Fire an event to the Cell within the specified {@link TableCellElement}.
   */
  @SuppressWarnings("unchecked")
  private <C> void fireEventToCell(Event event, String eventType,
      TableCellElement tableCell, T value, int col, int row) {
    Column<T, C> column = (Column<T, C>) columns.get(col);
    Cell<C> cell = column.getCell();
    if (cellConsumesEventType(cell, eventType)) {
      C cellValue = column.getValue(value);
      ProvidesKey<T> providesKey = getKeyProvider();
      Object key = providesKey == null ? value : providesKey.getKey(value);
      Element parentElem = tableCell.getFirstChildElement();
      boolean cellWasEditing = cell.isEditing(parentElem, cellValue, key);
      column.onBrowserEvent(
          parentElem, getPageStart() + row, value, event, providesKey);
      cellIsEditing = cell.isEditing(parentElem, cellValue, key);
      if (cellWasEditing && !cellIsEditing) {
        resetFocus();
      }
    }
  }

  private native int getClientHeight(Element element) /*-{
    return element.clientHeight;
  }-*/;

  private boolean handleKey(Event event) {
    int keyCode = event.getKeyCode();
    int oldColumn = keyboardSelectedColumn;
    int oldRow = keyboardSelectedRow;
    int numColumns = columns.size();
    int numRows = getRowCount();
    switch (keyCode) {
      case KeyCodes.KEY_UP:
        if (keyboardSelectedRow > 0) {
          --keyboardSelectedRow;
        }
        break;
      case KeyCodes.KEY_DOWN:
        if (keyboardSelectedRow < numRows - 1) {
          ++keyboardSelectedRow;
        }
        break;
      case KeyCodes.KEY_LEFT:
        if (keyboardSelectedColumn == 0 && keyboardSelectedRow > 0) {
          keyboardSelectedColumn = numColumns - 1;
          --keyboardSelectedRow;
        } else if (keyboardSelectedColumn > 0) {
          --keyboardSelectedColumn;
        }
        break;
      case KeyCodes.KEY_RIGHT:
        if (keyboardSelectedColumn == numColumns - 1
            && keyboardSelectedRow < numRows - 1) {
          keyboardSelectedColumn = 0;
          ++keyboardSelectedRow;
        } else if (keyboardSelectedColumn < numColumns - 1) {
          ++keyboardSelectedColumn;
        }
        break;
    }

    if (keyboardSelectedColumn != oldColumn || keyboardSelectedRow != oldRow) {
      int pageStart = getPageStart();
      int pageSize = getPageSize();

      // Remove old selection markers
      TableRowElement row = getRowElement(oldRow - pageStart);
      row.removeClassName(style.keyboardSelectedRow());
      TableCellElement td = row.getCells().getItem(oldColumn);
      td.removeClassName(style.keyboardSelectedCell());
      td.removeAttribute("tabIndex");

      // Move page start if needed
      if (keyboardSelectedRow >= pageStart + pageSize) {
        setPageStart(keyboardSelectedRow - pageSize + 1);
      } else if (keyboardSelectedRow < pageStart) {
        setPageStart(keyboardSelectedRow);
      }

      // Add new selection markers and re-establish focus
      resetFocus();
      return true;
    }

    return false;
  }

  /**
   * Schedule a redraw for the end of the event loop.
   */
  private void scheduleRedraw() {
    redrawCancelled = false;
    if (!redrawScheduled) {
      redrawScheduled = true;
      Scheduler.get().scheduleFinally(redrawCommand);
    }
  }

  /**
   * Show or hide the loading icon.
   *
   * @param visible true to show, false to hide.
   */
  private void setLoadingIconVisible(boolean visible) {
    // Clear the current data.
    if (visible) {
      tbody.setInnerText("");
    }

    // Update the colspan.
    TableCellElement td = tbodyLoading.getRows().getItem(0).getCells().getItem(
        0);
    td.setColSpan(Math.max(1, columns.size()));
    setVisible(tbodyLoading, visible);
  }

  /**
   * Update the dependsOnSelection and handlesSelection booleans.
   */
  private void updateDependsOnSelection() {
    dependsOnSelection = false;
    handlesSelection = false;
    for (Column<T, ?> column : columns) {
      Cell<?> cell = column.getCell();
      if (cell.dependsOnSelection()) {
        dependsOnSelection = true;
      }
      if (cell.handlesSelection()) {
        handlesSelection = true;
      }
    }
  }
}
