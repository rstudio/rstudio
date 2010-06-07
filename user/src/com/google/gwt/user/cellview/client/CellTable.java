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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableColElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.PagingListView;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.SelectionModel;

import java.util.ArrayList;
import java.util.List;

/**
 * A list view that supports paging and columns.
 * 
 * @param <T> the data type of each row
 */
public class CellTable<T> extends Widget implements PagingListView<T> {

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

    final Element tmpElem = Document.get().createDivElement();

    /**
     * Convert the rowHtml into Elements wrapped by the specifeid table section.
     * 
     * @param sectionTag the table section tag
     * @param rowHtml the Html for the rows
     * @return the section element
     */
    protected TableSectionElement convertToSectionElement(String sectionTag,
        String rowHtml) {
      // Render the rows into a table.
      // IE doesn't support innerHtml on a TableSection or Table element, so we
      // generate the entire table.
      sectionTag = sectionTag.toLowerCase();
      String innerHtml = "<table><" + sectionTag + ">" + rowHtml + "</"
          + sectionTag + "></table>";
      tmpElem.setInnerHTML(innerHtml);
      TableElement tableElem = tmpElem.getFirstChildElement().cast();

      // Get the section out of the table.
      if ("tbody".equals(sectionTag)) {
        return tableElem.getTBodies().getItem(0);
      } else if ("thead".equals(sectionTag)) {
        return tableElem.getTHead();
      } else if ("tfoot".equals(sectionTag)) {
        return tableElem.getTFoot();
      }
      throw new IllegalArgumentException("Invalid table section tag: "
          + sectionTag);
    }

