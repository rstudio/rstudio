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
package com.google.gwt.sample.expenses.gwt.customized;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.sample.expenses.gwt.request.EmployeeRecord;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TakesValueList;

import java.util.List;

/**
 * Manages the Employee ListBox. This should grow into a proper View, with a
 * corresponding Presenter factored out of {@link Customized}
 */
public final class EmployeeList implements TakesValueList<EmployeeRecord> {
  interface Listener {
    void onEmployeeSelected(EmployeeRecord e);
  }

  private final class MyChangeHandler implements ChangeHandler {
    public void onChange(ChangeEvent event) {
      int selectedIndex = listBox.getSelectedIndex();
      EmployeeRecord values = employeeValues.get(selectedIndex);
      listener.onEmployeeSelected(values);
    }
  }

  private final ListBox listBox;
  private List<EmployeeRecord> employeeValues;
  private Listener listener;

  /**
   * @param listBox
   */
  public EmployeeList(ListBox listBox) {
    this.listBox = listBox;
    listBox.addChangeHandler(new MyChangeHandler());
  }

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  public void setValueList(List<EmployeeRecord> newValues) {
    this.employeeValues = newValues;
    listBox.clear();
    for (int i = 0; i < employeeValues.size(); i++) {
      EmployeeRecord values = employeeValues.get(i);
      listBox.addItem(values.getDisplayName());
    }
  }
}
