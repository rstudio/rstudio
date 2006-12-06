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
import com.google.gwt.user.client.Event;

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

    // Add focus event to actual input widget. Required by Opera and old
    // Mozilla.
    unsinkEvents(Event.FOCUSEVENTS | Event.ONCLICK);
    DOM.sinkEvents(inputElem, Event.FOCUSEVENTS | Event.ONCLICK
      | DOM.getEventsSunk(inputElem));

    DOM.appendChild(getElement(), inputElem);
    DOM.appendChild(getElement(), labelElem);

    String uid = "check" + (++uniqueId);
    DOM.setAttribute(inputElem, "id", uid);
    DOM.setAttribute(labelElem, "htmlFor", uid);
  }

  public String getHTML() {
    return DOM.getInnerHTML(labelElem);
  }

  public String getName() {
    return DOM.getAttribute(inputElem, "name");
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
    return DOM.getBooleanAttribute(inputElem, propName);
  }

  public boolean isEnabled() {
    return !DOM.getBooleanAttribute(inputElem, "disabled");
  }

  /**
   * Checks or unchecks this check box.
   * 
   * @param checked <code>true</code> to check the check box
   */
  public void setChecked(boolean checked) {
    DOM.setBooleanAttribute(inputElem, "checked", checked);
    DOM.setBooleanAttribute(inputElem, "defaultChecked", checked);
  }

  public void setEnabled(boolean enabled) {
    DOM.setBooleanAttribute(inputElem, "disabled", !enabled);
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
    DOM.setAttribute(inputElem, "name", name);
  }

  public void setTabIndex(int index) {
    getFocusImpl().setTabIndex(inputElem, index);
  }

  public void setText(String text) {
    DOM.setInnerText(labelElem, text);
  }

  /**
   * This method is called when a widget is detached from the browser's
   * document. Overridden because of IE bug that throws away checked state.
   */
  protected void onDetach() {
    setChecked(isChecked());
    super.onDetach();
  }
}
