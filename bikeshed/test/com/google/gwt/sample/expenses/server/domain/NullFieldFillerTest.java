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

import junit.framework.TestCase;

import java.util.Date;

/**
 * Eponymous test class.
 */
public class NullFieldFillerTest extends TestCase {
  public void testEmpty() {
    final EntityTester tester = new EntityTester();
    tester.run(new EntityVisitor<Void>() {

      public Void visit(Currency currency) {
        Currency full = new Currency(1L, 2);
        full.setCode("USD");
        full.setName("U.S. Dollars");
        
        doFillAndVerify(currency, full);

        assertEquals("USD", currency.getCode());
        assertEquals("U.S. Dollars", currency.getName());
        return null;
      }

      public Void visit(Employee employee) {
        Employee full = new Employee(1L, 2);
        full.setUserName("ldap");
        full.setDisplayName("Lawrence D. A. Pimrose");
        
        doFillAndVerify(employee, full);

        assertEquals("ldap", employee.getUserName());
        assertEquals("Lawrence D. A. Pimrose", employee.getDisplayName());
        return null;
      }

      public Void visit(Report report) {
        Report full = new Report(1L, 2);
        full.setApprovedSupervisor(tester.employee);
        full.setPurpose("purpose");
        full.setReporter(tester.employee);
        full.setStatus(Status.Paid);
        full.setCreated(new Date(1234567890));
        
        doFillAndVerify(report, full);
        
        assertSame(tester.employee, report.getApprovedSupervisor());
        assertEquals("purpose", report.getPurpose());
        assertSame(tester.employee, report.getReporter());
        assertEquals(Status.Paid, report.getStatus());
        assertEquals(new Date(1234567890), report.getCreated());

        return null;
      }

      public Void visit(ReportItem reportItem) {
        ReportItem full = new ReportItem(1L, 2);
        full.setAmount(123.45f);
        full.setCurrency(tester.currency);
        full.setIncurred(new Date(1234567890));
        full.setPurpose("purpose");
        full.setReport(tester.report);
        
        doFillAndVerify(reportItem, full);
        
        assertEquals(123.45f, reportItem.getAmount());
        assertEquals(tester.currency, reportItem.getCurrency());
        assertEquals(new Date(1234567890), reportItem.getIncurred());
        assertEquals("purpose", reportItem.getPurpose());
        assertEquals(tester.report, reportItem.getReport());

        return null;
      }
    });
  }
  
  private void assertNullEntityTag(Entity employee) {
    assertNull(employee.getId());
    assertNull(employee.getVersion());
  }

  private void doFillAndVerify(Entity sparse, Entity full) {
    full.accept(new NullFieldFiller(sparse));
    assertNullEntityTag(sparse);
  }
}
