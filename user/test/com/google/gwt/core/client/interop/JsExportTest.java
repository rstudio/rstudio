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

  public void testMethodExport() {
    // Test exported method can be called from JS in host page
    ScriptInjector.fromString("exportedFromJava();").setWindow(TOP_WINDOW).inject();
    assertTrue(MyClassExportsMethod.calledFromJs);

    MyClassExportsMethod.calledFromJs = false;
    // Test exported constructor called from JS in module window
    ScriptInjector.fromString("$wnd.exportedFromJava();").inject();
    assertTrue(MyClassExportsMethod.calledFromJs);
  }

  public void testMethodExport_noTypeTightenParams() {
    // If we type-tighten, java side will see no calls and think that parameter could only be null.
    // As a result, it will be optimized to null.nullMethod().
    ScriptInjector.fromString("$wnd.callBar($wnd.newA());").inject();
    assertTrue(MyClassExportsMethod.calledFromBar);

    // If we type-tighten, java side will only see a call to subclass and think that parameter could
    // be optimized to that one. As a result, the method call will be inlined.
    MyClassExportsMethod.callFoo(new MyClassExportsMethod.SubclassOfA());
    ScriptInjector.fromString("$wnd.callFoo($wnd.newA());").inject();
    assertTrue(MyClassExportsMethod.calledFromFoo);
  }

  public void testMethodExport_notReferencedFromJava() {
    // Exported by MyClassExportsMethodWithoutReference which is not referenced by Java. This
    // ensures that we correctly collect root types.
    assertEquals(42, onlyCalledFromJs());
  }

  private native int onlyCalledFromJs() /*-{
    return $wnd.onlyCalledFromJs();
  }-*/;

  public void testClinit() {
    ScriptInjector.fromString("new $wnd.MyClassExportsMethodWithClinit();").inject();
    assertEquals(23, MyClassExportsMethodWithClinit.magicNumber);
  }

  public void testClinit_staticField() {
    assertNotNull(getStaticInitializerStaticField1());
    assertNotNull(getStaticInitializerStaticField2());
    assertNotNull(getExportedFieldOnInterface());
  }

  private native Object getStaticInitializerStaticField1() /*-{
    return $wnd.woo.StaticInitializerStaticField.EXPORTED_1;
  }-*/;

  private native Object getStaticInitializerStaticField2() /*-{
    return $wnd.woo.StaticInitializerStaticField.EXPORTED_2;
  }-*/;

  private native Object getExportedFieldOnInterface() /*-{
    return $wnd.woo.StaticInitializerStaticField.InterfaceWithField.STATIC;
  }-*/;

  public void testClinit_staticMethod() {
    assertNotNull(getStaticInitializerStaticMethod());
  }

  private native Object getStaticInitializerStaticMethod() /*-{
    return $wnd.woo.StaticInitializerStaticMethod.getInstance();
  }-*/;

  public void testClinit_virtualMethod() {
    assertNotNull(getStaticInitializerVirtualMethod());
  }

  private native Object getStaticInitializerVirtualMethod() /*-{
    var obj = new $wnd.woo.StaticInitializerVirtualMethod();
    return obj.getInstance();
  }-*/;

  public void testExportClass_implicitConstructor() {
    assertNotNull(createMyExportedClassWithImplicitConstructor());
  }

  private native Object createMyExportedClassWithImplicitConstructor() /*-{
    return new $wnd.woo.MyExportedClassWithImplicitConstructor();
  }-*/;

  public void testExportClass_multipleConstructors() {
    assertEquals(3, getSumByDefaultConstructor());
    assertEquals(30, getSumByConstructor());
  }

  private native int getSumByDefaultConstructor() /*-{
    var obj = new $wnd.MyClassConstructor1();
    return obj.sum();
  }-*/;

  private native int getSumByConstructor() /*-{
    var obj = new $wnd.MyClassConstructor2(10, 20);
    return obj.sum();
  }-*/;

  public void testExportClass_instanceOf() {
    assertTrue(createMyExportedClassWithMultipleConstructors1()
        instanceof MyExportedClassWithMultipleConstructors);
    assertTrue(createMyExportedClassWithMultipleConstructors2()
        instanceof MyExportedClassWithMultipleConstructors);
  }

  private native Object createMyExportedClassWithMultipleConstructors1() /*-{
    return new $wnd.MyClassConstructor1();
  }-*/;

  private native Object createMyExportedClassWithMultipleConstructors2() /*-{
    return new $wnd.MyClassConstructor2(10, 20);
  }-*/;

  public void testExportConstructors() {
    assertEquals(4, getFooByConstructorWithExportSymbol());
    assertNull(getNotExportedConstructor());
  }

  private native int getFooByConstructorWithExportSymbol() /*-{
    var obj = new $wnd.MyClassExportsConstructors1(2);
    return obj.foo();
  }-*/;

  private native Object getNotExportedConstructor() /*-{
    return $wnd.woo.MyClassExportsConstructors;
  }-*/;

  public void testExportedField() {
    assertEquals(100, MyExportedClass.EXPORTED_1);
    assertEquals(100, getExportedField());
    setExportedField(1000);
    assertEquals(100, MyExportedClass.EXPORTED_1);
    assertEquals(1000, getExportedField());
  }

  private native int getExportedField() /*-{
    return $wnd.woo.MyExportedClass.EXPORTED_1;
  }-*/;

  private native void setExportedField(int a) /*-{
    $wnd.woo.MyExportedClass.EXPORTED_1 = a;
  }-*/;

  public void testExportedMethod() {
    assertEquals(200, MyExportedClass.foo());
    assertEquals(200, callExportedMethod());
    setExportedMethod();
    assertEquals(200, MyExportedClass.foo());
    assertEquals(1000, callExportedMethod());
  }

  private native int callExportedMethod() /*-{
    return $wnd.woo.MyExportedClass.foo();
  }-*/;

  private native int setExportedMethod() /*-{
    $wnd.woo.MyExportedClass.foo = function () {
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
    return $wnd.woo.MyExportedClass.bar(a, b);
  }-*/;

  private native void setExportedField2(int a) /*-{
    $wnd.woo.MyExportedClass.EXPORTED_2 = $wnd.newInnerClass(a);
  }-*/;

  private native int getExportedField2() /*-{
    return $wnd.woo.MyExportedClass.EXPORTED_2.field;
  }-*/;

  public void testNoExport() {
    assertNull(getNotExportedMethods());
    assertNull(getNotExportedFields());
  }

  private native Object getNotExportedFields() /*-{
    return $wnd.woo.StaticInitializerStaticField.NOT_EXPORTED_1
        || $wnd.woo.StaticInitializerStaticField.NOT_EXPORTED_2
        || $wnd.woo.StaticInitializerStaticField.NOT_EXPORTED_3
        || $wnd.woo.StaticInitializerStaticField.NOT_EXPORTED_4
        || $wnd.woo.StaticInitializerStaticField.NOT_EXPORTED_5;
  }-*/;

  private native Object getNotExportedMethods() /*-{
    return $wnd.woo.StaticInitializerStaticMethod.notExported_1
        || $wnd.woo.StaticInitializerStaticMethod.notExported_2;
  }-*/;

  public static void testInheritClassNamespace() {
    assertEquals(42, getBAR());
  }

  private static native int getBAR() /*-{
    return $wnd.foo.MyExportedClassWithNamespace.BAR;
  }-*/;

  public static void testInheritClassNamespace_empty() {
    assertEquals(82, getDAN());
    assertNotNull(createNestedExportedClassWithEmptyNamespace());
  }

  private static native int getDAN() /*-{
    return $wnd.MyClassWithEmptyNamespace.DAN;
  }-*/;

  private static native Object createNestedExportedClassWithEmptyNamespace() /*-{
    return new $wnd.MyClassWithEmptyNamespace();
  }-*/;

  public static void testInheritClassNamespace_noExport() {
    assertEquals(99, getBAZ());
  }

  private static native int getBAZ() /*-{
    return $wnd.foobaz.MyClassWithNamespace.BAZ;
  }-*/;

  public static void testInheritClassNamespace_nested() {
    assertEquals(999, getWOOZ());
    assertNotNull(createNestedExportedClassWithNamespace());
  }

  private static native int getWOOZ() /*-{
    return $wnd.zoo.InnerWithNamespace.WOOZ;
  }-*/;

  private static native Object createNestedExportedClassWithNamespace() /*-{
    return new $wnd.zoo.InnerWithNamespace();
  }-*/;

  public void testInheritPackageNamespace() {
    assertEquals(1001, getWOO());
  }

  private static native int getWOO() /*-{
    return $wnd.woo.MyExportedClassWithPackageNamespace.WOO;
  }-*/;

  public void testInheritPackageNamespace_nestedClass() {
    assertEquals(99, getNestedWOO());
    assertNotNull(createNestedExportedClass());
  }

  private static native int getNestedWOO() /*-{
    return $wnd.woo.MyClassWithNestedExportedClass.Inner.WOO;
  }-*/;

  private static native Object createNestedExportedClass() /*-{
    return new $wnd.woo.MyClassWithNestedExportedClass.Inner();
  }-*/;

  public void testInheritPackageNamespace_nestedEnum() {
    assertNotNull(getNestedEnum());
  }

  private static native Object getNestedEnum() /*-{
    return $wnd.woo.MyClassWithNestedExportedClass.InnerEnum.AA;
  }-*/;

  public void testInheritPackageNamespace_subpackage() {
    assertNull(getNestedSubpackage());
    assertNotNull(getNestedSubpackageCorrect());
  }

  private static native Object getNestedSubpackage() /*-{
    return $wnd.woo.subpackage;
  }-*/;

  private static native Object getNestedSubpackageCorrect() /*-{
    return $wnd.com.google.gwt.core.client.interop.subpackage.
        MyNestedExportedClassSansPackageNamespace;
  }-*/;

  public void testEnum_enumerations() {
    assertNotNull(getEnumerationTEST1());
    assertNotNull(getEnumerationTEST2());
  }

  private static native Object getEnumerationTEST1() /*-{
    return $wnd.woo.MyExportedEnum.TEST1;
  }-*/;

  private static native Object getEnumerationTEST2() /*-{
    return $wnd.woo.MyExportedEnum.TEST2;
  }-*/;

  public void testEnum_exportedMethods() {
    assertNotNull(getPublicStaticMethodInEnum());
    assertNotNull(getValuesMethodInEnum());
    assertNotNull(getValueOfMethodInEnum());
  }

  private static native Object getPublicStaticMethodInEnum() /*-{
    return $wnd.woo.MyExportedEnum.publicStaticMethod;
  }-*/;

  private static native Object getValuesMethodInEnum() /*-{
    return $wnd.woo.MyExportedEnum.values;
  }-*/;

  private static native Object getValueOfMethodInEnum() /*-{
    return $wnd.woo.MyExportedEnum.valueOf;
  }-*/;

  public void testEnum_exportedFields() {
    assertEquals(1, getPublicStaticFinalFieldInEnum());

    // explicitly marked @JsExport fields must be final
    // but ones that are in a @JsExported class don't need to be final
    assertEquals(2, getPublicStaticFieldInEnum());
  }

  private static native int getPublicStaticFinalFieldInEnum() /*-{
    return $wnd.woo.MyExportedEnum.publicStaticFinalField;
  }-*/;

  private static native int getPublicStaticFieldInEnum() /*-{
    return $wnd.woo.MyExportedEnum.publicStaticField;
  }-*/;

  public void testEnum_notExported() {
    assertNull(getNotExportedFieldsInEnum());
    assertNull(getNotExportedMethodsInEnum());
  }

  private native Object getNotExportedFieldsInEnum() /*-{
    return $wnd.woo.MyExportedEnum.publicFinalField
        || $wnd.woo.MyExportedEnum.privateStaticFinalField
        || $wnd.woo.MyExportedEnum.protectedStaticFinalField
        || $wnd.woo.MyExportedEnum.defaultStaticFinalField;
  }-*/;

  private native Object getNotExportedMethodsInEnum() /*-{
    return $wnd.woo.MyExportedEnum.publicMethod
        || $wnd.woo.MyExportedEnum.protectedStaticMethod
        || $wnd.woo.MyExportedEnum.privateStaticMethod
        || $wnd.woo.MyExportedEnum.defaultStaticMethod;
  }-*/;

  public void testEnum_subclassEnumerations() {
    assertNotNull(getEnumerationA());
    assertNotNull(getEnumerationB());
    assertNotNull(getEnumerationC());
  }

  private static native Object getEnumerationA() /*-{
    return $wnd.woo.MyEnumWithSubclassGen.A;
  }-*/;

  private static native Object getEnumerationB() /*-{
    return $wnd.woo.MyEnumWithSubclassGen.B;
  }-*/;

  private static native Object getEnumerationC() /*-{
    return $wnd.woo.MyEnumWithSubclassGen.C;
  }-*/;

  public void testEnum_subclassMethodCallFromExportedEnumerations() {
    assertEquals(100, callPublicMethodFromEnumerationA());
    assertEquals(200, callPublicMethodFromEnumerationB());
    assertEquals(1, callPublicMethodFromEnumerationC());
  }

  private static native int callPublicMethodFromEnumerationA() /*-{
    return $wnd.woo.MyEnumWithSubclassGen.A.foo();
  }-*/;

  private static native int callPublicMethodFromEnumerationB() /*-{
    return $wnd.woo.MyEnumWithSubclassGen.B.foo();
  }-*/;

  private static native int callPublicMethodFromEnumerationC() /*-{
    return $wnd.woo.MyEnumWithSubclassGen.C.foo();
  }-*/;
}
