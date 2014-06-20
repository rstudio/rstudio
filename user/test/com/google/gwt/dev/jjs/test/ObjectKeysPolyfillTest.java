/*
 * Copyright 2014 Google Inc.
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
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.HashSet;
import java.util.Set;

/**
 * Test our polyfill for Object.keys.
 */
public class ObjectKeysPolyfillTest extends GWTTestCase {

  private static native JsArrayString getKeys(JavaScriptObject o) /*-{
    return Object.keys(o);
  }-*/;

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testGetKeys() {
    JsArrayString keys = getKeys(createSimpleObject());

    Set<String> set = toSet(keys);

    assertEquals(3, set.size());
    assertTrue(set.contains("a"));
    assertTrue(set.contains("b"));
    assertTrue(set.contains("c"));
  }

  public void testPrototypeProperties() {
    JsArrayString keys = getKeys(createObjectWithPropertiesOnProto());

    Set<String> set = toSet(keys);

    assertEquals(3, set.size());
    assertTrue(set.contains("childA"));
    assertTrue(set.contains("childB"));
    assertTrue(set.contains("childC"));
  }

  public void testEnumBug() {
    JsArrayString keys = getKeys(createObjectWithEnumBug());

    Set<String> set = toSet(keys);

    assertEquals(4, set.size());
    assertTrue(set.contains("a"));
    assertTrue(set.contains("b"));
    assertTrue(set.contains("c"));
    assertTrue(set.contains("toString"));
  }

  private Set<String> toSet(JsArrayString keys) {
    Set<String> set = new HashSet<String>();
    for (int i = 0; i < keys.length(); i++) {
      set.add(keys.get(i));
    }
    return set;
  }

  private native JavaScriptObject createObjectWithPropertiesOnProto() /*-{
    var parent = {parentA: 1, parentB: 2, parentC: 3};

    function Child() {
      this.childA = "1";
      this.childB = "2";
      this.childC = "3";
    }

    Child.prototype = parent;

    return new Child();
  }-*/;

  private native JavaScriptObject createObjectWithEnumBug() /*-{
    var o = {a : "1", b : "2", c : "3"};
    // create toString property with a value of null to trigger the enum bug
    // that is present in some browsers like IE8. Those browsers do not loop through
    // properties like toString if they are present on the object but null.
    o.toString = null;
    return o;
  }-*/;

  private native JavaScriptObject createSimpleObject() /*-{
    return {a : "1", b : "2", c : "3"};
  }-*/;
}
