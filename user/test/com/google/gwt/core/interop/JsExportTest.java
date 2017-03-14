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

import com.google.gwt.core.interop.MyExportedClass.InnerClass;
import com.google.gwt.junit.client.GWTTestCase;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Tests presence and naming of exported classes, fields, and methods.
 */
public class JsExportTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Interop";
  }

  @Override
  public void gwtSetUp() throws Exception {
    setupGlobal();
  }

  // $global always points to scope of exports
  private native void setupGlobal() /*-{
    $global = window.goog && window.goog.global || $wnd;
    $wnd.$global = $global;
  }-*/;

  private native boolean isClosureFormattedOutputEnabled() /*-{
    return !!(window.goog && window.goog.global);
  }-*/;

  public void testMethodExport() {
    myClassExportsMethodCallMe1();
    assertTrue(MyClassExportsMethod.calledFromCallMe1);

    myClassExportsMethodCallMe2();
    assertTrue(MyClassExportsMethod.calledFromCallMe2);

    myClassExportsMethodCallMe3();
    assertTrue(MyClassExportsMethod.calledFromCallMe3);

    myClassExportsMethodCallMe4();
    assertTrue(MyClassExportsMethod.calledFromCallMe4);

    myClassExportsMethodCallMe5();
    assertTrue(MyClassExportsMethod.calledFromCallMe5);
  }

  @JsMethod(namespace = "$global", name = "exported")
  private static native void myClassExportsMethodCallMe1();

  @JsMethod(namespace = "$global.exportNamespace", name = "exported")
  private static native void myClassExportsMethodCallMe2();

  @JsMethod(namespace = "$global.exportNamespace", name = "callMe3")
  private static native void myClassExportsMethodCallMe3();

  @JsMethod(namespace = "$global.woo.MyClassExportsMethod", name = "exported")
  private static native void myClassExportsMethodCallMe4();

  @JsMethod(namespace = "$global.woo.MyClassExportsMethod", name = "callMe5")
  private static native void myClassExportsMethodCallMe5();

  public void testMethodExportWithLong() {
    NativeMyJsTypeThatUsesLongType obj = new NativeMyJsTypeThatUsesLongType();

    assertEquals(42.0, obj.addLong(40.0, 2.0));
    assertEquals(82.0, NativeMyJsTypeThatUsesLongType.addLongStatic(80.0, 2.0));
  }

  /**
   * Native interface to type MyJsTypeThatUsesLongType which has been exported to a particular
   * namespaces.
   */
  @JsType(isNative = true, namespace = "$global.woo", name = "MyJsTypeThatUsesLongType")
  private static class NativeMyJsTypeThatUsesLongType {
    public native double addLong(double a, double b);

    public static native double addLongStatic(double a, double b);
  }

  public void testMethodExport_notReferencedFromJava() {
    // Exported by MyClassExportsMethodWithoutReference which is not referenced by Java. This
    // ensures that we correctly collect root types.
    assertEquals(42, onlyCalledFromJs());
  }

  @JsMethod(
      namespace = "$global.woo.MyClassExportsMethodWithoutReference", name = "onlyCalledFromJs")
  private static native int onlyCalledFromJs();

  public void testClinit() {
    new NativeMyClassExportsMethodWithClinit();
    assertEquals(23, MyClassExportsMethodWithClinit.magicNumber);
  }

  /**
   * Native interface to type MyClassExportsMethodWithClinit which has been exported to a particular
   * namespaces.
   */
  @JsType(isNative = true, namespace = "$global.woo", name = "MyClassExportsMethodWithClinit")
  private static class NativeMyClassExportsMethodWithClinit { }

  public void testClinit_staticField() {
    assertNotNull(getStaticInitializerStaticFieldExported1());
    assertNotNull(getStaticInitializerStaticFieldExported2());
    assertNotNull(getStaticInitializerStaticFieldInterfaceStatic());
  }

  @JsProperty(namespace = "$global.woo.StaticInitializerStaticField", name = "EXPORTED_1")
  private static native Object getStaticInitializerStaticFieldExported1();

  @JsProperty(namespace = "$global.woo.StaticInitializerStaticField", name = "EXPORTED_2")
  private static native Object getStaticInitializerStaticFieldExported2();

  @JsProperty(
      namespace = "$global.woo.StaticInitializerStaticField.InterfaceWithField", name = "STATIC")
  private static native Object getStaticInitializerStaticFieldInterfaceStatic();

  public void testClinit_staticMethod() {
    assertNotNull(getStaticInitializerStaticMethod());
  }

  @JsMethod(
      namespace = "$global.woo.StaticInitializerStaticMethod", name = "getInstance")
  private static native int getStaticInitializerStaticMethod();

  public void testClinit_virtualMethod() {
    assertNotNull(new NativeStaticInitializerVirtualMethod().getInstance());
  }

  /**
   * Native interface to type StaticInitializerVirtualMethod which has been exported to a particular
   * namespaces.
   */
  @JsType(isNative = true, namespace = "$global.woo", name = "StaticInitializerVirtualMethod")
  private static class NativeStaticInitializerVirtualMethod {
    public native Object getInstance();
  }

  @JsType(namespace = "bar.foo.baz")
  class MyExportedClassCorrectNamespace {
    public MyExportedClassCorrectNamespace() { }
  }

  public void testExportClass_correctNamespace() {
    assertNull(getBarMyExportedClassCorrectNamespace());
    assertNull(getBarFooMyExportedClassCorrectNamespace());
    assertTrue(getBarFooBazMyExportedClassCorrectNamespace() instanceof NativeFunction);
    Object o = new NativeMyExportedClassCorrectNamespace();
    assertNotNull(o);
    assertTrue(o instanceof MyExportedClassCorrectNamespace);
  }

  @JsProperty(namespace = "$global.bar", name = "MyExportedClassCorrectNamespace")
  private static native Object getBarMyExportedClassCorrectNamespace();

  @JsProperty(namespace = "$global.bar.foo", name = "MyExportedClassCorrectNamespace")
  private static native Object getBarFooMyExportedClassCorrectNamespace();

  @JsProperty(namespace = "$global.bar.foo.baz", name = "MyExportedClassCorrectNamespace")
  private static native Object getBarFooBazMyExportedClassCorrectNamespace();

  @JsType(isNative = true, namespace = GLOBAL, name = "Function")
  private static class NativeFunction { }

  @JsType(
      isNative = true, namespace = "$global.bar.foo.baz", name = "MyExportedClassCorrectNamespace")
  private static class NativeMyExportedClassCorrectNamespace { }

  public void testExportClass_implicitConstructor() {
    Object o = new NativeMyExportedClassWithImplicitConstructor();
    assertNotNull(o);
    assertTrue(o instanceof MyExportedClassWithImplicitConstructor);
  }

  @JsType(
      isNative = true, namespace = "$global.woo", name = "MyExportedClassWithImplicitConstructor")
  private static class NativeMyExportedClassWithImplicitConstructor { }

  public void testExportConstructors() {
    MyClassExportsConstructor nativeMyClassExportsConstructor =
        (MyClassExportsConstructor) (Object) new NativeMyClassExportsConstructor(2);
    assertEquals(4, nativeMyClassExportsConstructor.foo());
    assertEquals(2, new MyClassExportsConstructor().foo());
  }

  @JsType(
      isNative = true, namespace = "$global.woo", name = "MyClassExportsConstructor")
  private static class NativeMyClassExportsConstructor {
    public NativeMyClassExportsConstructor(@SuppressWarnings("unused") int a) { }
  }

  public void testGetJsConstructor() {
    if (isClosureFormattedOutputEnabled()) {
      return; // Closure formatted output doesn't support jsConstructor.
    }
    Object constructorFn = getJsConstructor(MyClassExportsConstructor.class);
    assertSame(getMyClassExportsConstructor(), constructorFn);
    assertSame(MyClassExportsConstructor.class, getClass(constructorFn));

    assertNull(getJsConstructor(Object.class));
    assertNull(getJsConstructor(String.class));
  }

  @JsProperty(namespace = "$global.woo", name = "MyClassExportsConstructor")
  private static native Object getMyClassExportsConstructor();

  private static native Object getJsConstructor(Class<?> clazz) /*-{
    return clazz.@Class::jsConstructor;
  }-*/;

  private static native Class<?> getClass(Object ctor) /*-{
    return ctor.prototype.@Object::___clazz;
  }-*/;

  public void testExportedField() {
    assertEquals(100, MyExportedClass.EXPORTED_1);
    assertEquals(100, getExportedField());
    setExportedField(1000);
    assertEquals(100, MyExportedClass.EXPORTED_1);
    assertEquals(1000, getExportedField());
  }

  @JsProperty(namespace = "$global.woo.MyExportedClass", name = "EXPORTED_1")
  private static native int getExportedField();

  @JsProperty(namespace = "$global.woo.MyExportedClass", name = "EXPORTED_1")
  private static native void setExportedField(int value);

  public void testExportedMethod() {
    assertEquals(200, MyExportedClass.foo());
    assertEquals(200, callExportedMethod());
    setExportedMethod(getReplacementExportedMethod());
    assertEquals(200, MyExportedClass.foo());
    assertEquals(1000, callExportedMethod());
  }

  @JsMethod(
      namespace = "$global.woo.MyExportedClass", name = "foo")
  private static native int callExportedMethod();

  @JsProperty(
      namespace = "$global.woo.MyExportedClass", name = "foo")
  private static native void setExportedMethod(Object object);

  @JsProperty(
      namespace = "$global.woo.MyExportedClass", name = "replacementFoo")
  private static native Object getReplacementExportedMethod();

  public void testExportedFieldRefInExportedMethod() {
    assertEquals(5, MyExportedClass.bar(0, 0));
    assertEquals(5, callExportedFieldByExportedMethod(0, 0));
    setExportedField2(myExportedClassNewInnerClass(10));

    assertEquals(10, getExportedField2());
    assertEquals(7, MyExportedClass.bar(1, 1));
    assertEquals(7, callExportedFieldByExportedMethod(1, 1));
  }

  @JsMethod(namespace = "$global.woo.MyExportedClass", name = "bar")
  private static native int callExportedFieldByExportedMethod(int a, int b);

  @JsProperty(namespace = "$global.woo.MyExportedClass", name = "EXPORTED_2")
  private static native void setExportedField2(InnerClass a);

  @JsMethod(namespace = "$global.woo.MyExportedClass", name = "newInnerClass")
  private static native InnerClass myExportedClassNewInnerClass(int a);

  @JsProperty(namespace = "$global.woo.MyExportedClass.EXPORTED_2", name = "field")
  private static native int getExportedField2();

  public void testNoExport() {
    assertNull(getNotExportedMethod1());
    assertNull(getNotExportedMethod2());

    assertNull(getNotExported1());
    assertNull(getNotExported2());
    assertNull(getNotExported3());
    assertNull(getNotExported4());
    assertNull(getNotExported5());
  }

  @JsProperty(namespace = "$global.woo.StaticInitializerStaticMethod", name = "notExported_1")
  private static native Object getNotExportedMethod1();

  @JsProperty(namespace = "$global.woo.StaticInitializerStaticMethod", name = "notExported_2")
  private static native Object getNotExportedMethod2();

  @JsProperty(namespace = "$global.woo.StaticInitializerStaticField", name = "NOT_EXPORTED_1")
  private static native Object getNotExported1();

  @JsProperty(namespace = "$global.woo.StaticInitializerStaticField", name = "NOT_EXPORTED_2")
  private static native Object getNotExported2();

  @JsProperty(namespace = "$global.woo.StaticInitializerStaticField", name = "NOT_EXPORTED_3")
  private static native Object getNotExported3();

  @JsProperty(namespace = "$global.woo.StaticInitializerStaticField", name = "NOT_EXPORTED_4")
  private static native Object getNotExported4();

  @JsProperty(namespace = "$global.woo.StaticInitializerStaticField", name = "NOT_EXPORTED_5")
  private static native Object getNotExported5();

  public static void testInheritClassNamespace() {
    assertEquals(42, getBAR());
  }

  @JsProperty(namespace = "$global.foo.MyExportedClassWithNamespace", name = "BAR")
  private static native int getBAR();

  public static void testInheritClassNamespace_empty() {
    assertEquals(82, getDAN());
    assertNotNull(new NativeMyClassWithEmptyNamespace());
  }

  @JsProperty(namespace = "$global.MyClassWithEmptyNamespace", name = "DAN")
  private static native int getDAN();

  @JsType(isNative = true, namespace = "$global", name = "MyClassWithEmptyNamespace")
  private static class NativeMyClassWithEmptyNamespace { }

  public static void testInheritClassNamespace_withName() {
    assertEquals(42, getBooBAR());
  }

  @JsProperty(namespace = "$global.foo.boo", name = "BAR")
  private static native int getBooBAR();

  public static void testInheritClassNamespace_noExport() {
    assertEquals(99, getBAZ());
  }

  @JsProperty(namespace = "$global.foobaz.MyClassWithNamespace", name = "BAZ")
  private static native int getBAZ();

  public static void testInheritClassNamespace_nested() {
    assertEquals(99, getLOO());
    assertNotNull(new BlooInner());
  }

  @JsProperty(namespace = "$global.woo.Bloo.Inner", name = "LOO")
  private static native int getLOO();

  @JsType(isNative = true, namespace = "$global.woo.Bloo", name = "Inner")
  private static class BlooInner { }

  public static void testInheritClassNamespace_nestedNoExport() {
    assertEquals(999, getWOOZ());
    assertNotNull(new NativeInnerWithNamespace());
  }

  @JsProperty(namespace = "$global.zoo.InnerWithNamespace", name = "WOOZ")
  private static native int getWOOZ();

  @JsType(isNative = true, namespace = "$global.zoo", name = "InnerWithNamespace")
  private static class NativeInnerWithNamespace { }

  public void testInheritPackageNamespace() {
    assertEquals(1001, getWOO());
  }

  @JsProperty(namespace = "$global.woo.MyExportedClassWithPackageNamespace", name = "WOO")
  private static native int getWOO();

  public void testInheritPackageNamespace_nestedClass() {
    assertEquals(99, getNestedWOO());
    assertNotNull(new NativeMyClassWithNestedExportedClassInner());
  }

  @JsProperty(namespace = "$global.woo.MyClassWithNestedExportedClass.Inner", name = "WOO")
  private static native int getNestedWOO();

  @JsType(isNative = true, namespace = "$global.woo.MyClassWithNestedExportedClass", name = "Inner")
  private static class NativeMyClassWithNestedExportedClassInner { }

  public void testInheritPackageNamespace_nestedEnum() {
    assertNotNull(getNestedEnum());
  }

  @JsProperty(namespace = "$global.woo.MyClassWithNestedExportedClass.InnerEnum", name = "AA")
  private static native Object getNestedEnum();

  public void testInheritPackageNamespace_subpackage() {
    assertNull(getNestedSubpackage());
    assertNotNull(getNestedSubpackageCorrect());
  }

  @JsProperty(namespace = "$global.woo", name = "subpackage")
  private static native Object getNestedSubpackage();

  @JsProperty(
      namespace = "$global.com.google.gwt.core.interop.subpackage",
      name = "MyNestedExportedClassSansPackageNamespace")
  private static native Object getNestedSubpackageCorrect();

  public void testEnum_enumerations() {
    assertNotNull(getEnumerationTEST1());
    assertNotNull(getEnumerationTEST2());
  }

  @JsProperty(namespace = "$global.woo.MyExportedEnum", name = "TEST1")
  private static native Object getEnumerationTEST1();

  @JsProperty(namespace = "$global.woo.MyExportedEnum", name = "TEST2")
  private static native Object getEnumerationTEST2();

  public void testEnum_exportedMethods() {
    assertNotNull(getPublicStaticMethodInEnum());
    assertNotNull(getValuesMethodInEnum());
    assertNotNull(getValueOfMethodInEnum());
  }

  @JsProperty(namespace = "$global.woo.MyExportedEnum", name = "publicStaticMethod")
  private static native Object getPublicStaticMethodInEnum();

  @JsProperty(namespace = "$global.woo.MyExportedEnum", name = "values")
  private static native Object getValuesMethodInEnum();

  @JsProperty(namespace = "$global.woo.MyExportedEnum", name = "valueOf")
  private static native Object getValueOfMethodInEnum();

  public void testEnum_exportedFields() {
    assertEquals(1, getPublicStaticFinalFieldInEnum());

    // explicitly marked @JsType() fields must be final
    // but ones that are in a @JsType()ed class don't need to be final
    assertEquals(2, getPublicStaticFieldInEnum());
  }

  @JsProperty(namespace = "$global.woo.MyExportedEnum", name = "publicStaticFinalField")
  private static native int getPublicStaticFinalFieldInEnum();

  @JsProperty(namespace = "$global.woo.MyExportedEnum", name = "publicStaticField")
  private static native int getPublicStaticFieldInEnum();

  public void testEnum_notExported() {
    assertNull(myExportedEnumPublicFinalField());
    assertNull(myExportedEnumPrivateStaticFinalField());
    assertNull(myExportedEnumProtectedStaticFinalField());
    assertNull(myExportedEnumDefaultStaticFinalField());

    assertNull(myExportedEnumPublicMethod());
    assertNull(myExportedEnumProtectedStaticMethod());
    assertNull(myExportedEnumPrivateStaticMethod());
    assertNull(myExportedEnumDefaultStaticMethod());
  }

  @JsProperty(namespace = "$global.woo.MyExportedEnum", name = "publicFinalField")
  private static native Object myExportedEnumPublicFinalField();

  @JsProperty(namespace = "$global.woo.MyExportedEnum", name = "privateStaticFinalField")
  private static native Object myExportedEnumPrivateStaticFinalField();

  @JsProperty(namespace = "$global.woo.MyExportedEnum", name = "protectedStaticFinalField")
  private static native Object myExportedEnumProtectedStaticFinalField();

  @JsProperty(namespace = "$global.woo.MyExportedEnum", name = "defaultStaticFinalField")
  private static native Object myExportedEnumDefaultStaticFinalField();

  @JsProperty(namespace = "$global.woo.MyExportedEnum", name = "publicMethod")
  private static native Object myExportedEnumPublicMethod();

  @JsProperty(namespace = "$global.woo.MyExportedEnum", name = "protectedStaticMethod")
  private static native Object myExportedEnumProtectedStaticMethod();

  @JsProperty(namespace = "$global.woo.MyExportedEnum", name = "privateStaticMethod")
  private static native Object myExportedEnumPrivateStaticMethod();

  @JsProperty(namespace = "$global.woo.MyExportedEnum", name = "defaultStaticMethod")
  private static native Object myExportedEnumDefaultStaticMethod();

  public void testEnum_subclassEnumerations() {
    assertNotNull(getEnumerationA());
    assertNotNull(getEnumerationB());
    assertNotNull(getEnumerationC());
  }

  @JsProperty(namespace = "$global.woo.MyEnumWithSubclassGen", name = "A")
  private static native Object getEnumerationA();

  @JsProperty(namespace = "$global.woo.MyEnumWithSubclassGen", name = "B")
  private static native Object getEnumerationB();

  @JsProperty(namespace = "$global.woo.MyEnumWithSubclassGen", name = "C")
  private static native Object getEnumerationC();

  public void testEnum_subclassMethodCallFromExportedEnumerations() {
    assertEquals(100, callPublicMethodFromEnumerationA());
    assertEquals(200, callPublicMethodFromEnumerationB());
    assertEquals(1, callPublicMethodFromEnumerationC());
  }

  @JsMethod(namespace = "$global.woo.MyEnumWithSubclassGen.A", name = "foo")
  private static native int callPublicMethodFromEnumerationA();

  @JsMethod(namespace = "$global.woo.MyEnumWithSubclassGen.B", name = "foo")
  private static native int callPublicMethodFromEnumerationB();

  @JsMethod(namespace = "$global.woo.MyEnumWithSubclassGen.C", name = "foo")
  private static native int callPublicMethodFromEnumerationC();
}
