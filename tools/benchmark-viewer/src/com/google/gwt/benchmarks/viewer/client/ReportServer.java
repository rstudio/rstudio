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

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import java.util.List;

/**
 * Provides Benchmark report summaries and details. This service must be running
 * in order to view the reports via ReportViewer.
 * 
 * @see com.google.gwt.junit.viewer.server.ReportServerImpl
 * @see ReportViewer
 */
@RemoteServiceRelativePath("test_reports")
public interface ReportServer extends RemoteService {

  /**
   * Returns the full details of the specified report.
   * 
   * @param reportId The id of the report. Originates from the ReportSummary.
   * @return the matching Report, or null if the Report could not be found.
   */
  Report getReport(String reportId);

  /**
   * Returns a list of summaries of all the Benchmark reports.
   * 
   * @return a non-null list of ReportSummary
   */
  List<ReportSummary> getReportSummaries();
}
