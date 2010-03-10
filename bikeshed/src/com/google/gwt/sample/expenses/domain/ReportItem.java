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
package com.google.gwt.sample.expenses.domain;

import java.util.Date;

/**
 * Models a line item in an expense report.
 */
// @javax.persistence.Entity
public class ReportItem implements Entity {
  private Long id;

  private Integer version;

//  @javax.validation.constraints.NotNull
  // @javax.persistence.ManyToOne(targetEntity = Report.class)
  // @javax.persistence.JoinColumn
  private Report report;

//  @javax.validation.constraints.NotNull
//  @javax.validation.constraints.Past
  // @javax.persistence.Temporal(javax.persistence.TemporalType.TIMESTAMP)
  private Date incurred;

//  @javax.validation.constraints.Size(min = 3, max = 100)
  private String purpose;

//  @javax.validation.constraints.NotNull
  // @javax.persistence.ManyToOne(targetEntity = Currency.class)
  // @javax.persistence.JoinColumn
  private Currency currency;

//  @javax.validation.constraints.NotNull
//  @javax.validation.constraints.Min(0L)
  private Float amount;

  public ReportItem() {
    id = null;
    version = null;
  }

  ReportItem(Long id, Integer version) {
    this.id = id;
    this.version = version;
  }

  public <T> T accept(EntityVisitor<T> visitor) {
    return visitor.visit(this);
  }

  /**
   * @return the amount
   */
  public Float getAmount() {
    return amount;
  }

  /**
   * @return the currency
   */
  public Currency getCurrency() {
    return currency;
  }

  /**
   * @return the id
   */
  public Long getId() {
    return id;
  }

  /**
   * @return the incurred
   */
  public Date getIncurred() {
    return incurred;
  }

  /**
   * @return the purpose
   */
  public String getPurpose() {
    return purpose;
  }

  /**
   * @return the report
   */
  public Report getReport() {
    return report;
  }

  /**
   * @return the version
   */
  public Integer getVersion() {
    return version;
  }

  /**
   * @param amount the amount to set
   */
  public void setAmount(Float amount) {
    this.amount = amount;
  }

  /**
   * @param currency the currency to set
   */
  public void setCurrency(Currency currency) {
    this.currency = currency;
  }

  /**
   * @param incurred the incurred to set
   */
  public void setIncurred(java.util.Date incurred) {
    this.incurred = incurred;
  }

  /**
   * @param purpose the purpose to set
   */
  public void setPurpose(String purpose) {
    this.purpose = purpose;
  }

  /**
   * @param report the report to set
   */
  public void setReport(Report report) {
    this.report = report;
  }
}
