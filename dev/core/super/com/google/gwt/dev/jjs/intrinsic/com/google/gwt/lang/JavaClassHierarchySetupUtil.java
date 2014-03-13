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
 * Utility class for defining class prototyes to setup an equivalent to the Java class hierarchy in
 * JavaScript.
 */
public class JavaClassHierarchySetupUtil {
  /*
  * Holds a map from typeIds to prototype objects.
  */
  private static JavaScriptObject prototypesByTypeId = JavaScriptObject.createObject();

  /**
   * If not already created it creates the prototype for the class and stores it in
   * {@code prototypesByTypeId}. If superTypeId is null, it means that the class being defined
   * is the topmost class (i.e. java.lang.Object) and creates an empty prototype for it.
   * Otherwise it creates the prototype for the class by calling {@code createSubclassPrototype()}.
   * It also assigns the castable type map and sets the constructors prototype field to the
   * current prototype.
   * Finally adds the class literal if it was created before the call to {@code defineClass}.
   * Class literals might be created before the call to {@code defineClass} if they are in separate
   * code-split fragments. In that case Class.createFor* methods will have created a placeholder and
   * stored in {@code prototypesByTypeId} the class literal.<p></p>
   *
   * As a prerequisite if superSeed is not null, it is assumed that defineClass for the supertype
   * has already been called.
   */
  public static native JavaScriptObject defineClass(JavaScriptObject typeId,
      JavaScriptObject superTypeId, JavaScriptObject castableTypeMap) /*-{
    // Setup aliases for (horribly long) JSNI references.
    var prototypesByTypeId = @com.google.gwt.lang.JavaClassHierarchySetupUtil::prototypesByTypeId;
    var createSubclassPrototype =
        @com.google.gwt.lang.JavaClassHierarchySetupUtil::createSubclassPrototype(*)
    var maybeGetClassLiteralFromPlaceHolder =  @com.google.gwt.lang.JavaClassHierarchySetupUtil::
        maybeGetClassLiteralFromPlaceHolder(*);
    // end of alias definitions.

    var prototype = prototypesByTypeId[typeId];
    var clazz = maybeGetClassLiteralFromPlaceHolder(prototype);
    if (prototype && !clazz) {
      // not a placeholder entry setup by Class.setClassLiteral
      _ = prototype;
    } else {
      _ = prototypesByTypeId[typeId]  = (!superTypeId) ? {} : createSubclassPrototype(superTypeId);
      _.@java.lang.Object::castableTypeMap = castableTypeMap;
    }
    for (var i = 3; i < arguments.length; ++i) {
      // Assign the type prototype to each constructor.
      arguments[i].prototype = _;
    }
    if (clazz) {
      _.@java.lang.Object::___clazz = clazz;
    }
  }-*/;

  /**
   * Create a subclass prototype.
   */
  public static native JavaScriptObject createSubclassPrototype(JavaScriptObject superTypeId) /*-{
    // Setup aliases for (horribly long) JSNI references.
    var prototypesByTypeId = @com.google.gwt.lang.JavaClassHierarchySetupUtil::prototypesByTypeId;
    // end of alias definitions.

    // Don't name it just constructor as it does not work!
    var constructorFn = function() {}
    constructorFn.prototype = prototypesByTypeId[superTypeId];
    return new constructorFn();
  }-*/;

  /**
   * Retrieves the class literal if stored in a place holder, {@code null} otherwise.
   */
  private static native JavaScriptObject maybeGetClassLiteralFromPlaceHolder(
      JavaScriptObject entry) /*-{
    // TODO(rluble): Relies on Class.createFor*() storing the class literal wrapped as an array
    // to distinguish it from an actual prototype.
    return (entry instanceof Array) ? entry[0] : null;
  }-*/;

  /**
   * Do polyfills for all methods expected in a modern browser.
   */
  public static native void modernizeBrowser() /*-{
    // Patch up Array.isArray for browsers that don't support the fast native check.
    if (!Array.isArray) {
        Array.isArray = function (vArg) {
          return Object.prototype.toString.call(vArg) === "[object Array]";
        };
    }
  }-*/;

  /**
   * Retrieves the prototype for a type if it exists, null otherwise.
   */
  public static native JavaScriptObject getClassPrototype(JavaScriptObject typeId) /*-{
    var prototypeForTypeId =
        @com.google.gwt.lang.JavaClassHierarchySetupUtil::prototypesByTypeId[typeId];
    return prototypeForTypeId;
  }-*/;
}
