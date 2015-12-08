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
package com.google.gwt.core.interop;

import static jsinterop.annotations.JsPackage.GLOBAL;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.junit.client.GWTTestCase;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Tests JsProperty functionality.
 */
public class JsPropertyTest extends GWTTestCase {

  private static final int SET_PARENT_X = 500;
  private static final int GET_PARENT_X = 1000;
  private static final int GET_X = 100;
  private static final int SET_X = 50;

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Interop";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    ScriptInjector.fromString(
        "function JsPropertyTest_MyNativeJsType(x) { this.x = x; this.ctorExecuted = true; }\n"
        + "JsPropertyTest_MyNativeJsType.staticX = 33;"
        + "JsPropertyTest_MyNativeJsType.answerToLife = function() { return 42;};"
        + "JsPropertyTest_MyNativeJsType.prototype.sum = "
        + "    function sum(bias) { return this.x + bias; };"
        + "function JsPropertyTest_MyNativeJsTypeInterface() {}\n"
        + "JsPropertyTest_MyNativeJsTypeInterface.prototype.sum = "
        + "    function sum(bias) { return this.x + bias; };")
        .setWindow(ScriptInjector.TOP_WINDOW).inject();
  }

  @JsType
  interface MyJsTypeInterfaceWithProperty {
    @JsProperty
    int getX();

    @JsProperty
    void setX(int x);
  }

  static class MyJavaTypeImplementingMyJsTypeInterfaceWithProperty
      implements MyJsTypeInterfaceWithProperty {
    private int x;

    public int getX() {
      return x + GET_X;
    }

    public void setX(int x) {
      this.x = x + SET_X;
    }
  }

  public void testJavaClassImplementingMyJsTypeInterfaceWithProperty() {
    MyJavaTypeImplementingMyJsTypeInterfaceWithProperty obj =
        new MyJavaTypeImplementingMyJsTypeInterfaceWithProperty();
    assertEquals(0 + GET_X, getProperty(obj, "x"));
    assertEquals(0 + GET_X, obj.getX());
    assertEquals(0, obj.x);

    setProperty(obj, "x", 10);
    assertEquals(10 + GET_X + SET_X, getProperty(obj, "x"));
    assertEquals(10 + GET_X + SET_X, obj.getX());
    assertEquals(10 + SET_X, obj.x);

    obj.setX(12);
    assertEquals(12 + GET_X + SET_X, getProperty(obj, "x"));
    assertEquals(12 + GET_X + SET_X, obj.getX());
    assertEquals(12 + SET_X, obj.x);

    MyJsTypeInterfaceWithProperty intf = new MyJavaTypeImplementingMyJsTypeInterfaceWithProperty();
    assertEquals(0 + GET_X, getProperty(intf, "x"));
    assertEquals(0 + GET_X, intf.getX());
    assertEquals(0, ((MyJavaTypeImplementingMyJsTypeInterfaceWithProperty) intf).x);

    setProperty(intf, "x", 10);
    assertEquals(10 + GET_X + SET_X, getProperty(intf, "x"));
    assertEquals(10 + GET_X + SET_X, intf.getX());
    assertEquals(10 + SET_X, ((MyJavaTypeImplementingMyJsTypeInterfaceWithProperty) intf).x);

    intf.setX(12);
    assertEquals(12 + GET_X + SET_X, getProperty(intf, "x"));
    assertEquals(12 + GET_X + SET_X, intf.getX());
    assertEquals(12 + SET_X, ((MyJavaTypeImplementingMyJsTypeInterfaceWithProperty) intf).x);
  }

  @JsType
  static class MyConcreteJsType {
    private int x;

    @JsProperty
    public int getY() {
      return x + GET_X;
    }

    @JsProperty
    public void setY(int x) {
      this.x = x + SET_X;
    }
  }

  public void testConcreteJsType() {
    MyConcreteJsType obj = new MyConcreteJsType();
    assertEquals(0 + GET_X, getProperty(obj, "y"));
    assertEquals(0 + GET_X,obj.getY());
    assertEquals(0, obj.x);

    setProperty(obj, "y", 10);
    assertEquals(10 + GET_X + SET_X, getProperty(obj, "y"));
    assertEquals(10 + GET_X + SET_X, obj.getY());
    assertEquals(10 + SET_X, obj.x);

    obj.setY(12);
    assertEquals(12 + GET_X + SET_X, getProperty(obj, "y"));
    assertEquals(12 + GET_X + SET_X, obj.getY());
    assertEquals(12 + SET_X, obj.x);
  }

  @JsType(isNative = true, namespace = GLOBAL, name = "JsPropertyTest_MyNativeJsType")
  static class MyNativeJsType {

    public static int staticX;

    public static native int answerToLife();

    public boolean ctorExecuted;

    public int x;

    @JsProperty
    public native int getY();

    @JsProperty
    public native void setY(int x);

    public native int sum(int bias);
  }

  public void testNativeJsType() {
    assertEquals(33, MyNativeJsType.staticX);
    MyNativeJsType.staticX = 34;
    assertEquals(34, MyNativeJsType.staticX);
    assertEquals(42, MyNativeJsType.answerToLife());

    MyNativeJsType obj = new MyNativeJsType();
    assertTrue(obj.ctorExecuted);
    assertTrue(isUndefined(obj.x));
    obj.x = 72;
    assertEquals(72, obj.x);
    assertEquals(74, obj.sum(2));

    assertTrue(isUndefined(obj.getY()));
    obj.setY(91);
    assertEquals(91, obj.getY());
  }

  static class MyNativeJsTypeSubclass extends MyNativeJsType {

    MyNativeJsTypeSubclass() {
      this.x = 42;
      setY(52);
    }

    @Override
    public int sum(int bias) {
      return super.sum(bias) + GET_X;
    }
  }

  public void testNativeJsTypeSubclass() {
    MyNativeJsTypeSubclass mc = new MyNativeJsTypeSubclass();
    assertTrue(mc.ctorExecuted);
    assertEquals(143, mc.sum(1));

    mc.x = -mc.x;
    assertEquals(58, mc.sum(0));

    assertEquals(52, mc.getY());
  }

  static class MyNativeJsTypeSubclassNoOverride extends MyNativeJsType { }

  // TODO(rluble): enable when the subclass is setup correctly.
  public void _disabled_testNativeJsTypeSubclassNoOverride() {
    MyNativeJsTypeSubclassNoOverride myNativeJsType = new MyNativeJsTypeSubclassNoOverride();
    myNativeJsType.x = 12;
    assertEquals(42, myNativeJsType.sum(30));
  }

  @JsType(isNative = true, namespace = GLOBAL, name = "JsPropertyTest_MyNativeJsType")
  static class MyNativeJsTypeWithConstructor {
    public MyNativeJsTypeWithConstructor(int x) { }
    public boolean ctorExecuted;
    public int x;
  }

  public void testNativeJsTypeWithConstructor() {
    MyNativeJsTypeWithConstructor obj = new MyNativeJsTypeWithConstructor(12);
    assertTrue(obj.ctorExecuted);
    assertEquals(12, obj.x);
  }

  static class MyNativeJsTypeWithConstructorSubclass extends MyNativeJsTypeWithConstructor {
    public MyNativeJsTypeWithConstructorSubclass(int x) {
      super(x);
    }
  }

  public void testNativeJsTypeWithConstructorSubclass() {
    MyNativeJsTypeWithConstructorSubclass obj = new MyNativeJsTypeWithConstructorSubclass(12);
    assertTrue(obj.ctorExecuted);
    assertEquals(12, obj.x);
  }

  @JsType(isNative = true, namespace = GLOBAL, name = "JsPropertyTest_MyNativeJsTypeInterface")
  interface MyNativeJsTypeInterface {
    @JsProperty
    int getX();

    @JsProperty
    void setX(int x);
  }

  static class MyNativeJsTypeInterfaceImplementorNeedingBridge
      extends AccidentalImplementer implements MyNativeJsTypeInterface {
  }

  static abstract class AccidentalImplementer {
    private int x;

    public int getX() {
      return x + GET_X;
    }

    public void setX(int x) {
      this.x = x + SET_X;
    }

    public int sum(int bias) {
      return bias + x;
    }
  }

  public void testJsPropertyBridges() {
    MyNativeJsTypeInterface object = new MyNativeJsTypeInterfaceImplementorNeedingBridge();

    object.setX(3);
    assertEquals(3 + 150, object.getX());
    assertEquals(3 + SET_X, ((AccidentalImplementer) object).x);

    AccidentalImplementer accidentalImplementer = (AccidentalImplementer) object;

    accidentalImplementer.setX(3);
    assertEquals(3 + 150, accidentalImplementer.getX());
    assertEquals(3 + 150, getProperty(object, "x"));
    assertEquals(3 + SET_X, accidentalImplementer.x);

    setProperty(object, "x", 4);
    assertEquals(4 + 150, accidentalImplementer.getX());
    assertEquals(4 + 150, getProperty(object, "x"));
    assertEquals(4 + SET_X, accidentalImplementer.x);

    assertEquals(3 + 4 + SET_X, accidentalImplementer.sum(3));
  }

  static class MyNativeJsTypeInterfaceImplNeedingBridgeSubclassed
      extends OtherAccidentalImplementer implements MyNativeJsTypeInterface {
  }

  static abstract class OtherAccidentalImplementer {
    private int x;

    public int getX() {
      return x + GET_PARENT_X;
    }

    public void setX(int x) {
      this.x = x + SET_PARENT_X;
    }

    public int sum(int bias) {
      return bias + x;
    }
  }

  static class MyNativeJsTypeInterfaceImplNeedingBridgeSubclass
      extends MyNativeJsTypeInterfaceImplNeedingBridgeSubclassed {
    private int y;

    public int getX() {
      return y + GET_X;
    }

    public void setX(int y) {
      this.y = y + SET_X;
    }

    public void setParentX(int value) {
      super.setX(value);
    }

    public int getXPlusY() {
      return super.getX() + y;
    }
  }

  public void testJsPropertyBridgesSubclass() {
    MyNativeJsTypeInterface object = new MyNativeJsTypeInterfaceImplNeedingBridgeSubclass();

    object.setX(3);
    assertEquals(3 + 150, object.getX());

    OtherAccidentalImplementer simple = (OtherAccidentalImplementer) object;

    simple.setX(3);
    assertEquals(3 + GET_X + SET_X, simple.getX());
    assertEquals(3 + GET_X + SET_X, getProperty(object, "x"));
    assertEquals(3 + SET_X, ((MyNativeJsTypeInterfaceImplNeedingBridgeSubclass) object).y);
    assertEquals(0, ((OtherAccidentalImplementer) object).x);

    setProperty(object, "x", 4);
    assertEquals(4 + GET_X + SET_X, simple.getX());
    assertEquals(4 + GET_X + SET_X, getProperty(object, "x"));
    assertEquals(4 + SET_X, ((MyNativeJsTypeInterfaceImplNeedingBridgeSubclass) object).y);
    assertEquals(0, ((OtherAccidentalImplementer) object).x);

    MyNativeJsTypeInterfaceImplNeedingBridgeSubclass subclass =
        (MyNativeJsTypeInterfaceImplNeedingBridgeSubclass) object;

    subclass.setParentX(5);
    assertEquals(8 + SET_PARENT_X, simple.sum(3));
    assertEquals(9 + SET_PARENT_X + GET_PARENT_X + SET_X, subclass.getXPlusY());
    assertEquals(4 + SET_X, ((MyNativeJsTypeInterfaceImplNeedingBridgeSubclass) object).y);
    assertEquals(5 + SET_PARENT_X, ((OtherAccidentalImplementer) object).x);
  }

  @JsType(isNative = true)
  interface MyJsTypeInterfaceWithProtectedNames {
    String var();

    @JsProperty
    String getNullField(); // Defined in object scope but shouldn't obfuscate

    @JsProperty
    String getImport();

    @JsProperty
    void setImport(String str);
  }

  public void testProtectedNames() {
    MyJsTypeInterfaceWithProtectedNames obj = createMyJsInterfaceWithProtectedNames();
    assertEquals("var", obj.var());
    assertEquals("nullField", obj.getNullField());
    assertEquals("import", obj.getImport());
    obj.setImport("import2");
    assertEquals("import2", obj.getImport());
  }

  @JsType(isNative = true)
  interface JsTypeIsProperty {

    @JsProperty
    boolean isX();

    @JsProperty
    void setX(boolean x);
  }

  public void testJsPropertyIsX() {
    JsTypeIsProperty object = (JsTypeIsProperty) JavaScriptObject.createObject();

    assertFalse(object.isX());
    object.setX(true);
    assertTrue(object.isX());
    object.setX(false);
    assertFalse(object.isX());
  }

  @JsType(isNative = true)
  interface AccidentalOverridePropertyJsTypeInterface {
    @JsProperty
    int getX();
  }

  static class AccidentalOverridePropertyBase {
    public int getX() {
      return 50;
    }
  }

  static class AccidentalOverrideProperty extends AccidentalOverridePropertyBase
      implements AccidentalOverridePropertyJsTypeInterface {
  }

  public void testJsPropertyAccidentalOverrideSuperCall() {
    AccidentalOverrideProperty object = new AccidentalOverrideProperty();
    assertEquals(50, object.getX());
    assertEquals(50, getProperty(object, "x"));
  }

  @JsType
  static class RemovedAccidentalOverridePropertyBase {
    @JsProperty
    public int getX() {
      return 55;
    }
  }

  static class RemovedAccidentalOverrideProperty extends RemovedAccidentalOverridePropertyBase
      implements AccidentalOverridePropertyJsTypeInterface {
  }

  public void testJsPropertyRemovedAccidentalOverrideSuperCall() {
    RemovedAccidentalOverrideProperty object = new RemovedAccidentalOverrideProperty();
    // If the accidental override here were not removed the access to property x would result in
    // an infinite loop
    assertEquals(55, object.getX());
    assertEquals(55, getProperty(object, "x"));
  }

  @JsType(isNative = true)
  interface JsTypeGetProperty {

    @JsProperty
    int getX();

    @JsProperty
    void setX(int x);
  }

  public void testJsPropertyGetX() {
    JsTypeGetProperty object = (JsTypeGetProperty) JavaScriptObject.createObject();

    assertTrue(isUndefined(object.getX()));
    object.setX(10);
    assertEquals(10, object.getX());
    object.setX(0);
    assertEquals(0, object.getX());
  }

  private static native MyJsTypeInterfaceWithProtectedNames createMyJsInterfaceWithProtectedNames() /*-{
    var a = {};
    a["nullField"] = "nullField";
    a["import"] = "import";
    a["var"] = function() { return "var"; };
    return a;
  }-*/;

  private static native boolean isUndefined(int value) /*-{
    return value === undefined;
  }-*/;

  private static native boolean hasField(Object object, String fieldName) /*-{
    return object[fieldName] != undefined;
  }-*/;

  private static native int getProperty(Object object, String name) /*-{
    return object[name];
  }-*/;

  private static native void setProperty(Object object, String name, int value) /*-{
    object[name] = value;
  }-*/;

  public static void assertJsTypeHasFields(Object obj, String... fields) {
    for (String field : fields) {
      assertTrue("Field '" + field + "' should be exported", hasField(obj, field));
    }
  }

  public static void assertJsTypeDoesntHaveFields(Object obj, String... fields) {
    for (String field : fields) {
      assertFalse("Field '" + field + "' should not be exported", hasField(obj, field));
    }
  }

  static class B {
    @JsProperty
    public String field;
  }
  private native String getB(B b)/*-{
    return b.field;
  }-*/;

  public void testNotReadExpoprtedFieldNotPruned() {
    B b = new B();
    b.field = "secret";
    assertEquals("secret", getB(b));
  }
}
