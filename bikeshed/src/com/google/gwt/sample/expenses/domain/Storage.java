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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pretend pool of domain objects, trying to act more or less like persistence
 * frameworks do. For goodness sake don't imitate this for production code.
 */
class Storage {
  static final Storage INSTANCE;
  static {
    INSTANCE = new Storage();
    fill(INSTANCE);
  }

  public static <E extends Entity> E edit(E v1) {
    return v1.accept(new CreationVisitor<E>(v1));
  }

  /**
   * @param storage to fill with demo entities
   */
  static void fill(Storage storage) {
    Employee e = new Employee();
    e.setUserName("abc");
    e.setDisplayName("Able B. Charlie");
    e.setSupervisor(e);
    storage.persist(e);

    Employee e2 = new Employee();
    e2.setUserName("def");
    e2.setDisplayName("Delta E. Foxtrot");
    e2.setSupervisor(e);
    storage.persist(e2);

    e2 = new Employee();
    e2.setUserName("ghi");
    e2.setDisplayName("George H. Indigo");
    e2.setSupervisor(e);
    storage.persist(e2);
  }

  private final Map<Long, Entity> soup = new HashMap<Long, Entity>();
  private final Map<String, Long> employeeUserNameIndex = new HashMap<String, Long>();

  private long serial = 0;

  synchronized List<Employee> findAllEmployees() {
    List<Employee> rtn = new ArrayList<Employee>();
    for (Map.Entry<String, Long> entry : employeeUserNameIndex.entrySet()) {
      rtn.add((Employee) get(entry.getValue()));
    }
    return rtn;
  }

  synchronized Employee findEmployeeByUserName(String userName) {
    Long id = employeeUserNameIndex.get(userName);
    return (Employee) get(id);
  }

  @SuppressWarnings("unchecked")
  // We make runtime checks that return type matches in type
  synchronized <E extends Entity> E get(final E entity) {
    Entity previous = soup.get(entity.getId());
    if (null == previous) {
      throw new IllegalArgumentException(String.format("In %s, unknown id %d",
          entity, entity.getId()));
    }
    if (!previous.getClass().equals(entity.getClass())) {
      throw new IllegalArgumentException(String.format(
          "Type mismatch, fetched %s for %s", entity, previous));
    }
    return (E) previous;
  }

  synchronized <E extends Entity> E persist(final E delta) {
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

      next = previous.accept(new CreationVisitor<E>(++serial,
          previous.getVersion() + 1));

      NullFieldFiller filler = new NullFieldFiller(next);
      // Copy the changed fields into the new version
      delta.accept(filler);
      // And copy the old fields into any null fields remaining on the new
      // version
      previous.accept(filler);
    }

    updateIndices(previous, next);
    soup.put(next.getId(), next);
    return next;
  }

  private synchronized Entity get(Long id) {
    return soup.get(id);
  }

  private void updateIndices(final Entity previous, final Entity next) {
    next.accept(new EntityVisitor<Void>() {
      public Void visit(Currency currency) {
        return null;
      }

      public Void visit(Employee employee) {
        if (null == employee.getUserName())
          return null;
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
        return null;
      }

      public Void visit(ReportItem reportItem) {
        return null;
      }
    });
  }
}
