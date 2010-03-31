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

import com.google.gwt.requestfactory.client.gen.ClientRequestObject;
import com.google.gwt.requestfactory.client.impl.AbstractListJsonRequestObject;
import com.google.gwt.requestfactory.shared.EntityListRequest;
import com.google.gwt.requestfactory.shared.impl.RequestDataManager;
import com.google.gwt.sample.expenses.shared.EmployeeKey;
import com.google.gwt.sample.expenses.shared.ExpenseRequestFactory;

/**
 * "Code generated" implementation of {ExpenseRequestFactory.EmployeeRequest}.
 * <p>
 * IRL this will be generated as a side effect of a call to
 * GWT.create(ExpenseRequestFactory.class)
 */
public class EmployeeRequestImpl implements
    ExpenseRequestFactory.EmployeeRequest {

  private abstract class RequestImpl extends
      AbstractListJsonRequestObject<EmployeeKey, RequestImpl> {

    RequestImpl() {
      super(EmployeeKey.get(), factory);
    }

    @Override
    protected RequestImpl getThis() {
      return this;
    }
  }

  private final ExpenseRequestFactoryImpl factory;
  
  public EmployeeRequestImpl(ExpenseRequestFactoryImpl factory) {
    this.factory = factory;
  }

  public EntityListRequest<EmployeeKey> findAllEmployees() {
    return new RequestImpl() {
      public String getRequestData() {
        return ClientRequestObject.getRequestString(RequestDataManager.getRequestMap(
            ExpenseRequestFactory.ServerSideOperation.FIND_ALL_EMPLOYEES.name(), null, null));
      }
    };
  }
}
