/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.emultest.java.lang;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * TODO: document me.
 */
public class ObjectTest extends GWTTestCase {
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  /*
   * Test method for 'java.lang.Object.hashCode()'
   */
  public void testHashCode() {
    Object obj1 = new Object();
    assertEquals(obj1.hashCode(), obj1.hashCode());

    Object obj2 = new Object();
    assertEquals(obj2.hashCode(), obj2.hashCode());
  }

  /**
   * Tests that 'java.lang.Object.castableTypeMap' does not shadow a local field.
   */
  public void testCastableTypeMapShadow() {
    final JavaScriptObject castableTypeMap = JavaScriptObject.createObject();
    class TestClass {
      public JavaScriptObject getCastableTypeMap() {
        return castableTypeMap;
      }
    }
    TestClass test = new TestClass();
    assertEquals(castableTypeMap, test.getCastableTypeMap());
  }

  /**
   * Tests that 'java.lang.Object.typeMarker' does not shadow a local field.
   */
  public void testTypeMarkerShadow() {
    final JavaScriptObject typeMarker = JavaScriptObject.createObject();
    class TestClass {
      public JavaScriptObject getTypeMarker() {
        return typeMarker;
      }
    }
    TestClass test = new TestClass();
    assertEquals(typeMarker, test.getTypeMarker());
  }
}
