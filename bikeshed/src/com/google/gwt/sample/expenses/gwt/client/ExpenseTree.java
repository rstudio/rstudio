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

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.IconCellDecorator;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.sample.bikeshed.style.client.Styles;
import com.google.gwt.sample.expenses.gwt.request.EmployeeRecord;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.SyncResult;
import com.google.gwt.view.client.AsyncListViewAdapter;
import com.google.gwt.view.client.ListView;
import com.google.gwt.view.client.ListViewAdapter;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwt.view.client.TreeViewModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * The employee tree located on the left of the app.
 */
public class ExpenseTree extends Composite {

  /**
   * Custom listener for this widget.
   */
  public interface Listener {

    /**
     * Called when the user selects a tree item.
     * 
     * @param department the selected department name
     * @param employee the selected employee
     */
    void onSelection(String department, EmployeeRecord employee);
  }

  /**
   * A {@link AbstractCell} that represents an {@link EmployeeRecord}.
   */
  private class EmployeeCell extends IconCellDecorator<EmployeeRecord> {

    public EmployeeCell() {
      super(Styles.resources().userIcon(), new AbstractCell<EmployeeRecord>() {

        private final String usernameStyle = Styles.common().usernameTreeItem();
        private final String usernameStyleSelected = Styles.common().usernameTreeItemSelected();

        @Override
        public boolean dependsOnSelection() {
          return true;
        }

        @Override
        public void render(EmployeeRecord value, Object viewData,
            StringBuilder sb) {
          if (value != null) {
            sb.append(value.getDisplayName()).append("<br>");
            sb.append("<span class='").append(usernameStyle);
            if (lastEmployee != null
                && lastEmployee.getId().equals(value.getId())) {
              sb.append(" ").append(usernameStyleSelected);
            }
            sb.append("'>");
            sb.append(value.getUserName());
            sb.append("</span>");
          }
        }
      });
    }
  }

  /**
   * The {@link ListViewAdapter} used for Employee lists.
   */
  private class EmployeeListViewAdapter extends
      AsyncListViewAdapter<EmployeeRecord> implements
      Receiver<List<EmployeeRecord>> {

    private final String department;

    public EmployeeListViewAdapter(String department) {
      this.department = department;
    }

    @Override
    public void addView(ListView<EmployeeRecord> view) {
      super.addView(view);

      // Request the count anytime a view is added.
      requestFactory.employeeRequest().countEmployeesByDepartment(department).fire(
          new Receiver<Long>() {
            public void onSuccess(Long response, Set<SyncResult> syncResults) {
              updateDataSize(response.intValue(), true);
            }
          });
    }

    public void onSuccess(List<EmployeeRecord> response, Set<SyncResult> syncResults) {
      updateViewData(0, response.size(), response);
    }

    @Override
    protected void onRangeChanged(ListView<EmployeeRecord> view) {
      Range range = view.getRange();
      requestFactory.employeeRequest().findEmployeeEntriesByDepartment(
          department, range.getStart(), range.getLength()).forProperties(
          getEmployeeMenuProperties()).fire(this);
    }
  }

  /**
   * The {@link TreeViewModel} used to browse expense reports.
   */
  private class ExpensesTreeViewModel implements TreeViewModel {

    /**
     * The department cell singleton.
     */
    private final Cell<String> departmentCell = new TextCell();

    /**
     * The {@link EmployeeCell} singleton.
     */
    private final EmployeeCell employeeCell = new EmployeeCell();

    public <T> NodeInfo<?> getNodeInfo(T value) {
      if (value == null) {
        // Top level.
        return new DefaultNodeInfo<String>(departments, departmentCell,
            selectionModel, null);
      } else if (isAllDepartment(value)) {
        // Employees are not displayed under the 'All' Department.
        return null;
      } else if (value instanceof String) {
        // Second level.
        EmployeeListViewAdapter adapter = new EmployeeListViewAdapter(
            (String) value);
        return new DefaultNodeInfo<EmployeeRecord>(adapter, employeeCell,
            selectionModel, null);
      }

      return null;
    }

    /**
     * @return true if the object is the All department
     */
    public boolean isAllDepartment(Object value) {
      return departments.getList().get(0).equals(value);
    }

    /**
     * @return true if the object is a department
     */
    public boolean isDepartment(Object value) {
      return departments.getList().contains(value.toString());
    }

    public boolean isLeaf(Object value) {
      return !isDepartment(value) || isAllDepartment(value);
    }
  }

  /**
   * The adapter that provides departments.
   */
  private ListViewAdapter<String> departments = new ListViewAdapter<String>();

  /**
   * The last selected department.
   */
  private String lastDepartment;

  /**
   * The last selected employee.
   */
  private EmployeeRecord lastEmployee;

  /**
   * The listener of this widget.
   */
  private Listener listener;

  /**
   * The factory used to send requests.
   */
  private ExpensesRequestFactory requestFactory;

  /**
   * The shared {@link SingleSelectionModel}.
   */
  private final SingleSelectionModel<Object> selectionModel = new SingleSelectionModel<Object>();

  /**
   * The main widget.
   */
  private CellTree tree;

  public ExpenseTree() {
    // Initialize the departments.
    List<String> departmentList = departments.getList();
    departmentList.add("All");
    for (String department : Expenses.DEPARTMENTS) {
      departmentList.add(department);
    }

    // Initialize the widget.
    createTree();
    initWidget(tree);
    getElement().getStyle().setOverflow(Overflow.AUTO);
  }

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  public void setRequestFactory(ExpensesRequestFactory factory) {
    this.requestFactory = factory;
  }

  /**
   * Create the {@link CellTree}.
   */
  private void createTree() {
    final ExpensesTreeViewModel model = new ExpensesTreeViewModel();

    // Listen for selection. We need to add this handler before the CellBrowser
    // adds its own handler.
    selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
      public void onSelectionChange(SelectionChangeEvent event) {
        Object selected = selectionModel.getSelectedObject();
        if (selected == null) {
          lastEmployee = null;
          lastDepartment = null;
        } else if (selected instanceof EmployeeRecord) {
          lastEmployee = (EmployeeRecord) selected;
        } else if (selected instanceof String) {
          lastEmployee = null;
          if (model.isAllDepartment(selected)) {
            lastDepartment = null;
          } else {
            lastDepartment = (String) selected;
          }
        }

        if (listener != null) {
          listener.onSelection(lastDepartment, lastEmployee);
        }
      }
    });
    selectionModel.setKeyProvider(new ProvidesKey<Object>() {
      public Object getKey(Object item) {
        if (item instanceof EmployeeRecord) {
          return Expenses.EMPLOYEE_RECORD_KEY_PROVIDER.getKey((EmployeeRecord) item);
        }
        return item;
      }
    });

    // Create a CellBrowser.
    CellTree.Resources resources = GWT.create(CellTree.CleanResources.class);
    tree = new CellTree(model, null, resources);
    tree.setAnimationEnabled(true);
  }

  private Collection<Property<?>> getEmployeeMenuProperties() {
    List<Property<?>> columns = new ArrayList<Property<?>>();
    columns.add(EmployeeRecord.displayName);
    columns.add(EmployeeRecord.userName);
    return columns;
  }
}
