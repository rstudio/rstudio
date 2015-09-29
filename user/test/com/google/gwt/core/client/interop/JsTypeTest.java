/*
 * Copyright 2013 Google Inc.
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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.core.client.js.JsFunction;
import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.Iterator;

/**
 * Tests JsType functionality.
 */
public class JsTypeTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Core";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    ScriptInjector.fromString("function JsTypeTest_MyNativeJsTypeInterface() {}\n"
        + "function JsTypeTest_MyNativeJsType() {}\n"
        + "JsTypeTest_MyNativeJsType.prototype.sum = "
        + "    function sum(bias) { return this.y + bias; };")
        .setWindow(TOP_WINDOW).inject();
  }

  public void testVirtualUpRefs() {
    ListImpl listWithExport = new ListImpl(); // Exports .add().
    FooImpl listNoExport = new FooImpl(); // Does not export .add().

    // Use a loose type reference to force polymorphic dispatch.
    Collection collectionWithExport = alwaysTrue() ? listWithExport : listNoExport;
    collectionWithExport.add("Loose");
    assertEquals("LooseListImpl", listWithExport.x);

    // Use a loose type reference to force polymorphic dispatch.
    Collection collectionNoExport = alwaysTrue() ? listNoExport : listWithExport;
    collectionNoExport.add("Loose");
    assertEquals("LooseCollectionBaseFooImpl", listNoExport.x);

    // Calls directly.
    listNoExport.add("Tight");
    assertEquals("TightCollectionBaseFooImpl", listNoExport.x);

    // Calls through a bridge method.
    listWithExport.add("Tight");
    assertEquals("TightListImpl", listWithExport.x);
  }

  public void testConcreteJsTypeAccess() {
    ConcreteJsType concreteJsType = new ConcreteJsType();

    assertJsTypeHasFields(concreteJsType, "publicMethod", "publicField");
    assertJsTypeDoesntHaveFields(concreteJsType, "publicStaticMethod", "privateMethod",
        "protectedMethod", "packageMethod", "publicStaticField", "privateField", "protectedField",
        "packageField");

    assertEquals(10, callIntFunction(concreteJsType, "publicMethod"));
  }

  public void testAbstractJsTypeAccess() {
    AbstractJsType jsType = new AbstractJsType() {
      @Override
      public int publicMethod() {
        return 32;
      }
    };

    assertJsTypeHasFields(jsType, "publicMethod");
    assertEquals(32, callIntFunction(jsType, "publicMethod"));
    assertEquals(32, jsType.publicMethod());
  }

  public void testConcreteJsTypeSubclassAccess() {
    ConcreteJsTypeSubclass concreteJsTypeSubclass = new ConcreteJsTypeSubclass();

    // A subclass of a JsType is not itself a JsType.
    assertJsTypeDoesntHaveFields(concreteJsTypeSubclass, "publicSubclassMethod",
        "publicSubclassField", "publicStaticSubclassMethod", "privateSubclassMethod",
        "protectedSubclassMethod", "packageSubclassMethod", "publicStaticSubclassField",
        "privateSubclassField", "protectedSubclassField", "packageSubclassField");

    // But if it overrides an exported method then the overriding method will be exported.
    assertJsTypeHasFields(concreteJsTypeSubclass, "publicMethod");

    assertEquals(20, callIntFunction(concreteJsTypeSubclass, "publicMethod"));
    assertEquals(10, concreteJsTypeSubclass.publicSubclassMethod());
  }

  public void testConcreteJsTypeNoTypeTightenField() {
    // If we type-tighten, java side will see no calls and think that field could only AImpl1.
    ConcreteJsType concreteJsType = new ConcreteJsType();
    setTheField(concreteJsType, new ConcreteJsType.AImpl2());
    assertEquals(101, concreteJsType.notTypeTightenedField.x());
  }

  private native void setTheField(ConcreteJsType obj, ConcreteJsType.A value)/*-{
    obj.notTypeTightenedField = value;
  }-*/;

  public void testRevealedOverrideJsType() {
    PlainParentType plainParentType = new PlainParentType();
    RevealedOverrideSubType revealedOverrideSubType = new RevealedOverrideSubType();

    // PlainParentType is neither @JsExport or @JsType and so exports no functions.
    assertFalse(hasField(plainParentType, "run"));

    // RevealedOverrideSubType defines no functions itself, it only inherits them, but it still
    // exports run() because it implements the @JsType interface JsTypeRunnable.
    assertTrue(hasField(revealedOverrideSubType, "run"));

    ConcreteJsTypeJsSubclass subclass = new ConcreteJsTypeJsSubclass();
    assertEquals(100, subclass.publicMethodAlsoExposedAsNonJsMethod());
    SubclassInterface subclassInterface = alwaysTrue() ? subclass : new SubclassInterface() {
      @Override
      public int publicMethodAlsoExposedAsNonJsMethod() {
        return 0;
      }
    };
    assertEquals(100, subclassInterface.publicMethodAlsoExposedAsNonJsMethod());
  }

  @JsType(prototype = "JsTypeTest_MyNativeJsTypeInterface")
  interface MyNativeJsTypeInterface {
  }

  class MyNativeJsTypeInterfaceImpl implements MyNativeJsTypeInterface {
  }

  public void testCasts() {
    Object myClass;
    assertNotNull(myClass = (ElementLikeJsInterface) createMyNativeJsTypeInterface());
    assertNotNull(myClass = (MyNativeJsTypeInterface) createMyNativeJsTypeInterface());
    assertNotNull(myClass = (HTMLElement) createNativeButton());

    try {
      assertNotNull(myClass = (HTMLElement) createMyNativeJsTypeInterface());
      fail();
    } catch (ClassCastException cce) {
      // Expected.
    }

    // Test cross cast for native types
    Object nativeButton1 = (HTMLElement) createNativeButton();
    Object nativeButton2 = (HTMLAnotherElement) nativeButton1;

    /*
     * If the optimizations are turned on, it is possible for the compiler to dead-strip the
     * variables since they are not used. Therefore the casts could potentially be stripped.
     */
    assertNotNull(myClass);
    assertNotNull(nativeButton1);
    assertNotNull(nativeButton2);
  }

  /**
   * A test class marked with JsType but isn't referenced from any Java code except instanceof.
   */
  @JsType(prototype = "JsTypeTest_MyNativeJsTypeInterface")
  public interface MyJsInterfaceWithPrototypeAndOnlyInstanceofReference {
  }

  public void testInstanceOf_jsoWithProto() {
    Object object = createMyNativeJsTypeInterface();

    assertTrue(object instanceof Object);
    assertFalse(object instanceof HTMLAnotherElement);
    assertFalse(object instanceof HTMLButtonElement);
    assertFalse(object instanceof HTMLElement);
    assertFalse(object instanceof Iterator);
    assertTrue(object instanceof MyNativeJsTypeInterface);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl);
    assertTrue(object instanceof ElementLikeJsInterface);
    assertFalse(object instanceof ElementLikeJsInterfaceImpl);
    assertTrue(object instanceof MyJsInterfaceWithOnlyInstanceofReference);
    assertTrue(object instanceof MyJsInterfaceWithPrototypeAndOnlyInstanceofReference);
    assertFalse(object instanceof MyJsClassWithPrototypeAndOnlyInstanceofReference);
    assertFalse(object instanceof ConcreteJsType);
  }

  public void testInstanceOf_jsoWithoutProto() {
    Object object = JavaScriptObject.createObject();

    assertTrue(object instanceof Object);
    assertFalse(object instanceof HTMLAnotherElement);
    assertFalse(object instanceof HTMLButtonElement);
    assertFalse(object instanceof HTMLElement);
    assertFalse(object instanceof Iterator);
    assertFalse(object instanceof MyNativeJsTypeInterface);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl);
    assertTrue(object instanceof ElementLikeJsInterface);
    assertFalse(object instanceof ElementLikeJsInterfaceImpl);
    assertTrue(object instanceof MyJsInterfaceWithOnlyInstanceofReference);
    assertFalse(object instanceof MyJsInterfaceWithPrototypeAndOnlyInstanceofReference);
    assertFalse(object instanceof MyJsClassWithPrototypeAndOnlyInstanceofReference);
    assertFalse(object instanceof ConcreteJsType);
  }

  public void testInstanceOf_jsoWithNativeButtonProto() {
    Object object = createNativeButton();

    assertTrue(object instanceof Object);
    assertTrue(object instanceof HTMLAnotherElement);
    assertTrue(object instanceof HTMLButtonElement);
    assertTrue(object instanceof HTMLElement);
    assertFalse(object instanceof Iterator);
    assertFalse(object instanceof MyNativeJsTypeInterface);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl);
    assertTrue(object instanceof ElementLikeJsInterface);
    assertFalse(object instanceof ElementLikeJsInterfaceImpl);
    assertTrue(object instanceof MyJsInterfaceWithOnlyInstanceofReference);
    assertFalse(object instanceof MyJsInterfaceWithPrototypeAndOnlyInstanceofReference);
    assertTrue(object instanceof MyJsClassWithPrototypeAndOnlyInstanceofReference);
    assertFalse(object instanceof ConcreteJsType);
  }

  public void testInstanceOf_implementsJsType() {
    // Foils type tightening.
    Object object = alwaysTrue() ? new ElementLikeJsInterfaceImpl() : new Object();

    assertTrue(object instanceof Object);
    assertFalse(object instanceof HTMLAnotherElement);
    assertFalse(object instanceof HTMLButtonElement);
    assertFalse(object instanceof HTMLElement);
    assertFalse(object instanceof Iterator);
    assertFalse(object instanceof MyNativeJsTypeInterface);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl);
    assertTrue(object instanceof ElementLikeJsInterface);
    assertTrue(object instanceof ElementLikeJsInterfaceImpl);
    assertFalse(object instanceof MyJsInterfaceWithOnlyInstanceofReference);
    assertFalse(object instanceof MyJsInterfaceWithPrototypeAndOnlyInstanceofReference);
    assertFalse(object instanceof MyJsClassWithPrototypeAndOnlyInstanceofReference);
    assertFalse(object instanceof ConcreteJsType);
  }

  public void testInstanceOf_implementsJsTypeWithPrototype() {
    // Foils type tightening.
    Object object = alwaysTrue() ? new MyNativeJsTypeInterfaceImpl() : new Object();

    assertTrue(object instanceof Object);
    assertFalse(object instanceof HTMLAnotherElement);
    assertFalse(object instanceof HTMLButtonElement);
    assertFalse(object instanceof HTMLElement);
    assertFalse(object instanceof Iterator);
    assertTrue(object instanceof MyNativeJsTypeInterface);
    assertTrue(object instanceof MyNativeJsTypeInterfaceImpl);
    assertFalse(object instanceof ElementLikeJsInterface);
    assertFalse(object instanceof ElementLikeJsInterfaceImpl);
    assertFalse(object instanceof MyJsInterfaceWithOnlyInstanceofReference);
    assertFalse(object instanceof MyJsInterfaceWithPrototypeAndOnlyInstanceofReference);
    assertFalse(object instanceof MyJsClassWithPrototypeAndOnlyInstanceofReference);
    assertFalse(object instanceof ConcreteJsType);
  }

  public void testInstanceOf_concreteJsType() {
    // Foils type tightening.
    Object object = alwaysTrue() ? new ConcreteJsType() : new Object();

    assertTrue(object instanceof Object);
    assertFalse(object instanceof HTMLAnotherElement);
    assertFalse(object instanceof HTMLButtonElement);
    assertFalse(object instanceof HTMLElement);
    assertFalse(object instanceof Iterator);
    assertFalse(object instanceof MyNativeJsTypeInterface);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl);
    assertFalse(object instanceof ElementLikeJsInterface);
    assertFalse(object instanceof ElementLikeJsInterfaceImpl);
    assertFalse(object instanceof MyJsInterfaceWithOnlyInstanceofReference);
    assertFalse(object instanceof MyJsInterfaceWithPrototypeAndOnlyInstanceofReference);
    assertFalse(object instanceof MyJsClassWithPrototypeAndOnlyInstanceofReference);
    assertTrue(object instanceof ConcreteJsType);
  }

  public void testInstanceOf_extendsJsTypeWithProto() {
    // Foils type tightening.
    Object object = alwaysTrue() ? new MyCustomHtmlButtonWithIterator() : new Object();

    assertTrue(object instanceof Object);
    assertTrue(object instanceof HTMLAnotherElement);
    assertTrue(object instanceof HTMLButtonElement);
    assertTrue(object instanceof HTMLElement);
    assertTrue(object instanceof Iterable);
    assertFalse(object instanceof MyNativeJsTypeInterface);
    assertFalse(object instanceof MyNativeJsTypeInterfaceImpl);
    assertFalse(object instanceof ElementLikeJsInterface);
    assertFalse(object instanceof ElementLikeJsInterfaceImpl);
    assertFalse(object instanceof MyJsInterfaceWithOnlyInstanceofReference);
    assertFalse(object instanceof MyJsInterfaceWithPrototypeAndOnlyInstanceofReference);
    assertTrue(object instanceof MyJsClassWithPrototypeAndOnlyInstanceofReference);
    assertFalse(object instanceof ConcreteJsType);
  }

  @JsType(prototype = "JsTypeTest_MyNativeJsType")
  static class MyNativeJsType {
    @JsProperty
    public native int getY();

    @JsProperty
    public native void setY(int value);

    public native int sum(int bias);
  }

  static class MyNativeJsTypeSubclass extends MyNativeJsType {
  }

  // TODO(rluble): enable when the subclass is setup correctly.
  public void _disabled_testNativeJsTypeSubclass() {
    MyNativeJsTypeSubclass myNativeJsTypeSubclass = new MyNativeJsTypeSubclass();
    myNativeJsTypeSubclass.setY(12);
    assertEquals(42, myNativeJsTypeSubclass.sum(30));
    assertTrue(myNativeJsTypeSubclass instanceof MyNativeJsType);
    assertTrue(myNativeJsTypeSubclass instanceof MyNativeJsTypeSubclass);
  }

  @JsType(prototype = "testfoo.bar.MyNativeType")
  interface MyNamespacedJsInterface {
  }

  public void testInstanceOf_withNameSpace() {
    Object obj1 = createMyNamespacedJsInterface();
    Object obj2 = createMyWrongNamespacedJsInterface();

    assertTrue(obj1 instanceof MyNamespacedJsInterface);
    assertFalse(obj1 instanceof MyNativeJsTypeInterface);

    assertFalse(obj2 instanceof MyNamespacedJsInterface);
  }

  public void testEnumeration() {
    assertEquals(2, callPublicMethodFromEnumeration(MyEnumWithJsType.TEST1));
    assertEquals(3, callPublicMethodFromEnumeration(MyEnumWithJsType.TEST2));
  }

  public void testEnumJsTypeAccess() {
    assertJsTypeHasFields(MyEnumWithJsType.TEST2, "publicMethod", "publicField");
    assertJsTypeDoesntHaveFields(MyEnumWithJsType.TEST2, "publicStaticMethod", "privateMethod",
        "protectedMethod", "packageMethod", "publicStaticField", "privateField", "protectedField",
        "packageField");
  }

  public void testEnumSubclassEnumeration() {
    assertEquals(100, callPublicMethodFromEnumerationSubclass(MyEnumWithSubclassGen.A));
    assertEquals(200, callPublicMethodFromEnumerationSubclass(MyEnumWithSubclassGen.B));
    assertEquals(1, callPublicMethodFromEnumerationSubclass(MyEnumWithSubclassGen.C));
  }

  private static native boolean alwaysTrue() /*-{
    return !!$wnd;
  }-*/;

  private static native int callIntFunction(Object object, String functionName) /*-{
    return object[functionName]();
  }-*/;

  private static native Object createNativeButton() /*-{
    return $doc.createElement("button");
  }-*/;

  private static native Object createMyNativeJsTypeInterface() /*-{
    return new $wnd.JsTypeTest_MyNativeJsTypeInterface();
  }-*/;

  private static native Object createMyNamespacedJsInterface() /*-{
    $wnd.testfoo = {};
    $wnd.testfoo.bar = {};
    $wnd.testfoo.bar.MyNativeType = function(){};
    return new $wnd.testfoo.bar.MyNativeType();
  }-*/;

  private static native Object createMyWrongNamespacedJsInterface() /*-{
    $wnd["testfoo.bar.MyNativeType"] = function(){};
    return new $wnd['testfoo.bar.MyNativeType']();
  }-*/;

  private static native boolean isUndefined(int value) /*-{
    return value === undefined;
  }-*/;

  private static native boolean hasField(Object object, String fieldName) /*-{
    return object[fieldName] != undefined;
  }-*/;

  private static native int callPublicMethodFromEnumeration(MyEnumWithJsType enumeration) /*-{
    return enumeration.idxAddOne();
  }-*/;

  private static native int callPublicMethodFromEnumerationSubclass(
      MyEnumWithSubclassGen enumeration) /*-{
    return enumeration.foo();
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

  @JsType
  interface SimpleJsTypeFieldInterface {
  }

  static class SimpleJsTypeFieldClass implements SimpleJsTypeFieldInterface {
  }

  @JsType
  static class SimpleJsTypeWithField {
    public SimpleJsTypeFieldInterface someField;
  }

  public void testJsTypeField() {
    new SimpleJsTypeFieldClass();
    SimpleJsTypeWithField holder = new SimpleJsTypeWithField();
    fillJsTypeField(holder);
    SimpleJsTypeFieldInterface someField = holder.someField;
    assertNotNull(someField);
  }

  private native void fillJsTypeField(SimpleJsTypeWithField jstype) /*-{
    jstype.someField = {};
  }-*/;

  @JsType
  interface InterfaceWithSingleJavaConcrete {
    int m();
  }

  static class JavaConcrete implements InterfaceWithSingleJavaConcrete {
    public int m() {
      return 5;
    }
  }

  private native Object nativeObjectImplementingM() /*-{
    return {m: function() { return 3;} }
  }-*/;

  public void testSingleJavaConcreteInterface() {
    // Create a couple of instances and use the objects in some way to avoid complete pruning
    // of JavaConcrete
    assertTrue(new JavaConcrete() != new JavaConcrete());
    assertSame(5, new JavaConcrete().m());
    assertSame(3, ((InterfaceWithSingleJavaConcrete) nativeObjectImplementingM()).m());
  }

  @JsFunction
  interface JsFunctionInterface {
    int m();
  }

  static class JavaConcreteJsFunction implements JsFunctionInterface {
    public int m() {
      return 5;
    }
  }

  private native Object nativeJsFunction() /*-{
    return function() { return 3;};
  }-*/;

  public void testSingleJavaConcreteJsFunction() {
    // Create a couple of instances and use the objects in some way to avoid complete pruning
    // of JavaConcrete
    assertTrue(new JavaConcreteJsFunction() != new JavaConcreteJsFunction());
    assertSame(5, new JavaConcreteJsFunction().m());
    assertSame(3, ((JsFunctionInterface) nativeJsFunction()).m());
  }

  private static native void setProperty(Object object, String name, int value) /*-{
    object[name] = value;
  }-*/;
}
