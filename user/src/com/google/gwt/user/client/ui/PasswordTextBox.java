/*
 * Copyright 2007 Google Inc.
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
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * A text box that visually masks its input to prevent eavesdropping.
 * 
 * <p>
 * <img class='gallery' src='PasswordTextBox.png'/>
 * </p>
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-PasswordTextBox { primary style }</li>
 * <li>.gwt-PasswordTextBox-readonly { dependent style set when the password text box is read-only }</li>
 * </ul>
 * 
 * <p>
 * <h3>Example</h3> {@example com.google.gwt.examples.TextBoxExample}
 * </p>
 */
public class PasswordTextBox extends TextBox {

  /**
   * Creates a PasswordTextBox widget that wraps an existing &lt;input
   * type='password'&gt; element.
   * 
   * This element must already be attached to the document.
   * 
   * @param element the element to be wrapped
   */
  public static PasswordTextBox wrap(com.google.gwt.dom.client.Element element) {
    // Assert that the element is of the correct type and is attached.
    assert InputElement.as(element).getType().equalsIgnoreCase("password");
    assert Document.get().getBody().isOrHasChild(element);

    PasswordTextBox textBox = new PasswordTextBox((Element) element);

    // Mark it attached and remember it for cleanup.
    textBox.onAttach();
    RootPanel.detachOnWindowClose(textBox);

    return textBox;
  }

  /**
   * Creates an empty password text box.
   */
  public PasswordTextBox() {
    super(DOM.createInputPassword());
    setStyleName("gwt-PasswordTextBox");
  }

  private PasswordTextBox(Element element) {
    super(element);
  }
}
