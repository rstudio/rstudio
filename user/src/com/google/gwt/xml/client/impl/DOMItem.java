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
package com.google.gwt.xml.client.impl;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.DOM;

/**
 * This class is the base class for all DOM object wrappers.
 */
class DOMItem {

  private JavaScriptObject jsObject;

  protected DOMItem(JavaScriptObject jso) {
    this.jsObject = jso;
  }

  /**
   * This method determines equality for DOMItems.
   * 
   * @param o - the other object being tested for equality
   * @return true iff the two objects are equal.
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(final Object o) {
    /*
     * This method uses the DOM equals method because it happens to work
     * perfectly for all the browsers we support, and that method is different
     * for each browser.
     */
    if (o instanceof DOMItem) {
      return DOM.compare(castToElement(this.getJsObject()),
          castToElement(((DOMItem) o).getJsObject()));
    }
    return false;
  }

  JavaScriptObject getJsObject() {
    return jsObject;
  }

  private native com.google.gwt.user.client.Element castToElement(
      JavaScriptObject toBeCast) /*-{
    return toBeCast;
  }-*/;
}
