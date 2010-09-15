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

import com.google.gwt.cell.client.AbstractEditableCell;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.DateCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.requestfactory.shared.EntityProxyChange;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestObject;
import com.google.gwt.requestfactory.shared.SyncResult;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.sample.expenses.client.request.EmployeeProxy;
import com.google.gwt.sample.expenses.client.request.ExpenseProxy;
import com.google.gwt.sample.expenses.client.request.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.client.request.ReportProxy;
import com.google.gwt.sample.expenses.client.style.Styles;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Details about the current expense report on the right side of the app,
 * including the list of expenses.
 */
public class ExpenseDetails extends Composite {

  interface ExpenseDetailsUiBinder extends UiBinder<Widget, ExpenseDetails> {
  }

  /**
   * The resources applied to the table.
   */
  interface TableResources extends CellTable.Resources {
    @Source({CellTable.Style.DEFAULT_CSS, "ExpenseDetailsCellTable.css"})
    TableStyle cellTableStyle();
  }

  /**
   * The styles applied to the table.
   */
  interface TableStyle extends CellTable.Style {
  }

  interface Template extends SafeHtmlTemplates {
    @Template("<select style=\"background-color:white;border:1px solid "
        + "#707172;width:10em;margin-right:10px;\" disabled=\"true\"><option>"
        + "</option>{0}{1}</select>")
    SafeHtml disabled(SafeHtml approvedOption, SafeHtml deniedOption);

    @Template("<select style=\"background-color:white;border:1px solid "
        + "#707172;width:10em;margin-right:10px;\"><option></option>{0}{1}"
        + "</select>")
    SafeHtml enabled(SafeHtml approvedOption, SafeHtml deniedOption);
  }

  /**
   * The cell used for approval status.
   */
  private class ApprovalCell extends
      AbstractEditableCell<String, ApprovalViewData> {

    private final String approvedText = Expenses.Approval.APPROVED.getText();
    private final String deniedText = Expenses.Approval.DENIED.getText();
    private final SafeHtml errorIconHtml;
    private final SafeHtml pendingIconHtml;

    public ApprovalCell() {
      super("change", "click");
      if (template == null) {
        template = GWT.create(Template.class);
      }

      // Cache the html string for the error icon.
      ImageResource errorIcon = Styles.resources().errorIcon();
      AbstractImagePrototype errorImg = AbstractImagePrototype.create(
          errorIcon);
      errorIconHtml = SafeHtmlUtils.fromTrustedString(errorImg.getHTML());

      // Cache the html string for the pending icon.
      ImageResource pendingIcon = Styles.resources().pendingCommit();
      AbstractImagePrototype pendingImg = AbstractImagePrototype.create(
          pendingIcon);
      pendingIconHtml = SafeHtmlUtils.fromTrustedString(pendingImg.getHTML());
    }

    @Override
    public void onBrowserEvent(Element parent, String value, Object key,
        NativeEvent event, ValueUpdater<String> valueUpdater) {
      String type = event.getType();
      ApprovalViewData viewData = getViewData(key);
      if ("change".equals(type)) {
        // Disable the select box.
        SelectElement select = parent.getFirstChild().cast();
        select.setDisabled(true);

        // Add the pending icon if it isn't already visible.
        if (viewData == null) {
          Element tmpElem = Document.get().createDivElement();
          tmpElem.setInnerHTML(pendingIconHtml.asString());
          parent.appendChild(tmpElem.getFirstChildElement());
        }

        // Remember which value is now selected.
        int index = select.getSelectedIndex();
        String pendingValue = select.getOptions().getItem(index).getValue();
        viewData = new ApprovalViewData(pendingValue);
        setViewData(key, viewData);

        // Update the value updater.
        if (valueUpdater != null) {
          valueUpdater.update(pendingValue);
        }
      } else if ("click".equals(type) && viewData != null
          && parent.getChildCount() >= 3) {
        // Alert the user of the error
        Element img = parent.getChild(1).cast();
        Element anchor = img.getNextSiblingElement();
        if (anchor.isOrHasChild(Element.as(event.getEventTarget().cast()))) {
          // Alert the user of the error.
          showErrorPopup(viewData.getRejectionText());

          // Clear the view data now that we've viewed the message.
          clearViewData(key);
          parent.removeChild(anchor);
          parent.removeChild(img);
        }
      }
    }

    @Override
    public void render(String value, Object key, SafeHtmlBuilder sb) {
      // Get the view data.
      ApprovalViewData viewData = getViewData(key);
      if (viewData != null && viewData.getPendingApproval().equals(value)) {
        clearViewData(key);
        viewData = null;
      }

      boolean isRejected = false;
      boolean isDisabled = false;
      String pendingValue = null;
      String renderValue = value;
      if (viewData != null) {
        isRejected = viewData.isRejected();
        pendingValue = viewData.getPendingApproval();
        if (!isRejected) {
          renderValue = pendingValue;
          // If there is a delta value that has not been rejected, then the
          // combo box should remain disabled.
          isDisabled = true;
        }
      }
      boolean isApproved = approvedText.equals(renderValue);
      boolean isDenied = deniedText.equals(renderValue);

      SafeHtml approvedOption = createOption(isApproved, approvedText);
      SafeHtml deniedOption = createOption(isDenied, deniedText);
      // Create the select element.
      if (isDisabled) {
        sb.append(template.disabled(approvedOption, deniedOption));
      } else {
        sb.append(template.enabled(approvedOption, deniedOption));
      }

      // Add an icon indicating the commit state.
      if (isRejected) {
        // Add error icon if viewData does not match.
        sb.append(errorIconHtml);
        sb.appendHtmlConstant(
            "<a style='padding-left:3px;color:red;' href='javascript:;'>Error!</a>");
      } else if (pendingValue != null) {
        // Add refresh icon if pending.
        sb.append(pendingIconHtml);
      }
    }

    private SafeHtml createOption(boolean selected, String text) {
      SafeHtmlBuilder builder = new SafeHtmlBuilder();
      if (selected) {
        builder.appendHtmlConstant("<option selected=\"selected\">");
      } else {
        builder.appendHtmlConstant("<option>");
      }
      builder.appendEscaped(text);
      builder.appendHtmlConstant("</option>");
      return builder.toSafeHtml();
    }
  }

