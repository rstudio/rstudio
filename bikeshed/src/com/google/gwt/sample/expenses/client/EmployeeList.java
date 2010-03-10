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

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.sample.expenses.shared.Employee;
import com.google.gwt.user.client.ui.HasValueList;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.valuestore.shared.Values;

import java.util.List;

/**
 * Manages the Employee ListBox. This shoudl grow into a proper View, with a
 * corresponding Presenter factored out of {@link Expenses}
 */
public final class EmployeeList implements HasValueList<Values<Employee>> {
  interface Listener {
    void onEmployeeSelected(Employee e);
  }

  private final class MyChangeHandler implements ChangeHandler {
    public void onChange(ChangeEvent event) {
      int selectedIndex = listBox.getSelectedIndex();
      Values<Employee> values = employeeValues.get(selectedIndex);
      Employee e = values.getPropertyHolder();
      listener.onEmployeeSelected(e);
    }
  }

  private final ListBox listBox;
  private List<Values<Employee>> employeeValues;
  private Listener listener;

  /**
   * @param shell
   * @param requestFactory
   */
  public EmployeeList(ListBox listBox) {
    this.listBox = listBox;
    listBox.addChangeHandler(new MyChangeHandler());
  }

  public void editValueList(boolean replace, int index,
      List<Values<Employee>> newValues) {
    throw new UnsupportedOperationException();
  }

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  public void setValueList(List<Values<Employee>> newValues) {
    this.employeeValues = newValues;
    listBox.clear();
    for (int i = 0; i < employeeValues.size(); i++) {
      Values<Employee> values = employeeValues.get(i);
      listBox.addItem(values.get(Employee.DISPLAY_NAME));
    }
  }

  public void setValueListSize(int size, boolean exact) {
    throw new UnsupportedOperationException();
  }
}