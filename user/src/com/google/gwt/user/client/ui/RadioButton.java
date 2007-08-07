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
 * A mutually-exclusive selection radio button widget.
 * 
 * <p>
 * <img class='gallery' src='RadioButton.png'/>
 * </p>
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-RadioButton { }</li>
 * </ul>
 * 
 * <p>
 * <h3>Example</h3> {@example com.google.gwt.examples.RadioButtonExample}
 * </p>
 */
public class RadioButton extends CheckBox {

  /**
   * Creates a new radio associated with a particular group name. All radio
   * buttons associated with the same group name belong to a mutually-exclusive
   * set.
   * 
   * Radio buttons are grouped by their name attribute, so changing their
   * name using the setName() method will also change their associated
   * group.
   * 
   * @param name the group name with which to associate the radio button
   */
  public RadioButton(String name) {
    super(DOM.createInputRadio(name));
    setStyleName("gwt-RadioButton");
  }

  /**
   * Creates a new radio associated with a particular group, and initialized
   * with the given HTML label. All radio buttons associated with the same group
   * name belong to a mutually-exclusive set.
   * 
   * Radio buttons are grouped by their name attribute, so changing their
   * name using the setName() method will also change their associated
   * group.
   * 
   * @param name the group name with which to associate the radio button
   * @param label this radio button's label
   */
  public RadioButton(String name, String label) {
    this(name);
    setText(label);
  }

  /**
   * Creates a new radio button associated with a particular group, and
   * initialized with the given label (optionally treated as HTML). All radio
   * buttons associated with the same group name belong to a mutually-exclusive
   * set.
   * 
   * Radio buttons are grouped by their name attribute, so changing their
   * name using the setName() method will also change their associated
   * group.
   * 
   * @param name name the group with which to associate the radio button
   * @param label this radio button's label
   * @param asHTML <code>true</code> to treat the specified label as HTML
   */
  public RadioButton(String name, String label, boolean asHTML) {
    this(name);
    if (asHTML) {
      setHTML(label);
    } else {
      setText(label);
    }
  }

  /**
   * Change the group name of this radio button.
   * 
   * Radio buttons are grouped by their name attribute, so changing their
   * name using the setName() method will also change their associated
   * group.
   * 
   * If changing this group name results in a new radio group with
   * multiple radio buttons selected, this radio button will remain
   * selected and the other radio buttons will be unselected.
   *  
   * @param name name the group with which to associate the radio button
   */
  public void setName(String name) {
    super.replaceInputElement(DOM.createInputRadio(name));
  }
}
