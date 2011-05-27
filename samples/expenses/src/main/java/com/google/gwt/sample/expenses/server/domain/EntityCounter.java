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

import com.google.apphosting.api.DeadlineExceededException;

import java.util.logging.Logger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * Stores the current entity counts.
 */
@Entity
public class EntityCounter {
  
  private static final Logger log = Logger.getLogger(EntityCounter.class.getName());

  private static final int KIND_EMPLOYEE = 0;

  private static final int KIND_EXPENSE = 1;

  private static final String[] KIND_NAMES = {"Employee", "Expense", "Report"};

  private static final int KIND_REPORT = 2;

  private static final Long ONE = Long.valueOf(1L);

  private static final Long ZERO = Long.valueOf(0L);

//  private static final boolean DENSE_IDS = false;

  public static final EntityManager entityManager() {
    return EMF.get().createEntityManager();
  }

  public static long getEmployeeCount() {
    EntityCounter counter = getCounter();
    Long l = counter.getNumEmployees();
    return l == null ? 0 : l.longValue();
  }

  public static long getExpenseCount() {
    EntityCounter counter = getCounter();
    Long l = counter.getNumExpenses();
    return l == null ? 0 : l.longValue();
  }

  public static long getReportCount() {
    EntityCounter counter = getCounter();
    Long l = counter.getNumReports();
    return l == null ? 0 : l.longValue();
  }

  public static void reset() {
    EntityCounter counter = getCounter();
    counter.clear();

    EntityManager em = entityManager();
    try {
      em.merge(counter);
    } finally {
      em.close();
    }
  }

  public static long updateEmployeeCount() {
    return update(KIND_EMPLOYEE);
  }

  public static long updateExpenseCount() {
    return update(KIND_EXPENSE);
  }

  public static long updateReportCount() {
    return update(KIND_REPORT);
  }

  private static void copy(EntityCounter dest, EntityCounter src) {
    dest.setId(src.getId());
    dest.setMaxCheckedEmployeeId(src.getMaxCheckedEmployeeId());
    dest.setMaxCheckedExpenseId(src.getMaxCheckedExpenseId());
    dest.setMaxCheckedReportId(src.getMaxCheckedReportId());
    dest.setNumEmployees(src.getNumEmployees());
    dest.setNumExpenses(src.getNumExpenses());
    dest.setNumReports(src.getNumReports());
  }

  private static EntityCounter getCounter() {
    EntityManager em = entityManager();
    try {
      EntityCounter counter = em.find(EntityCounter.class, 1);
      if (counter == null) {
        counter = new EntityCounter();
        counter.clear();
        em.persist(counter);
      }
      return counter;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      em.close();
    }
  }

