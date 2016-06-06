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

import com.google.gwt.dev.jjs.test.StaticObject.InstanceObject;
import com.google.gwt.dev.jjs.test.StaticObject.InstanceObject.NestedInstanceObject;
import com.google.gwt.dev.jjs.test.StaticObject.NoArgObject;
import com.google.gwt.dev.jjs.test.StaticObject.NoInitObject;
import com.google.gwt.dev.jjs.test.StaticObject.StaticInnerObject;
import com.google.gwt.dev.jjs.test.StaticObject.StaticObjectException;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests JSNI invocation of Java constructors.
 */
public class JsniConstructorTest extends GWTTestCase {

  private interface A1 {
    void a1();
  }

  private interface A2 extends A1 {
    void a2();
  }

  private interface A3 {
    void a3();
  }

  private static abstract class C1 implements A2 {
    static void s1() {
    }

    public abstract void c1();
  }

  private static class C2 extends C1 implements A3 {
    static void s2() {
    }

    @Override
    public void a1() {
    }

    @Override
    public void a2() {
    }

    @Override
    public void a3() {
    }

    @Override
    public void c1() {
    }

    public void c2() {
    }

    @Override
    public String toString() {
      return "";
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  /**
   * Ensure that exceptions propagate correctly.
   */
  public void testExceptions() {
    try {
      staticArg(false);
      fail("Should have thrown a checked exception");
    } catch (StaticObjectException e) {
      // Expected
    } catch (Throwable t) {
      fail("Expecting a StaticObjectException, got a " + t.getClass().getName());
    }

    try {
      staticArg(true);
      fail("Should have thrown a runtime exception");
    } catch (RuntimeException e) {
      // Expected
    } catch (Throwable t) {
      fail("Expecting a RuntimeException, got a " + t.getClass().getName());
    }
  }

  public native void testInheritedMethodRef() /*-{
    @com.google.gwt.dev.jjs.test.JsniConstructorTest.C1::s1()();
    @com.google.gwt.dev.jjs.test.JsniConstructorTest.C2::s2()();

    var o = @com.google.gwt.dev.jjs.test.JsniConstructorTest.C2::new()();
    o.@com.google.gwt.dev.jjs.test.JsniConstructorTest.A1::a1()();
    o.@com.google.gwt.dev.jjs.test.JsniConstructorTest.A2::a1()();
    o.@com.google.gwt.dev.jjs.test.JsniConstructorTest.C1::a1()();
    o.@com.google.gwt.dev.jjs.test.JsniConstructorTest.C2::a1()();

    o.@com.google.gwt.dev.jjs.test.JsniConstructorTest.A2::a2()();
    o.@com.google.gwt.dev.jjs.test.JsniConstructorTest.C1::a2()();
    o.@com.google.gwt.dev.jjs.test.JsniConstructorTest.C2::a2()();

    o.@com.google.gwt.dev.jjs.test.JsniConstructorTest.A3::a3()();
    o.@com.google.gwt.dev.jjs.test.JsniConstructorTest.C2::a3()();

    o.@com.google.gwt.dev.jjs.test.JsniConstructorTest.C1::c1()();
    o.@com.google.gwt.dev.jjs.test.JsniConstructorTest.C2::c1()();

    o.@com.google.gwt.dev.jjs.test.JsniConstructorTest.C2::c2()();

    o.@java.lang.Object::toString()();
    o.@com.google.gwt.dev.jjs.test.JsniConstructorTest.C1::toString()();
    o.@com.google.gwt.dev.jjs.test.JsniConstructorTest.C2::toString()();
  }-*/;

  public native void testJsniResolution() /*-{
    @JsniConstructorTest.C1::s1()();
    @JsniConstructorTest.C2::s2()();

    var o = @JsniConstructorTest.C2::new()();
    o.@JsniConstructorTest.A1::a1()();
    o.@JsniConstructorTest.A2::a1()();
    o.@JsniConstructorTest.C1::a1()();
    o.@JsniConstructorTest.C2::a1()();

    o.@JsniConstructorTest.A2::a2()();
    o.@JsniConstructorTest.C1::a2()();
    o.@JsniConstructorTest.C2::a2()();

    o.@JsniConstructorTest.A3::a3()();
    o.@JsniConstructorTest.C2::a3()();

    o.@JsniConstructorTest.C1::c1()();
    o.@JsniConstructorTest.C2::c1()();

    o.@JsniConstructorTest.C2::c2()();

    o.@java.lang.Object::toString()();
    o.@JsniConstructorTest.C1::toString()();
    o.@JsniConstructorTest.C2::toString()();

    this.@InstanceObject::foo(*);
    this.@StaticObject.InstanceObject::foo(*);
    this.@StaticObject::foo(*);
  }-*/;

  public void testJsniConstructors() {
    StaticObject o = staticArg(1);
    assertEquals(1, o.foo());

    InstanceObject i = instanceArg(o, 1);
    assertEquals(o.foo() + 1, i.foo());

    NestedInstanceObject n = nestedInstanceArg(i, 1);
    assertEquals(i.foo() + 1, n.foo());

    StaticInnerObject inner = staticInnerArg(3);
    assertEquals(3, inner.foo());

    NoArgObject noArg = noArg();
    assertEquals(4, noArg.foo());

    NoInitObject noInit = noInit();
    assertEquals(5, noInit.foo());

    StaticObject o2 = passAndReturnStatic(6);
    assertEquals(6, o2.foo());

    InstanceObject i2 = passAndReturnInstance(o2, 1);
    assertEquals(o2.foo() + 1, i2.foo());
  }

  public native void testJsniDevirtualizedConstructors() /*-{
    var aDoubleConstructor = @java.lang.Double::new(Ljava/lang/String;);
    @junit.framework.Assert::assertEquals(DDD)(1.5,  aDoubleConstructor("1.5"), 0.1);
    @junit.framework.Assert::assertEquals(DDD)(1.5,
        @java.lang.Double::new(Ljava/lang/String;)("1.5"), 0.1);

    var aStringConstructor = @java.lang.String::new(Ljava/lang/String;);
    @junit.framework.Assert::assertEquals(Ljava/lang/String;Ljava/lang/String;)(
        "Hello", aStringConstructor("Hello"))
    @junit.framework.Assert::assertEquals(Ljava/lang/String;Ljava/lang/String;)(
        "Hello", @java.lang.String::new(Ljava/lang/String;)("Hello"))

  }-*/;

  private native InstanceObject instanceArg(StaticObject obj, int i) /*-{
    return @com.google.gwt.dev.jjs.test.StaticObject.InstanceObject::new(Lcom/google/gwt/dev/jjs/test/StaticObject;I)(obj,i);
  }-*/;

  private native NestedInstanceObject nestedInstanceArg(InstanceObject obj,
      int i) /*-{
    return @com.google.gwt.dev.jjs.test.StaticObject.InstanceObject.NestedInstanceObject::new(Lcom/google/gwt/dev/jjs/test/StaticObject$InstanceObject;I)(obj,i);
  }-*/;

  private native NoArgObject noArg() /*-{
    return @com.google.gwt.dev.jjs.test.StaticObject.NoArgObject::new()();
  }-*/;

  private native NoInitObject noInit() /*-{
    return @com.google.gwt.dev.jjs.test.StaticObject.NoInitObject::new()();
  }-*/;

  private native InstanceObject passAndReturnInstance(StaticObject obj, int i) /*-{
    var f = @com.google.gwt.dev.jjs.test.StaticObject.InstanceObject::new(Lcom/google/gwt/dev/jjs/test/StaticObject;I);
    return f.call(null, obj, i);
  }-*/;

  private native StaticObject passAndReturnStatic(int i) /*-{
    var f = @com.google.gwt.dev.jjs.test.StaticObject::new(I);
    return f(i);
  }-*/;

  /**
   * Calls a constructor that always throws an exception.
   */
  private native StaticObject staticArg(boolean throwRuntime)
      throws StaticObjectException /*-{
    return @com.google.gwt.dev.jjs.test.StaticObject::new(Z)(throwRuntime);
  }-*/;

  private native StaticObject staticArg(int i) /*-{
    return @com.google.gwt.dev.jjs.test.StaticObject::new(I)(i);
  }-*/;

  private native StaticInnerObject staticInnerArg(int i) /*-{
    return @com.google.gwt.dev.jjs.test.StaticObject.StaticInnerObject::new(I)(i);
  }-*/;
}
