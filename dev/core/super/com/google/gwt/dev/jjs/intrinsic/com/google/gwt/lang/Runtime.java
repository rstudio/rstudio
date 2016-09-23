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

import javaemul.internal.annotations.ForceInline;

/**
 * Utility class that provides methods for dealing with the runtime representation of Java classes
 * and bootstraping code.
 */
public class Runtime {
  /**
   * Holds a map from typeIds to prototype objects.
   */
  private static JavaScriptObject prototypesByTypeId;

  /**
   * If not already created it creates the prototype for the class and stores it in
   * {@code prototypesByTypeId}. If superTypeIdOrPrototype is null, it means that the class being
   * defined is the topmost class (i.e. java.lang.Object) and creates an empty prototype for it.
   * Otherwise it creates the prototype for the class by calling {@code createSubclassPrototype()}.
   * It also assigns the castable type map and sets the constructors prototype field to the
   * current prototype.
   * Finally adds the class literal if it was created before the call to {@code defineClass}.
   * Class literals might be created before the call to {@code defineClass} if they are in separate
   * code-split fragments. In that case Class.createFor* methods will have created a placeholder and
   * stored in {@code prototypesByTypeId} the class literal.
   * <p>
   * As a prerequisite if superTypeIdOrPrototype is not null, it is assumed that defineClass for the
   * supertype has already been called.
   * <p>
   * This method has the effect of assigning the newly created prototype to the global temp variable
   * '_'.
   */
  public static native void defineClass(JavaScriptObject typeId,
      JavaScriptObject superTypeIdOrPrototype, JavaScriptObject castableTypeMap) /*-{
    // Setup aliases for (horribly long) JSNI references.
    var prototypesByTypeId = @Runtime::prototypesByTypeId;
    // end of alias definitions.

    var prototype = prototypesByTypeId[typeId];
    var clazz = @Runtime::maybeGetClassLiteralFromPlaceHolder(*)(prototype);
    if (prototype && !clazz) {
      // not a placeholder entry setup by Class.setClassLiteral
      _ = prototype;
    } else {
      _ = @Runtime::createSubclassPrototype(*)(superTypeIdOrPrototype);
      _.@Object::castableTypeMap = castableTypeMap;
      _.constructor = _;
      if (!superTypeIdOrPrototype) {
        // Set the typeMarker on java.lang.Object's prototype, implicitly setting it for all
        // Java subclasses (String and Arrays have special handling in Cast and Array respectively).
        _.@Object::typeMarker = @Runtime::typeMarkerFn(*);
      }
      prototypesByTypeId[typeId] = _;
    }
    for (var i = 3; i < arguments.length; ++i) {
      // Assign the type prototype to each constructor.
      arguments[i].prototype = _;
    }
    if (clazz) {
      _.@Object::___clazz = clazz;
    }
  }-*/;

  private static native JavaScriptObject portableObjCreate(JavaScriptObject obj) /*-{
    function F() {};
    F.prototype = obj || {};
    return new F();
  }-*/;

  /**
   * Create a subclass prototype.
   */
  private static native JavaScriptObject createSubclassPrototype(
      JavaScriptObject superTypeIdOrPrototype) /*-{
    var superPrototype = superTypeIdOrPrototype && superTypeIdOrPrototype.prototype;
    if (!superPrototype) {
      // If it is not a prototype, then it should be a type id.
      superPrototype = @Runtime::prototypesByTypeId[superTypeIdOrPrototype];
    }
    return @Runtime::portableObjCreate(*)(superPrototype);
  }-*/;

  public static native void copyObjectProperties(JavaScriptObject from, JavaScriptObject to) /*-{
    for (var property in from) {
      if (to[property] === undefined) {
        to[property] = from[property];
      }
    }
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
   * Creates a JS namespace to attach exported classes to.
   * @param namespace a dotted js namespace string
   * @return a nested object literal representing the namespace
   */
  public static native JavaScriptObject provide(JavaScriptObject namespace,
      JavaScriptObject optCtor) /*-{
    var cur = $wnd;

    if (namespace === '') {
        return cur;
    }

    // borrowed from Closure's base.js
    var parts = namespace.split('.');

    // Internet Explorer exhibits strange behavior when throwing errors from
    // methods externed in this manner.  See the testExportSymbolExceptions in
    // base_test.html for an example.
    if (!(parts[0] in cur) && cur.execScript) {
        cur.execScript('var ' + parts[0]);
    }

    // Certain browsers cannot parse code in the form for((a in b); c;);
    // This pattern is produced by the JSCompiler when it collapses the
    // statement above into the conditional loop below. To prevent this from
    // happening, use a for-loop and reserve the init logic as below.

    // Parentheses added to eliminate strict JS warning in Firefox.
    for (var part; parts.length && (part = parts.shift());) {
        // assign the constructor to the last node (when parts is empty)
        cur = cur[part] = cur[part] || !parts.length && optCtor || {};
    }
    return cur;
  }-*/;

  /**
   * Create a function that applies the specified samMethod on itself, and whose __proto__ points to
   * <code>instance</code>.
   */
  public static native JavaScriptObject makeLambdaFunction(
      JavaScriptObject samMethod,
      JavaScriptObject ctor,
      JavaScriptObject ctorArguments) /*-{
    var lambda = function() { return samMethod.apply(lambda, arguments); }
    ctor.apply(lambda, ctorArguments);
    return lambda;
  }-*/;

  /**
   * Called at the beginning to setup required structures before any of the classes is defined.
   * The code written in and invoked by this method should be plain JavaScript.
   */
  public static native void bootstrap() /*-{
    @Runtime::prototypesByTypeId = {};

    // Do polyfills for all methods expected in a modern browser.
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
    return @Runtime::prototypesByTypeId[typeId];
  }-*/;

  /**
   * Marker function. All Java Objects (except Strings) have a typeMarker field pointing to
   * this function.
   */
  static native void typeMarkerFn() /*-{
  }-*/;

  /**
   * A global noop function. Replaces clinits after execution.
   */
  static native void emptyMethod() /*-{
  }-*/;

  @ForceInline
  static native JavaScriptObject uniqueId(String id) /*-{
    return jsinterop.closure.getUniqueId(id);
  }-*/;

  static native void defineProperties(
      JavaScriptObject proto, JavaScriptObject propertyDefinition) /*-{
    for (var key in propertyDefinition) {
      propertyDefinition[key]['configurable'] = true;
    }
    Object.defineProperties(proto,  propertyDefinition);
  }-*/;

  public static native String toString(Object object) /*-{
    if (Array.isArray(object) && @Util::hasTypeMarker(*)(object)) {
       return @Object::toString(Ljava/lang/Object;)(object);
    }
    return object.toString();
  }-*/;
}
