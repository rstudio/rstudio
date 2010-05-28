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
package com.google.gwt.sample.expenses.gwt.scaffold.place;

import com.google.gwt.sample.expenses.gwt.request.EmployeeRecord;

/**
 * A place in the scaffold app for working with an {@EmployeeRecord
 * }.
 */
public class EmployeeScaffoldPlace extends ScaffoldRecordPlace {

  public EmployeeScaffoldPlace(EmployeeRecord record, Operation operation) {
    super(record.getId(), operation);
  }

  public EmployeeScaffoldPlace(String id, Operation operation) {
    super(id, operation);
  }

  @Override
  public void accept(ScaffoldPlaceProcessor visitor) {
    visitor.process(this);
  }

  @Override
  public <T> T acceptFilter(ScaffoldPlaceFilter<T> filter) {
    return filter.filter(this);
  }
}
