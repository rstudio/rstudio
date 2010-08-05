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

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.requestfactory.client.AuthenticationFailureHandler;
import com.google.gwt.requestfactory.client.LoginWidget;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestEvent;
import com.google.gwt.requestfactory.shared.UserInformationRecord;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.sample.bikeshed.style.client.Styles;
import com.google.gwt.sample.expenses.gwt.request.EmployeeRecord;
import com.google.gwt.sample.expenses.gwt.request.ExpenseRecord;
import com.google.gwt.sample.expenses.gwt.request.ExpenseRecordChanged;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.gwt.request.ReportRecord;
import com.google.gwt.sample.expenses.gwt.request.ReportRecordChanged;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.valuestore.shared.SyncResult;
import com.google.gwt.view.client.ProvidesKey;

import java.util.Set;

/**
 * Entry point for the Expenses app.
 */
public class Expenses implements EntryPoint {

  /**
   * An enum describing the approval status.
   */
  public static enum Approval {
    BLANK("", "inherit", Styles.resources().blankIcon()), APPROVED("Approved",
        "#00aa00", Styles.resources().approvedIcon()), DENIED("Denied",
        "#ff0000", Styles.resources().deniedIcon());

    /**
     * Get the {@link Approval} from the specified string.
     * 
     * @param approval the approval string
     * @return the {@link Approval}
     */
    public static Approval from(String approval) {
      if (APPROVED.is(approval)) {
        return APPROVED;
      } else if (DENIED.is(approval)) {
        return DENIED;
      }
      return BLANK;
    }

    private final String color;
    private final String iconHtml;
    private final String text;

    private Approval(String text, String color, ImageResource res) {
      this.text = text;
      this.color = color;
      this.iconHtml = AbstractImagePrototype.create(res).getHTML();
    }

    public String getColor() {
      return color;
    }

    public String getIconHtml() {
      return iconHtml;
    }

    public String getText() {
      return text;
    }

    public boolean is(String compare) {
      return text.equals(compare);
    }
  }

  public static final String[] DEPARTMENTS = {
      "Engineering", "Finance", "Marketing", "Operations", "Sales"};

  /**
   * The key provider for {@link EmployeeRecord}s.
   */
  public static final ProvidesKey<EmployeeRecord> EMPLOYEE_RECORD_KEY_PROVIDER = new ProvidesKey<EmployeeRecord>() {
    public Object getKey(EmployeeRecord item) {
      return item == null ? null : item.getId();
    }
  };

  /**
   * The key provider for {@link ExpenseRecord}s.
   */
  public static final ProvidesKey<ExpenseRecord> EXPENSE_RECORD_KEY_PROVIDER = new ProvidesKey<ExpenseRecord>() {
    public Object getKey(ExpenseRecord item) {
      return item == null ? null : item.getId();
    }
  };

  /**
   * The key provider for {@link ReportRecord}s.
   */
  public static final ProvidesKey<ReportRecord> REPORT_RECORD_KEY_PROVIDER = new ProvidesKey<ReportRecord>() {
    public Object getKey(ReportRecord item) {
      return item == null ? null : item.getId();
    }
  };

  private String lastDepartment;
  private EmployeeRecord lastEmployee;
  private ExpensesRequestFactory requestFactory;
  private ExpensesShell shell;

  public void onModuleLoad() {
    final HandlerManager eventBus = new HandlerManager(null);
    requestFactory = GWT.create(ExpensesRequestFactory.class);
    requestFactory.init(eventBus);

    RootLayoutPanel root = RootLayoutPanel.get();

    shell = new ExpensesShell();
    final ExpenseTree expenseTree = shell.getExpenseTree();
    final ExpenseList expenseList = shell.getExpenseList();
    final ExpenseDetails expenseDetails = shell.getExpenseDetails();

    root.add(shell);
    
    // Check for Authentication failures or mismatches
    eventBus.addHandler(RequestEvent.TYPE, new AuthenticationFailureHandler());

    // Add a login widget to the page
    final LoginWidget login = shell.getLoginWidget();
    Receiver<UserInformationRecord> receiver = new Receiver<UserInformationRecord>() {
      public void onSuccess(UserInformationRecord userInformationRecord, Set<SyncResult> syncResults) {
        login.setUserInformation(userInformationRecord);
      }       
     };
     requestFactory.userInformationRequest().getCurrentUserInformation(
         Location.getHref()).fire(receiver);

    // Listen for requests from ExpenseTree.
    expenseTree.setListener(new ExpenseTree.Listener() {
      public void onSelection(String department, EmployeeRecord employee) {
        lastDepartment = department;
        lastEmployee = employee;
        expenseList.setEmployee(department, employee);
        shell.showExpenseDetails(false);
      }
    });
    expenseTree.setRequestFactory(requestFactory);

    // Listen for requests from the ExpenseList.
    expenseList.setListener(new ExpenseList.Listener() {
      public void onReportSelected(ReportRecord report) {
        expenseDetails.setExpensesRequestFactory(requestFactory);
        expenseDetails.setReportRecord(report, lastDepartment, lastEmployee);
        shell.showExpenseDetails(true);
      }
    });
    expenseList.setRequestFactory(requestFactory);
    eventBus.addHandler(ReportRecordChanged.TYPE, expenseList);

    // Forward change events to the expense details.
    eventBus.addHandler(ExpenseRecordChanged.TYPE, expenseDetails);
    eventBus.addHandler(ReportRecordChanged.TYPE, expenseDetails);
  }
}
