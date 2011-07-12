/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.core.client;

import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Test Js Identity.
 */
public class JsIdentityTest extends GWTTestCase {
  /*
   * (non-Javadoc)
   * 
   * @see com.google.gwt.junit.client.GWTTestCase#getModuleName()
   */
  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Core";
  }

  /**
   * Store some JavaObjects in a JsArrayOf, then ask Javascript to run find
   * elements via indexOf().
   */
  @DoNotRunWith(Platform.HtmlUnitUnknown)
  public void testJsIdentity() {
    JsArrayOf<Object> elements = JsArrayOf.create();
    Object id1 = new Object();
    Object id2 = new Object();
    Object id3 = new Object();
    Object id4 = new Object();

    elements.push(id1);
    elements.push(id2);
    elements.push(id3);
    // id4 not pushed for failure test.

    assertEquals(true, elements.contains(id1)); // pass (0)
    assertEquals(true, elements.contains(id2)); // pass (1)
    assertEquals(true, elements.contains(id3)); // pass (2)
    assertEquals(false, elements.contains(id4)); // pass (-1)
  }

  /**
   * Test that the same  Java object passed twice is in fact the same in JS. 
   */
  public void testJavaToJs() {
    Object Foo = new Object();
    assertTrue(jsID(Foo,Foo));
  }
  
  /**
   * Store a JavaObject in Javascript, then pass the object back to JS to test
   * for identity.
   */
  public void testJavaObjectStorage() {
    Object Foo = new Object();
    set(Foo);
    assertTrue(isSet(Foo));
    assertTrue(isStrictlySet(Foo));
  }

  /**
   * Store a JavaScriptObject in Javascript, then fetch the JSO twice to test
   * for identity (multiple NPObject requests).
   */
  public void testJsoJavaComparison() {
    storeJsoIdentity();
    JavaScriptObject obj1 = getJsoIdentity();
    JavaScriptObject obj2 = getJsoIdentity();
    assertSame(obj1,obj2);
  }

  /**
   * Store a JavaObject in JavascriptArray and store it in an array, then try to
   * fetch it back. Specific test for old plugin problem.
   */
  public void testJavaArrayArray() {
    Object id1 = new Object();
    Object id2 = new Object();
    JsArray<JsoTestArray<Object>> elements = JavaScriptObject.createArray().cast();

    elements.push(JsoTestArray.create(id1));
    elements.push(JsoTestArray.create(id2));

    Object get1 = elements.get(0).getT();
    Object get2 = elements.get(1).getT();

    assertEquals(2, elements.length());
    assertSame(id1,get1);
    assertSame(id2,get2);
  }

  static final class JsoTestArray<T> extends JavaScriptObject {
    public static native <T> JsoTestArray<T> create(T cmd) /*-{
      return [cmd, false];
    }-*/;

    protected JsoTestArray() {
    }

    /**
     * Has implicit cast.
     */
    public native T getT() /*-{
      return this[0];
    }-*/;

    public native boolean isRepeating() /*-{
      return this[1];
    }-*/;
  }

  static native void set(Object obj) /*-{
    $wnd.__idTest_JavaObj = obj;
  }-*/;

  static native boolean isSet(Object obj) /*-{
    return $wnd.__idTest_JavaObj == obj;
  }-*/;

  static native boolean isStrictlySet(Object obj) /*-{
    return $wnd.__idTest_JavaObj === obj;
  }-*/;

  static native void storeJsoIdentity() /*-{
    var idObject = {
        something : 'another',
        another : 1234
    };
    $wnd.__idTest_Jso = idObject;
  }-*/;

  static native JavaScriptObject getJsoIdentity() /*-{
    return $wnd.__idTest_Jso;
  }-*/;

  static native boolean jsID(Object a, Object b) /*-{
    return a === b;
  }-*/;
}

/**
 * JavaScript native implementation for arrays.
 */
final class JsArrayOf<T> extends JavaScriptObject {

  /**
   * Create a new empty Array instance.
   */
  public static <T> JsArrayOf<T> create() {
    return JavaScriptObject.createArray().cast();
  }

  protected JsArrayOf() {
  }

  public boolean contains(T value) {
    return indexOf(value) != -1;
  }

  public native T get(int index) /*-{
    return this[index];
  }-*/;

  public native int indexOf(T value) /*-{
    if(this.indexOf) {
      return this.indexOf(value);
    } else {
      for(var i=0; i<this.length; i++) {
        if(this[i]==value) {
          return i;
        }
      }
      return -1;
    }
  }-*/;

  public native int length() /*-{
    return this.length;
  }-*/;

  /**
   * Pushes the given value onto the end of the array.
   */
  public native void push(T value) /*-{
    this[this.length] = value;
  }-*/;

  public native void set(int index, T value) /*-{
    this[index] = value;
  }-*/;
}
