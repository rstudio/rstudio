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

/**
 * Eponymous unit test.
 */
public class StorageTest extends TestCase {
  Storage store = new Storage();
  
  public void testUserNameIndex() {
    Storage s = new Storage();
    Storage.fill(s);
    
    Employee abc = s.findEmployeeByUserName("abc");
    assertEquals("Able B. Charlie", abc.getDisplayName());
    abc = Storage.edit(abc);
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
        doTestEdit(doTestNew(currency));
        return null;
      }

      public Boolean visit(Employee employee) {
        doTestEdit(doTestNew(employee));
        return null;
      }

      public Boolean visit(Report report) {
        doTestEdit(doTestNew(report));
        return null;
      }

      public Boolean visit(ReportItem reportItem) {
        doTestEdit(doTestNew(reportItem));
        return null;
      }
    });
  }

  private void doTestEdit(Entity v1) {
    Entity delta = Storage.edit(v1);
    Entity v2 = store.persist(delta);
    assertEquals(Integer.valueOf(v1.getVersion() + 1), v2.getVersion());
    assertSame(v2, store.get(Storage.edit(v2)));
  }

  private Entity doTestNew(Entity e) {
    Entity v1 = store.persist(e);
    assertEquals(Integer.valueOf(0), v1.getVersion());
    assertNotNull(v1.getId());
    assertSame(v1, store.get(Storage.edit(v1)));
    return v1;
  }
}
