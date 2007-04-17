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
 * A standard check box widget (also serves as a base class for
 * {@link com.google.gwt.user.client.ui.RadioButton}.
 * <p>
 * <img class='gallery' src='CheckBox.png'/>
 * </p>
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-CheckBox { }</li>
 * </ul>
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.CheckBoxExample}
 * </p>
 */
public class CheckBox extends ButtonBase implements HasName {
  private static int uniqueId;
  private Element inputElem, labelElem;

  /**
   * Creates a check box with no label.
   */
  public CheckBox() {
    this(DOM.createInputCheck());
    setStyleName("gwt-CheckBox");
  }

  /**
   * Creates a check box with the specified text label.
   * 
   * @param label the check box's label
   */
  public CheckBox(String label) {
    this();
    setText(label);
  }

  /**
   * Creates a check box with the specified text label.
   * 
   * @param label the check box's label
   * @param asHTML <code>true</code> to treat the specified label as html
   */
  public CheckBox(String label, boolean asHTML) {
    this();
    if (asHTML) {
      setHTML(label);
    } else {
      setText(label);
    }
  }

  protected CheckBox(Element elem) {
    super(DOM.createSpan());
    inputElem = elem;
    labelElem = DOM.createLabel();

    // Hook events to input widget rather than the check box element.
    DOM.sinkEvents(inputElem, DOM.getEventsSunk(this.getElement()));
    DOM.sinkEvents(this.getElement(), 0);
    DOM.appendChild(getElement(), inputElem);
    DOM.appendChild(getElement(), labelElem);

    String uid = "check" + (++uniqueId);
    DOM.setElementProperty(inputElem, "id", uid);
    DOM.setElementProperty(labelElem, "htmlFor", uid);
  }

  public String getHTML() {
    return DOM.getInnerHTML(labelElem);
  }

  public String getName() {
    return DOM.getElementProperty(inputElem, "name");
  }

  public String getText() {
    return DOM.getInnerText(labelElem);
  }

  /**
   * Determines whether this check box is currently checked.
   * 
   * @return <code>true</code> if the check box is checked
   */
  public boolean isChecked() {
    String propName = isAttached() ? "checked" : "defaultChecked";
    return DOM.getElementPropertyBoolean(inputElem, propName);
  }

  public boolean isEnabled() {
    return !DOM.getElementPropertyBoolean(inputElem, "disabled");
  }

  public void setAccessKey(char key) {
    DOM.setElementProperty(inputElem, "accessKey", "" + key);
  }

  /**
   * Checks or unchecks this check box.
   * 
   * @param checked <code>true</code> to check the check box
   */
  public void setChecked(boolean checked) {
    DOM.setElementPropertyBoolean(inputElem, "checked", checked);
    DOM.setElementPropertyBoolean(inputElem, "defaultChecked", checked);
  }

  public void setEnabled(boolean enabled) {
    DOM.setElementPropertyBoolean(inputElem, "disabled", !enabled);
  }

  public void setFocus(boolean focused) {
    if (focused) {
      getFocusImpl().focus(inputElem);
    } else {
      getFocusImpl().blur(inputElem);
    }
  }

  public void setHTML(String html) {
    DOM.setInnerHTML(labelElem, html);
  }

  public void setName(String name) {
    DOM.setElementProperty(inputElem, "name", name);
  }

  public void setTabIndex(int index) {
    getFocusImpl().setTabIndex(inputElem, index);
  }

  public void setText(String text) {
    DOM.setInnerText(labelElem, text);
  }

  /**
   * This method is called when a widget is attached to the browser's document.
   * onAttach needs special handling for the CheckBox case. Must still call
   * {@link Widget#onAttach()} to preserve the <code>onAttach</code> contract.
   */
  protected void onAttach() {
    // Sets the event listener on the inputElem, as in this case that's the
    // element we want so input on.
    DOM.setEventListener(inputElem, this);
    super.onAttach();
  }

  /**
   * This method is called when a widget is detached from the browser's
   * document. Overridden because of IE bug that throws away checked state and
   * in order to clear the event listener off of the <code>inputElem</code>.
   */
  protected void onDetach() {
    // Clear out the inputElem's event listener (breaking the circular
    // reference between it and the widget).
    DOM.setEventListener(inputElem, null);
    setChecked(isChecked());
    super.onDetach();
  }
}
