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

  public void testNoExport() {
    assertNull(getNotExportedMethods());
    assertNull(getNotExportedFields());
  }

  private native Object getNotExportedFields() /*-{
    return $wnd.woo.StaticInitializerStaticField.NOT_EXPORTED_1
        || $wnd.woo.StaticInitializerStaticField.NOT_EXPORTED_2;
  }-*/;

  private native Object getNotExportedMethods() /*-{
    return $wnd.woo.StaticInitializerStaticMethod.notExported_1
        || $wnd.woo.StaticInitializerStaticMethod.notExported_2;
  }-*/;

  public static void testInheritClassNamespace() {
    assertEquals(MyExportedClassWithNamespace.BAR, getFooBAR());
  }

  private static native int getFooBAR() /*-{
    return $wnd.foo.MyExportedClassWithNamespace.BAR || 0;
  }-*/;

  public void testInheritPackageNamespace() {
    assertEquals(MyExportedClassWithPackageNamespace.WOO, getWOO());
  }

  private static native int getWOO() /*-{
    return $wnd.woo.MyExportedClassWithPackageNamespace.WOO || 0;
  }-*/;

  public void testNestedEnum() {
    assertEquals(MyClassWithNestedEnum.NestedEnum.FOO.name(),
        getEnumNameViaJs(MyClassWithNestedEnum.NestedEnum.FOO));
  }

  private static native String getEnumNameViaJs(MyClassWithNestedEnum.NestedEnum ref) /*-{
    return ref.name2();
  }-*/;
}
