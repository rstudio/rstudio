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
package com.google.gwt.sample.expenses.client;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.DateCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.requestfactory.shared.Property;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.SyncResult;
import com.google.gwt.sample.expenses.client.request.EmployeeRecord;
import com.google.gwt.sample.expenses.client.request.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.client.request.ReportRecord;
import com.google.gwt.sample.expenses.client.request.ReportRecordChanged;
import com.google.gwt.sample.expenses.client.style.Styles;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.SimplePager.TextLocation;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.SelectionChangeEvent;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The list of expense reports on the right side of the app.
 */
public class ExpenseList extends Composite
    implements ReportRecordChanged.Handler {

  /**
   * The auto refresh interval in milliseconds.
   */
  private static final int REFRESH_INTERVAL = 5000;

  private static ExpenseListUiBinder uiBinder = GWT.create(
      ExpenseListUiBinder.class);

  /**
   * Utility method to get the first part of the breadcrumb based on the
   * department and employee.
   *
   * @param department the selected department
   * @param employee the selected employee
   * @return the breadcrumb
   */
  public static String getBreadcrumb(
      String department, EmployeeRecord employee) {
    if (employee != null) {
      return "Reports for " + employee.getDisplayName();
    } else if (department != null) {
      return "Reports for " + department;
    } else {
      return "All Reports";
    }
  }

  /**
   * A text box that displays default text.
   */
  private static class DefaultTextBox extends TextBox {

    /**
     * The text color used when the box is disabled and empty.
     */
    private static final String TEXTBOX_DISABLED_COLOR = "#aaaaaa";

    private final String defaultText;

    public DefaultTextBox(final String defaultText) {
      this.defaultText = defaultText;
      resetDefaultText();

      // Add focus and blur handlers.
      addFocusHandler(new FocusHandler() {
        public void onFocus(FocusEvent event) {
          getElement().getStyle().clearColor();
          if (defaultText.equals(getText())) {
            setText("");
          }
        }
      });
      addBlurHandler(new BlurHandler() {
        public void onBlur(BlurEvent event) {
          if ("".equals(getText())) {
            resetDefaultText();
          }
        }
      });
    }

    public String getDefaultText() {
      return defaultText;
    }

    /**
     * Reset the text box to the default text.
     */
    public void resetDefaultText() {
      setText(defaultText);
      getElement().getStyle().setColor(TEXTBOX_DISABLED_COLOR);
    }
  }

  interface ExpenseListUiBinder extends UiBinder<Widget, ExpenseList> {
  }
  /**
   * Custom listener for this widget.
   */
  interface Listener {

    /**
     * Called when the user selects a report.
     *
     * @param report the selected report
     */
    void onReportSelected(ReportRecord report);
  }

  /**
   * The styles applied to the table.
   */
  interface TableStyle extends CellTable.CleanStyle {
    String evenRow();

    String hoveredRow();

    String oddRow();

    String selectedRow();
  }

  /**
   * The resources applied to the table.
   */
  interface TableResources extends CellTable.CleanResources {
    @Source("ExpenseListCellTable.css")
    TableStyle cellTableStyle();
  }

  /**
   * A cell used to highlight search text.
   */
  private class HighlightCell extends AbstractCell<String> {

    private static final String replaceString =
        "<span style='color:red;font-weight:bold;'>$1</span>";

    @Override
    public void render(String value, Object viewData, StringBuilder sb) {
      if (value != null) {
        if (searchRegExp != null) {
          value = searchRegExp.replace(value, replaceString);
        }
        sb.append(value);
      }
    }
  }

  /**
   * The data provider used to retrieve reports.
   */
  private class ReportDataProvider extends AsyncDataProvider<ReportRecord> {
    @Override
    protected void onRangeChanged(HasData<ReportRecord> display) {
      requestReports(false);
    }
  }

  @UiField
  Element breadcrumb;
  @UiField
  SimplePager pager;
  @UiField(provided = true)
  DefaultTextBox searchBox;
  @UiField
  Image searchButton;

  /**
   * The main table. We provide this in the constructor before calling
   * {@link UiBinder#createAndBindUi(Object)} because the pager depends on it.
   */
  @UiField(provided = true)
  CellTable<ReportRecord> table;

  private List<SortableHeader> allHeaders = new ArrayList<SortableHeader>();

  /**
   * The department being searched.
   */
  private String department;

  /**
   * The employee being searched.
   */
  private EmployeeRecord employee;

  /**
   * Indicates that the report count is stale.
   */
  private boolean isCountStale = true;

  /**
   * The field to sort by.
   */
  private String orderBy = ReportRecord.purpose.getName();

  /**
   * The set of Report keys that we have seen. When a new key is added, we
   * compare it to the list of known keys to determine if it is new.
   */
  private Set<Object> knownReportKeys = null;

  /**
   * Keep track of the last receiver so that we know if a response is stale.
   */
  private Receiver<List<ReportRecord>> lastDataReceiver;

  /**
   * Keep track of the last receiver so that we know if a response is stale.
   */
  private Receiver<Long> lastDataSizeReceiver;

  private Listener listener;

  /**
   * The {@link Timer} used to periodically refresh the table.
   */
  private final Timer refreshTimer = new Timer() {
    @Override
    public void run() {
      isCountStale = true;
      requestReports(true);
    }
  };

  /**
   * The columns to request with each report.
   */
  private final String[] reportColumns = new String[]{
      ReportRecord.created.getName(), ReportRecord.purpose.getName(),
      ReportRecord.notes.getName()};

  /**
   * The data provider that provides reports.
   */
  private final ReportDataProvider reports = new ReportDataProvider();

  /**
   * The factory used to send requests.
   */
  private ExpensesRequestFactory requestFactory;

  /**
   * The string that the user searched for.
   */
  private RegExp searchRegExp;

  /**
   * The starts with search string.
   */
  private String startsWithSearch;

  public ExpenseList() {
    reports.setKeyProvider(Expenses.REPORT_RECORD_KEY_PROVIDER);

    // Initialize the widget.
    createTable();
    searchBox = new DefaultTextBox("search");
    initWidget(uiBinder.createAndBindUi(this));

    // Add the view to the data provider.
    reports.addDataDisplay(table);

    // Listen for key events from the text boxes.
    searchBox.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        // Search on enter.
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
          search();
          return;
        }

        // Highlight as the user types.
        String text = searchBox.getText();
        if (text.length() > 0) {
          searchRegExp = RegExp.compile("(" + text + ")", "ig");
        } else {
          searchRegExp = null;
        }
        table.redraw();
      }
    });
    searchButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        search();
      }
    });
  }

  public void onReportChanged(ReportRecordChanged event) {
    ReportRecord changed = event.getRecord();
    Long changedId = changed.getId();
    List<ReportRecord> records = table.getDisplayedItems();
    int i = 0;
    for (ReportRecord record : records) {
      if (record != null && changedId.equals(record.getId())) {
        List<ReportRecord> changedList = new ArrayList<ReportRecord>();
        changedList.add(changed);
        reports.updateRowData(i + table.getPageStart(), changedList);
      }
      i++;
    }
  }

  /**
   * Set the current department and employee to filter on.
   *
   * @param department the department, or null if none selected
   * @param employee the employee, or null if none selected
   */
  public void setEmployee(String department, EmployeeRecord employee) {
    this.department = department;
    this.employee = employee;
    isCountStale = true;
    searchBox.resetDefaultText();
    startsWithSearch = null;
    breadcrumb.setInnerText(getBreadcrumb(department, employee));
    searchRegExp = null;

    // Refresh the table.
    pager.setPageStart(0);
    requestReports(false);
  }

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  public void setRequestFactory(ExpensesRequestFactory factory) {
    this.requestFactory = factory;
    requestReports(false);
  }

  @UiFactory
  SimplePager createPager() {
    SimplePager p = new SimplePager(TextLocation.RIGHT);
    p.setDisplay(table);
    p.setRangeLimited(true);
    return p;
  }

  /**
   * Add a sortable column to the table.
   *
   * @param <C> the data type for the column
   * @param text the header text
   * @param cell the cell used to render the column
   * @param getter the getter to retrieve the value for the column
   * @param property the property to sort by
   * @return the column
   */
  private <C> Column<ReportRecord, C> addColumn(final String text,
      final Cell<C> cell, final GetValue<ReportRecord, C> getter,
      final Property<?> property) {
    final Column<ReportRecord, C> column = new Column<ReportRecord, C>(cell) {
      @Override
      public C getValue(ReportRecord object) {
        return getter.getValue(object);
      }
    };
    final SortableHeader header = new SortableHeader(text);
    allHeaders.add(header);

    // Sort created by default.
    if (property == ReportRecord.created) {
      header.setSorted(true);
      header.setReverseSort(true);
      orderBy = property.getName() + " DESC";
    }

    header.setUpdater(new ValueUpdater<String>() {
      public void update(String value) {
        header.setSorted(true);
        header.toggleReverseSort();

        for (SortableHeader otherHeader : allHeaders) {
          if (otherHeader != header) {
            otherHeader.setSorted(false);
            otherHeader.setReverseSort(true);
          }
        }
        table.redrawHeaders();

        // Request sorted rows.
        orderBy = property.getName();
        if (header.getReverseSort()) {
          orderBy += " DESC";
        }
        searchBox.resetDefaultText();
        searchRegExp = null;

        // Go to the first page of the newly-sorted results
        pager.firstPage();
        requestReports(false);
      }
    });
    table.addColumn(column, header);
    return column;
  }

  /**
   * Create the {@link CellTable}.
   */
  private void createTable() {
    CellTable.Resources resources = GWT.create(TableResources.class);
    table = new CellTable<ReportRecord>(20, resources);
    Styles.Common common = Styles.common();
    table.addColumnStyleName(0, common.spacerColumn());
    table.addColumnStyleName(1, common.expenseListPurposeColumn());
    table.addColumnStyleName(3, common.expenseListDepartmentColumn());
    table.addColumnStyleName(4, common.expenseListCreatedColumn());
    table.addColumnStyleName(5, common.spacerColumn());

    // Add a selection model.
    final NoSelectionModel<ReportRecord> selectionModel = new NoSelectionModel<
        ReportRecord>();
    table.setSelectionModel(selectionModel);
    selectionModel.addSelectionChangeHandler(
        new SelectionChangeEvent.Handler() {
          public void onSelectionChange(SelectionChangeEvent event) {
            Object selected = selectionModel.getLastSelectedObject();
            if (selected != null && listener != null) {
              listener.onReportSelected((ReportRecord) selected);
            }
          }
        });

    // Spacer column.
    table.addColumn(new Column<ReportRecord, String>(new TextCell()) {
      @Override
      public String getValue(ReportRecord object) {
        return "<div style='display:none;'/>";
      }
    });

    // Purpose column.
    addColumn(
        "Purpose", new HighlightCell(), new GetValue<ReportRecord, String>() {
          public String getValue(ReportRecord object) {
            return object.getPurpose();
          }
        }, ReportRecord.purpose);

    // Notes column.
    addColumn(
        "Notes", new HighlightCell(), new GetValue<ReportRecord, String>() {
          public String getValue(ReportRecord object) {
            return object.getNotes();
          }
        }, ReportRecord.notes);

    // Department column.
    addColumn(
        "Department", new TextCell(), new GetValue<ReportRecord, String>() {
          public String getValue(ReportRecord object) {
            return object.getDepartment();
          }
        }, ReportRecord.department);

    // Created column.
    addColumn("Created", new DateCell(DateTimeFormat.getFormat("MMM dd yyyy")),
        new GetValue<ReportRecord, Date>() {
          public Date getValue(ReportRecord object) {
            return object.getCreated();
          }
        }, ReportRecord.created);

    // Spacer column.
    table.addColumn(new Column<ReportRecord, String>(new TextCell()) {
      @Override
      public String getValue(ReportRecord object) {
        return "<div style='display:none;'/>";
      }
    });
  }

  /**
   * Send a request for reports in the current range.
   *
   * @param isPolling true if this request is caused by polling
   */
  private void requestReports(boolean isPolling) {
    // Cancel the refresh timer.
    refreshTimer.cancel();

    // Early exit if we don't have a request factory to request from.
    if (requestFactory == null) {
      return;
    }

    // Clear the known keys.
    if (!isPolling) {
      knownReportKeys = null;
    }

    // Get the parameters.
    String startsWith = startsWithSearch;
    if (startsWith == null || searchBox.getDefaultText().equals(startsWith)) {
      startsWith = "";
    }
    Range range = table.getVisibleRange();
    Long employeeId = employee == null ? -1 : new Long(employee.getId());
    String dept = department == null ? "" : department;

    // If a search string is specified, the results will not be sorted.
    if (startsWith.length() > 0) {
      for (SortableHeader header : allHeaders) {
        header.setSorted(false);
        header.setReverseSort(false);
      }
      table.redrawHeaders();
    }

    // Request the total data size.
    if (isCountStale) {
      isCountStale = false;
      if (!isPolling) {
        pager.startLoading();
      }
      lastDataSizeReceiver = new Receiver<Long>() {
        public void onSuccess(Long response, Set<SyncResult> syncResults) {
          if (this == lastDataSizeReceiver) {
            int count = response.intValue();
            // Treat count == 1000 as inexact due to AppEngine limitation
            reports.updateRowCount(count, count != 1000);
          }
        }
      };
      requestFactory.reportRequest().countReportsBySearch(
          employeeId, dept, startsWith).fire(lastDataSizeReceiver);
    }

    // Request reports in the current range.
    lastDataReceiver = new Receiver<List<ReportRecord>>() {
      public void onSuccess(
          List<ReportRecord> newValues, Set<SyncResult> syncResults) {
        if (this == lastDataReceiver) {
          int size = newValues.size();
          if (size < table.getPageSize()) {
            // Now we know the exact data size
            reports.updateRowCount(table.getPageStart() + size, true);
          }
          if (size > 0) {
            reports.updateRowData(table.getPageStart(), newValues);
          }

          // Add the new keys to the known keys.
          boolean isInitialData = knownReportKeys == null;
          if (knownReportKeys == null) {
            knownReportKeys = new HashSet<Object>();
          }
          for (ReportRecord value : newValues) {
            Object key = reports.getKey(value);
            if (!isInitialData && !knownReportKeys.contains(key)) {
              (new PhaseAnimation.CellTablePhaseAnimation<ReportRecord>(
                  table, value, reports)).run();
            }
            knownReportKeys.add(key);
          }
        }
        refreshTimer.schedule(REFRESH_INTERVAL);
      }
    };

    requestFactory.reportRequest().findReportEntriesBySearch(employeeId, dept,
        startsWith, orderBy, range.getStart(), range.getLength()).with(
        reportColumns).fire(lastDataReceiver);
  }

  /**
   * Search based on the search box text.
   */
  private void search() {
    isCountStale = true;
    startsWithSearch = searchBox.getText();
    requestReports(false);
  }
}
