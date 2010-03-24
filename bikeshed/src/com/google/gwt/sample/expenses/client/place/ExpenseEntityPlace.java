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
package com.google.gwt.sample.expenses.client.place;

import com.google.gwt.sample.expenses.shared.ExpensesEntityKey;
import com.google.gwt.valuestore.shared.Values;

/**
 * A place in the app focused on a particular entity.
 */
public abstract class ExpenseEntityPlace extends AbstractExpensesPlace {

  private final Values<? extends ExpensesEntityKey<?>> entity;

  /**
   * @param entity
   */
  public ExpenseEntityPlace(Values<? extends ExpensesEntityKey<?>> entity) {

    this.entity = entity;
  }

  /**
   * @return the entity
   */
  public Values<? extends ExpensesEntityKey<?>> getEntity() {
    return entity;
  }

}
