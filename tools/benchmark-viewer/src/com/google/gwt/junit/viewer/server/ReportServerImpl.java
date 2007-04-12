/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.junit.viewer.server;

import com.google.gwt.junit.viewer.client.Report;
import com.google.gwt.junit.viewer.client.ReportServer;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import java.util.List;

/**
 * Implements the ReportServer RPC interface.
 */
public class ReportServerImpl extends RemoteServiceServlet implements
    ReportServer {

  public Report getReport(String reportId) {
    return ReportDatabase.getInstance().getReport(reportId);
  }

  /**
   * @gwt.typeArgs <com.google.gwt.junit.viewer.client.ReportSummary>
   */
  public List/* <ReportSummary> */getReportSummaries() {
    return ReportDatabase.getInstance().getReportSummaries();
  }
}