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

package com.google.gwt.user.client.ui;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * Represents a hidden field in an HTML form.
 */
public class Hidden extends Widget implements HasName {
  /**
   * Constructor for <code>Hidden</code>.
   */
  public Hidden() {
    Element e = DOM.createElement("input");
    setElement(e);
    DOM.setAttribute(e, "type", "hidden");
  }

  /**
   * Constructor for <code>Hidden</code>.
   * 
   * @param name name of the hidden field
   */
  public Hidden(String name) {
    this();
    setName(name);
  }

  /**
   * Constructor for <code>Hidden</code>.
   * 
   * @param name name of the hidden field
   * @param value value of the hidden field
   */
  public Hidden(String name, String value) {
    this(name);
    setValue(value);
  }

  /**
   * Gets the default value of the hidden field.
   * 
   * @return the default value
   */
  public String getDefaultValue() {
    return DOM.getAttribute(getElement(), "defaultValue");
  }

  /**
   * Gets the id of the hidden field.
   * 
   * @return the id
   */
  public String getID() {
    return DOM.getAttribute(getElement(), "id");
  }

  /**
   * Gets the name of the hidden field.
   * 
   * @return the name
   */

  public String getName() {
    return DOM.getAttribute(getElement(), "name");
  }

  /**
   * Gets the value of the hidden field.
   * 
   * @return the value
   */
  public String getValue() {
    return DOM.getAttribute(getElement(), "value");
  }

  /**
   * Sets the default value of the hidden field.
   * 
   * @param defaultValue default value to set
   */
  public void setDefaultValue(String defaultValue) {
    DOM.setAttribute(getElement(), "defaultValue", defaultValue);
  }

  /**
   * Sets the id of the hidden field.
   * 
   * @param id id to set
   */
  public void setID(String id) {
    DOM.setAttribute(getElement(), "id", id);
  }

  /**
   * Sets the name of the hidden field.
   * 
   * @param name name of the field
   */
  public void setName(String name) {
    if (name == null) {
      throw new NullPointerException("Name cannot be null");
    } else if (name.equals("")) {
      throw new IllegalArgumentException("Name cannot be an empty string.");
    }
    DOM.setAttribute(getElement(), "name", name);
  }

  /**
   * Sets the value of the hidden field.
   * 
   * @param value value to set
   */
  public void setValue(String value) {
    DOM.setAttribute(getElement(), "value", value);
  }

}
