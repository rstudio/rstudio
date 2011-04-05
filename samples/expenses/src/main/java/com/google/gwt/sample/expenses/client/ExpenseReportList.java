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

import com.google.gwt.activity.shared.Activity;
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
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.sample.expenses.client.place.ReportListPlace;
import com.google.gwt.sample.expenses.client.style.Styles;
import com.google.gwt.sample.expenses.shared.EmployeeProxy;
import com.google.gwt.sample.expenses.shared.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.shared.ReportProxy;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.SimplePager.TextLocation;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.RangeChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.web.bindery.requestfactory.gwt.ui.client.EntityProxyKeyProvider;
import com.google.web.bindery.requestfactory.shared.EntityProxyChange;
import com.google.web.bindery.requestfactory.shared.EntityProxyId;
import com.google.web.bindery.requestfactory.shared.Receiver;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The list of expense reports on the right side of the app.
 */
public class ExpenseReportList extends Composite implements
    EntityProxyChange.Handler<ReportProxy>, Activity {

  interface Binder extends UiBinder<Widget, ExpenseReportList> {
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
    void onReportSelected(ReportProxy report);
  }

  /**
   * The resources applied to the table.
   */
  interface TableResources extends CellTable.Resources {
    @Source({CellTable.Style.DEFAULT_CSS, "ExpenseListCellTable.css"})
    TableStyle cellTableStyle();
  }

  /**
   * The styles applied to the table.
   */
  interface TableStyle extends CellTable.Style {
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

  /**
   * A cell used to highlight search text.
   */
  private class HighlightCell extends AbstractCell<String> {

    private static final String replaceString = "<span style='color:red;font-weight:bold;'>$1</span>";

    @Override
    public void render(Context context, String value, SafeHtmlBuilder sb) {
      if (value != null) {
        if (searchRegExp != null) {
          // The search regex has already been html-escaped
          value = searchRegExp.replace(SafeHtmlUtils.htmlEscape(value),
              replaceString);
          sb.append(SafeHtmlUtils.fromTrustedString(value));
        } else {
          sb.appendEscaped(value);
        }
      }
    }
  }

  private static final ProvidesKey<ReportProxy> keyProvider = new EntityProxyKeyProvider<ReportProxy>();

  /**
   * The auto refresh interval in milliseconds.
   */
  private static final int REFRESH_INTERVAL = 5000;

  private static Binder uiBinder = GWT.create(Binder.class);

  /**
   * Utility method to get the first part of the breadcrumb based on the
   * department and employee.
   * 
   * @param department the selected department
   * @param employee the selected employee
   * @return the breadcrumb
   */
  public static String getBreadcrumb(String department, EmployeeProxy employee) {
    assert null != department;
    if (employee != null) {
      return "Reports for " + employee.getDisplayName();
    } else if (!"".equals(department)) {
      return "Reports for " + department;
    } else {
      return "All Reports";
    }
  }

  @UiField
  Element breadcrumb;
  @UiField
  SimplePager pager;
  @UiField
  Image searchButton;

  @UiField(provided = true)
  DefaultTextBox searchBox;

  /**
   * The main table. We provide this in the constructor before calling
   * {@link UiBinder#createAndBindUi(Object)} because the pager depends on it.
   */
  @UiField(provided = true)
  CellTable<ReportProxy> table;

  private final List<SortableHeader> allHeaders = new ArrayList<SortableHeader>();

  /**
   * The department being searched.
   */
  private String department;

  /**
   * The employee being searched.
   */
  private EmployeeProxy employee;

  /**
   * Indicates that the report count is stale.
   */
  private boolean isCountStale = true;

  /**
   * The field to sort by.
   */
  private String orderBy = "purpose";

  /**
   * The set of Report keys that we have seen. When a new key is added, we
   * compare it to the list of known keys to determine if it is new.
   */
  private Set<Object> knownReportKeys = null;

  /**
   * Keep track of the last receiver so that we know if a response is stale.
   */
  private Receiver<List<ReportProxy>> lastDataReceiver;

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
  private final String[] reportColumns = new String[] {
      "created", "purpose", "notes"};

  /**
   * The factory used to send requests.
   */
  private final ExpensesRequestFactory requestFactory;

  /**
   * The string that the user searched for.
   */
  private RegExp searchRegExp;

  /**
   * The starts with search string.
   */
  private String startsWithSearch;

  private ReportListPlace place;

  private boolean running;

  public ExpenseReportList(ExpensesRequestFactory requestFactory) {
    this.requestFactory = requestFactory;

    // Initialize the widget.
    createTable();
    table.addRangeChangeHandler(new RangeChangeEvent.Handler() {
      public void onRangeChange(RangeChangeEvent event) {
        requestReports(false);
      }
    });
    searchBox = new DefaultTextBox("search");
    initWidget(uiBinder.createAndBindUi(this));

    // Listen for key events from the text boxes.
    searchBox.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        // Search on enter.
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
          search();
          return;
        }

        // Highlight as the user types.
        String text = SafeHtmlUtils.htmlEscape(searchBox.getText());
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

  public String mayStop() {
    return null;
  }

  public void onCancel() {
    onStop();
  }

  public void onProxyChange(EntityProxyChange<ReportProxy> event) {
    EntityProxyId<ReportProxy> changedId = event.getProxyId();
    List<ReportProxy> records = table.getVisibleItems();
    int i = 0;
    for (ReportProxy record : records) {
      if (record != null && changedId.equals(record.stableId())) {
        List<ReportProxy> changedList = new ArrayList<ReportProxy>();
        changedList.add(record);
        table.setRowData(i + table.getPageStart(), changedList);
      }
      i++;
    }
  }

  public void onStop() {
    running = false;
    refreshTimer.cancel();
  }

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  public void start(AcceptsOneWidget panel, EventBus eventBus) {
    running = true;
    doUpdateForPlace();

    EntityProxyChange.registerForProxyType(eventBus, ReportProxy.class, this);
    requestReports(false);
    panel.setWidget(this);
  }

  /**
   * In this application, called by {@link ExpensesActivityMapper} each time a
   * ReportListPlace is posted. In a more typical set up, this would be a
   * constructor argument to a one shot activity, perhaps managing a shared
   * widget view instance.
   */
  public void updateForPlace(final ReportListPlace place) {
    this.place = place;
    if (running) {
      doUpdateForPlace();
    }
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
  private <C> Column<ReportProxy, C> addColumn(final String text,
      final Cell<C> cell, final GetValue<ReportProxy, C> getter,
      final String property) {
    final Column<ReportProxy, C> column = new Column<ReportProxy, C>(cell) {
      @Override
      public C getValue(ReportProxy object) {
        return getter.getValue(object);
      }
    };
    final SortableHeader header = new SortableHeader(text);
    allHeaders.add(header);

    // Sort created by default.
    if ("created".equals(property)) {
      header.setSorted(true);
      header.setReverseSort(true);
      orderBy = "created" + " DESC";
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
        orderBy = property;
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
    table = new CellTable<ReportProxy>(20, resources);
    table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
    Styles.Common common = Styles.common();
    table.addColumnStyleName(0, common.spacerColumn());
    table.addColumnStyleName(1, common.expenseListPurposeColumn());
    table.addColumnStyleName(3, common.expenseListDepartmentColumn());
    table.addColumnStyleName(4, common.expenseListCreatedColumn());
    table.addColumnStyleName(5, common.spacerColumn());

    // Add a selection model.
    final NoSelectionModel<ReportProxy> selectionModel = new NoSelectionModel<ReportProxy>();
    table.setSelectionModel(selectionModel);
    selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
      public void onSelectionChange(SelectionChangeEvent event) {
        Object selected = selectionModel.getLastSelectedObject();
        if (selected != null && listener != null) {
          listener.onReportSelected((ReportProxy) selected);
        }
      }
    });

    // Spacer column.
    table.addColumn(new SpacerColumn<ReportProxy>());

    // Purpose column.
    addColumn("Purpose", new HighlightCell(),
        new GetValue<ReportProxy, String>() {
          public String getValue(ReportProxy object) {
            return object.getPurpose();
          }
        }, "purpose");

    // Notes column.
    addColumn("Notes", new HighlightCell(),
        new GetValue<ReportProxy, String>() {
          public String getValue(ReportProxy object) {
            return object.getNotes();
          }
        }, "notes");

    // Department column.
    addColumn("Department", new TextCell(),
        new GetValue<ReportProxy, String>() {
          public String getValue(ReportProxy object) {
            return object.getDepartment();
          }
        }, "department");

    // Created column.
    addColumn("Created", new DateCell(DateTimeFormat.getFormat("MMM dd yyyy")),
        new GetValue<ReportProxy, Date>() {
          public Date getValue(ReportProxy object) {
            return object.getCreated();
          }
        }, "created");

    // Spacer column.
    table.addColumn(new SpacerColumn<ReportProxy>());
  }

  private void doUpdateForPlace() {
    if (place.getEmployeeId() == null) {
      findDepartmentOrEmployee(place.getDepartment(), null);
    } else {
      requestFactory.find(place.getEmployeeId()).fire(
          new Receiver<EmployeeProxy>() {
            @Override
            public void onSuccess(EmployeeProxy response) {
              findDepartmentOrEmployee("", response);
            }
          });
    }
  }

  /**
   * Set the current department and employee to filter on.
   * 
   * @param department the department, or null if none selected
   * @param employee the employee, or null if none selected
   */
  private void findDepartmentOrEmployee(String department,
      EmployeeProxy employee) {
    this.department = department;
    this.employee = employee;
    isCountStale = true;
    searchBox.resetDefaultText();
    startsWithSearch = null;
    breadcrumb.setInnerText(getBreadcrumb(department, employee));
    searchRegExp = null;
    pager.setPageStart(0);
    requestReports(false);
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
        @Override
        public void onSuccess(Long response) {
          if (this == lastDataSizeReceiver) {
            int count = response.intValue();
            // Treat count == 1000 as inexact due to AppEngine limitation
            table.setRowCount(count, count != 1000);
          }
        }
      };
      requestFactory.reportRequest().countReportsBySearch(employeeId, dept,
          startsWith).fire(lastDataSizeReceiver);
    }

    // Request reports in the current range.
    lastDataReceiver = new Receiver<List<ReportProxy>>() {
      @Override
      public void onSuccess(List<ReportProxy> newValues) {
        if (this == lastDataReceiver) {
          int size = newValues.size();
          if (size < table.getPageSize()) {
            // Now we know the exact data size
            table.setRowCount(table.getPageStart() + size, true);
          }
          if (size > 0) {
            table.setRowData(table.getPageStart(), newValues);
          }

          // Add the new keys to the known keys.
          boolean isInitialData = knownReportKeys == null;
          if (knownReportKeys == null) {
            knownReportKeys = new HashSet<Object>();
          }
          for (ReportProxy value : newValues) {
            Object key = keyProvider.getKey(value);
            if (!isInitialData && !knownReportKeys.contains(key)) {
              new PhaseAnimation.CellTablePhaseAnimation<ReportProxy>(table,
                  value, keyProvider).run();
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
