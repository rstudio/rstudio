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
package com.google.gwt.sample.expenses.gwt.request;

import com.google.gwt.requestfactory.shared.EntityListRequest;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.ServerOperation;
import com.google.gwt.valuestore.shared.ValuesKey;

/**
 * Request selector.
 */
public interface EmployeeRequest {

  /**
   * Defines the server operations that handle these requests.
   */
  public enum ServerOperations implements RequestFactory.RequestDefinition {
    FIND_ALL_EMPLOYEES {
      public String getDomainMethodName() {
        return "findAllEmployees";
      }

      public Class<? extends ValuesKey<?>> getReturnType() {
        return com.google.gwt.sample.expenses.gwt.request.EmployeeKey.class;
      }
    };

    public String getDomainClassName() {
      return "com.google.gwt.sample.expenses.server.domain.Employee";
    }

    public Class<?>[] getParameterTypes() {
      return null;
    }
  }

  /**
   * @return a request object
   */
  @ServerOperation("FIND_ALL_EMPLOYEES")
  EntityListRequest<EmployeeKey> findAllEmployees();
}
