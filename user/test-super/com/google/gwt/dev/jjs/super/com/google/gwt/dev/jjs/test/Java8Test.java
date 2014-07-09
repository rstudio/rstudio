/*
 * Copyright 2014 Google Inc.
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

import com.google.gwt.core.client.GwtScriptOnly;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests Java 8 features. It is super sourced so that gwt can be compiles under Java 6.
 *
 * IMPORTANT: For each test here there must exist the corresponding method in the non super sourced
 * version.
 *
 * Eventually this test will graduate and not be super sourced.
 */
@GwtScriptOnly
public class Java8Test extends GWTTestCase {
  int local = 42;

  static abstract class SameClass {
    public int method1() { return 10; }
    public abstract int method2();
  }

  interface Lambda<T> {
    T run(int a, int b);
  }

  interface Lambda2<String> {
    boolean run(String a, String b);
  }

  class AcceptsLambda<T> {
    public T accept(Lambda<T> foo) {
      return foo.run(10, 20);
    }
    public boolean accept2(Lambda2<String> foo) {
      return foo.run("a", "b");
    }
  }

  class Pojo {
    private final int x;
    private final int y;

    public Pojo(int x, int y) {
      this.x = x;
      this.y = y;
    }

    public int fooInstance(int a, int b) {
      return a + b + x + y;
    }
  }

  class Inner {
    int local = 22;
    public void run() {
      assertEquals(94, new AcceptsLambda<Integer>().accept((a,b) -> Java8Test.this.local +  local + a + b).intValue());
    }
  }

  static class Static {
    static int staticField;
    static {
      staticField = 99;
    }
    static Integer staticMethod(int x, int y) { return x + y + staticField; }
  }

  static class StaticFailIfClinitRuns {
    static {
      fail("clinit() shouldn't run from just taking a reference to a method");
    }

    public static Integer staticMethod(int x, int y) {
      return null;
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.Java8Test";
  }

  public void testLambdaNoCapture() {
    assertEquals(30, new AcceptsLambda<Integer>().accept((a, b) -> a + b).intValue());
  }

  public void testLambdaCaptureLocal() {
    int x = 10;
    assertEquals(40, new AcceptsLambda<Integer>().accept((a,b) -> x + a + b).intValue());
  }

  public void testLambdaCaptureLocalAndField() {
    int x = 10;
    assertEquals(82, new AcceptsLambda<Integer>().accept((a,b) -> x + local + a + b).intValue());
  }

  public void testCompileLambdaCaptureOuterInnerField() throws Exception {
    new Inner().run();
  }

  public void testStaticReferenceBinding() throws Exception {
    assertEquals(129, new AcceptsLambda<Integer>().accept(Static::staticMethod).intValue());
    // if this next line runs a clinit, it fails
    Lambda l = dummyMethodToMakeCheckStyleHappy(StaticFailIfClinitRuns::staticMethod);
    try {
      // but now it should fail
      l.run(1,2);
      fail("Clinit should have run for the first time");
    } catch (AssertionError ae) {
      // success, it was supposed to throw!
    }
  }

  private static Lambda<Integer> dummyMethodToMakeCheckStyleHappy(Lambda<Integer> l) {
    return l;
  }

  public void testInstanceReferenceBinding() throws Exception {
    Pojo instance1 = new Pojo(1, 2);
    Pojo instance2 = new Pojo(3, 4);
    assertEquals(33, new AcceptsLambda<Integer>().accept(instance1::fooInstance).intValue());
    assertEquals(37, new AcceptsLambda<Integer>().accept(instance2::fooInstance).intValue());
  }

  public void testImplicitQualifierReferenceBinding() throws Exception {
    assertFalse(new AcceptsLambda<String>().accept2(String::equalsIgnoreCase));
  }

  public void testConstructorReferenceBinding() {
    assertEquals(30, new AcceptsLambda<Pojo>().accept(Pojo::new).fooInstance(0, 0));
  }

  public void testStaticInterfaceMethod() {
    assertEquals(99, (int) Static.staticMethod(0, 0));
  }

  interface ArrayCtor {
    ArrayElem [][][] copy(int i);
  }

  interface ArrayCtorBoxed {
    ArrayElem [][][] copy(Integer i);
  }

  static class ArrayElem {
  }

  public void testArrayConstructorReference() {
    ArrayCtor ctor = ArrayElem[][][]::new;
    ArrayElem[][][] array = ctor.copy(100);
    assertEquals(100, array.length);
  }

  public void testArrayConstructorReferenceBoxed() {
    ArrayCtorBoxed ctor = ArrayElem[][][]::new;
    ArrayElem[][][] array = ctor.copy(100);
    assertEquals(100, array.length);
  }

  interface ThreeArgs {
    int foo(int x, int y, int z);
  }

  interface ThreeVarArgs {
    int foo(int x, int y, int... z);
  }

  public static int addMany(int x, int y, int... nums) {
    int sum = x + y;
    for (int num : nums) {
      sum += num;
    }
    return sum;
  }

  public void testVarArgsReferenceBinding() {
    ThreeArgs t = Java8Test::addMany;
    assertEquals(6, t.foo(1,2,3));
  }

  public void testVarArgsPassthroughReferenceBinding() {
    ThreeVarArgs t = Java8Test::addMany;
    assertEquals(6, t.foo(1,2,3));
  }

  public void testVarArgsPassthroughReferenceBindingProvidedArray() {
    ThreeVarArgs t = Java8Test::addMany;
    assertEquals(6, t.foo(1,2, new int[] {3}));
  }

  interface I {
    int foo(Integer i);
  }

  public void testSuperReferenceExpression() {
    class Y {
      int foo(Integer i) {
        return 42;
      }
    }

    class X extends Y {
      int foo(Integer i) {
        return 23;
      }

      int goo() {
        I i = super::foo;
        return i.foo(0);
      }
    }

    assertEquals(42, new X().goo());
  }

  static class X2 {
    protected int field;
    void foo() {
      int local;
      class Y extends X2 {
        class Z extends X2 {
          void f() {
            Ctor c = X2::new;
            X2 x = c.makeX(123456);
            assertEquals(123456, x.field);
            c = Y::new;
            x = c.makeX(987654);
            x = new Y(987654);
            assertEquals(987655, x.field);
            c = Z::new;
            x = c.makeX(456789);
            x = new Z(456789);
            assertEquals(456791, x.field);
          }
          private Z(int z) {
            super(z + 2);
          }
          Z() {
          }
        }

        private Y(int y) {
          super(y + 1);
        }

        private Y() {
        }
      }
      new Y().new Z().f();
    }

    private X2(int x) {
      this.field = x;
    }
    X2() {
    }
  }

  public void testSuperReferenceExpressionWithVarArgs() {
    class Base {
      int foo(Object... objects) {
        return 0;
      }
    }

    class X extends Base {
      int foo(Object... objects) {
        throw new AssertionError();
      }

      void goo() {
        I i = super::foo;
        i.foo(10);
      }
    }
    new X().goo();
  }

  interface Ctor {
    X2 makeX(int x);
  }

  public void testPrivateConstructorReference() {
    new X2().foo();
  }
}
