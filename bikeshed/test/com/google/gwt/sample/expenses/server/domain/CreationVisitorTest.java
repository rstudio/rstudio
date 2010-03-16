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

/**
 * Eponymous test class.
 */
public class CreationVisitorTest extends TestCase {
  EntityTester tester = new EntityTester();

  private void doCreationTest(Entity entity) {
    Entity created = entity.accept(new CreationVisitor<Entity>(1, 2));
    assertEquals(entity.getClass(), created.getClass());
    assertEquals(Long.valueOf(1), created.getId());
    assertEquals(Integer.valueOf(2), created.getVersion());
  }

  public void testSimple() {
    tester.run(new EntityVisitor<Boolean>() {

      public Boolean visit(Currency currency) {
        doCreationTest(currency);
        return null;
      }

      public Boolean visit(Employee employee) {
        doCreationTest(employee);
        return null;
      }

      public Boolean visit(Report report) {
        doCreationTest(report);
        return null;
      }

      public Boolean visit(ReportItem reportItem) {
        doCreationTest(reportItem);
        return null;
      }
    });
  }
}
