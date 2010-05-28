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

import com.google.gwt.app.util.Renderer;
import com.google.gwt.sample.expenses.gwt.client.place.ListScaffoldPlace;
import com.google.gwt.sample.expenses.gwt.request.EmployeeRecord;
import com.google.gwt.sample.expenses.gwt.request.ReportRecord;
import com.google.gwt.valuestore.shared.Record;

/**
 * Renders {@link ListScaffoldPlace}s for display to users.
 */
public class ScaffoldListPlaceRenderer implements Renderer<ListScaffoldPlace> {

  public String render(ListScaffoldPlace object) {
    // TODO These class comparisons are gross, find a cleaner way.
    Class<? extends Record> type = object.getType();
    if (type.equals(EmployeeRecord.class)) {
      return "Employees";
    }
    if (type.equals(ReportRecord.class)) {
      return "Reports";
    }

    throw new IllegalArgumentException("Cannot render unknown type " + object);
  }
}