    /**
     * Render and replace a table section in the table.
     * 
     * @param section the {@link TableSectionElement} to replace
     * @param html the html to render
     * @return the new section element
     */
    protected TableSectionElement renderSectionContents(
        TableSectionElement section, String html) {
      section.setInnerHTML(html);
      return section;
    }
  }

  /**
   * Implementation of {@link CellTable} used by IE. Table sections do not
   * support setInnerHtml in IE, so we need to replace the entire elements.
   */
  @SuppressWarnings("unused")
  private static class ImplTrident extends Impl {

    @Override
    protected TableSectionElement renderSectionContents(
        TableSectionElement section, String html) {
      TableSectionElement newSection = convertToSectionElement(
          section.getTagName(), html);
      section.getParentElement().replaceChild(newSection, section);
      return newSection;
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

  private final TableColElement colgroup;
  private List<Column<T, ?>> columns = new ArrayList<Column<T, ?>>();
  private List<Header<?>> footers = new ArrayList<Header<?>>();
  private List<Header<?>> headers = new ArrayList<Header<?>>();

  /**
   * Set to true when the footer is stale.
   */
  private boolean headersStale;

  private TableRowElement hoveringRow;
  private final CellListImpl<T> impl;

  /**
   * If true, enable selection via the mouse.
   */
  private boolean isSelectionEnabled;

  /**
   * If null, each T will be used as its own key.
   */
  private ProvidesKey<T> providesKey;

  private final Style style;
  private final TableElement table;
  private TableSectionElement tbody;
  private final TableSectionElement tbodyLoading;
  private TableSectionElement tfoot;
  private TableSectionElement thead;

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
    if (TABLE_IMPL == null) {
      TABLE_IMPL = GWT.create(Impl.class);
    }
    this.style = resources.cellTableStyle();
    this.style.ensureInjected();

    setElement(table = Document.get().createTableElement());
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
    }

    // Create the implementation.
    this.impl = new CellListImpl<T>(this, pageSize, tbody) {

      @Override
      public void setData(List<T> values, int start) {
        createHeadersAndFooters();
        super.setData(values, start);
      }

      @Override
      protected Element convertToElements(String html) {
        return TABLE_IMPL.convertToSectionElement("tbody", html);
      }

      @Override
      protected boolean dependsOnSelection() {
        for (Column<T, ?> column : columns) {
          if (column.dependsOnSelection()) {
            return true;
          }
        }
        return false;
      }

      @Override
      protected void emitHtml(StringBuilder sb, List<T> values, int start,
          SelectionModel<? super T> selectionModel) {
        setLoadingIconVisible(false);

        String firstColumnStyle = style.firstColumn();
        String lastColumnStyle = style.lastColumn();
        int columnCount = columns.size();
        int length = values.size();
        int end = start + length;
        for (int i = start; i < end; i++) {
          T value = values.get(i - start);
          boolean isSelected = (selectionModel == null || value == null)
              ? false : selectionModel.isSelected(value);
          sb.append("<tr onclick='' __idx='").append(i).append("'");
          sb.append(" class='");
          sb.append(i % 2 == 0 ? style.evenRow() : style.oddRow());
          if (isSelected) {
            sb.append(" ").append(style.selectedRow());
          }
          sb.append("'>");
          int curColumn = 0;
          for (Column<T, ?> column : columns) {
            // TODO(jlabanca): How do we sink ONFOCUS and ONBLUR?
            sb.append("<td class='").append(style.cell());
            if (curColumn == 0) {
              sb.append(" ").append(firstColumnStyle);
            }
            // The first and last column could be the same column.
            if (curColumn == columnCount - 1) {
              sb.append(" ").append(lastColumnStyle);
            }
            sb.append("'>");
            int bufferLength = sb.length();
            if (value != null) {
              column.render(value, providesKey, sb);
            }

            // Add blank space to ensure empty rows aren't squished.
            if (bufferLength == sb.length()) {
              sb.append("&nbsp");
            }
            sb.append("</td>");
            curColumn++;
          }
          sb.append("</tr>");
        }
      }

      @Override
      protected Element renderChildContents(String html) {
        return (tbody = TABLE_IMPL.renderSectionContents(tbody, html));
      }

      @Override
      protected void setSelected(Element elem, boolean selected) {
        setStyleName(elem, style.selectedRow(), selected);
      }

      @Override
      protected void updateSelection() {
        // Refresh headers.
        for (Header<?> header : headers) {
          if (header != null && header.dependsOnSelection()) {
            createHeaders(false);
            break;
          }
        }

        // Refresh footers.
        for (Header<?> footer : footers) {
          if (footer != null && footer.dependsOnSelection()) {
            createHeaders(true);
            break;
          }
        }

        // Update data.
        super.updateSelection();
      }
    };

    setPageSize(pageSize);

    // TODO: Total hack. It would almost definitely be preferable to sink only
    // those events actually needed by cells.
    sinkEvents(Event.ONCLICK | Event.MOUSEEVENTS | Event.KEYEVENTS
        | Event.ONCHANGE | Event.FOCUSEVENTS);

    // Show the loading indicator by default.
    setLoadingIconVisible(true);
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
    headersStale = true;
    redraw();
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
  public void addColumn(Column<T, ?> col, String headerString,
      String footerString) {
    addColumn(col, new TextHeader(headerString), new TextHeader(footerString));
  }

  // TODO: remove(Column)

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

  public int getDataSize() {
    return impl.getDataSize();
  }

  public T getDisplayedItem(int indexOnPage) {
    checkRowBounds(indexOnPage);
    return impl.getData().get(indexOnPage);
  }

  public List<T> getDisplayedItems() {
    return new ArrayList<T>(impl.getData());
  }

  public int getHeaderHeight() {
    int height = getClientHeight(thead);
    return height;
  }

  public ProvidesKey<T> getKeyProvider() {
    return providesKey;
  }

  public int getNumDisplayedItems() {
    return impl.getDisplayedItemCount();
  }

  public int getPageSize() {
    return impl.getPageSize();
  }

  public int getPageStart() {
    return impl.getPageStart();
  }

  public Range getRange() {
    return impl.getRange();
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

  public int getSize() {
    return impl.getDataSize();
  }

  public boolean isDataSizeExact() {
    return impl.dataSizeIsExact();
  }

  /**
   * Check whether or not mouse selection is enabled.
   * 
   * @return true if enabled, false if disabled
   */
  public boolean isSelectionEnabled() {
    return isSelectionEnabled;
  }

  @Override
  public void onBrowserEvent(Event event) {
    super.onBrowserEvent(event);

    // Find the cell where the event occurred.
    EventTarget eventTarget = event.getEventTarget();
    TableCellElement cell = null;
    if (eventTarget != null && Element.is(eventTarget)) {
      cell = findNearestParentCell(Element.as(eventTarget));
    }
    if (cell == null) {
      return;
    }

    // Forward the event to the associated header, footer, or column.
    TableRowElement tr = TableRowElement.as(cell.getParentElement());
    TableSectionElement section = TableSectionElement.as(tr.getParentElement());
    int col = cell.getCellIndex();
    if (section == thead) {
      Header<?> header = headers.get(col);
      if (header != null) {
        header.onBrowserEvent(cell, event);
      }
    } else if (section == tfoot) {
      Header<?> footer = footers.get(col);
      if (footer != null) {
        footer.onBrowserEvent(cell, event);
      }
    } else if (section == tbody) {
      int row = tr.getSectionRowIndex();

      if (event.getType().equals("mouseover")) {
        if (hoveringRow != null) {
          hoveringRow.removeClassName(style.hoveredRow());
        }
        hoveringRow = tr;
        tr.addClassName(style.hoveredRow());
      } else if (event.getType().equals("mouseout")) {
        hoveringRow = null;
        tr.removeClassName(style.hoveredRow());
      }

      T value = impl.getData().get(row);
      Column<T, ?> column = columns.get(col);
      column.onBrowserEvent(cell, impl.getPageStart() + row, value, event,
          providesKey);

      // Update selection.
      if (isSelectionEnabled && event.getTypeInt() == Event.ONCLICK) {
        SelectionModel<? super T> selectionModel = impl.getSelectionModel();
        if (selectionModel != null) {
          selectionModel.setSelected(value, true);
        }
      }
    }
  }

  /**
   * Redraw the table using the existing data.
   */
  public void redraw() {
    setLoadingIconVisible(false);
    impl.redraw();
  }

  /**
   * Redraw the table, requesting data from the delegate.
   */
  public void refresh() {
    setLoadingIconVisible(true);
    impl.refresh();
  }

  public void refreshFooters() {
    createHeaders(true);
  }

  public void refreshHeaders() {
    createHeaders(false);
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

  public void setData(int start, int length, List<T> values) {
    impl.setData(values, start);
  }

  public void setDataSize(int size, boolean isExact) {
    impl.setDataSize(size, isExact);

    // If there is no data, then we are done loading.
    if (size <= 0) {
      setLoadingIconVisible(false);
    }
  }

  public void setDelegate(Delegate<T> delegate) {
    impl.setDelegate(delegate);
  }

  /**
   * Sets the {@link ProvidesKey} instance that will be used to generate keys
   * for each record object as needed.
   * 
   * @param providesKey an instance of {@link ProvidesKey} used to generate keys
   *          for record objects.
   */
  // TODO - when is this valid? Do we rehash column view data if it changes?
  public void setKeyProvider(ProvidesKey<T> providesKey) {
    this.providesKey = providesKey;
  }

  public void setPager(PagingListView.Pager<T> pager) {
    impl.setPager(pager);
  }

  /**
   * Set the number of rows per page and refresh the table.
   * 
   * @param pageSize the page size
   * 
   * @throws IllegalArgumentException if pageSize is negative or 0
   */
  public void setPageSize(int pageSize) {
    impl.setPageSize(pageSize);
  }

  /**
   * Set the starting index of the current visible page. The actual page start
   * will be clamped in the range [0, getSize() - 1].
   * 
   * @param pageStart the index of the row that should appear at the start of
   *          the page
   */
  public void setPageStart(int pageStart) {
    setLoadingIconVisible(true);
    impl.setPageStart(pageStart);
  }

  /**
   * Enable mouse and keyboard selection.
   * 
   * @param isSelectionEnabled true to enable, false to disable
   */
  public void setSelectionEnabled(boolean isSelectionEnabled) {
    this.isSelectionEnabled = isSelectionEnabled;
  }

  public void setSelectionModel(SelectionModel<? super T> selectionModel) {
    impl.setSelectionModel(selectionModel, true);
  }

  /**
   * Checks that the row is within the correct bounds.
   * 
   * @param row row index to check
   * @throws IndexOutOfBoundsException
   */
  protected void checkRowBounds(int row) {
    int rowSize = impl.getDisplayedItemCount();
    if ((row >= rowSize) || (row < 0)) {
      throw new IndexOutOfBoundsException("Row index: " + row + ", Row size: "
          + rowSize);
    }
  }

  /**
   * Render the header of footer.
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
        sb.append(isFooter ? style.firstColumnFooter()
            : style.firstColumnHeader());
      }
      // The first and last columns could be the same column.
      if (curColumn == columnCount - 1) {
        sb.append(" ");
        sb.append(isFooter ? style.lastColumnFooter()
            : style.lastColumnHeader());
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

    // Render and replace the table section.
    section = TABLE_IMPL.renderSectionContents(section, sb.toString());
    if (isFooter) {
      tfoot = section;
    } else {
      thead = section;
    }

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

  private native int getClientHeight(Element element) /*-{
    return element.clientHeight;
  }-*/;

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
}
