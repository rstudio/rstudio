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
import com.google.gwt.core.client.impl.Impl;

/**
 * The superclass of all other types. The GWT emulation library supports a
 * limited subset of methods on <code>Object</code> due to browser
 * limitations. The methods documented here are the only ones available.
 */
public class Object {

  /**
   * Used by {@link com.google.gwt.core.client.impl.WeakMapping} in web mode
   * to store an expando containing a String -> Object mapping.
   * 
   * @skip
   */
  @SuppressWarnings("unused")
  private transient JavaScriptObject expando;

  /**
   * magic magic magic.
   * 
   * @skip
   */
  @SuppressWarnings("unused")
  private transient int typeId;
  
  /**
   * A JavaScript Json map for looking up castability between types.
   * 
   * @skip
   */
  @SuppressWarnings("unused")
  private transient JavaScriptObject castableTypeMap;

  /**
   * magic magic magic.
   * 
   * @skip
   */
  @SuppressWarnings("unused")
  private transient JavaScriptObject typeMarker;

  public boolean equals(Object other) {
    return this == other;
  }

  /*
   * Magic; unlike the real JRE, we don't spec this method as final. The
   * compiler will generate a polymorphic override on every other class which
   * will return the correct class object.
   * 
   * TODO(scottb): declare this final, but have the compiler fix it up.
   */
  public Class<? extends Object> getClass() {
    return Object.class;
  }

  public int hashCode() {
    return Impl.getHashCode(this);
  }

  public String toString() {
    return getClass().getName() + '@' + Integer.toHexString(hashCode());
  }

  /**
   * Never called; here for compatibility.
   * 
   * @skip
   */
  protected void finalize() throws Throwable {
  }
}
