/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dev.util;

import junit.framework.TestCase;

/**
 * Tests the {@link JsniRef} class.
 */
public class JsniRefTest extends TestCase {
  public static void testBasics() {
    {
      JsniRef ref = JsniRef.parse("@some.package.SomeClass::someField");
      assertEquals("some.package.SomeClass", ref.className());
      assertEquals("someField", ref.memberName());
      assertEquals("someField", ref.memberSignature());
      assertFalse(ref.isMethod());
      assertTrue(ref.isField());
    }

    {
      JsniRef ref = JsniRef.parse("@some.package.SomeClass::someMeth()");
      assertEquals("some.package.SomeClass", ref.className());
      assertEquals("someMeth", ref.memberName());
      assertEquals("someMeth()", ref.memberSignature());
      assertTrue(ref.isMethod());
      assertFalse(ref.isField());
      assertFalse(ref.matchesAnyOverload());
      assertEquals(0, ref.paramTypes().length);
    }

    {
      // test with every JNI type included
      JsniRef ref = JsniRef.parse("@some.package.SomeClass::someMeth("
          + "[[ZBCDFIJLjava/lang/String;S)");
      assertEquals("some.package.SomeClass", ref.className());
      assertEquals("someMeth", ref.memberName());
      assertEquals("someMeth([[ZBCDFIJLjava/lang/String;S)",
          ref.memberSignature());
      assertTrue(ref.isMethod());
      assertFalse(ref.matchesAnyOverload());
      assertEquals(9, ref.paramTypes().length);
      assertEquals("[[Z", ref.paramTypes()[0]);
      assertEquals("B", ref.paramTypes()[1]);
      assertEquals("C", ref.paramTypes()[2]);
      assertEquals("D", ref.paramTypes()[3]);
      assertEquals("F", ref.paramTypes()[4]);
      assertEquals("I", ref.paramTypes()[5]);
      assertEquals("J", ref.paramTypes()[6]);
      assertEquals("Ljava/lang/String;", ref.paramTypes()[7]);
      assertEquals("S", ref.paramTypes()[8]);
    }

    {
      // Test with a wildcard parameter list
      JsniRef ref = JsniRef.parse("@some.package.SomeClass::someMeth(*)");
      assertEquals("some.package.SomeClass", ref.className());
      assertEquals("someMeth", ref.memberName());
      assertTrue(ref.isMethod());
      assertTrue(ref.matchesAnyOverload());
    }

    {
      // test some badly formatted wildcard strings
      assertNull(JsniRef.parse("@some.package.SomeClass::someMeth(*"));
      assertNull(JsniRef.parse("@some.package.SomeClass::someMeth(I*)"));
    }

    {
      // test with no preceding at sign
      JsniRef ref = JsniRef.parse("some.package.SomeClass::someField");
      assertEquals("some.package.SomeClass", ref.className());
      assertEquals("someField", ref.memberName());
      assertEquals("someField", ref.memberSignature());
      assertFalse(ref.isMethod());
      assertTrue(ref.isField());
    }
  }

  public void testEquals() {
    String[] tests = new String[] {
        "@some.package.SomeClass::someField",
        "@some.package.SomeClass::someMeth()",
        "@some.package.SomeClass::someMeth([[ZBCDFIJLjava/lang/String;S)"};

    for (String test : tests) {
      JsniRef ref1 = JsniRef.parse(test);
      JsniRef ref2 = JsniRef.parse(test);
      assertEquals(ref1, ref2);
    }
  }

  public void testHashCode() {
    String[] tests = new String[] {
        "@some.package.SomeClass::someField",
        "@some.package.SomeClass::someMeth()",
        "@some.package.SomeClass::someMeth([[ZBCDFIJLjava/lang/String;S)"};

    for (String test : tests) {
      JsniRef ref1 = JsniRef.parse(test);
      JsniRef ref2 = JsniRef.parse(test);
      assertEquals(ref1.hashCode(), ref2.hashCode());
    }
  }

  public void testToString() {
    String[] tests = new String[] {
        "@some.package.SomeClass::someField",
        "@some.package.SomeClass::someMeth()",
        "@some.package.SomeClass::someMeth([[ZBCDFIJLjava/lang/String;S)"};

    for (String test : tests) {
      JsniRef ref = JsniRef.parse(test);
      assertEquals(test, ref.toString());
    }
  }
}
