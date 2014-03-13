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
package com.google.gwt.dev.jjs.test;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * This should probably be refactored at some point.
 */
public class MiscellaneousTest extends GWTTestCase {

  interface I {
  }

  interface IBar extends I {
  }

  interface IFoo extends I {
  }

  static class PolyA implements IFoo {
    @Override
    public String toString() {
      return "A";
    }
  }

  static class PolyB implements IBar {
    @Override
    public String toString() {
      return "B";
    }
  }

  private static class HasClinit {
    public static int i = 1;

    private static HasClinit sInstance = new HasClinit();

    public static int sfoo() {
      return sInstance.foo();
    }

    private static native void clinitInNative() /*-{
    }-*/;

    private int foo() {
      this.toString();
      return 3;
    }
  }

  private static volatile boolean FALSE = false;

  private static volatile boolean TRUE = true;

  private static void assertAllCanStore(Object[] dest, Object[] src) {
    for (int i = 0; i < src.length; ++i) {
      dest[0] = src[i];
    }
  }

  private static void assertNoneCanStore(Object[] dest, Object[] src) {
    for (int i = 0; i < src.length; ++i) {
      try {
        dest[0] = src[i];
        fail();
      } catch (ArrayStoreException e) {
      }
    }
  }

  private static native void clinitFromNative() /*-{
    @com.google.gwt.dev.jjs.test.MiscellaneousTest$HasClinit::i = 5;
  }-*/;

  private static native void noOp() /*-{
  }-*/;

  private static native void throwNativeException() /*-{
    var a; a.asdf();
  }-*/;

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testArrayCasts() {
    {
      // thwart optimizer
      Object f1 = FALSE ? (Object) new PolyA() : (Object) new IFoo[1];
      if (expectClassMetadata()) {
        assertEquals("[Lcom.google.gwt.dev.jjs.test.MiscellaneousTest$IFoo;",
            f1.getClass().getName());
      }
      assertFalse(f1 instanceof PolyA[][]);
      assertFalse(f1 instanceof IFoo[][]);
      assertFalse(f1 instanceof PolyA[]);
      assertTrue(f1 instanceof IFoo[]);
      assertFalse(f1 instanceof PolyA);
      assertFalse(f1 instanceof IFoo);
      assertTrue(f1 instanceof Object[]);
      assertFalse(f1 instanceof Object[][]);

      assertAllCanStore((Object[]) f1, new Object[] {new PolyA(), new IFoo() {
      }});
      assertNoneCanStore((Object[]) f1, new Object[] {
          new PolyB(), new Object(), new Object[0]});
    }

    {
      // thwart optimizer
      Object a1 = FALSE ? (Object) new PolyA() : (Object) new PolyA[1];
      if (expectClassMetadata()) {
        assertEquals("[Lcom.google.gwt.dev.jjs.test.MiscellaneousTest$PolyA;",
            a1.getClass().getName());
      }
      assertFalse(a1 instanceof PolyA[][]);
      assertFalse(a1 instanceof IFoo[][]);
      assertTrue(a1 instanceof PolyA[]);
      assertTrue(a1 instanceof IFoo[]);
      assertFalse(a1 instanceof PolyA);
      assertFalse(a1 instanceof IFoo);
      assertTrue(a1 instanceof Object[]);
      assertFalse(a1 instanceof Object[][]);

      assertAllCanStore((Object[]) a1, new Object[] {new PolyA()});
      assertNoneCanStore((Object[]) a1, new Object[] {new IFoo() {
      }, new PolyB(), new Object(), new Object[0]});
    }

    {
      // thwart optimizer
      Object f2 = FALSE ? (Object) new PolyA() : (Object) new IFoo[1][];
      if (expectClassMetadata()) {
        assertEquals("[[Lcom.google.gwt.dev.jjs.test.MiscellaneousTest$IFoo;",
            f2.getClass().getName());
      }
      assertFalse(f2 instanceof PolyA[][]);
      assertTrue(f2 instanceof IFoo[][]);
      assertFalse(f2 instanceof PolyA[]);
      assertFalse(f2 instanceof IFoo[]);
      assertFalse(f2 instanceof PolyA);
      assertFalse(f2 instanceof IFoo);
      assertTrue(f2 instanceof Object[]);
      assertTrue(f2 instanceof Object[][]);

      assertAllCanStore((Object[]) f2, new Object[] {new PolyA[0], new IFoo[0]});
      assertNoneCanStore((Object[]) f2, new Object[] {new IFoo() {
      }, new PolyB(), new Object(), new Object[0]});
    }

    {
      // thwart optimizer
      Object a2 = FALSE ? (Object) new PolyA() : (Object) new PolyA[1][];
      if (expectClassMetadata()) {
        assertEquals("[[Lcom.google.gwt.dev.jjs.test.MiscellaneousTest$PolyA;",
            a2.getClass().getName());
      }
      assertTrue(a2 instanceof PolyA[][]);
      assertTrue(a2 instanceof IFoo[][]);
      assertFalse(a2 instanceof PolyA[]);
      assertFalse(a2 instanceof IFoo[]);
      assertFalse(a2 instanceof PolyA);
      assertFalse(a2 instanceof IFoo);
      assertTrue(a2 instanceof Object[]);
      assertTrue(a2 instanceof Object[][]);

      assertAllCanStore((Object[]) a2, new Object[] {new PolyA[0]});
      assertNoneCanStore((Object[]) a2, new Object[] {new IFoo() {
      }, new PolyB(), new Object(), new Object[0], new IFoo[0]});
    }
  }

