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

import com.google.gwt.sample.gaerequest.shared.MakesGaeRequests;
import com.google.web.bindery.requestfactory.shared.RequestFactory;

/**
 * RequestFactory interface. Instances created via {@link com.google.gwt.core.client.GWT#create}
 * can insantiate RPC request objects.
 */
public interface ExpensesRequestFactory extends RequestFactory, MakesGaeRequests {

  /**
   * @return a request selector
   */
  EmployeeRequest employeeRequest();

  /**
   * @return a request selector
   */
  ExpenseRequest expenseRequest();

  /**
   * @return a request selector
   */
  ReportRequest reportRequest();
}
