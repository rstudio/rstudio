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
package com.google.gwt.dev.jjs.test;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.List;

/**
 * Tests the Java arrays.
 */
public class ArrayTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  private native Object createJsArray(int length) /*-{
    return new Array(length);
  }-*/;

  @DoNotRunWith(Platform.Devel)
  public void testObjectArray_empty() {
    Object nativeArray = createJsArray(0);
    assertTrue(nativeArray instanceof Object[]);
    assertFalse(nativeArray instanceof Object[][]);
    assertFalse(nativeArray instanceof int[]);
    assertFalse(nativeArray instanceof List[]);
    assertTrue(nativeArray.getClass() == JavaScriptObject[].class);

    Object objectArray = new Object[0];
    assertTrue(objectArray instanceof Object[]);
    assertFalse(objectArray instanceof Object[][]);
    assertFalse(objectArray instanceof int[]);
    assertFalse(objectArray instanceof List[]);
    assertTrue(objectArray.getClass() == Object[].class);

    assertFalse(objectArray.equals(nativeArray));
  }

  @DoNotRunWith(Platform.Devel)
  public void testObjectArray_nonEmpty() {
    // Native array is an object array
    Object nativeArray = createJsArray(10);
    assertTrue(nativeArray instanceof Object[]);
    assertFalse(nativeArray instanceof Object[][]);
    assertFalse(nativeArray instanceof int[]);
    assertFalse(nativeArray instanceof List[]);
    assertTrue(nativeArray.getClass() != Object[].class);

    Object objectArray = new Object[10];
    assertTrue(objectArray instanceof Object[]);
    assertFalse(objectArray instanceof Object[][]);
    assertFalse(objectArray instanceof int[]);
    assertFalse(objectArray instanceof List[]);
    assertTrue(objectArray.getClass() == Object[].class);

    assertFalse(objectArray.equals(nativeArray));
  }

  public void testObjectObjectArray() {
    Object array = new Object[10][];
    assertTrue(array instanceof Object[]);
    assertTrue(array instanceof Object[][]);
    assertFalse(array instanceof int[]);
    assertFalse(array instanceof List[]);
    assertTrue(array.getClass() == Object[][].class);

    Object[] objectArray = (Object[]) array;
    objectArray[0] = new Object[0];
    objectArray[1] = new List[1];
    objectArray[2] = new Double[1];

    try {
      objectArray[3] = new int[0];
      fail("Should have thrown ArrayStoreException");
    } catch (ArrayStoreException expected) {
    }
    try {
      objectArray[4] = new Object();
      fail("Should have thrown ArrayStoreException");
    } catch (ArrayStoreException expected) {
    }
  }

  public void testArraysToString() {
    Object[] array = new Object[] { 1, 2 ,3 };
    assertEquals(Object[].class.getName(), ((Object) array).toString().split("@")[0]);
  }

}
