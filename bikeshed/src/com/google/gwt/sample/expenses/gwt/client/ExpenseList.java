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
package com.google.gwt.sample.expenses.gwt.client;

import com.google.gwt.bikeshed.list.client.CellTable;
import com.google.gwt.bikeshed.list.client.Column;
import com.google.gwt.bikeshed.list.client.SimplePager;
import com.google.gwt.bikeshed.list.client.SimplePager.TextLocation;
import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.DateCell;
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
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.sample.expenses.gwt.request.EmployeeRecord;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.gwt.request.ReportRecord;
import com.google.gwt.sample.expenses.gwt.request.ReportRecordChanged;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.view.client.AsyncListViewAdapter;
import com.google.gwt.view.client.ListView;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwt.view.client.SelectionModel.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionModel.SelectionChangeHandler;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The list of expense reports on the left side of the app.
 */
public class ExpenseList extends Composite implements
    Receiver<List<ReportRecord>>, ReportRecordChanged.Handler {

  private static ExpenseListUiBinder uiBinder = GWT.create(ExpenseListUiBinder.class);

  /**
   * Utility method to get the first part of the breadcrumb based on the
   * department and employee.
   * 
   * @param department the selected department
   * @param employee the selected employee
   * @return the breadcrumb
   */
  public static String getBreadcrumb(String department, EmployeeRecord employee) {
    if (employee != null) {
      return employee.getDisplayName() + "'s Reports";
    } else if (department != null) {
      return department + " Reports";
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
   * A cell used to highlight search text.
   */
  private class HighlightCell extends AbstractCell<String> {

    private static final String replaceString = "<span style='color:red;font-weight:bold;'>$1</span>";

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
   * The adapter used to retrieve reports.
   */
  private class ReportAdapter extends AsyncListViewAdapter<ReportRecord> {
    @Override
    protected void onRangeChanged(ListView<ReportRecord> view) {
      requestReports();
    }
  }

  @UiField
  Element breadcrumb;
  @UiField
  SimplePager<ReportRecord> pager;
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

  private Listener listener;

  /**
   * The columns to request with each report.
   */
  private final List<Property<?>> reportColumns;

  /**
   * The adapter that provides reports.
   */
  private final ReportAdapter reports = new ReportAdapter();

  /**
   * The factory used to send requests.
   */
  private ExpensesRequestFactory requestFactory;

  /**
   * The string that the user searched for.
   */
  private RegExp searchRegExp;

  public ExpenseList() {
    reportColumns = new ArrayList<Property<?>>();
    reportColumns.add(ReportRecord.created);
    reportColumns.add(ReportRecord.purpose);
    reportColumns.add(ReportRecord.notes);

    // Initialize the widget.
    createTable();
    searchBox = new DefaultTextBox("search");
    initWidget(uiBinder.createAndBindUi(this));

    // Add the view to the adapter.
    reports.addView(table);

    // Listen for key events from the text boxes.
    searchBox.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        // Search on enter.
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
          isCountStale = true;
          requestReports();
          return;
        }

        // Highlight as the user types.
        String text = searchBox.getText();
        if (text.length() > 0) {
          searchRegExp = RegExp.compile("(" + text + ")", "i");
        } else {
          searchRegExp = null;
        }
        table.redraw();
      }
    });
    searchButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        isCountStale = true;
        requestReports();
      }
    });
  }

  public void onReportChanged(ReportRecordChanged event) {
    ReportRecord changed = event.getRecord();
    String changedId = changed.getId();
    List<ReportRecord> records = table.getDisplayedItems();
    int i = 0;
    for (ReportRecord record : records) {
      if (record != null && changedId.equals(record.getId())) {
        List<ReportRecord> changedList = new ArrayList<ReportRecord>();
        changedList.add(changed);
        reports.updateViewData(i + table.getPageStart(), 1, changedList);
      }
      i++;
    }
  }

  public void onSuccess(List<ReportRecord> newValues) {
    reports.updateViewData(table.getPageStart(), newValues.size(), newValues);
  }

  /**
   * Set the current department and employee to filter on.
   * 
   * @param department the department, or null if none selected
   * @param employee the employee, or null if none selected
   */
  public void setEmployee(String department, EmployeeRecord employee) {
    this.employee = employee;
    isCountStale = true;
    searchBox.resetDefaultText();
    breadcrumb.setInnerText(getBreadcrumb(department, employee));

    // Refresh the table.
    pager.setPageStart(0);
    table.refresh();
  }

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  public void setRequestFactory(ExpensesRequestFactory factory) {
    this.requestFactory = factory;
    requestReports();
  }

  @UiFactory
  SimplePager<ReportRecord> createPager() {
    SimplePager<ReportRecord> p = new SimplePager<ReportRecord>(table,
        TextLocation.RIGHT);
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
        table.refreshHeaders();

        // Request sorted rows.
        orderBy = property.getName();
        if (header.getReverseSort()) {
          orderBy += " DESC";
        }
        searchBox.resetDefaultText();
        searchRegExp = null;
        requestReports();
      }
    });
    table.addColumn(column, header);
    return column;
  }

  /**
   * Create the {@link CellTable}.
   */
  private void createTable() {
    table = new CellTable<ReportRecord>(50);

    // Add a selection model.
    final SingleSelectionModel<ReportRecord> selectionModel = new SingleSelectionModel<ReportRecord>();
    table.setSelectionModel(selectionModel);
    table.setSelectionEnabled(true);
    selectionModel.addSelectionChangeHandler(new SelectionChangeHandler() {
      public void onSelectionChange(SelectionChangeEvent event) {
        Object selected = selectionModel.getSelectedObject();
        if (selected != null && listener != null) {
          listener.onReportSelected((ReportRecord) selected);
        }
      }
    });

    // Purpose column.
    addColumn("Purpose", new HighlightCell(),
        new GetValue<ReportRecord, String>() {
          public String getValue(ReportRecord object) {
            return object.getPurpose();
          }
        }, ReportRecord.purpose);

    // Notes column.
    addColumn("Notes", new HighlightCell(),
        new GetValue<ReportRecord, String>() {
          public String getValue(ReportRecord object) {
            return object.getNotes();
          }
        }, ReportRecord.notes);

    // Created column.
    addColumn("Created", new DateCell(), new GetValue<ReportRecord, Date>() {
      public Date getValue(ReportRecord object) {
        return object.getCreated();
      }
    }, ReportRecord.created);
  }

  /**
   * Send a request for reports in the current range.
   */
  private void requestReports() {
    if (requestFactory == null) {
      return;
    }

    // Get the parameters.
    String startsWith = searchBox.getText();
    if (searchBox.getDefaultText().equals(startsWith)) {
      startsWith = "";
    }
    Range range = table.getRange();
    Long employeeId = employee == null ? -1 : new Long(employee.getId());

    // If a search string is specified, the results will not be sorted.
    if (startsWith.length() > 0) {
      for (SortableHeader header : allHeaders) {
        header.setSorted(false);
        header.setReverseSort(false);
      }
      table.refreshHeaders();
    }

    // Request the total data size.
    if (isCountStale) {
      isCountStale = false;
      requestFactory.reportRequest().countReportsBySearch(employeeId,
          startsWith).to(new Receiver<Long>() {
        public void onSuccess(Long response) {
          reports.updateDataSize(response.intValue(), true);
        }
      }).fire();
    }

    // Request reports in the current range.
    requestFactory.reportRequest().findReportEntriesBySearch(employeeId,
        startsWith, orderBy, range.getStart(), range.getLength()).forProperties(
        reportColumns).to(this).fire();
  }
}
