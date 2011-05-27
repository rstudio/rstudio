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

import com.google.appengine.api.datastore.Cursor;
import com.google.gwt.sample.expenses.server.DataGenerationServiceImpl;

import org.datanucleus.store.appengine.query.JPACursorHelper;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheFactory;
import javax.cache.CacheManager;
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
 * Models an expense report.
 */
@Entity
public class Report {

  private static Cache cache;

  private static final Logger log = Logger.getLogger(Report.class.getName());

  /**
   * The total number of reports in the database.
   */
  private static long REPORT_COUNT = 5952180;

  public static long countReports() {
    EntityManager em = entityManager();
    try {
      return ((Number) em.createQuery("select count(o) from Report o").getSingleResult()).longValue();
    } finally {
      em.close();
    }
  }

  public static long countReportsBySearch(Long employeeId, String department,
      String startsWith) {
    EntityManager em = entityManager();
    try {
      Query query = queryReportsBySearch(em, employeeId, department, startsWith,
          null, true);
      if (query == null) {
        /*
         * Demo hack around app engine's 1000 record cap: a request for "all"
         * records comes in as startsWith=="", which returns null. Report how
         * many we prepopulated the thing with. A real app would have to
         * maintain its own count in the datastore and return that.
         */
        return REPORT_COUNT;
      }
      long count = ((Number) query.getSingleResult()).longValue();
      // Demo hack: if there are no records, make some up.
      if (count == 0) {
        new DataGenerationServiceImpl().generate(200);
        count = ((Number) query.getSingleResult()).longValue();
      }
      return count;
    } finally {
      em.close();
    }
  }

  public static final EntityManager entityManager() {
    return EMF.get().createEntityManager();
  }

  @SuppressWarnings("unchecked")
  public static List<Report> findAllReports() {
    EntityManager em = entityManager();
    try {
      List<Report> reportList = em.createQuery("select o from Report o").getResultList();
      // force it to materialize
      reportList.size();
      return reportList;
    } finally {
      em.close();
    }
  }

  public static Report findReport(Long id) {
    if (id == null) {
      return null;
    }
    EntityManager em = entityManager();
    try {
      return em.find(Report.class, id);
    } finally {
      em.close();
    }
  }

  @SuppressWarnings("unchecked")
  public static List<Report> findReportEntries(int firstResult,
      int maxResults) {
    EntityManager em = entityManager();
    try {
      List<Report> reportList = em.createQuery(
          "select o from Report o").setFirstResult(firstResult).setMaxResults(
          maxResults).getResultList();
      // force it to materialize
      reportList.size();
      return reportList;
    } finally {
      em.close();
    }
  }

