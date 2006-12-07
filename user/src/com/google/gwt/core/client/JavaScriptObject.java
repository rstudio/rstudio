/*
 * Copyright 2006 Google Inc.
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

/**
 * An opaque handle to a native JavaScript object. A
 * <code>JavaScriptObject</code> cannot be created directly.
 * <code>JavaScriptObject</code> should be declared as the return type of a
 * JSNI method that returns native (non-Java) objects. A
 * <code>JavaScriptObject</code> passed back into JSNI from Java becomes the
 * original object, and can be accessed in JavaScript as expected.
 * 
 * <p>
 * <b>SUBCLASSING IS NOT SUPPORTED EXCEPT FOR THE EXISTING SUBCLASSES.</b>
 * </p>
 */
public class JavaScriptObject {

  private static native boolean equalsImpl(JavaScriptObject o,
      JavaScriptObject other) /*-{
    return o === other;
  }-*/;

  private static native String toStringImpl(JavaScriptObject o) /*-{
    if (o.toString)
      return o.toString();
    return "[object]";
  }-*/;

  /**
   * the underlying JavaScript object.
   */
  protected final int opaque;

  /**
   * Creates a new <code>JavaScriptObject</code>. This constructor is used
   * internally and should never be called by a user.
   * 
   * @param opaque the underlying JavaScript object
   */
  protected JavaScriptObject(int opaque) {
    this.opaque = opaque;
  }

  public boolean equals(Object other) {
    if (!(other instanceof JavaScriptObject)) {
      return false;
    }
    return equalsImpl(this, (JavaScriptObject) other);
  };

  public int hashCode() {
    return Impl.getHashCode(this);
  }

  public String toString() {
    /*
     * Hosted mode will marshal an explicit argument from a JavaScriptObject
     * back to its underlying object, but it won't currently do that for the
     * implicit "this" arg. For now, can't implement instance methods on JSO
     * directly as natives, so use a delegator.
     */
    return toStringImpl(this);
  }
}
