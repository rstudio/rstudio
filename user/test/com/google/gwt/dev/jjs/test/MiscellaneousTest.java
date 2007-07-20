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

import com.google.gwt.core.client.GWT;
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
    public String toString() {
      return "A";
    }
  }

  static class PolyB implements IBar {
    public String toString() {
      return "B";
    }
  }

  private static class Foo extends JavaScriptObject {
    public String toString() {
      return "Foo";
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

  public static native boolean noOptimizeFalse() /*-{
    return false;
  }-*/;

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

  private static native Foo getFoo() /*-{
    return {};
  }-*/;

  private static native JavaScriptObject getJso() /*-{
    return {toString:function(){return 'jso';}}; 
  }-*/;

  private static native void throwNativeException() /*-{
    var a; a.asdf();
  }-*/;

  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testArrayCasts() {
    {
      // thwart optimizer
      Object f1 = noOptimizeFalse() ? (Object) new PolyA()
          : (Object) new IFoo[1];
      assertTrue(GWT.getTypeName(f1).equals(
          "[Lcom.google.gwt.dev.jjs.test.MiscellaneousTest$IFoo;"));
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
      Object a1 = noOptimizeFalse() ? (Object) new PolyA()
          : (Object) new PolyA[1];
      assertEquals("[Lcom.google.gwt.dev.jjs.test.MiscellaneousTest$PolyA;",
          GWT.getTypeName(a1));
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
      Object f2 = noOptimizeFalse() ? (Object) new PolyA()
          : (Object) new IFoo[1][];
      assertEquals("[[Lcom.google.gwt.dev.jjs.test.MiscellaneousTest$IFoo;",
          GWT.getTypeName(f2));
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
      Object a2 = noOptimizeFalse() ? (Object) new PolyA()
          : (Object) new PolyA[1][];
      assertEquals("[[Lcom.google.gwt.dev.jjs.test.MiscellaneousTest$PolyA;",
          GWT.getTypeName(a2));
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
    assertEquals("[I", GWT.getTypeName(c));
    int[][] d = new int[][] { {1, 2}, {3, 4}};
    assertEquals("[[I", GWT.getTypeName(d));
    assertEquals("[I", GWT.getTypeName(d[1]));
    int[][][] e = new int[][][] { { {1, 2}, {3, 4}}, { {5, 6}, {7, 8}}};
    assertEquals("[[[I", GWT.getTypeName(e));
    assertEquals("[[I", GWT.getTypeName(e[1]));
    assertEquals("[I", GWT.getTypeName(e[1][1]));
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

  public void testCasts() {
    Object o = noOptimizeFalse() ? (Object) new PolyA() : (Object) new PolyB();
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

  public void testJso() {
    Foo foo = getFoo();
    assertEquals("Foo", foo.toString());
    JavaScriptObject jso = foo;
    assertEquals("Foo", jso.toString());
    Object y = noOptimizeFalse() ? new Object() : foo;
    assertEquals("Foo", y.toString());
    jso = getJso();
    assertEquals("jso", jso.toString());
  }

  public void testJsoArrayInit() {
    Object[] jsos = {getJso(), ""};
    JavaScriptObject jso = (JavaScriptObject) jsos[0];
  }

  public void testJsoArrayStore() {
    // Verify that a JSO stored into an array was correctly wrapped
    String[] strings = {""};
    JavaScriptObject[] jsos = {getJso()};
    Object[] objArray = noOptimizeFalse() ? (Object[]) strings : jsos;
    JavaScriptObject jso = (JavaScriptObject) objArray[0];

    // Verify that ArrayStoreExceptions are generated in the correct cases
    try {
      JavaScriptObject[] typeTightenedFooArray = new Foo[3];
      typeTightenedFooArray[0] = getJso();
      fail();
    } catch (ArrayStoreException e) {
    }

    try {
      JavaScriptObject[] fooArray = noOptimizeFalse() ? new JavaScriptObject[3]
          : new Foo[3];
      fooArray[0] = getJso();
      fail();
    } catch (ArrayStoreException e) {
    }

    JavaScriptObject[] jsoArray = noOptimizeFalse() ? new Foo[3]
        : new JavaScriptObject[3];
    jsoArray[0] = getJso();
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

  public String toString() {
    return "com.google.gwt.dev.jjs.test.MiscellaneousTest";
  }

}
