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
import com.google.gwt.cell.client.IconCellDecorator;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.sample.expenses.client.style.Styles;
import com.google.gwt.sample.expenses.shared.EmployeeProxy;
import com.google.gwt.sample.expenses.shared.ExpensesRequestFactory;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwt.view.client.TreeViewModel;
import com.google.web.bindery.requestfactory.gwt.ui.client.EntityProxyKeyProvider;
import com.google.web.bindery.requestfactory.shared.EntityProxyId;
import com.google.web.bindery.requestfactory.shared.Receiver;

import java.util.List;

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
    void onSelection(String department, EntityProxyId<EmployeeProxy> employeeId);
  }

  interface Template extends SafeHtmlTemplates {
    @Template("<span class=\"{0}\">{1}</span>")
    SafeHtml span(String classes, String userName);
  }

  /**
   * A {@link AbstractCell} that represents an {@link EmployeeProxy}.
   */
  private class EmployeeCell extends IconCellDecorator<EmployeeProxy> {

    public EmployeeCell() {
      super(Styles.resources().userIcon(), new AbstractCell<EmployeeProxy>() {

        private final String usernameStyle = Styles.common().usernameTreeItem();
        private final String usernameStyleSelected = Styles.common().usernameTreeItemSelected();

        @Override
        public boolean dependsOnSelection() {
          return true;
        }

        @Override
        public void render(Context context, EmployeeProxy value,
            SafeHtmlBuilder sb) {
          if (value != null) {
            StringBuilder classesBuilder = new StringBuilder(usernameStyle);
            if (lastEmployee != null
                && lastEmployee.getId().equals(value.getId())) {
              classesBuilder.append(" ").append(usernameStyleSelected);
            }

            sb.appendEscaped(value.getDisplayName());
            sb.appendHtmlConstant("<br>");
            sb.append(template.span(classesBuilder.toString(),
                value.getUserName()));
          }
        }
      });
      if (template == null) {
        template = GWT.create(Template.class);
      }
    }
  }

  /**
   * The {@link ListDataProvider} used for Employee lists.
   */
  private class EmployeeListDataProvider extends
      AsyncDataProvider<EmployeeProxy> {

    private final String department;

    public EmployeeListDataProvider(String department) {
      super(null);
      this.department = department;
    }

    @Override
    public void addDataDisplay(HasData<EmployeeProxy> display) {
      super.addDataDisplay(display);

      // Request the count anytime a view is added.
      requestFactory.employeeRequest().countEmployeesByDepartment(department).fire(
          new Receiver<Long>() {
            @Override
            public void onSuccess(Long response) {
              updateRowCount(response.intValue(), true);
            }
          });
    }

    @Override
    protected void onRangeChanged(HasData<EmployeeProxy> view) {
      Range range = view.getVisibleRange();
      requestFactory.employeeRequest().findEmployeeEntriesByDepartment(
          department, range.getStart(), range.getLength()).with(
          getEmployeeMenuProperties()).fire(
          new Receiver<List<EmployeeProxy>>() {
            @Override
            public void onSuccess(List<EmployeeProxy> response) {
              updateRowData(0, response);
            }
          });
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
        EmployeeListDataProvider dataProvider = new EmployeeListDataProvider(
            (String) value);
        return new DefaultNodeInfo<EmployeeProxy>(dataProvider, employeeCell,
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
      List<String> list = departments.getList();
      String string = value.toString();
      return list.contains(string);
    }

    public boolean isLeaf(Object value) {
      return value != null && (!isDepartment(value) || isAllDepartment(value));
    }
  }

  private static Template template;

  /**
   * The data provider that provides departments.
   */
  private final ListDataProvider<String> departments = new ListDataProvider<String>();

  /**
   * The last selected department.
   */
  private String lastDepartment;

  /**
   * The last selected employee.
   */
  private EmployeeProxy lastEmployee;

  /**
   * The listener of this widget.
   */
  private Listener listener;

  /**
   * The factory used to send requests.
   */
  private final ExpensesRequestFactory requestFactory;

  /**
   * The shared {@link SingleSelectionModel}.
   */
  private final SingleSelectionModel<Object> selectionModel = new SingleSelectionModel<Object>(
      new ProvidesKey<Object>() {
        EntityProxyKeyProvider<EmployeeProxy> keyProvider = new EntityProxyKeyProvider<EmployeeProxy>();

        public Object getKey(Object item) {
          if (item instanceof EmployeeProxy) {
            return keyProvider.getKey((EmployeeProxy) item);
          }
          return item;
        }
      });

  /**
   * The main widget.
   */
  private CellTree tree;

  public ExpenseTree(ExpensesRequestFactory requestFactory) {
    this.requestFactory = requestFactory;

    // Initialize the departments.
    List<String> departmentList = departments.getList();
    departmentList.add("All");
    for (String department : ExpensesApp.DEPARTMENTS) {
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
          lastDepartment = "";
        } else if (selected instanceof EmployeeProxy) {
          lastEmployee = (EmployeeProxy) selected;
          lastDepartment = lastEmployee.getDepartment();
        } else if (selected instanceof String) {
          lastEmployee = null;
          if (model.isAllDepartment(selected)) {
            lastDepartment = "";
          } else {
            lastDepartment = (String) selected;
          }
        }

        if (listener != null) {
          listener.onSelection(lastDepartment, lastEmployee == null ? null
              : lastEmployee.stableId());
        }
      }
    });

    // Create a CellBrowser.
    tree = new CellTree(model, null);
    tree.setAnimationEnabled(true);
  }

  private String[] getEmployeeMenuProperties() {
    return new String[] {"displayName", "userName"};
  }
}
