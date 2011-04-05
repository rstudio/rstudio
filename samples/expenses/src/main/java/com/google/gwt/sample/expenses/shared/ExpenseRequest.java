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
package com.google.gwt.sample.expenses.shared;

import com.google.gwt.sample.expenses.server.domain.Expense;
import com.google.web.bindery.requestfactory.shared.InstanceRequest;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.Service;

import java.util.List;

/**
 * Builds requests for the ExpenseRequest service.
 */
@Service(Expense.class)
public interface ExpenseRequest extends RequestContext {

  /**
   * @return a request object
   */
  Request<List<ExpenseProxy>> findAllExpenses();

  /**
   * @return a request object
   */
  Request<ExpenseProxy> findExpense(Long id);

  /**
   * @return a request object
   */
  Request<List<ExpenseProxy>> findExpensesByReport(Long id);

  /**
   * @return a request object
   */
  InstanceRequest<ExpenseProxy, Void> persist();

 /**
  * @return a request object
  */
  InstanceRequest<ExpenseProxy, Void> remove();

}
