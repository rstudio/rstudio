/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.jjs.test;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests the new JDK 1.5 enum functionality.
 */
public class EnumsTest extends GWTTestCase {

  enum SimpleEnum {

    A("X"), B("Y"), C("Z");

    private String value;

    SimpleEnum(String value) {
      this.value = value;
    }

    public String value() {
      return value;
    }
  }

  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testCompareTo() {
    assertTrue(SimpleEnum.A.compareTo(SimpleEnum.valueOf("A")) == 0);
    assertFalse(SimpleEnum.B.compareTo(SimpleEnum.A) == 0);
  }

  public void testMethod() {
    assertEquals(SimpleEnum.A.value(), "X");
    assertEquals(SimpleEnum.B.value(), "Y");
    assertEquals(SimpleEnum.C.value(), "Z");
  }

  public void testName() {
    assertEquals(SimpleEnum.A.name(), "A");
    assertEquals(SimpleEnum.B.name(), "B");
    assertEquals(SimpleEnum.C.name(), "C");
  }

  public void testOrdinals() {
    assertEquals(SimpleEnum.A.ordinal(), 0);
    assertEquals(SimpleEnum.B.ordinal(), 1);
    assertEquals(SimpleEnum.C.ordinal(), 2);
  }

  public void testSwitch() {
    SimpleEnum e = SimpleEnum.B;

    switch (e) {
      case B:
        break;
      default:
        fail("Switch failed");
    }
  }

  public void testValueOf() {
    assertEquals(SimpleEnum.valueOf("A"), SimpleEnum.A);
    assertEquals(SimpleEnum.valueOf("B"), SimpleEnum.B);
    assertEquals(SimpleEnum.valueOf("C"), SimpleEnum.C);
  }

  public void testValues() {
    SimpleEnum[] enums = SimpleEnum.values();

    assertEquals(enums[0], SimpleEnum.A);
    assertEquals(enums[1], SimpleEnum.B);
    assertEquals(enums[2], SimpleEnum.C);
  }
}
