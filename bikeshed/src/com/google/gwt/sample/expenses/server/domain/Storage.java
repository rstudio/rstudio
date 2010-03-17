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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pretend pool of domain objects, trying to act more or less like persistence
 * frameworks do. For goodness sake don't imitate this for production code.
 */
public class Storage {
  public static final Storage INSTANCE;
  static {
    INSTANCE = new Storage();
    fill(INSTANCE);
  }

  /**
   * @param storage to fill with demo entities
   */
  static void fill(Storage storage) {
    Employee abc = new Employee();
    abc.setUserName("abc");
    abc.setDisplayName("Able B. Charlie");
    abc = storage.persist(abc);
    abc.setSupervisor(abc);
    abc = storage.persist(abc);

    Employee def = new Employee();
    def.setUserName("def");
    def.setDisplayName("Delta E. Foxtrot");
    def.setSupervisor(abc);
    def = storage.persist(def);

    Employee ghi = new Employee();
    ghi.setUserName("ghi");
    ghi.setDisplayName("George H. Indigo");
    ghi.setSupervisor(abc);
    ghi = storage.persist(ghi);

    Report abc1 = new Report();
    abc1.setReporter(abc);
    abc1.setCreated(new Date());
    abc1.setPurpose("Spending lots of money");
    abc1 = storage.persist(abc1);

    Report abc2 = new Report();
    abc2.setReporter(abc);
    abc2.setCreated(new Date());
    abc2.setPurpose("Team building diamond cutting offsite");
    abc2 = storage.persist(abc2);

    Report abc3 = new Report();
    abc3.setReporter(abc);
    abc3.setCreated(new Date());
    abc3.setPurpose("Visit to Istanbul");
    storage.persist(abc3);

    Report def1 = new Report();
    def1.setReporter(def);
    def1.setCreated(new Date());
    def1.setPurpose("Money laundering");
    def1 = storage.persist(def1);

    Report def2 = new Report();
    def2.setReporter(def);
    def2.setCreated(new Date());
    def2.setPurpose("Donut day");
    storage.persist(def2);

    Report ghi1 = new Report();
    ghi1.setReporter(ghi);
    ghi1.setCreated(new Date());
    ghi1.setPurpose("ISDN modem for telecommuting");
    storage.persist(ghi1);

    Report ghi2 = new Report();
    ghi2.setReporter(ghi);
    ghi2.setCreated(new Date());
    ghi2.setPurpose("Sushi offsite");
    ghi2 = storage.persist(ghi2);

    Report ghi3 = new Report();
    ghi3.setReporter(ghi);
    ghi3.setCreated(new Date());
    ghi3.setPurpose("Baseball card research");
    ghi3 = storage.persist(ghi3);

    Report ghi4 = new Report();
    ghi4.setReporter(ghi);
    ghi4.setCreated(new Date());
    ghi4.setPurpose("Potato chip cooking offsite");
    ghi4 = storage.persist(ghi4);
  }

  /**
   * Useful for making a surgical update to an entity, e.g. in response to a web
   * update.
   * <p>
   * Given an entity, returns an empty copy: all fields are null except id and
   * version. When this copy is later persisted, only non-null fields will be
   * changed.
   */
  static <E extends Entity> E startSparseEdit(E v1) {
    return v1.accept(new CreationVisitor<E>(v1));
  }

  private final Map<Long, Entity> soup = new HashMap<Long, Entity>();
  private final Map<String, Long> employeeUserNameIndex = new HashMap<String, Long>();
  private final Map<Long, Set<Long>> reportsByEmployeeIndex = new HashMap<Long, Set<Long>>();

  private Map<Long, Entity> freshForCurrentGet;
  private int getDepth = 0;
  private long serial = 0;

  public synchronized <E extends Entity> E persist(final E delta) {
    E next = null;
    E previous = null;

    if (delta.getId() == null) {
      next = delta.accept(new CreationVisitor<E>(++serial, 0));
      delta.accept(new NullFieldFiller(next));
    } else {
      previous = get(delta);
      if (!previous.getVersion().equals(delta.getVersion())) {
        throw new IllegalArgumentException(String.format(
            "Version mismatch of %s. Cannot update %d from %d", delta,
            previous.getVersion(), delta.getVersion()));
      }

      next = previous.accept(new CreationVisitor<E>(previous.getId(),
          previous.getVersion() + 1));

      NullFieldFiller filler = new NullFieldFiller(next);
      // Copy the changed fields into the new version
      delta.accept(filler);
      // And copy the old fields into any null fields remaining on the new
      // version
      previous.accept(filler);
    }

    next.accept(new RelationshipValidationVisitor());

    updateIndices(previous, next);
    soup.put(next.getId(), next);
    return get(next);
  }

