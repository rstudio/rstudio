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
package com.google.gwt.user.client.ui;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.i18n.client.BidiUtils;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * A standard single-line text box.
 * 
 * <p>
 * <img class='gallery' src='TextBox.png'/>
 * </p>
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-TextBox { primary style }</li>
 * <li>.gwt-TextBox-readonly { dependent style set when the text box is read-only }</li>
 * </ul>
 * 
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.TextBoxExample}
 * </p>
 */
public class TextBox extends TextBoxBase implements HasDirection {

  /**
   * Creates a TextBox widget that wraps an existing &lt;input type='text'&gt;
   * element.
   * 
   * This element must already be attached to the document.
   * 
   * @param element the element to be wrapped
   */
  public static TextBox wrap(com.google.gwt.dom.client.Element element) {
    // Assert that the element is of the correct type and is attached.
    assert InputElement.as(element).getType().equalsIgnoreCase("text");
    assert Document.get().getBody().isOrHasChild(element);

    TextBox textBox = new TextBox((Element) element);

    // Mark it attached and remember it for cleanup.
    textBox.onAttach();
    RootPanel.detachOnWindowClose(textBox);

    return textBox;
  }

  /**
   * Creates an empty text box.
   */
  public TextBox() {
    super(DOM.createInputText());
    setStyleName("gwt-TextBox");
  }

  /**
   * Protected constructor for use by subclasses.
   * @param element element
   */
  TextBox(Element element) {
    super(element); 
  }

  public Direction getDirection() {
    return BidiUtils.getDirectionOnElement(getElement());    
  }
  
  /**
   * Gets the maximum allowable length of the text box.
   * 
   * @return the maximum length, in characters
   */
  public int getMaxLength() {
    return DOM.getElementPropertyInt(getElement(), "maxLength");
  }

  /**
   * Gets the number of visible characters in the text box.
   * 
   * @return the number of visible characters
   */
  public int getVisibleLength() {
    return DOM.getElementPropertyInt(getElement(), "size");
  }

  public void setDirection(Direction direction) {
    BidiUtils.setDirectionOnElement(getElement(), direction);
  }
    
  /**
   * Sets the maximum allowable length of the text box.
   * 
   * @param length the maximum length, in characters
   */
  public void setMaxLength(int length) {
    DOM.setElementPropertyInt(getElement(), "maxLength", length);
  }

  /**
   * Sets the number of visible characters in the text box.
   * 
   * @param length the number of visible characters
   */
  public void setVisibleLength(int length) {
    DOM.setElementPropertyInt(getElement(), "size", length);
  }
}