  @SuppressWarnings("unchecked")
  public static List<Report> findReportEntriesBySearch(Long employeeId,
      String department, String startsWith, String orderBy, int firstResult,
      int maxResults) {
    EntityManager em = entityManager();
    try {
      Query query = queryReportsBySearch(em, employeeId, department, startsWith,
          orderBy, false);

      // Try to get the memcache
      if (cache == null) {
        try {
          CacheFactory cacheFactory = CacheManager.getInstance().getCacheFactory();
          cache = cacheFactory.createCache(Collections.emptyMap());
        } catch (CacheException e) {
          log.warning("Exception retrieving memcache instance: " + e);
        }
      }

      // Try to get a cursor for the current query
      String encodedCursor = null;
      if (cache != null) {
        String key = createKey(employeeId, department, startsWith, orderBy,
            firstResult);
        encodedCursor = (String) cache.get(key);
      }

      if (encodedCursor != null) {
        // Got a cursor, use it
        Cursor cursor = Cursor.fromWebSafeString(encodedCursor);
        query.setHint(JPACursorHelper.CURSOR_HINT, cursor);
        query.setFirstResult(0);
      } else if (firstResult + maxResults < 1000) {
        // Results may be retrieved directly using "OFFSET"
        query.setHint(JPACursorHelper.CURSOR_HINT, null);
        query.setFirstResult(firstResult);
      } else {
        // Skip results
        int pos = (firstResult / 1000) * 1000;
        Cursor trialCursor = null;
        while (pos > 0) {
          String trialKey = createKey(employeeId, department, startsWith,
              orderBy, pos);
          String trialEncodedCursor = (String) cache.get(trialKey);
          if (trialEncodedCursor != null) {
            trialCursor = Cursor.fromWebSafeString(trialEncodedCursor);
            break;
          }
          pos -= 1000;
        }

        // If trialCursor is null, we'll start searching from result 0
        query.setHint(JPACursorHelper.CURSOR_HINT, trialCursor);
        while (firstResult > pos) {
          int min = Math.min(firstResult - pos, 1000);

          // If we need to skip more than 1000 records, ensure the
          // breaks occur at multiples of 1000 in order to increase the
          // chances of reusing cursors from the memcache
          if (pos + min < firstResult) {
            int mod = (pos + min) % 1000;
            min -= mod;
          }

          query.setMaxResults(min);
          List<Report> results = query.getResultList();
          int count = results.size();
          if (count == 0) {
            break;
          }
          pos += count;

          // Save the cursor for later
          Cursor cursor = JPACursorHelper.getCursor(results);
          if (cache != null) {
            String key = createKey(employeeId, department, startsWith, orderBy,
                pos);
            cache.put(key, cursor.toWebSafeString());
          }

          query.setHint(JPACursorHelper.CURSOR_HINT, cursor);
        }
      }

      query.setMaxResults(maxResults);

      List<Report> reportList = query.getResultList();
      // force it to materialize
      reportList.size();

      Cursor cursor = JPACursorHelper.getCursor(reportList);
      if (cache != null) {
        int pos = firstResult + reportList.size();
        String key = createKey(employeeId, department, startsWith, orderBy,
            pos);
        cache.put(key, cursor.toWebSafeString());
      }

      return reportList;
    } finally {
      em.close();
    }
  }

  @SuppressWarnings("unchecked")
  public static List<Report> findReportsByEmployee(Long employeeId) {
    EntityManager em = entityManager();
    try {
      Query query = em.createQuery(
          "select o from Report o where o.reporterKey =:reporterKey");
      query.setParameter("reporterKey", employeeId);
      List<Report> reportList = query.getResultList();
      // force it to materialize
      reportList.size();
      return reportList;
    } finally {
      em.close();
    }
  }

  private static String createKey(Long employeeId, String department,
      String startsWith, String orderBy, int firstResult) {
    return "" + employeeId + "+" + encode(department) + "+" + encode(startsWith)
        + "+" + encode(orderBy) + "+" + firstResult;
  }

  /**
   * Returns a String based on an input String that provides the following
   * guarantees.
   *
   * <ol> <li>The result contains no '+' characters <li>Distinct inputs always
   * produce distinct results </ol>
   *
   * <p> Note that the transformation is not required to be reversible.
   *
   * @param s the input String
   * @return a String suitable for use as part of a a memcache key
   */
  private static String encode(String s) {
    if (s == null) {
      return "";
    }
    s = s.replace("@", "@@");
    s = s.replace("+", "@");
    return s;
  }