  /**
   * The ViewData associated with the {@link ApprovalCell}.
   */
  private static class ApprovalViewData {
    private final String pendingApproval;
    private String rejectionText;

    public ApprovalViewData(String approval) {
      this.pendingApproval = approval;
    }

    public String getPendingApproval() {
      return pendingApproval;
    }

    public String getRejectionText() {
      return rejectionText;
    }

    public boolean isRejected() {
      return rejectionText != null;
    }

    public void reject(String text) {
      this.rejectionText = text;
    }
  }

  /**
   * The popup used to enter the rejection reason.
   */
  private class DenialPopup extends PopupPanel {
    private final Button cancelButton = new Button("Cancel", new ClickHandler() {
      public void onClick(ClickEvent event) {
        reasonDenied = "";
        hide();
      }
    });
    private final Button confirmButton = new Button("Confirm", new ClickHandler() {
      public void onClick(ClickEvent event) {
        reasonDenied = reasonBox.getText();
        hide();
      }
    });

    private ExpenseProxy expenseRecord;
    private final TextBox reasonBox = new TextBox();
    private String reasonDenied;

    public DenialPopup() {
      super(false, true);
      setStyleName(Styles.common().popupPanel());
      setGlassEnabled(true);
      confirmButton.setWidth("11ex");
      cancelButton.setWidth("11ex");
      reasonBox.getElement().getStyle().setMarginLeft(10.0, Unit.PX);
      reasonBox.getElement().getStyle().setMarginRight(10.0, Unit.PX);

      HorizontalPanel hPanel = new HorizontalPanel();
      hPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
      hPanel.add(new HTML("<b>Reason:</b>"));
      hPanel.add(reasonBox);
      hPanel.add(confirmButton);
      hPanel.add(cancelButton);
      setWidget(hPanel);
      cancelButton.getElement().getParentElement().getStyle().setPaddingLeft(
          5.0, Unit.PX);
    }

    public ExpenseProxy getExpenseRecord() {
      return expenseRecord;
    }

    public String getReasonDenied() {
      return reasonDenied;
    }

    public void popup() {
      center();
      reasonBox.setFocus(true);
    }

    public void setExpenseRecord(ExpenseProxy expenseRecord) {
      this.expenseRecord = expenseRecord;
    }