  private static long update(int kind) {
    EntityManager em = entityManager();
    
    String kindName = KIND_NAMES[kind];
    log.info("Updating count for " + kindName);
    
    EntityCounter oldCounter = getCounter();
    EntityCounter counter = new EntityCounter();
    copy(counter, oldCounter);
    
    log.info("Starting at getMaxCheckedEmployeeId() = " + counter.getMaxCheckedEmployeeId());
    log.info("Starting at getMaxCheckedExpenseId() = " + counter.getMaxCheckedExpenseId());
    log.info("Starting at getMaxCheckedReportId() = " + counter.getMaxCheckedReportId());
    log.info("Starting at getNumEmployees() = " + counter.getNumEmployees());
    log.info("Starting at getNumExpenses() = " + counter.getNumExpenses());
    log.info("Starting at getNumReports() = " + counter.getNumReports());
    
    long endTime = System.currentTimeMillis() + 20000;
    EntityTransaction transaction = em.getTransaction();
    transaction.begin();

    try {
      while (System.currentTimeMillis() < endTime) {
        Long min;
        switch (kind) {
          case KIND_EMPLOYEE:
            min = counter.getMaxCheckedEmployeeId();
            break;
          case KIND_EXPENSE:
            min = counter.getMaxCheckedExpenseId();
            break;
          case KIND_REPORT:
            min = counter.getMaxCheckedReportId();
            break;
          default:
            throw new RuntimeException("kind = " + kind);
        }
        long mmin = min == null ? 0L : min.longValue();
        long mmax = mmin + 1000;
        mmin = Math.max(1L, mmin);
        
        String query = "select count(o) from " + kindName + " o where id >= "
            + mmin + " and id < " + mmax;
        Number count = (Number) em.createQuery(query).getSingleResult();
        long value = count.longValue();
//        if (value == 0 && DENSE_IDS) {
//          log.info("Got 0 results between " + mmin + " and " + mmax);
//          break;
//        }

        mmin = mmax;
        min = Long.valueOf(mmin);
        switch (kind) {
          case KIND_EMPLOYEE:
            counter.setMaxCheckedEmployeeId(min);
            Long emp = counter.getNumEmployees();
            long totalEmp = (emp == null) ? value : value + emp.longValue();
            counter.setNumEmployees(Long.valueOf(totalEmp));
            break;
          case KIND_EXPENSE:
            counter.setMaxCheckedExpenseId(min);
            Long exp = counter.getNumExpenses();
            long totalExp = (exp == null) ? value : value + exp.longValue();
            counter.setNumExpenses(Long.valueOf(totalExp));
            break;
          case KIND_REPORT:
            counter.setMaxCheckedReportId(min);
            Long rep = counter.getNumReports();
            long totalRep = (rep == null) ? value : value + rep.longValue();
            counter.setNumReports(Long.valueOf(totalRep));
            break;
          default:
            throw new RuntimeException("kind = " + kind);
        }
      }

      em.merge(counter);
      transaction.commit();
      transaction = null;

      log.info("Ending at getMaxCheckedEmployeeId() = " + counter.getMaxCheckedEmployeeId());
      log.info("Ending at getMaxCheckedExpenseId() = " + counter.getMaxCheckedExpenseId());
      log.info("Ending at getMaxCheckedReportId() = " + counter.getMaxCheckedReportId());
      log.info("Ending at getNumEmployees() = " + counter.getNumEmployees());
      log.info("Ending at getNumExpenses() = " + counter.getNumExpenses());
      log.info("Ending at getNumReports() = " + counter.getNumReports());
    } catch (DeadlineExceededException e) {
      if (transaction != null) {
        transaction.commit();
        transaction = null;
        
        log.info("Ending at getMaxCheckedEmployeeId() = " + counter.getMaxCheckedEmployeeId());
        log.info("Ending at getMaxCheckedExpenseId() = " + counter.getMaxCheckedExpenseId());
        log.info("Ending at getMaxCheckedReportId() = " + counter.getMaxCheckedReportId());
        log.info("Ending at getNumEmployees() = " + counter.getNumEmployees());
        log.info("Ending at getNumExpenses() = " + counter.getNumExpenses());
        log.info("Ending at getNumReports() = " + counter.getNumReports());
      }
    } catch (RuntimeException e) {
      log.warning("Got exception " + e.getMessage());
      throw e;
    } finally {
      if (transaction != null) {
        log.warning("Rolling back transaction");
        transaction.rollback();
      }
      transaction = null;
      em.close();
    }

    long total;
    switch (kind) {
      case KIND_EMPLOYEE:
        total = counter.getNumEmployees();
        break;
      case KIND_EXPENSE:
        total = counter.getNumExpenses();
        break;
      case KIND_REPORT:
        total = counter.getNumReports();
        break;
      default:
        throw new RuntimeException("kind = " + kind);
    }
    log.info("Returning total = " + total);
    return total;
  }

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  
  private Long maxCheckedEmployeeId;
  private Long maxCheckedExpenseId;
  private Long maxCheckedReportId;
  private Long numEmployees;
  private Long numExpenses;
  private Long numReports;
  
  public Long getId() {
    return id;
  }

  public Long getMaxCheckedEmployeeId() {
    return maxCheckedEmployeeId;
  }

  public Long getMaxCheckedExpenseId() {
    return maxCheckedExpenseId;
  }

  public Long getMaxCheckedReportId() {
    return maxCheckedReportId;
  }

  public Long getNumEmployees() {
    return numEmployees;
  }

  public Long getNumExpenses() {
    return numExpenses;
  }

  public Long getNumReports() {
    return numReports;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setMaxCheckedEmployeeId(Long maxCheckedEmployeeId) {
    this.maxCheckedEmployeeId = maxCheckedEmployeeId;
  }

  public void setMaxCheckedExpenseId(Long maxCheckedExpenseId) {
    this.maxCheckedExpenseId = maxCheckedExpenseId;
  }

  public void setMaxCheckedReportId(Long maxCheckedReportId) {
    this.maxCheckedReportId = maxCheckedReportId;
  }

  public void setNumEmployees(Long numEmployees) {
    this.numEmployees = numEmployees;
  }

  public void setNumExpenses(Long numExpenses) {
    this.numExpenses = numExpenses;
  }

  public void setNumReports(Long numReports) {
    this.numReports = numReports;
  }

  private void clear() {
    setId(ONE);
    setNumEmployees(ZERO);
    setNumExpenses(ZERO);
    setNumReports(ZERO);
    setMaxCheckedEmployeeId(ZERO);
    setMaxCheckedExpenseId(ZERO);
    setMaxCheckedReportId(ZERO);
  }
}
