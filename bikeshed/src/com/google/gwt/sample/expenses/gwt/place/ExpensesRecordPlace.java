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
package com.google.gwt.sample.expenses.gwt.place;

import com.google.gwt.valuestore.shared.Record;

/**
 * A place in the app focused on the {@link Values} of a particular type of
 * {@link com.google.gwt.valuestore.shared.ValueStore ValueStore} record.
 */
public abstract class ExpensesRecordPlace extends ExpensesPlace {

  private final Record record;

  /**
   * @param record
   */
  public ExpensesRecordPlace(Record record) {

    this.record = record;
  }

  /**
   * @return the entity
   */
  public Record getEntity() {
    return record;
  }

}
