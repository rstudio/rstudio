/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.jjs.test.singlejso;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests SingleJso semantics in non-trivial type hierarchies.
 */
public class TypeHierarchyTest extends GWTTestCase {

  /**
   * Used with PlainJso and PlainJsoWithInterface to mix interfaces into
   * existing base classes.
   */
  interface Arrayish {
    int getLength();

    JavaScriptObject getObject(int i);

    /**
     * Used to test virtual override where the implementation has a narrower
     * return type.
     */
    Wide wide();
  }

  /**
   * The bottom type for a non-trivial diamond-shaped inheritance pattern.
   */
  static class DiamondImpl extends JavaScriptObject implements IDiamond2A,
      IDiamond2B {
    public static native DiamondImpl create() /*-{
      return {size : 42};
    }-*/;

    protected DiamondImpl() {
    }

    public final native int size() /*-{
      return this.size;
    }-*/;
  }

  /**
   * The root type for a non-trivial diamond-shaped inheritance pattern.
   */
  interface IDiamond1 {
    int size();
  }

  /**
   * The left type for a non-trivial diamond-shaped inheritance pattern.
   */
  interface IDiamond2A extends IDiamond1 {
  }

  /**
   * The right type for a non-trivial diamond-shaped inheritance pattern.
   */
  interface IDiamond2B extends IDiamond1 {
  }

  /**
   * Used for testing virtual overrides.
   */
  static class Narrow extends Wide {
    public String toString() {
      return "Narrow";
    }
  }

  /**
   * This is a base class that is used to test adding interfaces to a JSO via a
   * subclass.
   */
  static class PlainJso extends JavaScriptObject {
    protected PlainJso() {
    }

    public final native int getLength()/*-{
      return this.length;
    }-*/;

    public final native JavaScriptObject getObject(int i) /*-{
      return this[i];
    }-*/;

    public final Narrow wide() {
      return new Narrow();
    }
  }

  /**
   * We'll mix in an interface into PlainJso.
   */
  static class PlainJsoWithInterface extends PlainJso implements Arrayish {
    public static PlainJsoWithInterface create() {
      return JavaScriptObject.createArray().cast();
    }

    protected PlainJsoWithInterface() {
    }
  }

  /**
   * Used for testing virtual overrides.
   */
  static class Wide {
  }

  private interface Element extends Node {
  }

  private static class JvmNode implements Node {
    public JvmNode appendChild(Node node) {
      return (JvmNode) node;
    }
  }
  
  private final static class JvmElement extends JvmNode implements Element {
    static Element create() {
      return new JvmElement();
    }
  }
  
  private final static class JsElement extends JsNode implements Element {
    static Element create() {
      return (Element) JavaScriptObject.createObject();
    }

    protected JsElement() {
    }
  }

  private static class JsNode extends JavaScriptObject implements Node {
    public final native JsNode appendChild(Node node) /*-{
      return node;
    }-*/;

    protected JsNode() {
    }
  }

  private interface Node {
    Node appendChild(Node node);
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testCase1() {
    A a = A.create();
    assertEquals("A", a.whoAmI());

    B1 b1 = new B1();
    assertEquals("B1", b1.whoAmI());

    B2 b2 = new B2();
    assertEquals("B2", b2.whoAmI());
  }

  public void testCase2() {
    IA a = A.create();
    assertEquals("A", a.whoAmI());

    IA b1 = new B1();
    assertEquals("B1", b1.whoAmI());

    IA b2 = new B2();
    assertEquals("B2", b2.whoAmI());
  }
  
  public void testCase3() {
    IA a = A.create();
    assertEquals("A", a.whoAmI());

    IB b1 = new B1();
    assertEquals("B1", b1.whoAmI());

    IB b2 = new B2();
    assertEquals("B2", b2.whoAmI());
  }

  public void testDiamond() {
    IDiamond1 d1 = DiamondImpl.create();
    assertEquals(42, d1.size());

    IDiamond2A d2a = DiamondImpl.create();
    assertEquals(42, d2a.size());

    IDiamond2B d2b = DiamondImpl.create();
    assertEquals(42, d2b.size());
  }
  
  /**
   * Tests that dispatches through a hierarchy of interfaces works properly.
   */
  public void testInterfaceHierarchyDispatch() {
    Element jsElement = JsElement.create();
    assertEquals(jsElement, jsElement.appendChild(jsElement));
    
    Element jvmElement = JvmElement.create();
    assertEquals(jvmElement, jvmElement.appendChild(jvmElement));
  }

  public void testVirtualOverrides() {
    Arrayish array = PlainJsoWithInterface.create();
    assertEquals(0, array.getLength());
    assertNull(array.getObject(0));
    assertEquals("Narrow", array.wide().toString());
  }
}
