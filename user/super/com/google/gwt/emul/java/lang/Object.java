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
package java.lang;

/**
 * The superclass of all other types. The GWT emulation library supports a
 * limited subset of methods on <code>Object</code> due to browser
 * limitations. The methods documented here are the only ones available.
 */
public class Object {

  /**
   * magic magic magic.
   * 
   * @skip
   */
  protected transient int typeId;

  /**
   * magic magic magic.
   * 
   * @skip
   */
  protected transient String typeName;

  public native boolean equals(Object other) /*-{
    return this === other;
  }-*/;

  public int hashCode() {
    return System.identityHashCode(this);
  }

  public String toString() {
    return typeName + "@" + hashCode();
  }

  /**
   * Never called.
   * 
   * @skip
   */
  protected void finalize() throws Throwable {
  }
}
