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
import com.google.gwt.requestfactory.shared.ServerOperation;
import com.google.gwt.valuestore.shared.ValueRef;

/**
 * Request selector.
 */
public interface ReportRequest {

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