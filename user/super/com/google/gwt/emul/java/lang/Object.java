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
package java.lang;

import com.google.gwt.core.client.JavaScriptObject;

import javaemul.internal.HashCodes;

/**
 * The superclass of all other types. The GWT emulation library supports a
 * limited subset of methods on <code>Object</code> due to browser
 * limitations. The methods documented here are the only ones available.
 */
public class Object {

  /**
   * Holds class literal for subtypes of Object.
   */
  // CHECKSTYLE_OFF
  private transient Class<?> ___clazz;
  // CHECKSTYLE_ON

  /**
   * Used by {@link com.google.gwt.core.client.impl.WeakMapping} in web mode
   * to store an expando containing a String -> Object mapping.
   *
   * @skip
   */
  @SuppressWarnings("unused")
  private transient JavaScriptObject expando;

  /**
   * A JavaScript Json map for looking up castability between types.
   *
   * @skip
   */
  @SuppressWarnings("unused")
  private transient JavaScriptObject castableTypeMap;

  /**
   * A special marker field used internally to the GWT compiler. For example, it
   * is used for distinguishing whether an object is a Java object or a
   * JavaScriptObject. It is also used to differentiate our own Java objects
   * from foreign objects in a different module on the same page.
   *
   * @see com.google.gwt.lang.Cast
   *
   * @skip
   */
  @SuppressWarnings("unused")
  private transient JavaScriptObject typeMarker;

  public boolean equals(Object other) {
    return this == other;
  }

  // TODO(rluble): declare as final
  public Class<? extends Object> getClass() {
    return ___clazz;
  }

  public int hashCode() {
    return HashCodes.getObjectIdentityHashCode(this);
  }

  public String toString() {
    return toString(this);
  }

  private static String toString(Object object) {
    return object.getClass().getName() + '@' + Integer.toHexString(object.hashCode());
  }
  /**
   * Never called; here for JRE compatibility.
   *
   * @skip
   */
  protected void finalize() throws Throwable {
  }
}
// DO NOT REMOVE OR MODIFY THIS magic COMMENT. It is checked by the build system to ensure we
// are compiling GWT JRE.