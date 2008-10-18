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
   * Returns the results of calling <code>toString</code> in JavaScript on the
   * object, if the object implements toString; otherwise returns "[JavaScriptObject]".
   */
  @Override
  public final native String toString() /*-{
    return this.toString ? this.toString() : "[JavaScriptObject]";
  }-*/;
}
