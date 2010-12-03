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
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableColElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.ImportedWithPrefix;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.HasDataPresenter.LoadingState;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
import com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A tabular view that supports paging and columns.
 * 
 * <p>
 * <h3>Columns</h3>
 * The {@link Column} class defines the {@link Cell} used to render a column.
 * Implement {@link Column#getValue(Object)} to retrieve the field value from
 * the row object that will be rendered in the {@link Cell}.
 * </p>
 * 
 * <p>
 * <h3>Headers and Footers</h3>
 * A {@link Header} can be placed at the top (header) or bottom (footer) of the
 * {@link CellTable}. You can specify a header as text using
 * {@link #addColumn(Column, String)}, or you can create a custom {@link Header}
 * that can change with the value of the cells, such as a column total. The
 * {@link Header} will be rendered every time the row data changes or the table
 * is redrawn. If you pass the same header instance (==) into adjacent columns,
 * the header will span the columns.
 * </p>
 * 
 * <p>
 * <h3>Examples</h3>
 * <dl>
 * <dt>Trivial example</dt>
 * <dd>{@example com.google.gwt.examples.cellview.CellTableExample}</dd>
 * <dt>FieldUpdater example</dt>
 * <dd>{@example com.google.gwt.examples.cellview.CellTableFieldUpdaterExample}</dd>
 * <dt>Key provider example</dt>
 * <dd>{@example com.google.gwt.examples.view.KeyProviderExample}</dd>
 * </dl>
 * </p>
 * 
 * @param <T> the data type of each row
 */
public class CellTable<T> extends AbstractHasData<T> {

  /**
   * Resources that match the GWT standard style theme.
   */
  public interface BasicResources extends Resources {
    /**
     * The styles used in this widget.
     */
    @Source(BasicStyle.DEFAULT_CSS)
    BasicStyle cellTableStyle();
  }

  /**
   * A ClientBundle that provides images for this widget.
   */
  public interface Resources extends ClientBundle {
    /**
     * The background used for footer cells.
     */
    @Source("cellTableHeaderBackground.png")
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal, flipRtl = true)
    ImageResource cellTableFooterBackground();

    /**
     * The background used for header cells.
     */
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal, flipRtl = true)
    ImageResource cellTableHeaderBackground();

    /**
     * The loading indicator used while the table is waiting for data.
     */
    @ImageOptions(flipRtl = true)
    ImageResource cellTableLoading();

    /**
     * The background used for selected cells.
     */
    @Source("cellListSelectedBackground.png")
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal, flipRtl = true)
    ImageResource cellTableSelectedBackground();

    /**
     * The styles used in this widget.
     */
    @Source(Style.DEFAULT_CSS)
    Style cellTableStyle();
  }

  /**
   * Styles used by this widget.
   */
  @ImportedWithPrefix("gwt-CellTable")
  public interface Style extends CssResource {
    /**
     * The path to the default CSS styles used by this resource.
     */
    String DEFAULT_CSS = "com/google/gwt/user/cellview/client/CellTable.css";

    /**
     * Applied to every cell.
     */
    String cellTableCell();

    /**
     * Applied to even rows.
     */
    String cellTableEvenRow();

    /**
     * Applied to cells in even rows.
     */
    String cellTableEvenRowCell();

    /**
     * Applied to the first column.
     */
    String cellTableFirstColumn();

    /**
     * Applied to the first column footers.
     */
    String cellTableFirstColumnFooter();

    /**
     * Applied to the first column headers.
     */
    String cellTableFirstColumnHeader();

    /**
     * Applied to footers cells.
     */
    String cellTableFooter();

    /**
     * Applied to headers cells.
     */
    String cellTableHeader();

    /**
     * Applied to the hovered row.
     */
    String cellTableHoveredRow();

    /**
     * Applied to the cells in the hovered row.
     */
    String cellTableHoveredRowCell();

    /**
     * Applied to the keyboard selected cell.
     */
    String cellTableKeyboardSelectedCell();

    /**
     * Applied to the keyboard selected row.
     */
    String cellTableKeyboardSelectedRow();

    /**
     * Applied to the cells in the keyboard selected row.
     */
    String cellTableKeyboardSelectedRowCell();

    /**
     * Applied to the last column.
     */
    String cellTableLastColumn();

    /**
     * Applied to the last column footers.
     */
    String cellTableLastColumnFooter();

    /**
     * Applied to the last column headers.
     */
    String cellTableLastColumnHeader();

    /**
     * Applied to the loading indicator.
     */
    String cellTableLoading();

    /**
     * Applied to odd rows.
     */
    String cellTableOddRow();

    /**
     * Applied to cells in odd rows.
     */
    String cellTableOddRowCell();

    /**
     * Applied to selected rows.
     */
    String cellTableSelectedRow();

    /**
     * Applied to cells in selected rows.
     */
    String cellTableSelectedRowCell();

    /**
     * Applied to the table.
     */
    String cellTableWidget();
  }

  /**
   * Styles used by {@link BasicResources}.
   */
  @ImportedWithPrefix("gwt-CellTable")
  interface BasicStyle extends Style {
    /**
     * The path to the default CSS styles used by this resource.
     */
    String DEFAULT_CSS = "com/google/gwt/user/cellview/client/CellTableBasic.css";
  }

  interface Template extends SafeHtmlTemplates {
    @Template("<div style=\"outline:none;\">{0}</div>")
    SafeHtml div(SafeHtml contents);

    @Template("<div style=\"outline:none;\" tabindex=\"{0}\">{1}</div>")
    SafeHtml divFocusable(int tabIndex, SafeHtml contents);

    @Template("<div style=\"outline:none;\" tabindex=\"{0}\" accessKey=\"{1}\">{2}</div>")
    SafeHtml divFocusableWithKey(int tabIndex, char accessKey, SafeHtml contents);

    @Template("<div class=\"{0}\"/>")
    SafeHtml loading(String loading);

    @Template("<table><tbody>{0}</tbody></table>")
    SafeHtml tbody(SafeHtml rowHtml);

    @Template("<td class=\"{0}\">{1}</td>")
    SafeHtml td(String classes, SafeHtml contents);

    @Template("<td class=\"{0}\" align=\"{1}\" valign=\"{2}\">{3}</td>")
    SafeHtml tdBothAlign(String classes, String hAlign, String vAlign,
        SafeHtml contents);

    @Template("<td class=\"{0}\" align=\"{1}\">{2}</td>")
    SafeHtml tdHorizontalAlign(String classes, String hAlign, SafeHtml contents);

    @Template("<td class=\"{0}\" valign=\"{1}\">{2}</td>")
    SafeHtml tdVerticalAlign(String classes, String vAlign, SafeHtml contents);

    @Template("<table><tfoot>{0}</tfoot></table>")
    SafeHtml tfoot(SafeHtml rowHtml);

    @Template("<th colspan=\"{0}\" class=\"{1}\">{2}</th>")
    SafeHtml th(int colspan, String classes, SafeHtml contents);

    @Template("<table><thead>{0}</thead></table>")
    SafeHtml thead(SafeHtml rowHtml);

    @Template("<tr onclick=\"\" class=\"{0}\">{1}</tr>")
    SafeHtml tr(String classes, SafeHtml contents);
  }

  /**
   * Implementation of {@link CellTable}.
   */
  private static class Impl {

    private final com.google.gwt.user.client.Element tmpElem = Document.get().createDivElement().cast();

    /**
     * Convert the rowHtml into Elements wrapped by the specified table section.
     * 
     * @param table the {@link CellTable}
     * @param sectionTag the table section tag
     * @param rowHtml the Html for the rows
     * @return the section element
     */
    protected TableSectionElement convertToSectionElement(CellTable<?> table,
        String sectionTag, SafeHtml rowHtml) {
      // Attach an event listener so we can catch synchronous load events from
      // cached images.
      DOM.setEventListener(tmpElem, table);

      // Render the rows into a table.
      // IE doesn't support innerHtml on a TableSection or Table element, so we
      // generate the entire table.
      sectionTag = sectionTag.toLowerCase();
      if ("tbody".equals(sectionTag)) {
        tmpElem.setInnerHTML(template.tbody(rowHtml).asString());
      } else if ("thead".equals(sectionTag)) {
        tmpElem.setInnerHTML(template.thead(rowHtml).asString());
      } else if ("tfoot".equals(sectionTag)) {
        tmpElem.setInnerHTML(template.tfoot(rowHtml).asString());
      } else {
        throw new IllegalArgumentException("Invalid table section tag: "
            + sectionTag);
      }
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
      } else {
        throw new IllegalArgumentException("Invalid table section tag: "
            + sectionTag);
      }
    }

    /**
     * Render a table section in the table.
     * 
     * @param table the {@link CellTable}
     * @param section the {@link TableSectionElement} to replace
     * @param html the html to render
     */
    protected void replaceAllRows(CellTable<?> table,
        TableSectionElement section, SafeHtml html) {
      // If the widget is not attached, attach an event listener so we can catch
      // synchronous load events from cached images.
      if (!table.isAttached()) {
        DOM.setEventListener(table.getElement(), table);
      }

      // Render the html.
      section.setInnerHTML(html.asString());

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
    protected void replaceAllRows(CellTable<?> table,
        TableSectionElement section, SafeHtml html) {
      // Remove all children.
      Element child = section.getFirstChildElement();
      while (child != null) {
        Element next = child.getNextSiblingElement();
        section.removeChild(child);
        child = next;
      }

      // Add new child elements.
      TableSectionElement newSection = convertToSectionElement(table,
          section.getTagName(), html);
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

  private static Template template;

  private static Resources getDefaultResources() {
    if (DEFAULT_RESOURCES == null) {
      DEFAULT_RESOURCES = GWT.create(Resources.class);
    }
    return DEFAULT_RESOURCES;
  }

  private boolean cellIsEditing;

  private final TableColElement colgroup;

  private final List<Column<T, ?>> columns = new ArrayList<Column<T, ?>>();

  /**
   * Indicates that at least one column depends on selection.
   */
  private boolean dependsOnSelection;

  private final List<Header<?>> footers = new ArrayList<Header<?>>();

  /**
   * Indicates that at least one column handles selection.
   */
  private boolean handlesSelection;

  private final List<Header<?>> headers = new ArrayList<Header<?>>();

  private TableRowElement hoveringRow;

  /**
   * Indicates that at least one column is interactive.
   */
  private boolean isInteractive;

  private int keyboardSelectedColumn = 0;

  private RowStyles<T> rowStyles;
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
    this(pageSize, getDefaultResources(), null);
  }

  /**
   * Constructs a table with a default page size of 15, and the given
   * {@link ProvidesKey key provider}.
   * 
   * @param keyProvider an instance of ProvidesKey<T>, or null if the record
   *          object should act as its own key
   */
  public CellTable(ProvidesKey<T> keyProvider) {
    this(DEFAULT_PAGESIZE, getDefaultResources(), keyProvider);
  }

  /**
   * Constructs a table with the given page size with the specified
   * {@link Resources}.
   * 
   * @param pageSize the page size
   * @param resources the resources to use for this widget
   */
  public CellTable(final int pageSize, Resources resources) {
    this(pageSize, resources, null);
  }

  /**
   * Constructs a table with the given page size and the given
   * {@link ProvidesKey key provider}.
   * 
   * @param pageSize the page size
   * @param keyProvider an instance of ProvidesKey<T>, or null if the record
   *          object should act as its own key
   */
  public CellTable(final int pageSize, ProvidesKey<T> keyProvider) {
    this(pageSize, getDefaultResources(), keyProvider);
  }

  /**
   * Constructs a table with the given page size, the specified
   * {@link Resources}, and the given key provider.
   * 
   * @param pageSize the page size
   * @param resources the resources to use for this widget
   * @param keyProvider an instance of ProvidesKey<T>, or null if the record
   *          object should act as its own key
   */
  public CellTable(final int pageSize, Resources resources,
      ProvidesKey<T> keyProvider) {
    super(Document.get().createTableElement(), pageSize, keyProvider);
    if (TABLE_IMPL == null) {
      TABLE_IMPL = GWT.create(Impl.class);
    }
    if (template == null) {
      template = GWT.create(Template.class);
    }
    this.style = resources.cellTableStyle();
    this.style.ensureInjected();

    table = getElement().cast();
    table.setCellSpacing(0);
    colgroup = Document.get().createColGroupElement();
    table.appendChild(colgroup);
    thead = table.createTHead();
    // Some browsers create a tbody automatically, others do not.
    if (table.getTBodies().getLength() > 0) {
      tbody = table.getTBodies().getItem(0);
    } else {
      tbody = Document.get().createTBodyElement();
      table.appendChild(tbody);
    }
    table.appendChild(tbodyLoading = Document.get().createTBodyElement());
    tfoot = table.createTFoot();
    setStyleName(this.style.cellTableWidget());

    // Create the loading indicator.
    {
      TableCellElement td = Document.get().createTDElement();
      TableRowElement tr = Document.get().createTRElement();
      tbodyLoading.appendChild(tr);
      tr.appendChild(td);
      td.setAlign("center");
      td.setInnerHTML(template.loading(style.cellTableLoading()).asString());
      setLoadingIconVisible(false);
    }

    // Sink events.
    Set<String> eventTypes = new HashSet<String>();
    eventTypes.add("mouseover");
    eventTypes.add("mouseout");
    CellBasedWidgetImpl.get().sinkEvents(this, eventTypes);
  }

  /**
   * Adds a column to the end of the table.
   * 
   * @param col the column to be added
   */
  public void addColumn(Column<T, ?> col) {
    insertColumn(getColumnCount(), col);
  }

  /**
   * Adds a column to the end of the table with an associated header.
   * 
   * @param col the column to be added
   * @param header the associated {@link Header}
   */
  public void addColumn(Column<T, ?> col, Header<?> header) {
    insertColumn(getColumnCount(), col, header);
  }

  /**
   * Adds a column to the end of the table with an associated header and footer.
   * 
   * @param col the column to be added
   * @param header the associated {@link Header}
   * @param footer the associated footer (as a {@link Header} object)
   */
  public void addColumn(Column<T, ?> col, Header<?> header, Header<?> footer) {
    insertColumn(getColumnCount(), col, header, footer);
  }

  /**
   * Adds a column to the end of the table with an associated String header.
   * 
   * @param col the column to be added
   * @param headerString the associated header text, as a String
   */
  public void addColumn(Column<T, ?> col, String headerString) {
    insertColumn(getColumnCount(), col, headerString);
  }

  /**
   * Adds a column to the end of the table with an associated {@link SafeHtml}
   * header.
   * 
   * @param col the column to be added
   * @param headerHtml the associated header text, as safe HTML
   */
  public void addColumn(Column<T, ?> col, SafeHtml headerHtml) {
    insertColumn(getColumnCount(), col, headerHtml);
  }

  /**
   * Adds a column to the end of the table with an associated String header and
   * footer.
   * 
   * @param col the column to be added
   * @param headerString the associated header text, as a String
   * @param footerString the associated footer text, as a String
   */
  public void addColumn(Column<T, ?> col, String headerString,
      String footerString) {
    insertColumn(getColumnCount(), col, headerString, footerString);
  }

  /**
   * Adds a column to the end of the table with an associated {@link SafeHtml}
   * header and footer.
   * 
   * @param col the column to be added
   * @param headerHtml the associated header text, as safe HTML
   * @param footerHtml the associated footer text, as safe HTML
   */
  public void addColumn(Column<T, ?> col, SafeHtml headerHtml,
      SafeHtml footerHtml) {
    insertColumn(getColumnCount(), col, headerHtml, footerHtml);
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

  /**
   * Return the height of the table body.
   * 
   * @return an int representing the body height
   */
  public int getBodyHeight() {
    int height = getClientHeight(tbody);
    return height;
  }

  /**
   * Get the column at the specified index.
   * 
   * @param col the index of the column to retrieve
   * @return the {@link Column} at the index
   */
  public Column<T, ?> getColumn(int col) {
    checkColumnBounds(col);
    return columns.get(col);
  }

  /**
   * Get the number of columns in the table.
   * 
   * @return the column count
   */
  public int getColumnCount() {
    return columns.size();
  }

  /**
   * Return the height of the table header.
   * 
   * @return an int representing the header height
   */
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
    getPresenter().flush();
    checkRowBounds(row);
    NodeList<TableRowElement> rows = tbody.getRows();
    return rows.getLength() > row ? rows.getItem(row) : null;
  }

  /**
   * Inserts a column into the table at the specified index.
   * 
   * @param beforeIndex the index to insert the column
   * @param col the column to be added
   */
  public void insertColumn(int beforeIndex, Column<T, ?> col) {
    insertColumn(beforeIndex, col, (Header<?>) null, (Header<?>) null);
  }

  /**
   * Inserts a column into the table at the specified index with an associated
   * header.
   * 
   * @param beforeIndex the index to insert the column
   * @param col the column to be added
   * @param header the associated {@link Header}
   */
  public void insertColumn(int beforeIndex, Column<T, ?> col, Header<?> header) {
    insertColumn(beforeIndex, col, header, null);
  }

  /**
   * Inserts a column into the table at the specified index with an associated
   * header and footer.
   * 
   * @param beforeIndex the index to insert the column
   * @param col the column to be added
   * @param header the associated {@link Header}
   * @param footer the associated footer (as a {@link Header} object)
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public void insertColumn(int beforeIndex, Column<T, ?> col, Header<?> header,
      Header<?> footer) {
    // Allow insert at the end.
    if (beforeIndex != getColumnCount()) {
      checkColumnBounds(beforeIndex);
    }

    headers.add(beforeIndex, header);
    footers.add(beforeIndex, footer);
    columns.add(beforeIndex, col);
    boolean wasinteractive = isInteractive;
    updateDependsOnSelection();

    // Move the keyboard selected column if the current column is not
    // interactive.
    if (!wasinteractive && isInteractive) {
      keyboardSelectedColumn = beforeIndex;
    }

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

    redraw();
  }

  /**
   * Inserts a column into the table at the specified index with an associated
   * String header.
   * 
   * @param beforeIndex the index to insert the column
   * @param col the column to be added
   * @param headerString the associated header text, as a String
   */
  public void insertColumn(int beforeIndex, Column<T, ?> col,
      String headerString) {
    insertColumn(beforeIndex, col, new TextHeader(headerString), null);
  }

  /**
   * Inserts a column into the table at the specified index with an associated
   * {@link SafeHtml} header.
   * 
   * @param beforeIndex the index to insert the column
   * @param col the column to be added
   * @param headerHtml the associated header text, as safe HTML
   */
  public void insertColumn(int beforeIndex, Column<T, ?> col,
      SafeHtml headerHtml) {
    insertColumn(beforeIndex, col, new SafeHtmlHeader(headerHtml), null);
  }

  /**
   * Inserts a column into the table at the specified index with an associated
   * String header and footer.
   * 
   * @param beforeIndex the index to insert the column
   * @param col the column to be added
   * @param headerString the associated header text, as a String
   * @param footerString the associated footer text, as a String
   */
  public void insertColumn(int beforeIndex, Column<T, ?> col,
      String headerString, String footerString) {
    insertColumn(beforeIndex, col, new TextHeader(headerString),
        new TextHeader(footerString));
  }

  /**
   * Inserts a column into the table at the specified index with an associated
   * {@link SafeHtml} header and footer.
   * 
   * @param beforeIndex the index to insert the column
   * @param col the column to be added
   * @param headerHtml the associated header text, as safe HTML
   * @param footerHtml the associated footer text, as safe HTML
   */
  public void insertColumn(int beforeIndex, Column<T, ?> col,
      SafeHtml headerHtml, SafeHtml footerHtml) {
    insertColumn(beforeIndex, col, new SafeHtmlHeader(headerHtml),
        new SafeHtmlHeader(footerHtml));
  }

  /**
   * Redraw the table's footers.
   */
  public void redrawFooters() {
    createHeaders(true);
  }

  /**
   * Redraw the table's headers.
   */
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

    // Find an interactive column. Stick with 0 if no column is interactive.
    if (index <= keyboardSelectedColumn) {
      keyboardSelectedColumn = 0;
      if (isInteractive) {
        for (int i = 0; i < columns.size(); i++) {
          if (isColumnInteractive(columns.get(i))) {
            keyboardSelectedColumn = i;
            break;
          }
        }
      }
    }

    // Redraw the table asynchronously.
    redraw();

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

  /**
   * Sets the object used to determine how a row is styled; the change will take
   * effect the next time that the table is rendered.
   * 
   * @param rowStyles a {@link RowStyles} object
   */
  public void setRowStyles(RowStyles<T> rowStyles) {
    this.rowStyles = rowStyles;
  }

  @Override
  protected Element convertToElements(SafeHtml html) {
    return TABLE_IMPL.convertToSectionElement(CellTable.this, "tbody", html);
  }

  @Override
  protected boolean dependsOnSelection() {
    return dependsOnSelection;
  }

  /**
   * Called when a user action triggers selection.
   * 
   * @param event the event that triggered selection
   * @param value the value that was selected
   * @param row the row index of the value on the page
   * @param column the column index where the event occurred
   * @deprecated use
   *             {@link #addCellPreviewHandler(com.google.gwt.view.client.CellPreviewEvent.Handler)}
   *             instead
   */
  @Deprecated
  protected void doSelection(Event event, T value, int row, int column) {
  }

  @Override
  protected Element getChildContainer() {
    return tbody;
  }

  @Override
  protected Element getKeyboardSelectedElement() {
    // Do not use getRowElement() because that will flush the presenter.
    int rowIndex = getKeyboardSelectedRow();
    NodeList<TableRowElement> rows = tbody.getRows();
    if (rowIndex < rows.getLength() && columns.size() > 0) {
      TableRowElement tr = rows.getItem(rowIndex);
      TableCellElement td = tr.getCells().getItem(keyboardSelectedColumn);
      return getCellParent(td);
    }
    return null;
  }

  @Override
  protected boolean isKeyboardNavigationSuppressed() {
    return cellIsEditing;
  }

  @Override
  protected void onBlur() {
    Element elem = getKeyboardSelectedElement();
    if (elem != null) {
      TableCellElement td = elem.getParentElement().cast();
      TableRowElement tr = td.getParentElement().cast();
      td.removeClassName(style.cellTableKeyboardSelectedCell());
      setRowStyleName(tr, style.cellTableKeyboardSelectedRow(),
          style.cellTableKeyboardSelectedRowCell(), false);
    }
  }

  @SuppressWarnings("deprecation")
  @Override
  protected void onBrowserEvent2(Event event) {
    // Get the event target.
    EventTarget eventTarget = event.getEventTarget();
    if (!Element.is(eventTarget)) {
      return;
    }
    final Element target = event.getEventTarget().cast();

    // Ignore keydown events unless the cell is in edit mode
    String eventType = event.getType();
    if ("keydown".equals(eventType) && !isKeyboardNavigationSuppressed()
        && KeyboardSelectionPolicy.DISABLED != getKeyboardSelectionPolicy()) {
      if (handleKey(event)) {
        return;
      }
    }

    // Find the cell where the event occurred.
    TableCellElement tableCell = findNearestParentCell(target);
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
      if (header != null && cellConsumesEventType(header.getCell(), eventType)) {
        Context context = new Context(0, col, header.getKey());
        header.onBrowserEvent(context, tableCell, event);
      }
    } else if (section == tfoot) {
      Header<?> footer = footers.get(col);
      if (footer != null && cellConsumesEventType(footer.getCell(), eventType)) {
        Context context = new Context(0, col, footer.getKey());
        footer.onBrowserEvent(context, tableCell, event);
      }
    } else if (section == tbody) {
      // Update the hover state.
      boolean isClick = "click".equals(eventType);
      int row = tr.getSectionRowIndex();
      if ("mouseover".equals(eventType)) {
        // Unstyle the old row if it is still part of the table.
        if (hoveringRow != null && tbody.isOrHasChild(hoveringRow)) {
          setRowStyleName(hoveringRow, style.cellTableHoveredRow(),
              style.cellTableHoveredRowCell(), false);
        }
        hoveringRow = tr;
        setRowStyleName(hoveringRow, style.cellTableHoveredRow(),
            style.cellTableHoveredRowCell(), true);
      } else if ("mouseout".equals(eventType) && hoveringRow != null) {
        setRowStyleName(hoveringRow, style.cellTableHoveredRow(),
            style.cellTableHoveredRowCell(), false);
        hoveringRow = null;
      } else if (isClick
          && ((getPresenter().getKeyboardSelectedRowInView() != row) || (keyboardSelectedColumn != col))) {
        // Move keyboard focus. Since the user clicked, allow focus to go to a
        // non-interactive column.
        boolean isFocusable = CellBasedWidgetImpl.get().isFocusable(target);
        isFocused = isFocused || isFocusable;
        keyboardSelectedColumn = col;
        getPresenter().setKeyboardSelectedRow(row, !isFocusable, true);
      }

      // Update selection. Selection occurs before firing the event to the cell
      // in case the cell operates on the currently selected item.
      if (!isRowWithinBounds(row)) {
        // If the event causes us to page, then the physical index will be out
        // of bounds of the underlying data.
        return;
      }
      boolean isSelectionHandled = handlesSelection
          || KeyboardSelectionPolicy.BOUND_TO_SELECTION == getKeyboardSelectionPolicy();
      T value = getVisibleItem(row);
      Context context = new Context(row + getPageStart(), col,
          getValueKey(value));
      CellPreviewEvent<T> previewEvent = CellPreviewEvent.fire(this, event,
          this, context, value, cellIsEditing, isSelectionHandled);
      if (isClick && !cellIsEditing && !isSelectionHandled) {
        doSelection(event, value, row, col);
      }

      // Pass the event to the cell.
      if (!previewEvent.isCanceled()) {
        fireEventToCell(event, eventType, tableCell, value, context,
            columns.get(col));
      }
    }
  }

  @Override
  protected void onFocus() {
    Element elem = getKeyboardSelectedElement();
    if (elem != null) {
      TableCellElement td = elem.getParentElement().cast();
      TableRowElement tr = td.getParentElement().cast();
      td.addClassName(style.cellTableKeyboardSelectedCell());
      setRowStyleName(tr, style.cellTableKeyboardSelectedRow(),
          style.cellTableKeyboardSelectedRowCell(), true);
    }
  }

  @Override
  protected void renderRowValues(SafeHtmlBuilder sb, List<T> values, int start,
      SelectionModel<? super T> selectionModel) {
    createHeadersAndFooters();

    int keyboardSelectedRow = getKeyboardSelectedRow() + getPageStart();
    String evenRowStyle = style.cellTableEvenRow();
    String oddRowStyle = style.cellTableOddRow();
    String cellStyle = style.cellTableCell();
    String evenCellStyle = " " + style.cellTableEvenRowCell();
    String oddCellStyle = " " + style.cellTableOddRowCell();
    String firstColumnStyle = " " + style.cellTableFirstColumn();
    String lastColumnStyle = " " + style.cellTableLastColumn();
    String selectedRowStyle = " " + style.cellTableSelectedRow();
    String selectedCellStyle = " " + style.cellTableSelectedRowCell();
    String keyboardRowStyle = " " + style.cellTableKeyboardSelectedRow();
    String keyboardRowCellStyle = " "
        + style.cellTableKeyboardSelectedRowCell();
    String keyboardCellStyle = " " + style.cellTableKeyboardSelectedCell();
    int columnCount = columns.size();
    int length = values.size();
    int end = start + length;
    for (int i = start; i < end; i++) {
      T value = values.get(i - start);
      boolean isSelected = (selectionModel == null || value == null) ? false
          : selectionModel.isSelected(value);
      boolean isEven = i % 2 == 0;
      boolean isKeyboardSelected = i == keyboardSelectedRow && isFocused;
      String trClasses = isEven ? evenRowStyle : oddRowStyle;
      if (isSelected) {
        trClasses += selectedRowStyle;
      }
      if (isKeyboardSelected) {
        trClasses += keyboardRowStyle;
      }

      if (rowStyles != null) {
        String extraRowStyles = rowStyles.getStyleNames(value, i);
        if (extraRowStyles != null) {
          trClasses += " ";
          trClasses += extraRowStyles;
        }
      }

      SafeHtmlBuilder trBuilder = new SafeHtmlBuilder();
      int curColumn = 0;
      for (Column<T, ?> column : columns) {
        String tdClasses = cellStyle;
        tdClasses += isEven ? evenCellStyle : oddCellStyle;
        if (curColumn == 0) {
          tdClasses += firstColumnStyle;
        }
        if (isSelected) {
          tdClasses += selectedCellStyle;
        }
        if (isKeyboardSelected) {
          tdClasses += keyboardRowCellStyle;
        }
        // The first and last column could be the same column.
        if (curColumn == columnCount - 1) {
          tdClasses += lastColumnStyle;
        }

        SafeHtmlBuilder cellBuilder = new SafeHtmlBuilder();
        if (value != null) {
          Context context = new Context(i, curColumn, getValueKey(value));
          column.render(context, value, cellBuilder);
        }

        // Build the contents.
        SafeHtml contents = SafeHtmlUtils.EMPTY_SAFE_HTML;
        if (i == keyboardSelectedRow && curColumn == keyboardSelectedColumn) {
          // This is the focused cell.
          if (isFocused) {
            tdClasses += keyboardCellStyle;
          }
          char accessKey = getAccessKey();
          if (accessKey != 0) {
            contents = template.divFocusableWithKey(getTabIndex(), accessKey,
                cellBuilder.toSafeHtml());
          } else {
            contents = template.divFocusable(getTabIndex(),
                cellBuilder.toSafeHtml());
          }
        } else {
          contents = template.div(cellBuilder.toSafeHtml());
        }

        // Build the cell.
        HorizontalAlignmentConstant hAlign = column.getHorizontalAlignment();
        VerticalAlignmentConstant vAlign = column.getVerticalAlignment();
        if (hAlign != null && vAlign != null) {
          trBuilder.append(template.tdBothAlign(tdClasses,
              hAlign.getTextAlignString(), vAlign.getVerticalAlignString(),
              contents));
        } else if (hAlign != null) {
          trBuilder.append(template.tdHorizontalAlign(tdClasses,
              hAlign.getTextAlignString(), contents));
        } else if (vAlign != null) {
          trBuilder.append(template.tdVerticalAlign(tdClasses,
              vAlign.getVerticalAlignString(), contents));
        } else {
          trBuilder.append(template.td(tdClasses, contents));
        }

        curColumn++;
      }

      sb.append(template.tr(trClasses, trBuilder.toSafeHtml()));
    }
  }

  @Override
  protected void replaceAllChildren(List<T> values, SafeHtml html) {
    TABLE_IMPL.replaceAllRows(CellTable.this, tbody,
        CellBasedWidgetImpl.get().processHtml(html));
  }

  @Override
  protected boolean resetFocusOnCell() {
    int row = getKeyboardSelectedRow();
    if (isRowWithinBounds(row) && columns.size() > 0) {
      Column<T, ?> column = columns.get(keyboardSelectedColumn);
      return resetFocusOnCellImpl(row, keyboardSelectedColumn, column);
    }
    return false;
  }

  @Override
  protected void setKeyboardSelected(int index, boolean selected,
      boolean stealFocus) {
    if (KeyboardSelectionPolicy.DISABLED == getKeyboardSelectionPolicy()
        || !isRowWithinBounds(index) || columns.size() == 0) {
      return;
    }

    TableRowElement tr = getRowElement(index);
    String cellStyle = style.cellTableKeyboardSelectedCell();
    boolean updatedSelection = !selected || isFocused || stealFocus;
    setRowStyleName(tr, style.cellTableKeyboardSelectedRow(),
        style.cellTableKeyboardSelectedRowCell(), selected);
    NodeList<TableCellElement> cells = tr.getCells();
    for (int i = 0; i < cells.getLength(); i++) {
      TableCellElement td = cells.getItem(i);

      // Update the selected style.
      setStyleName(td, cellStyle, updatedSelection && selected
          && i == keyboardSelectedColumn);

      // Mark as focusable.
      final com.google.gwt.user.client.Element cellParent = getCellParent(td).cast();
      setFocusable(cellParent, selected && i == keyboardSelectedColumn);
    }

    // Move focus to the cell.
    if (selected && stealFocus && !cellIsEditing) {
      TableCellElement td = tr.getCells().getItem(keyboardSelectedColumn);
      final com.google.gwt.user.client.Element cellParent = getCellParent(td).cast();
      CellBasedWidgetImpl.get().resetFocus(new Scheduler.ScheduledCommand() {
        public void execute() {
          cellParent.focus();
        }
      });
    }
  }

  /**
   * @deprecated this method is never called by AbstractHasData, render the
   *             selected styles in
   *             {@link #renderRowValues(SafeHtmlBuilder, List, int, SelectionModel)}
   */
  @Override
  @Deprecated
  protected void setSelected(Element elem, boolean selected) {
    TableRowElement tr = elem.cast();
    setRowStyleName(tr, style.cellTableSelectedRow(),
        style.cellTableSelectedRowCell(), selected);
  }

  @Override
  void setLoadingState(LoadingState state) {
    setLoadingIconVisible(state == LoadingState.LOADING);
  }

  /**
   * Check that the specified column is within bounds.
   * 
   * @param col the column index
   * @throws IndexOutOfBoundsException if the column is out of bounds
   */
  private void checkColumnBounds(int col) {
    if (col < 0 || col >= getColumnCount()) {
      throw new IndexOutOfBoundsException("Column index is out of bounds: "
          + col);
    }
  }

  /**
   * Render the header or footer.
   * 
   * @param isFooter true if this is the footer table, false if the header table
   */
  private void createHeaders(boolean isFooter) {
    List<Header<?>> theHeaders = isFooter ? footers : headers;
    TableSectionElement section = isFooter ? tfoot : thead;
    String className = isFooter ? style.cellTableFooter()
        : style.cellTableHeader();

    boolean hasHeader = false;
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    sb.appendHtmlConstant("<tr>");
    int columnCount = columns.size();
    if (columnCount > 0) {
      // Setup the first column.
      Header<?> prevHeader = theHeaders.get(0);
      int prevColspan = 1;
      StringBuilder classesBuilder = new StringBuilder(className);
      classesBuilder.append(" ");
      classesBuilder.append(isFooter ? style.cellTableFirstColumnFooter()
          : style.cellTableFirstColumnHeader());

      // Loop through all column headers.
      int curColumn;
      for (curColumn = 1; curColumn < columnCount; curColumn++) {
        Header<?> header = theHeaders.get(curColumn);

        if (header != prevHeader) {
          // The header has changed, so append the previous one.
          SafeHtmlBuilder headerBuilder = new SafeHtmlBuilder();
          if (prevHeader != null) {
            hasHeader = true;
            Context context = new Context(0, curColumn - prevColspan,
                prevHeader.getKey());
            prevHeader.render(context, headerBuilder);
          }
          sb.append(template.th(prevColspan, classesBuilder.toString(),
              headerBuilder.toSafeHtml()));

          // Reset the previous header.
          prevHeader = header;
          prevColspan = 1;
          classesBuilder = new StringBuilder(className);
        } else {
          // Increment the colspan if the headers == each other.
          prevColspan++;
        }
      }

      // Append the last header.
      SafeHtmlBuilder headerBuilder = new SafeHtmlBuilder();
      if (prevHeader != null) {
        hasHeader = true;
        Context context = new Context(0, curColumn - prevColspan,
            prevHeader.getKey());
        prevHeader.render(context, headerBuilder);
      }

      // The first and last columns could be the same column.
      classesBuilder.append(" ");
      classesBuilder.append(isFooter ? style.cellTableLastColumnFooter()
          : style.cellTableLastColumnHeader());
      sb.append(template.th(prevColspan, classesBuilder.toString(),
          headerBuilder.toSafeHtml()));
    }
    sb.appendHtmlConstant("</tr>");

    // Render the section contents.
    TABLE_IMPL.replaceAllRows(this, section, sb.toSafeHtml());

    // If the section isn't used, hide it.
    setVisible(section, hasHeader);
  }

  private void createHeadersAndFooters() {
    createHeaders(false);
    createHeaders(true);
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
   * Find and return the index of the next interactive column. If no column is
   * interactive, 0 is returned. If the start index is the only interactive
   * column, it is returned.
   * 
   * @param start the start index, exclusive unless it is the only option
   * @param reverse true to do a reverse search
   * @return the interactive column index, or 0 if not interactive
   */
  private int findInteractiveColumn(int start, boolean reverse) {
    if (!isInteractive) {
      return 0;
    } else if (reverse) {
      for (int i = start - 1; i >= 0; i--) {
        if (isColumnInteractive(columns.get(i))) {
          return i;
        }
      }
      // Wrap to the end.
      for (int i = columns.size() - 1; i >= start; i--) {
        if (isColumnInteractive(columns.get(i))) {
          return i;
        }
      }
    } else {
      for (int i = start + 1; i < columns.size(); i++) {
        if (isColumnInteractive(columns.get(i))) {
          return i;
        }
      }
      // Wrap to the start.
      for (int i = 0; i <= start; i++) {
        if (isColumnInteractive(columns.get(i))) {
          return i;
        }
      }
    }
    return 0;
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
  private <C> void fireEventToCell(Event event, String eventType,
      TableCellElement tableCell, T value, Context context, Column<T, C> column) {
    Cell<C> cell = column.getCell();
    if (cellConsumesEventType(cell, eventType)) {
      C cellValue = column.getValue(value);
      Element parentElem = getCellParent(tableCell);
      boolean cellWasEditing = cell.isEditing(context, parentElem, cellValue);
      column.onBrowserEvent(context, parentElem, value, event);
      cellIsEditing = cell.isEditing(context, parentElem, cellValue);
      if (cellWasEditing && !cellIsEditing) {
        CellBasedWidgetImpl.get().resetFocus(new Scheduler.ScheduledCommand() {
          public void execute() {
            setFocus(true);
          }
        });
      }
    }
  }

  /**
   * Get the parent element that is passed to the {@link Cell} from the table
   * cell element.
   * 
   * @param td the table cell
   * @return the parent of the {@link Cell}
   */
  private Element getCellParent(TableCellElement td) {
    return td.getFirstChildElement();
  }

  private native int getClientHeight(Element element) /*-{
    return element.clientHeight;
  }-*/;

  private boolean handleKey(Event event) {
    HasDataPresenter<T> presenter = getPresenter();
    int oldRow = getKeyboardSelectedRow();
    boolean isRtl = LocaleInfo.getCurrentLocale().isRTL();
    int keyCodeLineEnd = isRtl ? KeyCodes.KEY_LEFT : KeyCodes.KEY_RIGHT;
    int keyCodeLineStart = isRtl ? KeyCodes.KEY_RIGHT : KeyCodes.KEY_LEFT;
    int keyCode = event.getKeyCode();
    if (keyCode == keyCodeLineEnd) {
      int nextColumn = findInteractiveColumn(keyboardSelectedColumn, false);
      if (nextColumn <= keyboardSelectedColumn) {
        // Wrap to the next row.
        if (presenter.hasKeyboardNext()) {
          keyboardSelectedColumn = nextColumn;
          presenter.keyboardNext();
          event.preventDefault();
          return true;
        }
      } else {
        // Reselect the row to move the selected column.
        keyboardSelectedColumn = nextColumn;
        getPresenter().setKeyboardSelectedRow(oldRow, true, true);
        event.preventDefault();
        return true;
      }
    } else if (keyCode == keyCodeLineStart) {
      int prevColumn = findInteractiveColumn(keyboardSelectedColumn, true);
      if (prevColumn >= keyboardSelectedColumn) {
        // Wrap to the previous row.
        if (presenter.hasKeyboardPrev()) {
          keyboardSelectedColumn = prevColumn;
          presenter.keyboardPrev();
          event.preventDefault();
          return true;
        }
      } else {
        // Reselect the row to move the selected column.
        keyboardSelectedColumn = prevColumn;
        getPresenter().setKeyboardSelectedRow(oldRow, true, true);
        event.preventDefault();
        return true;
      }
    }

    return false;
  }

  /**
   * Check if a column consumes events.
   */
  private boolean isColumnInteractive(Column<T, ?> column) {
    Set<String> consumedEvents = column.getCell().getConsumedEvents();
    return consumedEvents != null && consumedEvents.size() > 0;
  }

  private <C> boolean resetFocusOnCellImpl(int row, int col, Column<T, C> column) {
    Element parent = getKeyboardSelectedElement();
    T value = getVisibleItem(row);
    Object key = getValueKey(value);
    C cellValue = column.getValue(value);
    Cell<C> cell = column.getCell();
    Context context = new Context(row + getPageStart(), col, key);
    return cell.resetFocus(context, parent, cellValue);
  }

  /**
   * Show or hide the loading icon.
   * 
   * @param visible true to show, false to hide.
   */
  private void setLoadingIconVisible(boolean visible) {
    // Clear the current data.
    if (visible) {
      tbody.getStyle().setDisplay(Display.NONE);
    } else {
      tbody.getStyle().clearDisplay();
    }

    // Update the colspan.
    TableCellElement td = tbodyLoading.getRows().getItem(0).getCells().getItem(
        0);
    td.setColSpan(Math.max(1, columns.size()));
    setVisible(tbodyLoading, visible);
  }

  /**
   * Apply a style to a row and all cells in the row.
   * 
   * @param tr the row element
   * @param rowStyle the style to apply to the row
   * @param cellStyle the style to apply to the cells
   * @param add true to add the style, false to remove
   */
  private void setRowStyleName(TableRowElement tr, String rowStyle,
      String cellStyle, boolean add) {
    setStyleName(tr, rowStyle, add);
    NodeList<TableCellElement> cells = tr.getCells();
    for (int i = 0; i < cells.getLength(); i++) {
      setStyleName(cells.getItem(i), cellStyle, add);
    }
  }

  /**
   * Update the dependsOnSelection and handlesSelection booleans.
   */
  private void updateDependsOnSelection() {
    dependsOnSelection = false;
    handlesSelection = false;
    isInteractive = false;
    for (Column<T, ?> column : columns) {
      Cell<?> cell = column.getCell();
      if (cell.dependsOnSelection()) {
        dependsOnSelection = true;
      }
      if (cell.handlesSelection()) {
        handlesSelection = true;
      }
      if (isColumnInteractive(column)) {
        isInteractive = true;
      }
    }
  }
}
