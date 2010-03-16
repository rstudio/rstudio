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
 * Does the merging that a persistence framework would have done for us. If
 * persistence frameworks did this "null field means no change" kind of thing.
 * Which seems unlikely. But it was fun to write. D'oh.
 * <p>
 * Class cast exceptions thrown on merge type mismatch.
 */
final class NullFieldFiller implements EntityVisitor<Void> {
  private final Entity sparseEntity;

  /**
   * @param sparseEntity any null fields on this object will be filled from the
   *          fields of the Entity that accepts this NullFieldFiller
   */
  NullFieldFiller(Entity sparseEntity) {
    this.sparseEntity = sparseEntity;
  }

  public Void visit(Currency currency) {
    Currency sparse = ((Currency) sparseEntity);
    if (null == sparse.getCode()) {
      sparse.setCode(currency.getCode());
    }
    if (null == sparse.getName()) {
      sparse.setName(currency.getName());
    }
    return null;
  }

  public Void visit(Employee employee) {
    Employee sparse = ((Employee) sparseEntity);
    if (null == sparse.getUserName()) {
      sparse.setUserName(employee.getUserName());
    }
    if (null == sparse.getSupervisor()) {
      sparse.setSupervisor(employee.getSupervisor());
    }
    if (null == sparse.getDisplayName()) {
      sparse.setDisplayName(employee.getDisplayName());
    }
    return null;
  }

  public Void visit(Report report) {
    Report sparse = ((Report) sparseEntity);
    if (sparse.getApprovedSupervisor() == null) {
      sparse.setApprovedSupervisor(report.getApprovedSupervisor());
    }
    if (sparse.getCreated() == null) {
      sparse.setCreated(report.getCreated());
    }
    if (sparse.getPurpose() == null) {
      sparse.setPurpose(report.getPurpose());
    }
    if (sparse.getReporter() == null) {
      sparse.setReporter(report.getReporter());
    }
    if (sparse.getStatus() == null) {
      sparse.setStatus(report.getStatus());
    }
    return null;
  }

  public Void visit(ReportItem reportItem) {
    ReportItem sparse = ((ReportItem) sparseEntity);
    if (null == sparse.getAmount()) {
      sparse.setAmount(reportItem.getAmount());
    }
    if (null == sparse.getCurrency()) {
      sparse.setCurrency(reportItem.getCurrency());
    }
    if (null == sparse.getIncurred()) {
      sparse.setIncurred(reportItem.getIncurred());
    }
    if (null == sparse.getPurpose()) {
      sparse.setPurpose(reportItem.getPurpose());
    }
    if (null == sparse.getReport()) {
      sparse.setReport(reportItem.getReport());
    }
    return null;
  }
}