  synchronized List<Employee> findAllEmployees() {
    List<Employee> rtn = new ArrayList<Employee>();
    for (Map.Entry<String, Long> entry : employeeUserNameIndex.entrySet()) {
      rtn.add(get((Employee) rawGet(entry.getValue())));
    }
    return rtn;
  }
  
  synchronized List<Report> findAllReports() {
    List<Report> rtn = new ArrayList<Report>();
    for (Entity e : soup.values()) {
      if (e instanceof Report) {
        rtn.add(get((Report) e));
      }
    }
    return rtn;
  }

  /**
   * Returns Employee by id.
   * @param id
   * @return
   */
  synchronized Employee findEmployee(Long id) {
    return get((Employee) rawGet(id));
  }

  synchronized Employee findEmployeeByUserName(String userName) {
    Long id = employeeUserNameIndex.get(userName);
    return findEmployee(id);
  }

  /**
   * Returns report by id.
   * @param id
   * @return
   */
  synchronized Report findReport(Long id) {
    return get((Report) rawGet(id));
  }

  synchronized List<Report> findReportsByEmployee(long id) {
    Set<Long> reportIds = reportsByEmployeeIndex.get(id);
    if (reportIds == null) {
      return Collections.emptyList();
    }
    List<Report> reports = new ArrayList<Report>(reportIds.size());
    for (Long reportId : reportIds) {
      reports.add(get((Report) rawGet(reportId)));
    }
    return reports;
  }

  /**
   * @return An up to date copy of the given entity, safe for editing.
   */
  @SuppressWarnings("unchecked")
  // We make runtime checks that return type matches in type
  synchronized <E extends Entity> E get(final E entity) {
    if (getDepth == 0) {
      freshForCurrentGet = new HashMap<Long, Entity>();
    }
    getDepth++;
    try {
      if (entity == null) {
        return null;
      }
      Entity previous = rawGet(entity.getId());
      if (null == previous) {
        throw new IllegalArgumentException(String.format(
            "In %s, unknown id %d", entity, entity.getId()));
      }
      if (!previous.getClass().equals(entity.getClass())) {
        throw new IllegalArgumentException(String.format(
            "Type mismatch, fetched %s for %s", entity, previous));
      }

      Entity rtn = freshForCurrentGet.get(previous.getId());
      if (rtn == null) {
        // Make a defensive copy
        rtn = copy(previous);
        freshForCurrentGet.put(previous.getId(), rtn);
        // Make sure it has fresh copies of related entities
        rtn.accept(new RelationshipRefreshingVisitor(this));
      }
      return (E) rtn;
    } finally {
      getDepth--;
      if (getDepth == 0) {
        freshForCurrentGet = null;
      }
    }
  }

  /**
   * @param original Entity to copy
   * @return copy of original
   */
  private Entity copy(Entity original) {
    Entity copy = original.accept(new CreationVisitor<Entity>(original));
    original.accept(new NullFieldFiller(copy));
    return copy;
  }

  private synchronized Entity rawGet(Long id) {
    return soup.get(id);
  }

  private void updateIndices(final Entity previous, final Entity next) {
    next.accept(new EntityVisitor<Void>() {
      public Void visit(Currency currency) {
        return null;
      }

      public Void visit(Employee employee) {
        if (null == employee.getUserName()) {
          return null;
        }
        if (previous != null) {
          Employee prevEmployee = (Employee) previous;
          if (!prevEmployee.getUserName().equals(next)) {
            employeeUserNameIndex.remove(prevEmployee.getUserName());
          }
        }
        employeeUserNameIndex.put(employee.getUserName(), employee.getId());
        return null;
      }

      public Void visit(Report report) {
        Employee reporter = report.getReporter();
        if (reporter == null) {
          return null;
        }
        Long employeeId = reporter.getId();
        Set<Long> reportIds = reportsByEmployeeIndex.get(employeeId);
        if (reportIds == null) {
          reportIds = new LinkedHashSet<Long>();
          reportsByEmployeeIndex.put(employeeId, reportIds);
        }
        reportIds.add(report.getId());
        return null;
      }

      public Void visit(ReportItem reportItem) {
        return null;
      }
    });
  }
}
