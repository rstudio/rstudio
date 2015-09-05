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
package com.google.gwt.core.client.interop;

import static com.google.gwt.core.client.ScriptInjector.TOP_WINDOW;

import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests JsExport.
 */
public class JsExportTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Core";
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

  public void testMethodExport() {
    // Test exported method can be called from JS in host page
    ScriptInjector.fromString("$global.exported();").setWindow(TOP_WINDOW).inject();
    assertTrue(MyClassExportsMethod.calledFromCallMe1);

    ScriptInjector.fromString("$global.exportNamespace.exported();").setWindow(TOP_WINDOW).inject();
    assertTrue(MyClassExportsMethod.calledFromCallMe2);

    ScriptInjector.fromString("$global.exportNamespace.callMe3();").setWindow(TOP_WINDOW).inject();
    assertTrue(MyClassExportsMethod.calledFromCallMe3);

    ScriptInjector.fromString("$global.woo.MyClassExportsMethod.exported();").setWindow(TOP_WINDOW)
        .inject();
    assertTrue(MyClassExportsMethod.calledFromCallMe4);

    ScriptInjector.fromString("$global.woo.MyClassExportsMethod.callMe5();").setWindow(TOP_WINDOW)
        .inject();
    assertTrue(MyClassExportsMethod.calledFromCallMe5);

    MyClassExportsMethod.calledFromCallMe1 = false;
    // Test exported constructor called from JS in module window
    ScriptInjector.fromString("$global.exported();").inject();
    assertTrue(MyClassExportsMethod.calledFromCallMe1);
  }

  public void testMethodExportWithLong() {
    assertEquals(42.0, callLongMethod(40.0, 2.0));
    assertEquals(82.0, callStaticLongMethod(80.0, 2.0));
  }

  private native double callLongMethod(double a, double b) /*-{
    var obj = new $global.woo.MyJsTypeThatUsesLongType();
    return obj.addLong(a,b);
  }-*/;

  private native double callStaticLongMethod(double a, double b) /*-{
    return $global.woo.MyJsTypeThatUsesLongType.addLongStatic(a,b);
  }-*/;

  public void testMethodExport_noTypeTightenParams() {

    // If we type-tighten, java side will see no calls and think that parameter could only be null.
    // As a result, it will be optimized to null.nullMethod().
    ScriptInjector.fromString("$global.callBar($global.newA());").inject();
    assertTrue(MyClassExportsMethod.calledFromBar);

    // If we type-tighten, java side will only see a call to subclass and think that parameter could
    // be optimized to that one. As a result, the method call will be inlined.
    MyClassExportsMethod.callFoo(new MyClassExportsMethod.SubclassOfA());
    ScriptInjector.fromString("$global.callFoo($global.newA());").inject();
    assertTrue(MyClassExportsMethod.calledFromFoo);
  }

  public void testMethodExport_notReferencedFromJava() {
    // Exported by MyClassExportsMethodWithoutReference which is not referenced by Java. This
    // ensures that we correctly collect root types.
    assertEquals(42, onlyCalledFromJs());
  }

  private native int onlyCalledFromJs() /*-{
    return $global.woo.MyClassExportsMethodWithoutReference.onlyCalledFromJs();
  }-*/;

  public void testClinit() {
    ScriptInjector.fromString("new $global.woo.MyClassExportsMethodWithClinit();").inject();
    assertEquals(23, MyClassExportsMethodWithClinit.magicNumber);
  }

  public void testClinit_staticField() {
    assertNotNull(getStaticInitializerStaticField1());
    assertNotNull(getStaticInitializerStaticField2());
    assertNotNull(getExportedFieldOnInterface());
  }

  private native Object getStaticInitializerStaticField1() /*-{
    return $global.woo.StaticInitializerStaticField.EXPORTED_1;
  }-*/;

  private native Object getStaticInitializerStaticField2() /*-{
    return $global.woo.StaticInitializerStaticField.EXPORTED_2;
  }-*/;

  private native Object getExportedFieldOnInterface() /*-{
    return $global.woo.StaticInitializerStaticField.InterfaceWithField.STATIC;
  }-*/;

  public void testClinit_staticMethod() {
    assertNotNull(getStaticInitializerStaticMethod());
  }

  private native Object getStaticInitializerStaticMethod() /*-{
    return $global.woo.StaticInitializerStaticMethod.getInstance();
  }-*/;

  public void testClinit_virtualMethod() {
    assertNotNull(getStaticInitializerVirtualMethod());
  }

  private native Object getStaticInitializerVirtualMethod() /*-{
    var obj = new $global.woo.StaticInitializerVirtualMethod();
    return obj.getInstance();
  }-*/;

  public void testExportClass_implicitConstructor() {
    assertNotNull(createMyExportedClassWithImplicitConstructor());
  }

  private native Object createMyExportedClassWithImplicitConstructor() /*-{
    return new $global.woo.MyExportedClassWithImplicitConstructor();
  }-*/;

  public void testExportConstructors() {
    assertEquals(4, createMyClassExportsConstructor().foo());
    assertEquals(2, new MyClassExportsConstructor().foo());
  }

  private native MyClassExportsConstructor createMyClassExportsConstructor() /*-{
    return new $global.woo.MyClassExportsConstructor(2);
  }-*/;

  public void testExportedField() {
    assertEquals(100, MyExportedClass.EXPORTED_1);
    assertEquals(100, getExportedField());
    setExportedField(1000);
    assertEquals(100, MyExportedClass.EXPORTED_1);
    assertEquals(1000, getExportedField());
  }

  private native int getExportedField() /*-{
    return $global.woo.MyExportedClass.EXPORTED_1;
  }-*/;

  private native void setExportedField(int a) /*-{
    $global.woo.MyExportedClass.EXPORTED_1 = a;
  }-*/;

  public void testExportedMethod() {
    assertEquals(200, MyExportedClass.foo());
    assertEquals(200, callExportedMethod());
    setExportedMethod();
    assertEquals(200, MyExportedClass.foo());
    assertEquals(1000, callExportedMethod());
  }

  private native int callExportedMethod() /*-{
    return $global.woo.MyExportedClass.foo();
  }-*/;

  private native int setExportedMethod() /*-{
    $global.woo.MyExportedClass.foo = function () {
      return 1000;
    };
  }-*/;

  public void testExportedFieldRefInExportedMethod() {
    assertEquals(5, MyExportedClass.bar(0, 0));
    assertEquals(5, callExportedFieldByExportedMethod(0, 0));
    setExportedField2(10);

    assertEquals(10, getExportedField2());
    assertEquals(7, MyExportedClass.bar(1, 1));
    assertEquals(7, callExportedFieldByExportedMethod(1, 1));
  }

  private native int callExportedFieldByExportedMethod(int a, int b) /*-{
    return $global.woo.MyExportedClass.bar(a, b);
  }-*/;

  private native void setExportedField2(int a) /*-{
    $global.woo.MyExportedClass.EXPORTED_2 = $global.woo.MyExportedClass.newInnerClass(a);
  }-*/;

  private native int getExportedField2() /*-{
    return $global.woo.MyExportedClass.EXPORTED_2.field;
  }-*/;

  public void testNoExport() {
    assertNull(getNotExportedMethods());
    assertNull(getNotExportedFields());
  }

  private native Object getNotExportedFields() /*-{
    return $global.woo.StaticInitializerStaticField.NOT_EXPORTED_1
        || $global.woo.StaticInitializerStaticField.NOT_EXPORTED_2
        || $global.woo.StaticInitializerStaticField.NOT_EXPORTED_3
        || $global.woo.StaticInitializerStaticField.NOT_EXPORTED_4
        || $global.woo.StaticInitializerStaticField.NOT_EXPORTED_5;
  }-*/;

  private native Object getNotExportedMethods() /*-{
    return $global.woo.StaticInitializerStaticMethod.notExported_1
        || $global.woo.StaticInitializerStaticMethod.notExported_2;
  }-*/;

  public static void testInheritClassNamespace() {
    assertEquals(42, getBAR());
  }

  private static native int getBAR() /*-{
    return $global.foo.MyExportedClassWithNamespace.BAR;
  }-*/;

  public static void testInheritClassNamespace_empty() {
    assertEquals(82, getDAN());
    assertNotNull(createNestedExportedClassWithEmptyNamespace());
  }

  private static native int getDAN() /*-{
    return $global.MyClassWithEmptyNamespace.DAN;
  }-*/;

  private static native Object createNestedExportedClassWithEmptyNamespace() /*-{
    return new $global.MyClassWithEmptyNamespace();
  }-*/;

  public static void testInheritClassNamespace_withName() {
    assertEquals(42, getBooBAR());
  }

  private static native int getBooBAR() /*-{
    return $global.foo.boo.BAR;
  }-*/;

  public static void testInheritClassNamespace_noExport() {
    assertEquals(99, getBAZ());
  }

  private static native int getBAZ() /*-{
    return $global.foobaz.MyClassWithNamespace.BAZ;
  }-*/;

  public static void testInheritClassNamespace_nested() {
    assertEquals(99, getLOO());
    assertNotNull(createNestedExportedClassInExportedClass());
  }

  private static native int getLOO() /*-{
    return $global.woo.Bloo.Inner.LOO;
  }-*/;

  private static native Object createNestedExportedClassInExportedClass() /*-{
    return new $global.woo.Bloo.Inner();
  }-*/;

  public static void testInheritClassNamespace_nestedNoExport() {
    assertEquals(999, getWOOZ());
    assertNotNull(createNestedExportedClassWithNamespace());
  }

  private static native int getWOOZ() /*-{
    return $global.zoo.InnerWithNamespace.WOOZ;
  }-*/;

  private static native Object createNestedExportedClassWithNamespace() /*-{
    return new $global.zoo.InnerWithNamespace();
  }-*/;

  public void testInheritPackageNamespace() {
    assertEquals(1001, getWOO());
  }

  private static native int getWOO() /*-{
    return $global.woo.MyExportedClassWithPackageNamespace.WOO;
  }-*/;

  public void testInheritPackageNamespace_nestedClass() {
    assertEquals(99, getNestedWOO());
    assertNotNull(createNestedExportedClass());
  }

  private static native int getNestedWOO() /*-{
    return $global.woo.MyClassWithNestedExportedClass.Inner.WOO;
  }-*/;

  private static native Object createNestedExportedClass() /*-{
    return new $global.woo.MyClassWithNestedExportedClass.Inner();
  }-*/;

  public void testInheritPackageNamespace_nestedEnum() {
    assertNotNull(getNestedEnum());
  }

  private static native Object getNestedEnum() /*-{
    return $global.woo.MyClassWithNestedExportedClass.InnerEnum.AA;
  }-*/;

  public void testInheritPackageNamespace_subpackage() {
    assertNull(getNestedSubpackage());
    assertNotNull(getNestedSubpackageCorrect());
  }

  private static native Object getNestedSubpackage() /*-{
    return $global.woo.subpackage;
  }-*/;

  private static native Object getNestedSubpackageCorrect() /*-{
    return $global.com.google.gwt.core.client.interop.subpackage.
        MyNestedExportedClassSansPackageNamespace;
  }-*/;

  public void testEnum_enumerations() {
    assertNotNull(getEnumerationTEST1());
    assertNotNull(getEnumerationTEST2());
  }

  private static native Object getEnumerationTEST1() /*-{
    return $global.woo.MyExportedEnum.TEST1;
  }-*/;

  private static native Object getEnumerationTEST2() /*-{
    return $global.woo.MyExportedEnum.TEST2;
  }-*/;

  public void testEnum_exportedMethods() {
    assertNotNull(getPublicStaticMethodInEnum());
    assertNotNull(getValuesMethodInEnum());
    assertNotNull(getValueOfMethodInEnum());
  }

  private static native Object getPublicStaticMethodInEnum() /*-{
    return $global.woo.MyExportedEnum.publicStaticMethod;
  }-*/;

  private static native Object getValuesMethodInEnum() /*-{
    return $global.woo.MyExportedEnum.values;
  }-*/;

  private static native Object getValueOfMethodInEnum() /*-{
    return $global.woo.MyExportedEnum.valueOf;
  }-*/;

  public void testEnum_exportedFields() {
    assertEquals(1, getPublicStaticFinalFieldInEnum());

    // explicitly marked @JsExport fields must be final
    // but ones that are in a @JsExported class don't need to be final
    assertEquals(2, getPublicStaticFieldInEnum());
  }

  private static native int getPublicStaticFinalFieldInEnum() /*-{
    return $global.woo.MyExportedEnum.publicStaticFinalField;
  }-*/;

  private static native int getPublicStaticFieldInEnum() /*-{
    return $global.woo.MyExportedEnum.publicStaticField;
  }-*/;

  public void testEnum_notExported() {
    assertNull(getNotExportedFieldsInEnum());
    assertNull(getNotExportedMethodsInEnum());
  }

  private native Object getNotExportedFieldsInEnum() /*-{
    return $global.woo.MyExportedEnum.publicFinalField
        || $global.woo.MyExportedEnum.privateStaticFinalField
        || $global.woo.MyExportedEnum.protectedStaticFinalField
        || $global.woo.MyExportedEnum.defaultStaticFinalField;
  }-*/;

  private native Object getNotExportedMethodsInEnum() /*-{
    return $global.woo.MyExportedEnum.publicMethod
        || $global.woo.MyExportedEnum.protectedStaticMethod
        || $global.woo.MyExportedEnum.privateStaticMethod
        || $global.woo.MyExportedEnum.defaultStaticMethod;
  }-*/;

  public void testEnum_subclassEnumerations() {
    assertNotNull(getEnumerationA());
    assertNotNull(getEnumerationB());
    assertNotNull(getEnumerationC());
  }

  private static native Object getEnumerationA() /*-{
    return $global.woo.MyEnumWithSubclassGen.A;
  }-*/;

  private static native Object getEnumerationB() /*-{
    return $global.woo.MyEnumWithSubclassGen.B;
  }-*/;

  private static native Object getEnumerationC() /*-{
    return $global.woo.MyEnumWithSubclassGen.C;
  }-*/;

  public void testEnum_subclassMethodCallFromExportedEnumerations() {
    assertEquals(100, callPublicMethodFromEnumerationA());
    assertEquals(200, callPublicMethodFromEnumerationB());
    assertEquals(1, callPublicMethodFromEnumerationC());
  }

  private static native int callPublicMethodFromEnumerationA() /*-{
    return $global.woo.MyEnumWithSubclassGen.A.foo();
  }-*/;

  private static native int callPublicMethodFromEnumerationB() /*-{
    return $global.woo.MyEnumWithSubclassGen.B.foo();
  }-*/;

  private static native int callPublicMethodFromEnumerationC() /*-{
    return $global.woo.MyEnumWithSubclassGen.C.foo();
  }-*/;
}