  /**
   * Query for reports based on the search parameters. If startsWith is
   * specified, the results will not be ordered.
   *
   * @param em         the {@link EntityManager} to use
   * @param employeeId the employee id
   * @param department the department to search
   * @param startsWith the starting string
   * @param orderBy    the order of the results
   * @param isCount    true to query on the count only
   * @return the query, or null to return full report count.
   */
  private static Query queryReportsBySearch(EntityManager em, Long employeeId,
      String department, String startsWith, String orderBy, boolean isCount) {
    // Determine which parameters to include.
    boolean isFirstStatement = true;
    boolean hasEmployee = employeeId != null && employeeId >= 0;
    boolean hasDepartment = !hasEmployee && department != null
        && department.length() > 0;
    boolean hasStartsWith = startsWith != null && startsWith.length() > 0;

    // If we are counting and we don't have any query parameters, return null
    // to force #countReportsBySearch to return the full Report count.
    if (isCount && !hasEmployee && !hasDepartment && !hasStartsWith) {
      return null;
    }

    // Construct the query string.
    String retValue = isCount ? "count(o)" : "o";
    String queryString = "select " + retValue + " from Report o";
    if (hasEmployee) {
      queryString += isFirstStatement ? " WHERE" : " AND";
      isFirstStatement = false;
      queryString += " o.reporterKey =:reporterKey";
    }
    if (hasDepartment) {
      queryString += isFirstStatement ? " WHERE" : " AND";
      isFirstStatement = false;
      queryString += " o.department =:department";
    }
    if (hasStartsWith) {
      queryString += isFirstStatement ? " WHERE" : " AND";
      isFirstStatement = false;
      queryString += " o.purposeLowerCase >=:startsWith";
      queryString += " AND o.purposeLowerCase <=:startsWithZ";
    }
    if (!hasStartsWith && orderBy != null && orderBy.length() >= 0) {
      queryString += " ORDER BY " + orderBy;
    }

    // Construct the query;
    Query query = em.createQuery(queryString);
    if (hasEmployee) {
      query.setParameter("reporterKey", employeeId);
    }
    if (hasDepartment) {
      query.setParameter("department", department);
    }
    if (hasStartsWith) {
      String startsWithLc = startsWith.toLowerCase();
      query.setParameter("startsWith", startsWithLc);
      query.setParameter("startsWithZ", startsWithLc + "zzzzzz");
    }
    return query;
  }

  @Transient
  @SuppressWarnings("unused")
  private Employee approvedSupervisor;

  @Transient
  @SuppressWarnings("unused")
  private Employee reporter;

  // @JoinColumn

  private Long approvedSupervisorKey;

  private Date created;

  private String department;

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String notes;

  private String purpose;

  /**
   * Store a lower case version of the purpose for searching.
   */
  @SuppressWarnings("unused")
  private String purposeLowerCase;

  /**
   * Store reporter's key instead of reporter. See: http://code.google.com/appengine
   * /docs/java/datastore/relationships.html#Unowned_Relationships
   */
  // @JoinColumn
  private Long reporterKey;

  @Version
  @Column(name = "version")
  private Integer version;

  public Employee getApprovedSupervisor() {
    return approvedSupervisorKey != null ? Employee.findEmployee(
        approvedSupervisorKey) : null;
  }

  public Long getApprovedSupervisorKey() {
    return approvedSupervisorKey;
  }

  public Date getCreated() {
    return this.created;
  }

  public String getDepartment() {
    return this.department;
  }

  public Long getId() {
    return this.id;
  }

  public String getNotes() {
    return this.notes;
  }

  public String getPurpose() {
    return this.purpose;
  }

  public Employee getReporter() {
    return reporterKey != null ? Employee.findEmployee(reporterKey) : null;
  }

  public Long getReporterKey() {
    return this.reporterKey;
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
      Report attached = em.find(Report.class, this.id);
      em.remove(attached);
    } finally {
      em.close();
    }
  }

  public void setApprovedSupervisor(Employee reporter) {
    approvedSupervisorKey = reporter == null ? null : reporter.getId();
  }

  public void setApprovedSupervisorKey(Long approvedSupervisorKey) {
    this.approvedSupervisorKey = approvedSupervisorKey;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public void setDepartment(String department) {
    this.department = department;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public void setPurpose(String purpose) {
    this.purpose = purpose;
    this.purposeLowerCase = purpose == null ? "" : purpose.toLowerCase();
  }

  public void setReporter(Employee reporter) {
    reporterKey = reporter == null ? null : reporter.getId();
  }

  public void setReporterKey(Long reporter) {
    this.reporterKey = reporter;
  }

  public void setSupervisor(Employee supervisor) {
    approvedSupervisorKey = supervisor == null ? null : supervisor.getId();
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Id: ").append(getId()).append(", ");
    sb.append("Version: ").append(getVersion()).append(", ");
    sb.append("Created: ").append(getCreated()).append(", ");
    sb.append("Department: ").append(getDepartment()).append(", ");
    sb.append("Notes: ").append(getNotes()).append(", ");
    sb.append("Purpose: ").append(getPurpose()).append(", ");
    sb.append("Reporter: ").append(getReporterKey()).append(", ");
    sb.append("ApprovedSupervisor: ").append(getApprovedSupervisorKey());
    return sb.toString();
  }

}
