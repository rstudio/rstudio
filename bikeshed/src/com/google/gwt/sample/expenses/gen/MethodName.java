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

/**
 * Represents the MethodName.
 *
 * TODO: Remove this class in preference to RequestFactory interfaces or
 * generate automatically from RequestFactory interfaces.
 */
public enum MethodName {
  FIND_ALL_EMPLOYEES("Employee", "findAllEmployees"), FIND_ALL_REPORTS(
      "Report", "findAllReports"), FIND_EMPLOYEE("Employee", "findEmployee"), FIND_REPORTS_BY_EMPLOYEE(
      "Report", "findReportsByEmployee"), SYNC("", "");

  /* the className that contains the method */
  private final String className;

  /* the methodName */
  private final String methodName;

  private MethodName(String className, String methodName) {
    this.className = className;
    this.methodName = methodName;
  }

  /**
   * @return the className
   */
  public String getClassName() {
    return className;
  }

  /**
   * @return the methodName
   */
  public String getMethodName() {
    return methodName;
  }
}
