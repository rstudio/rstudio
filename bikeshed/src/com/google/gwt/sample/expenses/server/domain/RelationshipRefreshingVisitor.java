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
package com.google.gwt.sample.expenses.server.domain;


/**
 * Used by {@link Storage#get(Entity)} to refreshes fields that point to other
 * entities.
 */
class RelationshipRefreshingVisitor implements EntityVisitor<Void> {
  private final Storage s;

  public RelationshipRefreshingVisitor(Storage s) {
    this.s = s;
  }

  public Void visit(Currency currency) {
    return null;
  }

  public Void visit(Employee employee) {
    employee.setSupervisor(s.get(employee.getSupervisor()));
    return null;
  }

  public Void visit(Report report) {
    report.setApprovedSupervisor(s.get(report.getApprovedSupervisor()));
    report.setReporter(s.get(report.getReporter()));
    return null;
  }

  public Void visit(ReportItem reportItem) {
    reportItem.setReport(s.get(reportItem.getReport()));
    return null;
  }
}
