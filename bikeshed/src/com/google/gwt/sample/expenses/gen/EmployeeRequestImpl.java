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
package com.google.gwt.sample.expenses.gen;

import com.google.gwt.requestfactory.client.impl.AbstractListJsonRequestObject;
import com.google.gwt.requestfactory.shared.EntityListRequest;
import com.google.gwt.requestfactory.shared.RequestFactory.Service;
import com.google.gwt.sample.expenses.shared.EmployeeKey;
import com.google.gwt.sample.expenses.shared.ExpenseRequestFactory;
import com.google.gwt.valuestore.shared.ValueStore;

/**
 * "Code generated" implementation of {ExpenseRequestFactory.EmployeeRequest}
 * <p>
 * IRL this will be generated as a side effect of a call to
 * GWT.create(ExpenseRequestFactory.class)
 */
public class EmployeeRequestImpl implements
    ExpenseRequestFactory.EmployeeRequest {

  private abstract class Request extends
      AbstractListJsonRequestObject<EmployeeKey, Request> implements
      EntityListRequest<EmployeeKey> {

    Request() {
      super(EmployeeKey.get(), valueStore, requestService);
    }

    @Override
    protected Request getThis() {
      return this;
    }
  }

  private final ValueStore valueStore;
  public final Service requestService;

  public EmployeeRequestImpl(ValueStore valueStore, Service requestService) {
    this.valueStore = valueStore;
    this.requestService = requestService;
  }

  public EntityListRequest<EmployeeKey> findAllEmployees() {
    return new Request() {
      public String getRequestData(String data) {
        // TODO Dear Amit: your code here
        throw new UnsupportedOperationException();
      }

      @SuppressWarnings("deprecation")
      public String getRequestUrl() {
        return "/expenses/data?methodName="
            + MethodName.FIND_ALL_EMPLOYEES.name();
      }
    };
  }
}
