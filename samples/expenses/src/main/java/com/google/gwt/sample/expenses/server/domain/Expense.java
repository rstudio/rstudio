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

import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Query;
import javax.persistence.Transient;
import javax.persistence.Version;

/**
 * Models a line item of an expense report.
 */
@Entity
public class Expense {

  public static long countExpenses() {
    EntityManager em = entityManager();
    try {
      return ((Number) em.createQuery("select count(o) from Expense o").getSingleResult()).longValue();
    } finally {
      em.close();
    }
  }

  public static final EntityManager entityManager() {
    return EMF.get().createEntityManager();
  }

  @SuppressWarnings("unchecked")
  public static List<Expense> findAllExpenses() {
    EntityManager em = entityManager();
    try {
      List<Expense> expenseList = em.createQuery("select o from Expense o").getResultList();
      // force it to materialize
      expenseList.size();
      return expenseList;
    } finally {
      em.close();
    }
  }

  public static Expense findExpense(Long id) {
    if (id == null) {
      return null;
    }
    EntityManager em = entityManager();
    try {
      return em.find(Expense.class, id);
    } finally {
      em.close();
    }
  }

  @SuppressWarnings("unchecked")
  public static List<Expense> findExpensesByReport(Long reportId) {
    EntityManager em = entityManager();
    try {
      Query query = em.createQuery("select o from Expense o where o.reportId =:reportId");
      query.setParameter("reportId", reportId);
      List<Expense> expenseList = query.getResultList();
      // force it to materialize
      expenseList.size();
      return expenseList;
    } finally {
      em.close();
    }
  }

  public static List<Expense> findListOfOneExpense(Long id) {
    return Collections.singletonList(findExpense(id));
  }

  private Double amount;

  private String approval;

  private String category;

  private Date created;

  private String description;

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String reasonDenied;

  @Transient
  @SuppressWarnings("unused")
  private Report report;

  // @JoinColumn

  private Long reportId;

  @Version
  @Column(name = "version")
  private Integer version;

  public Double getAmount() {
    return this.amount;
  }

  public String getApproval() {
    return this.approval;
  }

  public String getCategory() {
    return this.category;
  }

  public Date getCreated() {
    return this.created;
  }

  public String getDescription() {
    return description;
  }

  public Long getId() {
    return this.id;
  }

  public String getReasonDenied() {
    return this.reasonDenied;
  }

  public Report getReport() {
    return reportId != null ? Report.findReport(reportId) : null;
  }

  public Long getReportId() {
    return this.reportId;
  }

  public Integer getVersion() {
    return this.version;
  }

  public void persist() {
    EntityManager em = entityManager();
    try {
      em.persist(this);
    } finally {
      em.close();
    }
  }

  public void remove() {
    EntityManager em = entityManager();
    try {
      Expense attached = em.find(Expense.class, this.id);
      em.remove(attached);
    } finally {
      em.close();
    }
  }

  public void setAmount(Double amount) {
    this.amount = amount;
  }

  public void setApproval(String approval) {
    this.approval = approval;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setReasonDenied(String reasonDenied) {
    this.reasonDenied = reasonDenied;
  }

  public void setReport(Report report) {
    reportId = report == null ? null : report.getId();
  }

  public void setReportId(Long reportId) {
    this.reportId = reportId;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Id: ").append(getId()).append(", ");
    sb.append("Version: ").append(getVersion()).append(", ");
    sb.append("ReportId: ").append(getReportId()).append(", ");
    sb.append("Amount: ").append(getAmount()).append(", ");
    sb.append("Approval: ").append(getApproval()).append(", ");
    sb.append("Category: ").append(getCategory()).append(", ");
    sb.append("Created: ").append(getCreated()).append(", ");
    sb.append("Description: ").append(getDescription());
    return sb.toString();
  }
}