    public void setReasonDenied(String reasonDenied) {
      this.reasonDenied = reasonDenied;
      reasonBox.setText(reasonDenied);
    }
  }

  private static Template template;

  /**
   * The maximum amount that can be approved for a given report.
   */
  private static final int MAX_COST = 250;

  /**
   * The auto refresh interval in milliseconds.
   */
  private static final int REFRESH_INTERVAL = 5000;

  private static ExpenseDetailsUiBinder uiBinder = GWT.create(
      ExpenseDetailsUiBinder.class);

  @UiField
  Element approvedLabel;

  @UiField
  Element costLabel;

  @UiField
  Element notes;

  @UiField
  TextBox notesBox;

  @UiField
  Anchor notesEditLink;

  @UiField
  Element notesEditLinkWrapper;

  @UiField
  Element notesPending;

  @UiField
  Element reportName;

  @UiField
  Anchor reportsLink;

  @UiField
  CellTable<ExpenseProxy> table;

  @UiField
  Element unreconciledLabel;

  private final List<SortableHeader> allHeaders = new ArrayList<SortableHeader>();

  private ApprovalCell approvalCell;

  /**
   * The default {@link Comparator} used for sorting.
   */
  private Comparator<ExpenseProxy> defaultComparator;

  /**
   * The popup used to display errors to the user.
   */
  private final PopupPanel errorPopup = new PopupPanel(false, true);

  /**
   * The label inside the error popup.
   */
  private final Label errorPopupMessage = new Label();

  private ExpensesRequestFactory expensesRequestFactory;

  /**
   * The data provider that provides expense items.
   */
  private final ListDataProvider<ExpenseProxy> items;

  /**
   * The set of Expense keys that we have seen. When a new key is added, we
   * compare it to the list of known keys to determine if it is new.
   */
  private Map<Object, ExpenseProxy> knownExpenseKeys = null;

  private Comparator<ExpenseProxy> lastComparator;

  /**
   * Keep track of the last receiver so we can ignore stale responses.
   */
  private Receiver<List<ExpenseProxy>> lastReceiver;

  /**
   * The {@link Timer} used to periodically refresh the table.
   */
  private final Timer refreshTimer = new Timer() {
    @Override
    public void run() {
      requestExpenses();
    }
  };

  /**
   * The current report being displayed.
   */
  private ReportProxy report;

  /**
   * The total amount that has been approved.
   */
  private double totalApproved;

