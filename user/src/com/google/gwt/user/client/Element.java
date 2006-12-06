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
package com.google.gwt.user.client;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * An opaque handle to a native DOM Element. An <code>Element</code> cannot be
 * created directly. Instead, use the <code>Element</code> type when returning
 * a native DOM element from JSNI methods. An <code>Element</code> passed back
 * into JSNI becomes the original DOM element the <code>Element</code> was
 * created from, and can be accessed in JavaScript code as expected. This is
 * typically done by calling methods in the
 * {@link com.google.gwt.user.client.DOM} class.
 */
public final class Element extends JavaScriptObject {

  /**
   * Creates a new <code>Element</code>. This constructor is used internally
   * and should never be called by a user.
   * 
   * @param opaque the underlying DOM element
   */
  Element(int opaque) {
    super(opaque);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object other) {
    if (other instanceof Element) {
      return DOM.compare(this, (Element) other);
    }

    return super.equals(other);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return super.hashCode();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return DOM.toString(this);
  };
}
