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

import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.Iterator;

/**
 * Tests JsType functionality.
 */
// TODO(cromwellian): Add test cases for property overriding of @JsProperty methods in java object
public class JsTypeTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Core";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    ScriptInjector.fromString("function MyJsInterface() {}\n"
        + "MyJsInterface.prototype.sum = function sum(bias) { return this.x + this.y + bias; }\n"
        + "MyJsInterface.prototype.go = function(cb) { cb('Hello'); }")
        .setWindow(TOP_WINDOW).inject();
    patchPrototype(MyClassExtendsJsPrototype.class);
  }

  /**
   * Workaround for the fact that the script is injected after defineClass() has been called.
   */
  private native void patchPrototype(Class<MyClassExtendsJsPrototype> myClass) /*-{
      @java.lang.Class::getPrototypeForClass(Ljava/lang/Class;)(myClass).prototype = $wnd.MyClass;
  }-*/;

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

    // TODO: fix me
    if (isIE8()) {
      return;
    }

    // Calls through a bridge method.
    listWithExport.add("Tight");
    assertEquals("TightListImpl", listWithExport.x);
  }

  public void testConcreteJsTypeAccess() {
    ConcreteJsType concreteJsType = new ConcreteJsType();

    assertTrue(hasField(concreteJsType, "publicMethod"));
    assertTrue(hasField(concreteJsType, "publicField"));

    assertFalse(hasField(concreteJsType, "publicStaticMethod"));
    assertFalse(hasField(concreteJsType, "privateMethod"));
    assertFalse(hasField(concreteJsType, "protectedMethod"));
    assertFalse(hasField(concreteJsType, "packageMethod"));
    assertFalse(hasField(concreteJsType, "publicStaticField"));
    assertFalse(hasField(concreteJsType, "privateField"));
    assertFalse(hasField(concreteJsType, "protectedField"));
    assertFalse(hasField(concreteJsType, "packageField"));
  }

  public void testConcreteJsTypeSubclassAccess() {
    ConcreteJsType concreteJsType = new ConcreteJsType();
    ConcreteJsTypeSubclass concreteJsTypeSubclass = new ConcreteJsTypeSubclass();

    // A subclass of a JsType is not itself a JsType.
    assertFalse(hasField(concreteJsTypeSubclass, "publicSubclassMethod"));
    assertFalse(hasField(concreteJsTypeSubclass, "publicSubclassField"));
    assertFalse(hasField(concreteJsTypeSubclass, "publicStaticSubclassMethod"));
    assertFalse(hasField(concreteJsTypeSubclass, "privateSubclassMethod"));
    assertFalse(hasField(concreteJsTypeSubclass, "protectedSubclassMethod"));
    assertFalse(hasField(concreteJsTypeSubclass, "packageSubclassMethod"));
    assertFalse(hasField(concreteJsTypeSubclass, "publicStaticSubclassField"));
    assertFalse(hasField(concreteJsTypeSubclass, "privateSubclassField"));
    assertFalse(hasField(concreteJsTypeSubclass, "protectedSubclassField"));
    assertFalse(hasField(concreteJsTypeSubclass, "packageSubclassField"));

    // But if it overrides an exported method then the overriding method will be exported.
    assertTrue(hasField(concreteJsType, "publicMethod"));
    assertTrue(hasField(concreteJsTypeSubclass, "publicMethod"));
    assertFalse(
        areSameFunction(concreteJsType, "publicMethod", concreteJsTypeSubclass, "publicMethod"));
    assertFalse(callIntFunction(concreteJsType, "publicMethod")
        == callIntFunction(concreteJsTypeSubclass, "publicMethod"));
  }

