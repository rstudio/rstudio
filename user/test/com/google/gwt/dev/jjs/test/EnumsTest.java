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
 * Tests enum functionality.
 */
public class EnumsTest extends GWTTestCase {

  enum Basic {
    A, B, C
  }

  enum Complex {
    A("X"), B("Y"), C("Z");

    String value;

    Complex(String value) {
      this.value = value;
    }

    public String value() {
      return value;
    }
  }

  enum Subclassing {
    A {
      @Override
      public String value() {
        return "X";
      }
    },
    B {
      @Override
      public String value() {
        return "Y";
      }
    },
    C {
      @Override
      public String value() {
        return "Z";
      }
    };

    public abstract String value();
  }

  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testCompareTo() {
    assertTrue(Basic.A.compareTo(Basic.valueOf("A")) == 0);
    assertTrue(Basic.B.compareTo(Basic.A) > 0);
    assertTrue(Basic.A.compareTo(Basic.B) < 0);

    assertTrue(Complex.A.compareTo(Complex.valueOf("A")) == 0);
    assertTrue(Complex.B.compareTo(Complex.A) > 0);
    assertTrue(Complex.A.compareTo(Complex.B) < 0);

    assertTrue(Subclassing.A.compareTo(Subclassing.valueOf("A")) == 0);
    assertTrue(Subclassing.B.compareTo(Subclassing.A) > 0);
    assertTrue(Subclassing.A.compareTo(Subclassing.B) < 0);
  }

  public void testField() {
    assertEquals("X", Complex.A.value);
    assertEquals("Y", Complex.B.value);
    assertEquals("Z", Complex.C.value);
  }

  public void testMethod() {
    assertEquals("X", Complex.A.value());
    assertEquals("Y", Complex.B.value());
    assertEquals("Z", Complex.C.value());

    assertEquals("X", Subclassing.A.value());
    assertEquals("Y", Subclassing.B.value());
    assertEquals("Z", Subclassing.C.value());
  }

  public void testName() {
    assertEquals("A", Basic.A.name());
    assertEquals("B", Basic.B.name());
    assertEquals("C", Basic.C.name());

    assertEquals("A", Complex.A.name());
    assertEquals("B", Complex.B.name());
    assertEquals("C", Complex.C.name());

    assertEquals("A", Subclassing.A.name());
    assertEquals("B", Subclassing.B.name());
    assertEquals("C", Subclassing.C.name());
  }

  public void testOrdinals() {
    assertEquals(0, Basic.A.ordinal());
    assertEquals(1, Basic.B.ordinal());
    assertEquals(2, Basic.C.ordinal());

    assertEquals(0, Complex.A.ordinal());
    assertEquals(1, Complex.B.ordinal());
    assertEquals(2, Complex.C.ordinal());

    assertEquals(0, Subclassing.A.ordinal());
    assertEquals(1, Subclassing.B.ordinal());
    assertEquals(2, Subclassing.C.ordinal());
  }

  public void testSwitch() {
    switch (Basic.A) {
      case A:
        break;
      case B:
        fail("Switch failed");
        break;
      case C:
        fail("Switch failed");
        break;
      default:
        fail("Switch failed");
        break;
    }
    switch (Complex.B) {
      case A:
        fail("Switch failed");
        break;
      case B:
        break;
      case C:
        fail("Switch failed");
        break;
      default:
        fail("Switch failed");
        break;
    }
    switch (Subclassing.C) {
      case A:
        fail("Switch failed");
        break;
      case B:
        fail("Switch failed");
        break;
      default:
        break;
    }
  }

  public void testValueOf() {
    assertEquals(Basic.A, Basic.valueOf("A"));
    assertEquals(Basic.B, Basic.valueOf("B"));
    assertEquals(Basic.C, Basic.valueOf("C"));

    assertEquals(Complex.A, Complex.valueOf("A"));
    assertEquals(Complex.B, Complex.valueOf("B"));
    assertEquals(Complex.C, Complex.valueOf("C"));

    assertEquals(Subclassing.A, Subclassing.valueOf("A"));
    assertEquals(Subclassing.B, Subclassing.valueOf("B"));
    assertEquals(Subclassing.C, Subclassing.valueOf("C"));
  }

  public void testValues() {
    Basic[] simples = Basic.values();
    assertEquals(Basic.A, simples[0]);
    assertEquals(Basic.B, simples[1]);
    assertEquals(Basic.C, simples[2]);

    Complex[] complexes = Complex.values();
    assertEquals(Complex.A, complexes[0]);
    assertEquals(Complex.B, complexes[1]);
    assertEquals(Complex.C, complexes[2]);

    Subclassing[] subs = Subclassing.values();
    assertEquals(Subclassing.A, subs[0]);
    assertEquals(Subclassing.B, subs[1]);
    assertEquals(Subclassing.C, subs[2]);
  }
}