  public void testArrays() {
    int[] c = new int[] {1, 2};
    int[][] d = new int[][] { {1, 2}, {3, 4}};
    int[][][] e = new int[][][] { { {1, 2}, {3, 4}}, { {5, 6}, {7, 8}}};
    if (expectClassMetadata()) {
      assertEquals("[I", c.getClass().getName());
      assertEquals("[[I", d.getClass().getName());
      assertEquals("[I", d[1].getClass().getName());
      assertEquals("[[[I", e.getClass().getName());
      assertEquals("[[I", e[1].getClass().getName());
      assertEquals("[I", e[1][1].getClass().getName());
    }
    assertEquals(2, c[1]);
    assertEquals(3, d[1][0]);
    assertEquals(8, e[1][1][1]);

    int[][][] b = new int[3][2][1];
    b[2][1][0] = 1;
    b = new int[3][2][];
    b[2][1] = null;
    b = new int[3][][];
    b[2] = null;
  }

  public void testAssociativityCond() {
    int result = (TRUE ? TRUE : FALSE) ? 100 : 200;
    assertEquals(100, result);
  }

  @SuppressWarnings("cast")
  public void testCasts() {
    Object o = FALSE ? (Object) new PolyA() : (Object) new PolyB();
    assertTrue(o instanceof I);
    assertFalse(o instanceof IFoo);
    assertTrue(o instanceof IBar);
    assertFalse(o instanceof PolyA);
    assertTrue(o instanceof PolyB);
    try {
      o = (PolyA) o;
      fail();
    } catch (ClassCastException e) {
    }
  }

  public void testClinit() {
    ++HasClinit.i;
    HasClinit x = new HasClinit();
    ++x.i;
    new HasClinit().i++;
    HasClinit.i /= HasClinit.i;
    HasClinit.sfoo();
    HasClinit.i /= HasClinit.sfoo();
    HasClinit.clinitInNative();
    clinitFromNative();
  }

  public void testExceptions() {
    int i;
    for (i = 0; i < 5; ++i) {
      boolean hitOuter = false;
      boolean hitInner = false;
      try {
        try {
          switch (i) {
            case 0:
              throw new RuntimeException();
            case 1:
              throw new IndexOutOfBoundsException();
            case 2:
              throw new Exception();
            case 3:
              throw new StringIndexOutOfBoundsException();
            case 4:
              throwNativeException();
          }
        } catch (StringIndexOutOfBoundsException e) {
          assertEquals(3, i);
        } finally {
          hitInner = true;
        }
      } catch (IndexOutOfBoundsException f) {
        assertEquals(1, i);
      } catch (JavaScriptException js) {
        assertEquals(4, i);
      } catch (RuntimeException g) {
        assertEquals(0, i);
      } catch (Throwable e) {
        assertEquals(2, i);
      } finally {
        assertTrue(hitInner);
        hitOuter = true;
      }
      assertTrue(hitOuter);
    }
    assertEquals(5, i);
  }

  public void testIssue2479() {
    if (TRUE) {
      FALSE = false;
    } else if (FALSE) {
      TRUE = true;
    } else {
      noOp();
    }
  }

  public void testString() {
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
        "blahcom.google.gwt.dev.jjs.test.MiscellaneousTestabctruefalsenullc27",
        ("blah" + this + String.valueOf(new char[] {'a', 'b', 'c'}) + true
            + false + null + 'c' + 27));
  }

  /**
   * Ensures that polymorphic dispatch to String works correctly.
   */
  @SuppressWarnings("unchecked")
  public void testStringDynamicMethods() {
    Object s = FALSE ? new Object() : "Hello, World!";
    assertEquals(String.class, s.getClass());
    assertEquals("Hello, World!".hashCode(), s.hashCode());
    assertTrue(s.equals("Hello, World!"));
    assertTrue("Hello, World!".equals(s));
    assertFalse(s.equals(""));
    assertFalse("".equals(s));
    assertEquals("Hello, World!", s.toString());
    assertTrue(s instanceof String);

    Comparable b = FALSE ? new Integer(1) : "Hello, World!";
    assertTrue(((Comparable) "Hello, World!").compareTo(b) == 0);
    assertTrue(b.compareTo("Hello, World!") == 0);
    assertTrue(((Comparable) "A").compareTo(b) < 0);
    assertTrue(b.compareTo("A") > 0);
    assertTrue(((Comparable) "Z").compareTo(b) > 0);
    assertTrue(b.compareTo("Z") < 0);
    assertTrue(b instanceof String);

    CharSequence c = FALSE ? new StringBuffer() : "Hello, World!";
    assertEquals('e', c.charAt(1));
    assertEquals(13, c.length());
    assertEquals("ello", c.subSequence(1, 5));
    assertTrue(c instanceof String);
  }

  /**
   * Ensures that dispatch to JavaScript native arrays that are NOT Java arrays works properly.
   */
  public void testNativeJavaScriptArray() {
    Object jsoArray = FALSE ? new Object() : JavaScriptObject.createArray();
    assertEquals(JavaScriptObject.class, jsoArray.getClass());
    assertFalse(jsoArray instanceof Object[]);

    Object objectArray = FALSE ? new Object() : new Object[10];
    assertEquals(Object[].class, objectArray.getClass());
    assertTrue(objectArray instanceof Object[]);

    assertFalse(jsoArray.toString().equals(objectArray.toString()));
  }

  @Override
  public String toString() {
    return "com.google.gwt.dev.jjs.test.MiscellaneousTest";
  }

  private boolean expectClassMetadata() {
    String name = Object.class.getName();

    if (name.equals("java.lang.Object")) {
      return true;
    } else if (name.startsWith("Class$")) {
      return false;
    }

    throw new RuntimeException("Unexpected class name " + name);
  }
}
