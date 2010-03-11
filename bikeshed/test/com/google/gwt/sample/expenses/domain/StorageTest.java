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

import junit.framework.TestCase;

import java.util.List;

/**
 * Eponymous unit test.
 */
public class StorageTest extends TestCase {
  Storage store = new Storage();

  public void testFreshRelationships() {
    Storage s = new Storage();
    Storage.fill(s);
  
    Employee abc = s.findEmployeeByUserName("abc");
    List<Report> reports = s.findReportsByEmployee(abc.getId());
    for (Report report : reports) {
      assertEquals(abc.getVersion(), report.getReporter().getVersion());
    }
    
    abc.setDisplayName("Herbert");
    s.persist(abc);
    List<Report> fresherReports = s.findReportsByEmployee(abc.getId());
    assertEquals(reports.size(), fresherReports.size());
    Integer expectedVersion = abc.getVersion() + 1;
    for (Report report : fresherReports) {
      assertEquals(abc.getId(), report.getReporter().getId());
      assertEquals(expectedVersion, report.getReporter().getVersion());
      assertEquals("Herbert", report.getReporter().getDisplayName());
    }
  }

  public void testReportsByEmployeeIndex() {
    Storage s = new Storage();
    Storage.fill(s);

    Employee abc = s.findEmployeeByUserName("abc");
    List<Report> reports = s.findReportsByEmployee(abc.getId());
    assertEquals(3, reports.size());
    
    Report report = new Report();
    report.setReporter(abc);
    report = s.persist(report);
    
    reports = s.findReportsByEmployee(abc.getId());
    assertEquals(4, reports.size());
    Report latestReport = reports.get(3);
    assertEquals(report.getId(), latestReport.getId());
    assertEquals(report.getVersion(), latestReport.getVersion());
  }
  
  public void testUserNameIndex() {
    Storage s = new Storage();
    Storage.fill(s);

    Employee abc = s.findEmployeeByUserName("abc");
    assertEquals("Able B. Charlie", abc.getDisplayName());
    abc = Storage.startSparseEdit(abc);
    abc.setUserName("xyz");
    abc = s.persist(abc);

    assertNull(s.findEmployeeByUserName("abc"));
    Employee xyz = s.findEmployeeByUserName("xyz");
    assertEquals("Able B. Charlie", xyz.getDisplayName());
    assertEquals(abc.getVersion(), xyz.getVersion());
  }

  public void testVersioning() {
    final EntityTester tester = new EntityTester();

    tester.run(new EntityVisitor<Boolean>() {

      public Boolean visit(Currency currency) {
        doTestSparseEdit(doTestNew(currency));
        return null;
      }

      public Boolean visit(Employee employee) {
        doTestSparseEdit(doTestNew(employee));
        return null;
      }

      public Boolean visit(Report report) {
        doTestSparseEdit(doTestNew(report));
        return null;
      }

      public Boolean visit(ReportItem reportItem) {
        doTestFullEdit(doTestSparseEdit(doTestNew(reportItem)));
        return null;
      }
    });
  }

  private void doTestFullEdit(Entity v1) {
    v1 = store.get(v1);
    Entity v2 = store.persist(v1);
    assertEquals(v1.getId(), v2.getId());
    assertEquals(Integer.valueOf(v1.getVersion() + 1), v2.getVersion());
    Entity anotherV2 = store.get(v2);
    assertNotSame(v2, anotherV2);
    assertEquals(v1.getId(), anotherV2.getId());
    assertEquals(v2.getVersion(), anotherV2.getVersion());
  }
  
  private Entity doTestNew(Entity e) {
    Entity v1 = store.persist(e);
    assertEquals(Integer.valueOf(0), v1.getVersion());
    assertNotNull(v1.getId());
    assertNotSame(v1, store.get(Storage.startSparseEdit(v1)));
    return v1;
  }

  private Entity doTestSparseEdit(Entity v1) {
    Entity delta = Storage.startSparseEdit(v1);
    Entity v2 = store.persist(delta);
    assertEquals(v1.getId(), v2.getId());
    assertEquals(Integer.valueOf(v1.getVersion() + 1), v2.getVersion());
    Entity anotherV2 = store.get(v2);
    assertNotSame(v2, anotherV2);
    assertEquals(v1.getId(), anotherV2.getId());
    assertEquals(v2.getVersion(), anotherV2.getVersion());
    return anotherV2;
  }
}
