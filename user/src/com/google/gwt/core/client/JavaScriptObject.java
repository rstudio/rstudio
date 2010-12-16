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
package com.google.gwt.core.client;

import com.google.gwt.core.client.impl.Impl;

/**
 * An opaque handle to a native JavaScript object. A
 * <code>JavaScriptObject</code> cannot be created directly.
 * <code>JavaScriptObject</code> should be declared as the return type of a
 * JSNI method that returns native (non-Java) objects. A
 * <code>JavaScriptObject</code> passed back into JSNI from Java becomes the
 * original object, and can be accessed in JavaScript as expected.
 */
public class JavaScriptObject {

  /**
   * Returns a new array.
   */
  public static native JavaScriptObject createArray() /*-{
    return [];
  }-*/;

  /**
   * Returns an empty function.
   */
  public static native JavaScriptObject createFunction() /*-{
    return function() {
    };
  }-*/;

  /**
   * Returns a new object.
   */
  public static native JavaScriptObject createObject() /*-{
    return {};
  }-*/;

  /**
   * Helper for {@link #toString()}, for lighter "more production" code.
   */
  private static native String toStringSimple(JavaScriptObject obj) /*-{
    return obj.toString ? obj.toString() : '[JavaScriptObject]';
  }-*/;

  /**
   * Helper for {@link #toString()}, when Development Mode or assertions are on.
   */
  private static native String toStringVerbose(JavaScriptObject obj) /*-{
    var defined = function(m) { return typeof m != 'undefined'; };
    var strip = function(s) { return s.replace(/\r\n/g, ""); };
    // Output nodes that have outerHTML
    if (defined(obj.outerHTML))
      return strip(obj.outerHTML);
    // Output nodes that have innerHTML
    if (defined(obj.innerHTML) && obj.cloneNode) {
      $doc.createElement('div').appendChild(obj.cloneNode(true)).innerHTML;
    }
    // Output text nodes
    if (defined(obj.nodeType) && obj.nodeType == 3) {
      return "'" +
        obj.data.replace(/ /g, "\u25ab").replace(/\u00A0/, "\u25aa") +
        "'";
    }
    // Output IE's TextRange (this code specific to IE7)
    if (typeof defined(obj.htmlText) && obj.collapse) {
      var html = obj.htmlText;
      if (html) {
        return 'IETextRange [' + strip(html) + ']';
      } else {
        // NOTE: using pasteHTML to place a | where the range is collapsed
        // if *very* useful when debugging. It also, however, in certain very
        // subtle circumstances change the range being toStringed! If you
        // see different behaviour in debug vs. release builds (or if logging
        // ranges changes the behaviour, comment out the 4 of the 6 lines
        // below containing dup.
        var dup = obj.duplicate();
        dup.pasteHTML('|');
        var out = 'IETextRange ' + strip(obj.parentElement().outerHTML);
        dup.moveStart('character', -1);
        dup.pasteHTML('');
        return out;
      }
    }
    return obj.toString ? obj.toString() : '[JavaScriptObject]';
  }-*/;

  /**
   * Not directly instantiable. All subclasses must also define a protected,
   * empty, no-arg constructor.
   */
  protected JavaScriptObject() {
  }

  /**
   * A helper method to enable cross-casting from any {@link JavaScriptObject}
   * type to any other {@link JavaScriptObject} type.
   *
   * @param <T> the target type
   * @return this object as a different type
   */
  @SuppressWarnings("unchecked")
  public final <T extends JavaScriptObject> T cast() {
    return (T) this;
  }

  /**
   * Returns <code>true</code> if the objects are JavaScript identical
   * (triple-equals).
   */
  @Override
  public final boolean equals(Object other) {
    return super.equals(other);
  }

  /**
   * Uses a monotonically increasing counter to assign a hash code to the
   * underlying JavaScript object. Do not call this method on non-modifiable
   * JavaScript objects.
   *
   * TODO: if the underlying object defines a 'hashCode' method maybe use that?
   *
   * @return the hash code of the object
   */
  @Override
  public final int hashCode() {
    return Impl.getHashCode(this);
  }

  /**
   * Call the toSource() on the JSO.
   */
  public native String toSource() /*-{
    this.toSource ? this.toSource() : "NO SOURCE";
  }-*/;

  /**
   * Makes a best-effort attempt to get a useful debugging string describing the
   * given JavaScriptObject. In Production Mode with assertions disabled, this
   * will either call and return the JSO's toString() if one exists, or just
   * return "[JavaScriptObject]". In Development Mode, or with assertions
   * enabled, some stronger effort is made to represent other types of JSOs,
   * including inspecting for document nodes' outerHTML and innerHTML, etc.
   */
  @Override
  public final String toString() {
    return JavaScriptObject.class.desiredAssertionStatus() ?
        toStringVerbose(this) : toStringSimple(this);
  }
}
