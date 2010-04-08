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
import com.google.gwt.requestfactory.shared.LongString;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.ServerOperation;
import com.google.gwt.valuestore.shared.ValueRef;
import com.google.gwt.valuestore.shared.ValuesKey;

/**
 * Request selector.
 */
public interface ReportRequest {

  /**
   * Defines the server operations that handle these requests.
   */
  public enum ServerOperations implements RequestFactory.RequestDefinition {
    FIND_REPORTS_BY_EMPLOYEE {
      public String getDomainMethodName() {
        return "findReportsByEmployee";
      }

      public Class<?>[] getParameterTypes() {
        return new Class[] { java.lang.Long.class };
      }

      public Class<? extends ValuesKey<?>> getReturnType() {
        return com.google.gwt.sample.expenses.gwt.request.ReportKey.class;
      }
    },

    FIND_ALL_REPORTS {
      public String getDomainMethodName() {
        return "findAllReports";
      }

      public Class<? extends ValuesKey<?>> getReturnType() {
        return com.google.gwt.sample.expenses.gwt.request.ReportKey.class;
      }
    };

    public String getDomainClassName() {
      return "com.google.gwt.sample.expenses.server.domain.Report";
    }

    public Class<?>[] getParameterTypes() {
      return null;
    }
  }

  /**
   * @return a request object
   */
  @ServerOperation("FIND_REPORTS_BY_EMPLOYEE")
  EntityListRequest<ReportKey> findReportsByEmployee(
      @LongString ValueRef<EmployeeKey, String> id);

  /**
   * @return a request object
   */
  @ServerOperation("FIND_ALL_REPORTS")
  EntityListRequest<ReportKey> findAllReports();
}
