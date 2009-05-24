/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.jjs.test.singlejso;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests SingleJso semantics in non-trivial type hierarchies.
 */
public class TypeHierarchyTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testCase1() {
    A a = A.create();
    assertEquals("A", a.whoAmI());

    B1 b1 = new B1();
    assertEquals("B1", b1.whoAmI());

    B2 b2 = new B2();
    assertEquals("B2", b2.whoAmI());
  }

  public void testCase2() {
    IA a = A.create();
    assertEquals("A", a.whoAmI());

    IA b1 = new B1();
    assertEquals("B1", b1.whoAmI());

    IA b2 = new B2();
    assertEquals("B2", b2.whoAmI());
  }

  public void testCase3() {
    IA a = A.create();
    assertEquals("A", a.whoAmI());

    IB b1 = new B1();
    assertEquals("B1", b1.whoAmI());

    IB b2 = new B2();
    assertEquals("B2", b2.whoAmI());
  }

}
