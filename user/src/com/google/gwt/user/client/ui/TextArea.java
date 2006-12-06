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
 * A text box that allows multiple lines of text to be entered.
 * 
 * <p>
 * <img class='gallery' src='TextArea.png'/>
 * </p>
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-TextArea { }</li>
 * </ul>
 * 
 * <p>
 * <h3>Example</h3> {@example com.google.gwt.examples.TextBoxExample}
 * </p>
 */
public class TextArea extends TextBoxBase {

  /**
   * Creates an empty text area.
   */
  public TextArea() {
    super(DOM.createTextArea());
    setStyleName("gwt-TextArea");
  }

  /**
   * Gets the requested width of the text box (this is not an exact value, as
   * not all characters are created equal).
   * 
   * @return the requested width, in characters
   */
  public int getCharacterWidth() {
    return DOM.getIntAttribute(getElement(), "cols");
  }

  public int getCursorPos() {
    return getImpl().getTextAreaCursorPos(getElement());
  }

  public int getSelectionLength() {
    return getImpl().getSelectionLength(getElement());
  }

  /**
   * Gets the number of text lines that are visible.
   * 
   * @return the number of visible lines
   */
  public int getVisibleLines() {
    return DOM.getIntAttribute(getElement(), "rows");
  }

  /**
   * Sets the requested width of the text box (this is not an exact value, as
   * not all characters are created equal).
   * 
   * @param width the requested width, in characters
   */
  public void setCharacterWidth(int width) {
    DOM.setIntAttribute(getElement(), "cols", width);
  }

  /**
   * Sets the number of text lines that are visible.
   * 
   * @param lines the number of visible lines
   */
  public void setVisibleLines(int lines) {
    DOM.setIntAttribute(getElement(), "rows", lines);
  }
}
