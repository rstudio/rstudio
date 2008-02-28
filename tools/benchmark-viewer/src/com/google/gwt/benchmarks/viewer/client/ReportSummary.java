/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.benchmarks.viewer.client;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Date;

/**
 * A data object summarizing the results of a report.
 */
public class ReportSummary implements IsSerializable {

  private boolean allTestsSucceeded;

  private Date date;

  // A temporary addition until we get better date formatting in GWT user
  private String dateString;

  private String id;

  // in GWT
  private int numTests;

  public ReportSummary() {
  }

  public ReportSummary(String id, Date date, String dateString, int numTests,
      boolean allTestsSucceeded) {
    this.id = id;
    this.date = date;
    this.dateString = dateString;
    this.numTests = numTests;
    this.allTestsSucceeded = allTestsSucceeded;
  }

  public boolean allTestsSucceeded() {
    return allTestsSucceeded;
  }

  public Date getDate() {
    return date;
  }

  public String getDateString() {
    return dateString;
  }

  public String getId() {
    return id;
  }

  public int getNumTests() {
    return numTests;
  }
}
