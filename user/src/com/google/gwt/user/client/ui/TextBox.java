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

/**
 * A standard single-line text box.
 * 
 * <p>
 * <img class='gallery' src='TextBox.png'/>
 * </p>
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-TextBox { }</li>
 * </ul>
 * 
 * <p>
 * <h3>Example</h3> {@example com.google.gwt.examples.TextBoxExample}
 * </p>
 */
public class TextBox extends TextBoxBase {

  /**
   * Creates an empty text box.
   */
  public TextBox() {
    super(DOM.createInputText());
    setStyleName("gwt-TextBox");
  }

  /**
   * Gets the maximum allowable length of the text box.
   * 
   * @return the maximum length, in characters
   */
  public int getMaxLength() {
    return DOM.getIntAttribute(getElement(), "maxLength");
  }

  /**
   * Gets the number of visible characters in the text box.
   * 
   * @return the number of visible characters
   */
  public int getVisibleLength() {
    return DOM.getIntAttribute(getElement(), "size");
  }

  /**
   * Sets the maximum allowable length of the text box.
   * 
   * @param length the maximum length, in characters
   */
  public void setMaxLength(int length) {
    DOM.setIntAttribute(getElement(), "maxLength", length);
  }

  /**
   * Sets the number of visible characters in the text box.
   * 
   * @param length the number of visible characters
   */
  public void setVisibleLength(int length) {
    DOM.setIntAttribute(getElement(), "size", length);
  }
}
