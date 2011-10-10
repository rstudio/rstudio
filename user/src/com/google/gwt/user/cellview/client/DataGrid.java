/*
 * Copyright 2011 Google Inc.
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
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.TableLayout;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.TableColElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.ImportedWithPrefix;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.user.cellview.client.LoadingStateChangeEvent.LoadingState;
import com.google.gwt.user.client.ui.CustomScrollPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HeaderPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;

/**
 * A tabular view with a fixed header and footer section and a scrollable data
 * section in the middle. This widget supports paging and columns.
 * 
 * <p>
 * <h3>Columns</h3> The {@link Column} class defines the
 * {@link com.google.gwt.cell.client.Cell} used to render a column. Implement
 * {@link Column#getValue(Object)} to retrieve the field value from the row
 * object that will be rendered in the {@link com.google.gwt.cell.client.Cell}.
 * </p>
 * 
 * <p>
 * <h3>Headers and Footers</h3> A {@link Header} can be placed at the top
 * (header) or bottom (footer) of the {@link DataGrid}. You can specify a header
 * as text using {@link #addColumn(Column, String)}, or you can create a custom
 * {@link Header} that can change with the value of the cells, such as a column
 * total. The {@link Header} will be rendered every time the row data changes or
 * the table is redrawn. If you pass the same header instance (==) into adjacent
 * columns, the header will span the columns.
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
public class DataGrid<T> extends AbstractCellTable<T> implements RequiresResize {

  /**
   * A ClientBundle that provides images for this widget.
   */
  public interface Resources extends ClientBundle {
    /**
     * The loading indicator used while the table is waiting for data.
     */
    @Source("cellTableLoading.gif")
    @ImageOptions(flipRtl = true)
    ImageResource dataGridLoading();

    /**
     * Icon used when a column is sorted in ascending order.
     */
    @Source("sortAscending.png")
    @ImageOptions(flipRtl = true)
    ImageResource dataGridSortAscending();

    /**
     * Icon used when a column is sorted in descending order.
     */
    @Source("sortDescending.png")
    @ImageOptions(flipRtl = true)
    ImageResource dataGridSortDescending();

    /**
     * The styles used in this widget.
     */
    @Source(Style.DEFAULT_CSS)
    Style dataGridStyle();
  }

  /**
   * Styles used by this widget.
   */
  @ImportedWithPrefix("gwt-CellTable")
  public interface Style extends CssResource {
    /**
     * The path to the default CSS styles used by this resource.
     */
    String DEFAULT_CSS = "com/google/gwt/user/cellview/client/DataGrid.css";

    /**
     * Applied to every cell.
     */
    String dataGridCell();

    /**
     * Applied to even rows.
     */
    String dataGridEvenRow();

    /**
     * Applied to cells in even rows.
     */
    String dataGridEvenRowCell();

    /**
     * Applied to the first column.
     */
    String dataGridFirstColumn();

    /**
     * Applied to the first column footers.
     */
    String dataGridFirstColumnFooter();

    /**
     * Applied to the first column headers.
     */
    String dataGridFirstColumnHeader();

    /**
     * Applied to footers cells.
     */
    String dataGridFooter();

    /**
     * Applied to headers cells.
     */
    String dataGridHeader();

    /**
     * Applied to the hovered row.
     */
    String dataGridHoveredRow();

    /**
     * Applied to the cells in the hovered row.
     */
    String dataGridHoveredRowCell();

    /**
     * Applied to the keyboard selected cell.
     */
    String dataGridKeyboardSelectedCell();

    /**
     * Applied to the keyboard selected row.
     */
    String dataGridKeyboardSelectedRow();

    /**
     * Applied to the cells in the keyboard selected row.
     */
    String dataGridKeyboardSelectedRowCell();

    /**
     * Applied to the last column.
     */
    String dataGridLastColumn();

    /**
     * Applied to the last column footers.
     */
    String dataGridLastColumnFooter();

    /**
     * Applied to the last column headers.
     */
    String dataGridLastColumnHeader();

    /**
     * Applied to odd rows.
     */
    String dataGridOddRow();

    /**
     * Applied to cells in odd rows.
     */
    String dataGridOddRowCell();

    /**
     * Applied to selected rows.
     */
    String dataGridSelectedRow();

    /**
     * Applied to cells in selected rows.
     */
    String dataGridSelectedRowCell();

    /**
     * Applied to header cells that are sortable.
     */
    String dataGridSortableHeader();

    /**
     * Applied to header cells that are sorted in ascending order.
     */
    String dataGridSortedHeaderAscending();

    /**
     * Applied to header cells that are sorted in descending order.
     */
    String dataGridSortedHeaderDescending();

    /**
     * Applied to the table.
     */
    String dataGridWidget();
  }

  /**
   * A simple widget wrapper around a table element.
   */
  static class TableWidget extends Widget {
    private final TableColElement colgroup;
    private TableSectionElement section;
    private final TableElement tableElem;

    public TableWidget() {
      // Setup the table.
      tableElem = Document.get().createTableElement();
      tableElem.setCellSpacing(0);
      tableElem.getStyle().setTableLayout(TableLayout.FIXED);
      tableElem.getStyle().setWidth(100.0, Unit.PCT);
      setElement(tableElem);

      // Add the colgroup.
      colgroup = Document.get().createColGroupElement();
      tableElem.appendChild(colgroup);
    }

    public void addColumnStyleName(int index, String styleName) {
      ensureTableColElement(index).addClassName(styleName);
    }

    /**
     * Get the {@link TableColElement} at the specified index, creating it if
     * necessary.
     * 
     * @param index the column index
     * @return the {@link TableColElement}
     */
    public TableColElement ensureTableColElement(int index) {
      // Ensure that we have enough columns.
      for (int i = colgroup.getChildCount(); i <= index; i++) {
        colgroup.appendChild(Document.get().createColElement());
      }
      return colgroup.getChild(index).cast();
    }

    public void removeColumnStyleName(int index, String styleName) {
      if (index >= colgroup.getChildCount()) {
        return;
      }
      ensureTableColElement(index).removeClassName(styleName);
    }

    /**
     * Hide columns that aren't used in the table.
     * 
     * @param start the first unused column index
     */
    void hideUnusedColumns(int start) {
      /*
       * Set the width to zero for all col elements that appear after the last
       * column. Clearing the width would cause it to take up the remaining
       * width in a fixed layout table.
       */
      int colCount = colgroup.getChildCount();
      for (int i = start; i < colCount; i++) {
        setColumnWidth(i, "0px");
      }
    }

    void setColumnWidth(int column, String width) {
      if (width == null) {
        ensureTableColElement(column).getStyle().clearWidth();
      } else {
        ensureTableColElement(column).getStyle().setProperty("width", width);
      }
    }
  }

  /**
   * Adapter class to convert {@link Resources} to
   * {@link AbstractCellTable.Resources}.
   */
  private static class ResourcesAdapter implements AbstractCellTable.Resources {

    private final DataGrid.Resources resources;
    private final StyleAdapter style;

    public ResourcesAdapter(DataGrid.Resources resources) {
      this.resources = resources;
      this.style = new StyleAdapter(resources.dataGridStyle());
    }

    @Override
    public ImageResource sortAscending() {
      return resources.dataGridSortAscending();
    }

    @Override
    public ImageResource sortDescending() {
      return resources.dataGridSortDescending();
    }

    @Override
    public AbstractCellTable.Style style() {
      return style;
    }
  }

  /**
   * Adapter class to convert {@link Style} to {@link AbstractCellTable.Style}.
   */
  private static class StyleAdapter implements AbstractCellTable.Style {
    private final DataGrid.Style style;

    public StyleAdapter(DataGrid.Style style) {
      this.style = style;
    }

    @Override
    public String cell() {
      return style.dataGridCell();
    }

    @Override
    public String evenRow() {
      return style.dataGridEvenRow();
    }

    @Override
    public String evenRowCell() {
      return style.dataGridEvenRowCell();
    }

    @Override
    public String firstColumn() {
      return style.dataGridFirstColumn();
    }

    @Override
    public String firstColumnFooter() {
      return style.dataGridFirstColumnFooter();
    }

    @Override
    public String firstColumnHeader() {
      return style.dataGridFirstColumnHeader();
    }

    @Override
    public String footer() {
      return style.dataGridFooter();
    }

    @Override
    public String header() {
      return style.dataGridHeader();
    }

    @Override
    public String hoveredRow() {
      return style.dataGridHoveredRow();
    }

    @Override
    public String hoveredRowCell() {
      return style.dataGridHoveredRowCell();
    }

    @Override
    public String keyboardSelectedCell() {
      return style.dataGridKeyboardSelectedCell();
    }

    @Override
    public String keyboardSelectedRow() {
      return style.dataGridKeyboardSelectedRow();
    }

    @Override
    public String keyboardSelectedRowCell() {
      return style.dataGridKeyboardSelectedRowCell();
    }

    @Override
    public String lastColumn() {
      return style.dataGridLastColumn();
    }

    @Override
    public String lastColumnFooter() {
      return style.dataGridLastColumnFooter();
    }

    @Override
    public String lastColumnHeader() {
      return style.dataGridLastColumnHeader();
    }

    @Override
    public String oddRow() {
      return style.dataGridOddRow();
    }

    @Override
    public String oddRowCell() {
      return style.dataGridOddRowCell();
    }

    @Override
    public String selectedRow() {
      return style.dataGridSelectedRow();
    }

    @Override
    public String selectedRowCell() {
      return style.dataGridSelectedRowCell();
    }

    @Override
    public String sortableHeader() {
      return style.dataGridSortableHeader();
    }

    @Override
    public String sortedHeaderAscending() {
      return style.dataGridSortedHeaderAscending();
    }

    @Override
    public String sortedHeaderDescending() {
      return style.dataGridSortedHeaderDescending();
    }

    @Override
    public String widget() {
      return style.dataGridWidget();
    }
  }

  private static final int DEFAULT_PAGESIZE = 50;
  private static Resources DEFAULT_RESOURCES;

  /**
   * Create the default loading indicator using the loading image in the
   * specified {@link Resources}.
   * 
   * @param resources the resources containing the loading image
   * @return a widget loading indicator
   */
  private static Widget createDefaultLoadingIndicator(Resources resources) {
    ImageResource loadingImg = resources.dataGridLoading();
    if (loadingImg == null) {
      return null;
    }
    Image image = new Image(loadingImg);
    image.getElement().getStyle().setMarginTop(30.0, Unit.PX);
    return image;
  }

  private static Resources getDefaultResources() {
    if (DEFAULT_RESOURCES == null) {
      DEFAULT_RESOURCES = GWT.create(Resources.class);
    }
    return DEFAULT_RESOURCES;
  }

  final TableWidget tableData;
  final TableWidget tableFooter;
  final TableWidget tableHeader;
  private final FlexTable emptyTableWidgetContainer;
  private final HeaderPanel headerPanel;
  private final FlexTable loadingIndicatorContainer;
  private final Style style;
  private final Element tableDataContainer;
  private final ScrollPanel tableDataScroller;
  private final SimplePanel tableFooterContainer;
  private final Element tableFooterScroller;
  private final SimplePanel tableHeaderContainer;
  private final Element tableHeaderScroller;

  /**
   * Constructs a table with a default page size of 50.
   */
  public DataGrid() {
    this(DEFAULT_PAGESIZE);
  }

  /**
   * Constructs a table with the given page size.
   * 
   * @param pageSize the page size
   */
  public DataGrid(final int pageSize) {
    this(pageSize, getDefaultResources());
  }

  /**
   * Constructs a table with the given page size and the given
   * {@link ProvidesKey key provider}.
   * 
   * @param pageSize the page size
   * @param keyProvider an instance of ProvidesKey<T>, or null if the record
   *          object should act as its own key
   */
  public DataGrid(int pageSize, ProvidesKey<T> keyProvider) {
    this(pageSize, getDefaultResources(), keyProvider);
  }

  /**
   * Constructs a table with the given page size with the specified
   * {@link Resources}.
   * 
   * @param pageSize the page size
   * @param resources the resources to use for this widget
   */
  public DataGrid(int pageSize, Resources resources) {
    this(pageSize, resources, null);
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
  public DataGrid(int pageSize, Resources resources, ProvidesKey<T> keyProvider) {
    this(pageSize, resources, keyProvider, createDefaultLoadingIndicator(resources));
  }

  /**
   * Constructs a table with the given page size, the specified
   * {@link Resources}, and the given key provider.
   * 
   * @param pageSize the page size
   * @param resources the resources to use for this widget
   * @param keyProvider an instance of ProvidesKey<T>, or null if the record
   *          object should act as its own key
   * @param loadingIndicator the widget to use as a loading indicator, or null
   *          to disable
   */
  public DataGrid(int pageSize, Resources resources, ProvidesKey<T> keyProvider,
      Widget loadingIndicator) {
    super(new HeaderPanel(), pageSize, new ResourcesAdapter(resources), keyProvider);
    headerPanel = (HeaderPanel) getWidget();

    // Inject the stylesheet.
    this.style = resources.dataGridStyle();
    this.style.ensureInjected();

    // Create the header and footer widgets..
    tableHeader = new TableWidget();
    tableHeader.section = tableHeader.tableElem.createTHead();
    tableFooter = new TableWidget();
    tableFooter.section = tableFooter.tableElem.createTFoot();

    /*
     * Wrap the header and footer widgets in a div because we cannot set the
     * min-width of a table element. We set the width/min-width of the div
     * container instead.
     */
    tableHeaderContainer = new SimplePanel(tableHeader);
    tableFooterContainer = new SimplePanel(tableFooter);

    /*
     * Get the element that wraps the container so we can adjust its scroll
     * position.
     */
    headerPanel.setHeaderWidget(tableHeaderContainer);
    tableHeaderScroller = tableHeaderContainer.getElement().getParentElement();
    headerPanel.setFooterWidget(tableFooterContainer);
    tableFooterScroller = tableFooterContainer.getElement().getParentElement();

    /*
     * Set overflow to hidden on the scrollable elements so we can change the
     * scrollLeft position.
     */
    tableHeaderScroller.getStyle().setOverflow(Overflow.HIDDEN);
    tableFooterScroller.getStyle().setOverflow(Overflow.HIDDEN);

    // Create the body.
    tableData = new TableWidget();
    if (tableData.tableElem.getTBodies().getLength() > 0) {
      tableData.section = tableData.tableElem.getTBodies().getItem(0);
    } else {
      tableData.section = Document.get().createTBodyElement();
      tableData.tableElem.appendChild(tableData.section);
    }
    tableDataScroller = new CustomScrollPanel(tableData);
    tableDataScroller.setHeight("100%");
    headerPanel.setContentWidget(tableDataScroller);
    tableDataContainer = tableData.getElement().getParentElement();

    /*
     * CustomScrollPanel applies the inline block style to the container
     * element, but we want the container to fill the available width.
     */
    tableDataContainer.getStyle().setDisplay(Display.BLOCK);

    /*
     * Create the containers for the empty table message and loading indicator.
     * The containers are centered tables that contain one cell, which aligns
     * the widget in the center of the panel.
     */
    emptyTableWidgetContainer = new FlexTable();
    emptyTableWidgetContainer.getElement().setAttribute("align", "center");
    loadingIndicatorContainer = new FlexTable();
    loadingIndicatorContainer.getElement().setAttribute("align", "center");

    // Set the loading indicator.
    setLoadingIndicator(loadingIndicator); // Can be null.

    // Synchronize the scroll positions of the three tables.
    tableDataScroller.addScrollHandler(new ScrollHandler() {
      @Override
      public void onScroll(ScrollEvent event) {
        int scrollLeft = tableDataScroller.getHorizontalScrollPosition();
        tableHeaderScroller.setScrollLeft(scrollLeft);
        tableFooterScroller.setScrollLeft(scrollLeft);
      }
    });
  }

  /**
   * Constructs a table with a default page size of 50, and the given
   * {@link ProvidesKey key provider}.
   * 
   * @param keyProvider an instance of ProvidesKey<T>, or null if the record
   *          object should act as its own key
   */
  public DataGrid(ProvidesKey<T> keyProvider) {
    this(DEFAULT_PAGESIZE, keyProvider);
  }

  @Override
  public void addColumnStyleName(int index, String styleName) {
    tableHeader.addColumnStyleName(index, styleName);
    tableFooter.addColumnStyleName(index, styleName);
    tableData.addColumnStyleName(index, styleName);
  }

  /**
   * Clear the width of the tables in this widget, which causes them to fill the
   * available width.
   * 
   * <p>
   * The table width is not the same as the width of this widget. If the tables
   * are narrower than this widget, there will be a gap between the table and
   * the edge of the widget. If the tables are wider than this widget, a
   * horizontal scrollbar will appear so the user can scroll horizontally.
   * </p>
   * 
   * @see #setMinimumTableWidth(double, Unit)
   * @see #setTableWidth(double, Unit)
   */
  public void clearTableWidth() {
    tableHeaderContainer.getElement().getStyle().clearWidth();
    tableFooterContainer.getElement().getStyle().clearWidth();
    tableDataContainer.getStyle().clearWidth();
  }

  @Override
  public void onResize() {
    headerPanel.onResize();
  }

  @Override
  public void removeColumnStyleName(int index, String styleName) {
    tableHeader.removeColumnStyleName(index, styleName);
    tableFooter.removeColumnStyleName(index, styleName);
    tableData.removeColumnStyleName(index, styleName);
  }

  @Override
  public void setEmptyTableWidget(Widget widget) {
    emptyTableWidgetContainer.setWidget(0, 0, widget);
    super.setEmptyTableWidget(widget);
  }

  @Override
  public void setLoadingIndicator(Widget widget) {
    loadingIndicatorContainer.setWidget(0, 0, widget);
    super.setLoadingIndicator(widget);
  }

  /**
   * Set the minimum width of the tables in this widget. If the widget become
   * narrower than the minimum width, a horizontal scrollbar will appear so the
   * user can scroll horizontally.
   * 
   * <p>
   * Note that this method is not supported in IE6 and earlier versions of IE.
   * </p>
   * 
   * @param value the width
   * @param unit the unit of the width
   * @see #setTableWidth(double, Unit)
   */
  public void setMinimumTableWidth(double value, Unit unit) {
    /*
     * The min-width style attribute doesn't apply to tables, so we set the
     * min-width of the element that contains the table instead. The table width
     * is fixed at 100%.
     */
    tableHeaderContainer.getElement().getStyle().setProperty("minWidth", value, unit);
    tableFooterContainer.getElement().getStyle().setProperty("minWidth", value, unit);
    tableDataContainer.getStyle().setProperty("minWidth", value, unit);
  }

  /**
   * Set the width of the tables in this widget. By default, the width is not
   * set and the tables take the available width.
   * 
   * <p>
   * The table width is not the same as the width of this widget. If the tables
   * are narrower than this widget, there will be a gap between the table and
   * the edge of the widget. If the tables are wider than this widget, a
   * horizontal scrollbar will appear so the user can scroll horizontally.
   * </p>
   * 
   * <p>
   * If your table has many columns and you want to ensure that the columns are
   * not truncated, you probably want to use
   * {@link #setMinimumTableWidth(double, Unit)} instead. That will ensure that
   * the table is wide enough, but it will still allow the table to expand to
   * 100% if the user has a wide screen.
   * </p>
   * 
   * <p>
   * Note that setting the width in percentages will not work on older versions
   * of IE because it does not account for scrollbars when calculating the
   * width.
   * </p>
   * 
   * @param value the width
   * @param unit the unit of the width
   * @see #setMinimumTableWidth(double, Unit)
   */
  public void setTableWidth(double value, Unit unit) {
    /*
     * The min-width style attribute doesn't apply to tables, so we set the
     * min-width of the element that contains the table instead. For
     * consistency, we set the width of the container as well.
     */
    tableHeaderContainer.getElement().getStyle().setWidth(value, unit);
    tableFooterContainer.getElement().getStyle().setWidth(value, unit);
    tableDataContainer.getStyle().setWidth(value, unit);
  }

  @Override
  protected void doSetColumnWidth(int column, String width) {
    if (width == null) {
      tableData.ensureTableColElement(column).getStyle().clearWidth();
      tableHeader.ensureTableColElement(column).getStyle().clearWidth();
      tableFooter.ensureTableColElement(column).getStyle().clearWidth();
    } else {
      tableData.ensureTableColElement(column).getStyle().setProperty("width", width);
      tableHeader.ensureTableColElement(column).getStyle().setProperty("width", width);
      tableFooter.ensureTableColElement(column).getStyle().setProperty("width", width);
    }
  }

  @Override
  protected void doSetHeaderVisible(boolean isFooter, boolean isVisible) {
    if (isFooter) {
      headerPanel.setFooterWidget(isVisible ? tableFooterContainer : null);
    } else {
      headerPanel.setHeaderWidget(isVisible ? tableHeaderContainer : null);
    }
  }

  @Override
  protected TableSectionElement getTableBodyElement() {
    return tableData.section;
  }

  @Override
  protected TableSectionElement getTableFootElement() {
    return tableFooter.section;
  }

  @Override
  protected TableSectionElement getTableHeadElement() {
    return tableHeader.section;
  }

  /**
   * Called when the loading state changes.
   * 
   * @param state the new loading state
   */
  @Override
  protected void onLoadingStateChanged(LoadingState state) {
    Widget message = tableData;
    if (state == LoadingState.LOADING) {
      // Loading indicator.
      message = loadingIndicatorContainer;
    } else if (state == LoadingState.LOADED && getPresenter().isEmpty()) {
      // Empty table.
      message = emptyTableWidgetContainer;
    }

    // Switch out the message to display.
    tableDataScroller.setWidget(message);

    // Fire an event.
    super.onLoadingStateChanged(state);
  }

  @Override
  protected void refreshColumnWidths() {
    super.refreshColumnWidths();

    // Hide unused col elements in the colgroup.
    int columnCount = getRealColumnCount();
    tableHeader.hideUnusedColumns(columnCount);
    tableData.hideUnusedColumns(columnCount);
    tableFooter.hideUnusedColumns(columnCount);
  }
}
