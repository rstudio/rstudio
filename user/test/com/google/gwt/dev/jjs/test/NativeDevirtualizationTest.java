/*
 * Copyright 2015 Google Inc.
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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * This should probably be refactored at some point.
 */
public class NativeDevirtualizationTest extends GWTTestCase {

  public static final String HELLO_WORLD = "Hello, World!";

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testStringDevirtualization() {
    String x = "hi";
    assertEquals("hi", x);
    assertEquals("hi", x.toString());
    x = new String();
    assertEquals("", x);
    x = new String(x);
    assertEquals("", x);
    x = new String("hi");
    assertEquals("hi", x);
    assertEquals('i', x.charAt(1));
    assertEquals("hiyay", x.concat("yay"));
    assertEquals("hihi", x + x);

    assertEquals(
        "blahabctruefalsenullc27",
        ("blah" + String.valueOf(new char[] {'a', 'b', 'c'}) + true
            + false + null + 'c' + 27));
    Object s = HELLO_WORLD;
    assertEquals(String.class, s.getClass());
    assertEquals(HELLO_WORLD, s.toString());

    assertTrue(s.equals(HELLO_WORLD));
    assertTrue(HELLO_WORLD.equals(s));
    assertFalse(s.equals(""));
    assertFalse("".equals(s));
    assertEquals(HELLO_WORLD, s.toString());
    assertTrue(s instanceof String);

    Comparable b = HELLO_WORLD;
    assertEquals(String.class, b.getClass());
    assertEquals(HELLO_WORLD, b.toString());
    assertEquals(HELLO_WORLD.hashCode(), b.hashCode());

    assertTrue(((Comparable) HELLO_WORLD).compareTo(b) == 0);
    assertTrue(b.compareTo(HELLO_WORLD) == 0);
    assertTrue(((Comparable) "A").compareTo(b) < 0);
    assertTrue(b.compareTo("A") > 0);
    assertTrue(((Comparable) "Z").compareTo(b) > 0);
    assertTrue(b.compareTo("Z") < 0);
    assertTrue(b instanceof String);

    CharSequence c = HELLO_WORLD;
    assertEquals(String.class, c.getClass());
    assertEquals(HELLO_WORLD, c.toString());
    assertEquals(HELLO_WORLD.hashCode(), c.hashCode());

    assertEquals('e', c.charAt(1));
    assertEquals(13, c.length());
    assertEquals("ello", c.subSequence(1, 5));
    assertTrue(c instanceof String);
  }

  public void testBooleanDevirtualization() {
    final Boolean FALSE = false;
    Object o = FALSE;
    assertEquals(Boolean.class, o.getClass());
    assertEquals("false", o.toString());

    assertFalse((Boolean) o);
    assertTrue(o instanceof Boolean);
    assertFalse(o instanceof Number);
    assertTrue(o instanceof Comparable);

    Comparable b = FALSE;
    assertEquals(Boolean.class, b.getClass());
    assertEquals("false", b.toString());
    assertEquals(FALSE.hashCode(), b.hashCode());
  }

  public void testDoubleDevirtualization() {
    final Double ONE_POINT_ONE = 1.1d;
    Object o = ONE_POINT_ONE;
    assertEquals(Double.class, o.getClass());
    assertEquals("1.1", o.toString());

    assertEquals(1.1d, o);
    assertTrue(o instanceof Double);
    assertTrue(o instanceof Number);
    assertTrue(o instanceof Comparable);

    Comparable b = ONE_POINT_ONE;
    assertEquals(Double.class, b.getClass());
    assertEquals("1.1", b.toString());
    assertEquals(ONE_POINT_ONE.hashCode(), b.hashCode());
    assertTrue(b.compareTo((Double) 1.2d) < 0);

    Number c = ONE_POINT_ONE;
    assertEquals(Double.class, c.getClass());
    assertEquals("1.1", c.toString());
    assertEquals(ONE_POINT_ONE.hashCode(), c.hashCode());
    assertEquals(1, c.intValue());
  }

  /**
   * Ensures that dispatch to JavaScript native arrays that are NOT Java arrays works properly.
   */
  @DoNotRunWith(Platform.Devel)
  public void testNativeJavaScriptArray() {
    Object jsoArray = JavaScriptObject.createArray(10);
    assertEquals(JavaScriptObject[].class, jsoArray.getClass());
    assertTrue(jsoArray instanceof JavaScriptObject);
    assertTrue(jsoArray instanceof JavaScriptObject[]);

    Object objectArray =  new Object[10];
    assertEquals(Object[].class, objectArray.getClass());
    assertTrue(objectArray instanceof Object[]);

    assertFalse(jsoArray.toString().equals(objectArray.toString()));
  }
}
