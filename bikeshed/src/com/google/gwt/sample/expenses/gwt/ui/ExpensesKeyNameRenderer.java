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
package com.google.gwt.sample.expenses.gwt.ui;

import com.google.gwt.sample.expenses.gwt.request.EmployeeRecord;
import com.google.gwt.sample.expenses.gwt.request.ReportRecord;
import com.google.gwt.user.client.ui.Renderer;
import com.google.gwt.valuestore.shared.Record;

/**
 * Renders the name of an {@link ExpensesKey}.
 */
//TODO i18n
public class ExpensesKeyNameRenderer implements Renderer<Record> {
  public String render(Record record) {
    if (record instanceof EmployeeRecord) {
      return "Employees";
    }
    if (record instanceof ReportRecord) {
      return "Reports";
    }
    throw new IllegalArgumentException("Unrecognized schema " + record);
  }
}
