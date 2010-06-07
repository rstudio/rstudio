/*
 * Copyright 2010 Google Inc.
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

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests JsArrayMixed methods.
 */
public class JsArrayMixedTest extends GWTTestCase {

  private static class JsTestFruit extends JavaScriptObject {
    @SuppressWarnings("unused")
    protected JsTestFruit() {
    }
  }

  JsArrayMixed mixedArray;

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Core";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    mixedArray = makeArray();
  }

  public void testGetBoolean() {
    assertTrue(mixedArray.getBoolean(0));
    // Test automatic type casting
    mixedArray.set(0, 0);
    assertFalse(mixedArray.getBoolean(0));
  }

  public void testGetNumber() {
    assertEquals(2.5, mixedArray.getNumber(1));
    assertEquals(1.0, mixedArray.getNumber(2));
    // Cast from boolean
    assertEquals(1.0, mixedArray.getNumber(0));
  }

  public void testGetObject() {
    assertTrue(compareObjects(makeObject("pear"),
        mixedArray.<JsTestFruit> getObject(3)));
  }

  public void testGetString() {
    assertEquals("orange", mixedArray.getString(4));
    assertEquals("true", mixedArray.getString(0));
    assertEquals("2.5", mixedArray.getString(1));
    assertEquals("1", mixedArray.getString(2));
  }

  public void testJoin() {
    assertEquals("true,2.5,1,[object Object],orange", mixedArray.join());
  }

  public void testJoinString() {
    assertEquals("true<br/>2.5<br/>1<br/>[object Object]<br/>orange",
        mixedArray.join("<br/>"));
  }

  public void testLength() {
    assertEquals(5, mixedArray.length());
  }

  public void testPushBoolean() {
    mixedArray.push(false);
    assertEquals(6, mixedArray.length());
    assertFalse(mixedArray.getBoolean(5));
  }

  public void testPushDouble() {
    mixedArray.push(1.5);
    assertEquals(6, mixedArray.length());
    assertEquals(1.5, mixedArray.getNumber(5));
  }

  public void testPushJavaScriptObject() {
    JsTestFruit fruit = makeObject("strawberry");
    mixedArray.push(fruit);
    assertEquals(6, mixedArray.length());
    assertEquals(fruit, mixedArray.<JsTestFruit> getObject(5));
  }

  public void testPushString() {
    mixedArray.push("kiwi");
    assertEquals(6, mixedArray.length());
    assertEquals("kiwi", mixedArray.getString(5));
  }

  public void testSetIntBoolean() {
    mixedArray.set(1, false);
    assertFalse(mixedArray.getBoolean(1));
  }

  public void testSetIntDouble() {
    mixedArray.set(0, 4.1);
    assertEquals(4.1, mixedArray.getNumber(0));
  }

  public void testSetIntJavaScriptObject() {
    JsTestFruit fruit = makeObject("kiwi");
    mixedArray.set(0, fruit);
    assertEquals(fruit, mixedArray.<JsTestFruit> getObject(0));
  }

  public void testSetIntString() {
    mixedArray.set(0, "apple");
    assertEquals("apple", mixedArray.getString(0));
  }

  public void testSetLength() {
    mixedArray.setLength(10);
    assertEquals(10, mixedArray.length());
  }

  public void testShiftBoolean() {
    assertEquals(5, mixedArray.length());
    assertTrue(mixedArray.shiftBoolean());
    assertEquals(4, mixedArray.length());
    assertTrue(mixedArray.shiftBoolean());
    assertTrue(mixedArray.shiftBoolean());
    assertTrue(mixedArray.shiftBoolean());
    assertTrue(mixedArray.shiftBoolean());
    assertEquals(0, mixedArray.length());
  }

  public void testShiftNumber() {
    assertEquals(5, mixedArray.length());
    assertEquals(1.0, mixedArray.shiftNumber());
    assertEquals(4, mixedArray.length());
    assertEquals(2.5, mixedArray.shiftNumber());
    assertEquals(1.0, mixedArray.shiftNumber());
    assertTrue(Double.isNaN(mixedArray.shiftNumber()));
    assertTrue(Double.isNaN(mixedArray.shiftNumber()));
    assertEquals(0, mixedArray.length());
  }

  public void testShiftObject() {
    assertEquals(5, mixedArray.length());
    assertEquals("true", mixedArray.<JavaScriptObject>shiftObject().toString());
    assertEquals(4, mixedArray.length());
    assertEquals("2.5", mixedArray.<JavaScriptObject>shiftObject().toString());
    assertEquals("1", mixedArray.<JavaScriptObject>shiftObject().toString());
    assertTrue(compareObjects(makeObject("pear"),
        mixedArray.<JsTestFruit> shiftObject()));
    assertEquals(1, mixedArray.length());
  }

  public void testShiftString() {
    assertEquals(5, mixedArray.length());
    assertEquals("true", mixedArray.shiftString());
    assertEquals(4, mixedArray.length());
    assertEquals("2.5", mixedArray.shiftString());
    assertEquals("1", mixedArray.shiftString());
    assertEquals("[object Object]", mixedArray.shiftString());
    assertEquals("orange", mixedArray.shiftString());
    assertEquals(0, mixedArray.length());
  }

  public void testUnshiftBoolean() {
    assertEquals(5, mixedArray.length());
    mixedArray.unshift(false);
    assertEquals(6, mixedArray.length());
    assertFalse(mixedArray.getBoolean(0));
  }

  public void testUnshiftDouble() {
    assertEquals(5, mixedArray.length());
    mixedArray.unshift(0.5);
    assertEquals(6, mixedArray.length());
    assertEquals(0.5, mixedArray.getNumber(0));
  }

  public void testUnshiftJavaScriptObject() {
    JsTestFruit fruit = makeObject("kiwi");
    assertEquals(5, mixedArray.length());
    mixedArray.unshift(fruit);
    assertEquals(6, mixedArray.length());
    assertEquals(fruit, mixedArray.<JsTestFruit> getObject(0));
  }

  public void testUnshiftString() {
    assertEquals(5, mixedArray.length());
    mixedArray.unshift("kiwi");
    assertEquals(6, mixedArray.length());
    assertEquals("kiwi", mixedArray.getString(0));
  }

  private native boolean compareObjects(JavaScriptObject expected,
      JavaScriptObject actual) /*-{
    for (key in expected) {
      if (expected[key] != actual[key]) {
        return false;
      }
    }
    return true;
  }-*/;

  private native JsArrayMixed makeArray() /*-{
    return [true, 2.5, 1, {kind: "pear"}, "orange"];
  }-*/;

  private native JsTestFruit makeObject(String theKind) /*-{
    return {kind: theKind};
  }-*/;

}
