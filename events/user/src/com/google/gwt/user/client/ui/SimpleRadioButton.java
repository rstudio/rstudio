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
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;

/**
 * A simple radio button widget, with no label.
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-SimpleRadioButton { }</li>
 * <li>.gwt-SimpleRadioButton-disabled { Applied when radio button is disabled }</li>
 * </ul>
 */
public class SimpleRadioButton extends SimpleCheckBox {

  /**
   * Creates a SimpleRadioButton widget that wraps an existing &lt;input
   * type='radio'&gt; element.
   * 
   * This element must already be attached to the document. If the element is
   * removed from the document, you must call
   * {@link RootPanel#detachNow(Widget)}.
   * 
   * @param element the element to be wrapped
   */
  public static SimpleRadioButton wrap(Element element) {
    // Assert that the element is attached.
    assert Document.get().getBody().isOrHasChild(element);

    SimpleRadioButton radioButton = new SimpleRadioButton(element);

    // Mark it attached and remember it for cleanup.
    radioButton.onAttach();
    RootPanel.detachOnWindowClose(radioButton);

    return radioButton;
  }

  /**
   * Creates a new radio associated with a particular group name. All radio
   * buttons associated with the same group name belong to a mutually-exclusive
   * set.
   * 
   * Radio buttons are grouped by their name attribute, so changing their name
   * using the setName() method will also change their associated group.
   * 
   * @param name the group name with which to associate the radio button
   */
  public SimpleRadioButton(String name) {
    super(Document.get().createRadioInputElement(name), "gwt-SimpleRadioButton");
  }

  /**
   * This constructor may be used by subclasses to explicitly use an existing
   * element. This element must be an &lt;input&gt; element whose type is
   * 'radio'.
   * 
   * @param element the element to be used
   */
  protected SimpleRadioButton(Element element) {
    super(element, null);
    assert InputElement.as(element).getType().equalsIgnoreCase("radio");
  }
}