//  // TODO: uncomment when bridge methods are being generated for revealed overrides.
//  public void testRevealedOverrideJsType() {
//    PlainParentType plainParentType = new PlainParentType();
//    RevealedOverrideSubType revealedOverrideSubType = new RevealedOverrideSubType();
//
//    // PlainParentType is neither @JsExport or @JsType and so exports no functions.
//    assertFalse(hasField(plainParentType, "run"));
//
//    // RevealedOverrideSubType defines no functions itself, it only inherits them, but it still
//    // exports run() because it implements the @JsType interface JsTypeRunnable.
//    assertTrue(hasField(revealedOverrideSubType, "run"));
//  }

  public void testSubClassWithSuperCalls() {
    MyClassExtendsJsPrototype mc = new MyClassExtendsJsPrototype();
    assertEquals(150, mc.sum(1));
  }

  public void testJsProperties() {
    MyClassExtendsJsPrototype mc = new MyClassExtendsJsPrototype();
    // Tests both fluent and non-fluent accessors.
    mc.x(-mc.x()).setY(0);
    assertEquals(58, mc.sum(0));
  }

  public void testCasts() {
    MyJsInterface myClass;
    assertNotNull(myClass = (MyJsInterface) createMyJsInterface());

    try {
      assertNotNull(myClass = (MyJsInterface) createNativeButton());
      fail();
    } catch (ClassCastException cce) {
      // Expected.
    }

    ElementLikeJsInterface button;
    // JsTypes without prototypes can cross-cast like JSOs
    assertNotNull(button = (ElementLikeJsInterface) createMyJsInterface());

    /*
     * If the optimizations are turned on, it is possible for the compiler to dead-strip the
     * variables since they are not used. Therefore the casts could potentially be stripped.
     */
    assertNotNull(myClass);
    assertNotNull(button);
  }

  public void testInstanceOf() {
    assertTrue(createMyJsInterface() instanceof MyJsInterface);

    // JsTypes without prototypes can cross-cast like JSOs
    assertTrue(createMyJsInterface() instanceof ElementLikeJsInterface);
  }

  public void testInstanceOfNative() {
    Object obj = createNativeButton();
    assertTrue(obj instanceof Object);
    assertTrue(obj instanceof HTMLButtonElement);
    assertTrue(obj instanceof HTMLElement);
    assertFalse(obj instanceof Iterator);
    assertTrue(obj instanceof HTMLAnotherElement);
    assertFalse(obj instanceof MyJsInterface);

    // to foil type tightening
    obj = alwaysTrue() ? new MyCustomHtmlButtonWithIterator() : null;
    assertTrue(obj instanceof Object);
    assertTrue(obj instanceof HTMLButtonElement);
    assertTrue(obj instanceof HTMLElement);
    assertTrue(obj instanceof Iterable);
    /*
     * TODO: this works, but only because Object can't be type-tightened to HTMLElement. But it will
     * evaluate statically to false for HTMLElement instanceof HTMLAnotherElement. Depending on
     * what the spec decides, fix JTypeOracle so that canTheoreticallyCast returns the appropriate
     * result, as well as add a test here that can be type-tightened.
     */
    assertTrue(obj instanceof HTMLAnotherElement);
    assertFalse(obj instanceof MyJsInterface);
  }

  public void testInstanceOfWithNameSpace() {
    Object obj1 = createMyNamespacedJsInterface();
    Object obj2 = createMyWrongNamespacedJsInterface();

    assertTrue(obj1 instanceof MyNamespacedJsInterface);
    assertFalse(obj1 instanceof MyJsInterface);

    assertFalse(obj2 instanceof MyNamespacedJsInterface);
  }

  private static native boolean alwaysTrue() /*-{
    return !!$wnd;
  }-*/;

  private static native boolean areSameFunction(
      Object thisObject, String thisFunctionName, Object thatObject, String thatFunctionName) /*-{
    return thisObject[thisFunctionName] === thatObject[thatFunctionName];
  }-*/;

  private static native int callIntFunction(Object object, String functionName) /*-{
    return object[functionName]();
  }-*/;

  private static native Object createNativeButton() /*-{
    return $doc.createElement("button");
  }-*/;

  private static native Object createMyJsInterface() /*-{
    return new $wnd.MyJsInterface();
  }-*/;

  private static native Object createMyNamespacedJsInterface() /*-{
    $wnd.testfoo = {};
    $wnd.testfoo.bar = {};
    $wnd.testfoo.bar.MyJsInterface = function(){};
    return new $wnd.testfoo.bar.MyJsInterface();
  }-*/;

  private static native Object createMyWrongNamespacedJsInterface() /*-{
    $wnd["testfoo.bar.MyJsInterface"] = function(){};
    return new $wnd['testfoo.bar.MyJsInterface']();
  }-*/;

  private static native boolean hasField(Object object, String fieldName) /*-{
    return object[fieldName] != undefined;
  }-*/;

  private static native boolean isIE8() /*-{
    return $wnd.navigator.userAgent.toLowerCase().indexOf('msie') != -1 && $doc.documentMode == 8;
  }-*/;

  private static native boolean isFirefox40OrEarlier() /*-{
    return @com.google.gwt.dom.client.DOMImplMozilla::isGecko2OrBefore()();
  }-*/;
}
