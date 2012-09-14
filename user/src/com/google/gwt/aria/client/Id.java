/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.gwt.aria.client;
/////////////////////////////////////////////////////////
// This is auto-generated code.  Do not manually edit! //
/////////////////////////////////////////////////////////

import com.google.gwt.dom.client.Element;

/**
 * Id reference attribute type
 */
public class Id implements AriaAttributeType {
  /**
   * Creates an Id instance for the {@code element} by getting
   * the element 'id' attribute.
   *
   * @param element A DOM element which should have a
   *        non empty, unique 'id' attribute set.
   */
  public static Id of(Element element) {
    return new Id(element);
  }

  /**
   * Creates an Id instance from the {@code elementId}.
   *
   * @param elementId A string identifier that should correspond
   *        to the 'id' attribute value of a DOM element.
   */
  public static Id of(String elementId) {
    return new Id(elementId);
  }

  private String id;

  /**
   * An instance of {@link Id} is created.
   *
   * @param element Element with a unique id value set
   */
  private Id(Element element) {
    assert element != null : "Element cannot be null";
    init(element.getId());
  }

  private Id(String elementId) {
    init(elementId);
  }

  @Override
  public String getAriaValue() {
    return id;
  }

  private void init(String elementId) {
    assert elementId != null || elementId.equals("") :
      "Invalid elementId: cannot be null or empty.";
    this.id = elementId;
  }
}