  public ExpenseDetails() {
    createErrorPopup();
    initWidget(uiBinder.createAndBindUi(this));
    items = new ListDataProvider<ExpenseProxy>();
    items.setKeyProvider(Expenses.EXPENSE_RECORD_KEY_PROVIDER);
    table.setKeyProvider(items);
    items.addDataDisplay(table);

    // Switch to edit notes.
    notesEditLink.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        setNotesEditState(true, false, report.getNotes());
      }
    });

    // Switch to view mode.
    notesBox.addBlurHandler(new BlurHandler() {
      public void onBlur(BlurEvent event) {
        // The text box will be blurred on cancel, so only save the notes if
        // it is visible.
        if (notesBox.isVisible()) {
          saveNotes();
        }
      }
    });
    notesBox.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        int keyCode = event.getNativeKeyCode();
        switch (keyCode) {
          case KeyCodes.KEY_ENTER:
            saveNotes();
            break;
          case KeyCodes.KEY_ESCAPE:
            // Cancel the edit.
            setNotesEditState(false, false, report.getNotes());
            break;
        }
      }
    });
  }

  public Anchor getReportsLink() {
    return reportsLink;
  }

  public void init(EventBus eventBus) {
    EntityProxyChange.registerForProxyType(eventBus, ExpenseProxy.class,
        new EntityProxyChange.Handler<ExpenseProxy>() {
          public void onProxyChange(EntityProxyChange<ExpenseProxy> event) {
            onExpenseRecordChanged(event);
          }
        });
    EntityProxyChange.registerForProxyType(eventBus, ReportProxy.class,
        new EntityProxyChange.Handler<ReportProxy>() {
          public void onProxyChange(EntityProxyChange<ReportProxy> event) {
            onReportChanged(event);
          }
        });
  }

  public void onExpenseRecordChanged(EntityProxyChange<ExpenseProxy> event) {
    ExpenseProxy newRecord = event.getProxy();
    Object newKey = items.getKey(newRecord);

    int index = 0;
    List<ExpenseProxy> list = items.getList();
    for (ExpenseProxy r : list) {
      if (items.getKey(r).equals(newKey)) {
        list.set(index, newRecord);

        // Update the view data if the approval has been updated.
        ApprovalViewData avd = approvalCell.getViewData(newKey);
        if (avd != null
            && avd.getPendingApproval().equals(newRecord.getApproval())) {
          syncCommit(newRecord, null);
        }
      }
      index++;
    }

    refreshCost();
    if (lastComparator != null) {
      sortExpenses(list, lastComparator);
    }
  }

  public void onReportChanged(EntityProxyChange<ReportProxy> event) {
    ReportProxy changed = event.getProxy();
    if (report != null && report.getId().equals(changed.getId())) {
      // Request the updated report.
      expensesRequestFactory.reportRequest().findReport(
          report.getId()).fire(new Receiver<ReportProxy>() {
        @Override
        public void onSuccess(
            ReportProxy response, Set<SyncResult> syncResults) {
          report = response;
          setNotesEditState(false, false, response.getNotes());
        }
      });
    }
  }

  public void setExpensesRequestFactory(
      ExpensesRequestFactory expensesRequestFactory) {
    this.expensesRequestFactory = expensesRequestFactory;
  }

  /**
   * Set the {@link ReportProxy} to show.
   * 
   * @param report the {@link ReportProxy}
   * @param department the selected department
   * @param employee the selected employee
   */
  public void setReportRecord(ReportProxy report, String department,
      EmployeeProxy employee) {
    this.report = report;
    knownExpenseKeys = null;
    reportName.setInnerText(report.getPurpose());
    costLabel.setInnerText("");
    approvedLabel.setInnerText("");
    unreconciledLabel.setInnerText("");
    setNotesEditState(false, false, report.getNotes());
    items.getList().clear();
    totalApproved = 0;

    // Update the breadcrumb.
    reportsLink.setText(ExpenseList.getBreadcrumb(department, employee));

    // Reset sorting state of table
    lastComparator = defaultComparator;
    for (SortableHeader header : allHeaders) {
      header.setSorted(false);
      header.setReverseSort(true);
    }
    allHeaders.get(0).setSorted(true);
    allHeaders.get(0).setReverseSort(false);
    table.redrawHeaders();

    // Request the expenses.
    requestExpenses();
  }

  @UiFactory
  CellTable<ExpenseProxy> createTable() {
    CellTable.Resources resources = GWT.create(TableResources.class);
    CellTable<ExpenseProxy> view = new CellTable<ExpenseProxy>(100, resources);
    Styles.Common common = Styles.common();
    view.addColumnStyleName(0, common.spacerColumn());
    view.addColumnStyleName(1, common.expenseDetailsDateColumn());
    view.addColumnStyleName(3, common.expenseDetailsCategoryColumn());
    view.addColumnStyleName(4, common.expenseDetailsAmountColumn());
    view.addColumnStyleName(5, common.expenseDetailsApprovalColumn());
    view.addColumnStyleName(6, common.spacerColumn());

    // Spacer column.
    view.addColumn(new SpacerColumn<ExpenseProxy>());

    // Created column.
    GetValue<ExpenseProxy, Date> createdGetter = new GetValue<ExpenseProxy, Date>() {
      public Date getValue(ExpenseProxy object) {
        return object.getCreated();
      }
    };
    defaultComparator = createColumnComparator(createdGetter, false);
    Comparator<ExpenseProxy> createdDesc = createColumnComparator(
        createdGetter, true);
    addColumn(view, "Created",
        new DateCell(DateTimeFormat.getFormat("MMM dd yyyy")), createdGetter,
        defaultComparator, createdDesc);
    lastComparator = defaultComparator;

    // Description column.
    addColumn(view, "Description", new TextCell(),
        new GetValue<ExpenseProxy, String>() {
          public String getValue(ExpenseProxy object) {
            return object.getDescription();
          }
        });

    // Category column.
    addColumn(view, "Category", new TextCell(),
        new GetValue<ExpenseProxy, String>() {
          public String getValue(ExpenseProxy object) {
            return object.getCategory();
          }
        });

    // Amount column.
    final GetValue<ExpenseProxy, Double> amountGetter = new GetValue<ExpenseProxy, Double>() {
      public Double getValue(ExpenseProxy object) {
        return object.getAmount();
      }
    };
    Comparator<ExpenseProxy> amountAsc = createColumnComparator(amountGetter,
        false);
    Comparator<ExpenseProxy> amountDesc = createColumnComparator(amountGetter,
        true);
    addColumn(view, "Amount", new NumberCell(NumberFormat.getCurrencyFormat()),
        new GetValue<ExpenseProxy, Number>() {
          public Number getValue(ExpenseProxy object) {
            return amountGetter.getValue(object);
          }
        }, amountAsc, amountDesc);

    // Dialog box to obtain a reason for a denial
    final DenialPopup denialPopup = new DenialPopup();
    denialPopup.addCloseHandler(new CloseHandler<PopupPanel>() {
      public void onClose(CloseEvent<PopupPanel> event) {
        String reasonDenied = denialPopup.getReasonDenied();
        ExpenseProxy record = denialPopup.getExpenseRecord();
        if (reasonDenied == null || reasonDenied.length() == 0) {
          // We need to redraw the table to reset the select box.
          syncCommit(record, null);
        } else {
          updateExpenseRecord(record, "Denied", reasonDenied);
        }
      }
    });

    // Approval column.
    approvalCell = new ApprovalCell();
    Column<ExpenseProxy, String> approvalColumn = addColumn(view,
        "Approval Status", approvalCell, new GetValue<ExpenseProxy, String>() {
          public String getValue(ExpenseProxy object) {
            return object.getApproval();
          }
        });
    approvalColumn.setFieldUpdater(new FieldUpdater<ExpenseProxy, String>() {
      public void update(int index, final ExpenseProxy object, String value) {
        if ("Denied".equals(value)) {
          denialPopup.setExpenseRecord(object);
          denialPopup.setReasonDenied(object.getReasonDenied());
          denialPopup.popup();
        } else {
          updateExpenseRecord(object, value, "");
        }
      }
    });

    // Spacer column.
    view.addColumn(new SpacerColumn<ExpenseProxy>());

    return view;
  }

  /**
   * Add a column of a {@link Comparable} type using default comparators.
   * 
   * @param <C> the column type
   * @param table the table
   * @param text the header text
   * @param cell the cell used to render values
   * @param getter the {@link GetValue} used to retrieve cell values
   * @return the new column
   */
  private <C extends Comparable<C>> Column<ExpenseProxy, C> addColumn(
      final CellTable<ExpenseProxy> table, final String text,
      final Cell<C> cell, final GetValue<ExpenseProxy, C> getter) {
    return addColumn(table, text, cell, getter,
        createColumnComparator(getter, false),
        createColumnComparator(getter, true));
  }

  /**
   * Add a column with the specified comparators.
   * 
   * @param <C> the column type
   * @param table the table
   * @param text the header text
   * @param cell the cell used to render values
   * @param getter the {@link GetValue} used to retrieve cell values
   * @param ascComparator the comparator used to sort ascending
   * @param descComparator the comparator used to sort ascending
   * @return the new column
   */
  private <C> Column<ExpenseProxy, C> addColumn(
      final CellTable<ExpenseProxy> table, final String text,
      final Cell<C> cell, final GetValue<ExpenseProxy, C> getter,
      final Comparator<ExpenseProxy> ascComparator,
      final Comparator<ExpenseProxy> descComparator) {

    // Create the column.
    final Column<ExpenseProxy, C> column = new Column<ExpenseProxy, C>(cell) {
      @Override
      public C getValue(ExpenseProxy object) {
        return getter.getValue(object);
      }
    };
    final SortableHeader header = new SortableHeader(text);
    allHeaders.add(header);

    // Hook up sorting.
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

        sortExpenses(items.getList(), header.getReverseSort() ? descComparator
            : ascComparator);
        table.redrawHeaders();
      }
    });
    table.addColumn(column, header);
    return column;
  }

  /**
   * Create a comparator for the column.
   * 
   * @param <C> the column type
   * @param getter the {@link GetValue} used to get the cell value
   * @param descending true if descending, false if ascending
   * @return the comparator
   */
  private <C extends Comparable<C>> Comparator<ExpenseProxy> createColumnComparator(
      final GetValue<ExpenseProxy, C> getter, final boolean descending) {
    return new Comparator<ExpenseProxy>() {
      public int compare(ExpenseProxy o1, ExpenseProxy o2) {
        // Null check the row object.
        if (o1 == null && o2 == null) {
          return 0;
        } else if (o1 == null) {
          return descending ? 1 : -1;
        } else if (o2 == null) {
          return descending ? -1 : 1;
        }

        // Compare the column value.
        C c1 = getter.getValue(o1);
        C c2 = getter.getValue(o2);
        if (c1 == null && c2 == null) {
          return 0;
        } else if (c1 == null) {
          return descending ? 1 : -1;
        } else if (c2 == null) {
          return descending ? -1 : 1;
        }
        int comparison = c1.compareTo(c2);
        return descending ? -comparison : comparison;
      }
    };
  }

  /**
   * Create the error message popup.
   */
  private void createErrorPopup() {
    errorPopup.setGlassEnabled(true);
    errorPopup.setStyleName(Styles.common().popupPanel());
    errorPopupMessage.addStyleName(
        Styles.common().expenseDetailsErrorPopupMessage());

    Button closeButton = new Button("Dismiss", new ClickHandler() {
      public void onClick(ClickEvent event) {
        errorPopup.hide();
      }
    });

    // Organize the widgets in the popup.
    VerticalPanel layout = new VerticalPanel();
    layout.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    layout.add(errorPopupMessage);
    layout.add(closeButton);
    errorPopup.setWidget(layout);
  }

  /**
   * Return a formatted currency string.
   * 
   * @param amount the amount in dollars
   * @return a formatted string
   */
  private String formatCurrency(double amount) {
    boolean negative = amount < 0;
    if (negative) {
      amount = -amount;
    }
    int dollars = (int) amount;
    int cents = (int) ((amount * 100) % 100);

    StringBuilder sb = new StringBuilder();
    if (negative) {
      sb.append("-");
    }
    sb.append("$");
    sb.append(dollars);
    sb.append('.');
    if (cents < 10) {
      sb.append('0');
    }
    sb.append(cents);
    return sb.toString();
  }

  /**
   * Get the error message from a sync operation.
   * 
   * @param response the response of the operation
   * @return the error message, or an empty string if no error.
   */
  private String getErrorMessageFromSync(Set<SyncResult> response) {
    String errorMessage = "";
    // TODO: Remove this method..
    return errorMessage;
  }

  /**
   * Get the columns displayed in the expense table.
   */
  private String[] getExpenseColumns() {
    return new String[]{ "amount", "approval", "category", "created", 
        "description", "reasonDenied"};
  }

  /**
   * Refresh the total cost and approved amount.
   */
  private void refreshCost() {
    double totalCost = 0;
    totalApproved = 0;
    List<ExpenseProxy> records = items.getList();
    for (ExpenseProxy record : records) {
      double cost = record.getAmount();
      totalCost += cost;
      if (Expenses.Approval.APPROVED.is(record.getApproval())) {
        totalApproved += cost;
      }
    }
    double unreconciled = totalCost - totalApproved;
    costLabel.setInnerText(formatCurrency(totalCost));
    approvedLabel.setInnerText(formatCurrency(totalApproved));
    unreconciledLabel.setInnerText(formatCurrency(unreconciled));
  }

  /**
   * Request the expenses.
   */
  private void requestExpenses() {
    // Cancel the timer since we are about to send a request.
    refreshTimer.cancel();
    lastReceiver = new Receiver<List<ExpenseProxy>>() {
      @Override
      public void onSuccess(List<ExpenseProxy> newValues,
          Set<SyncResult> syncResults) {
        if (this == lastReceiver) {
          List<ExpenseProxy> list = new ArrayList<ExpenseProxy>(newValues);
          sortExpenses(list, lastComparator);
          items.setList(list);
          refreshCost();

          // Add the new keys and changed values to the known keys.
          boolean isInitialData = knownExpenseKeys == null;
          if (knownExpenseKeys == null) {
            knownExpenseKeys = new HashMap<Object, ExpenseProxy>();
          }
          for (ExpenseProxy value : newValues) {
            Object key = items.getKey(value);
            if (!isInitialData) {
              ExpenseProxy existing = knownExpenseKeys.get(key);
              if (existing == null
                  || !value.getAmount().equals(existing.getAmount())
                  || !value.getDescription().equals(existing.getDescription())
                  || !value.getCategory().equals(existing.getCategory())) {
                (new PhaseAnimation.CellTablePhaseAnimation<ExpenseProxy>(
                    table, value, items)).run();
              }
            }
            knownExpenseKeys.put(key, value);
          }
        }

        // Reschedule the timer.
        refreshTimer.schedule(REFRESH_INTERVAL);
      }
    };

    expensesRequestFactory.expenseRequest().findExpensesByReport(
        report.getId()).with(getExpenseColumns()).fire(lastReceiver);
  }

  /**
   * Save the notes that the user entered in the notes box.
   */
  private void saveNotes() {
    // Early exit if the notes haven't changed.
    final String pendingNotes = notesBox.getText();
    if (pendingNotes.equals(report.getNotes())) {
      setNotesEditState(false, false, pendingNotes);
      return;
    }

    // Switch to the pending view.
    setNotesEditState(false, true, pendingNotes);

    // Submit the delta.
    RequestObject<Void> editRequest = expensesRequestFactory.reportRequest().persist(
        report);
    ReportProxy editableReport = editRequest.edit(report);
    editableReport.setNotes(pendingNotes);
    editRequest.fire(new Receiver<Void>() {
      @Override
      public void onSuccess(Void ignore, Set<SyncResult> response) {
        // We expect onReportChanged to be called if there are no errors.
        String errorMessage = getErrorMessageFromSync(response);
        if (errorMessage.length() > 0) {
          showErrorPopup(errorMessage);
          setNotesEditState(false, false, report.getNotes());
        }
      }
    });
  }

  /**
   * Set the state of the notes section.
   * 
   * @param editable true for edit state, false for view state
   * @param pending true if changes are pending, false if not
   * @param notesText the current notes
   */
  private void setNotesEditState(boolean editable, boolean pending,
      String notesText) {
    notesBox.setText(notesText);
    notes.setInnerText(notesText);

    notesBox.setVisible(editable && !pending);
    setVisible(notes, !editable);
    setVisible(notesEditLinkWrapper, !editable && !pending);
    setVisible(notesPending, pending);
    notesBox.setFocus(editable);
  }

  /**
   * Show the error popup.
   * 
   * @param errorMessage the error message
   */
  private void showErrorPopup(String errorMessage) {
    errorPopupMessage.setText(errorMessage);
    errorPopup.center();
  }

  private void sortExpenses(List<ExpenseProxy> list,
      final Comparator<ExpenseProxy> comparator) {
    lastComparator = comparator;
    Collections.sort(list, comparator);
  }

  /**
   * Update the state of a pending approval change.
   * 
   * @param record the {@link ExpenseProxy} to sync
   * @param message the error message if rejected, or null if accepted
   */
  private void syncCommit(ExpenseProxy record, String message) {
    final Object key = items.getKey(record);
    if (message != null) {
      final ApprovalViewData avd = approvalCell.getViewData(key);
      if (avd != null) {
        avd.reject(message);
      }
    }

    // Redraw the table so the changes are applied.
    table.redraw();
  }

  private void updateExpenseRecord(final ExpenseProxy record, String approval,
      String reasonDenied) {
    // Verify that the total is under the cap.
    if (Expenses.Approval.APPROVED.is(approval)
        && !Expenses.Approval.APPROVED.is(record.getApproval())) {
      double amount = record.getAmount();
      if (amount + totalApproved > MAX_COST) {
        syncCommit(record,
            "The total approved amount for an expense report cannot exceed $"
                + MAX_COST + ".");
        return;
      }
    }

    // Create a delta and sync with the value store.
    RequestObject<Void> editRequest = expensesRequestFactory.expenseRequest().persist(
        record);
    ExpenseProxy editableRecord = editRequest.edit(record);
    editableRecord.setApproval(approval);
    editableRecord.setReasonDenied(reasonDenied);
    editRequest.fire(new Receiver<Void>() {
      @Override
      public void onSuccess(Void ignore, Set<SyncResult> response) {
        String errorMessage = getErrorMessageFromSync(response);
        if (errorMessage.length() > 0) {
          syncCommit(record, errorMessage.length() > 0 ? errorMessage : null);
        }
      }
    });
  }
}
