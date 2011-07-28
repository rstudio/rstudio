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
package com.google.gwt.lang;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Utility class for fetching prototype-seed functions for injection into JsAST.
 */
public class SeedUtil {

  /*
  * Holds a map of seedId to anonymous Javascript functions (prototypes for class vtables).
  */
  private static JavaScriptObject seedTable = JavaScriptObject.createObject();

  /**
   * If not already created, generates an anonymous function and assigns it a slot in the global
   * seedTable. If superSeed is > -1, it creates an instance of the superSeed by invoking
   * newSeed() and then assigns it as the prototype of the seed being defined. It also sets up the
   * castableTypeMap, as well as any ctors which are passed in via Javascript varargs. Finally, if
   * the class literal for this seed id was setup first, which can happen if they are in separate
   * code-split fragments, the Class.createFor* methods will have created a placeholder seedTable
   * entry containing the Class literal, and this will be copied from the placeholder location
   * onto the current prototype.
   */
  public static native JavaScriptObject defineSeed(int id, int superSeed,
      JavaScriptObject castableTypeMap) /*-{
    var seed = @com.google.gwt.lang.SeedUtil::seedTable[id];
    if (seed && !seed.@java.lang.Object::___clazz) {
      // not a placeholder entry setup by Class.setClassLiteral
      _ = seed.prototype;
    } else {
      if (!seed) {
        seed = @com.google.gwt.lang.SeedUtil::seedTable[id] = function() {
        };
      }
      _ = seed.prototype = (superSeed < 0) ? {}
          : @com.google.gwt.lang.SeedUtil::newSeed(I)(superSeed);
      _.@java.lang.Object::castableTypeMap = castableTypeMap;
    }
    for (var i = 3; i < arguments.length; ++i) {
      arguments[i].prototype = _;
    }
    if (seed.@java.lang.Object::___clazz) {
      _.@java.lang.Object::___clazz = seed.@java.lang.Object::___clazz;
      seed.@java.lang.Object::___clazz = null;
    }
  }-*/;

  /**
   * Lookup seed function by id and instantiate an object with it.
   */
  public static native JavaScriptObject newSeed(int id) /*-{
    return new (@com.google.gwt.lang.SeedUtil::seedTable[id]);
  }-*/;
}
