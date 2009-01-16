/*
 * Copyright 2009 Google Inc.
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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * A standard check box widget.
 * 
 * This class also serves as a base class for
 * {@link com.google.gwt.user.client.ui.RadioButton}.
 * 
 * <p>
 * <img class='gallery' src='CheckBox.png'/>
 * </p>
 * 
 * <h3>CSS Style Rules</h3>
 * <dl>
 * <dt>.gwt-CheckBox</dt>
 * <dd>the outer element</dd>
 * <dt>.gwt-CheckBox-disabled</dt>
 * <dd>applied when Checkbox is disabled</dd>
 * </dl>
 * 
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.CheckBoxExample}
 * </p>
 */
public class CheckBox extends ButtonBase implements HasName, HasValue<Boolean> {
  private Element inputElem, labelElem;
  private boolean valueChangeHandlerInitialized;

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

    DOM.appendChild(getElement(), inputElem);
    DOM.appendChild(getElement(), labelElem);

    String uid = DOM.createUniqueId();
    DOM.setElementProperty(inputElem, "id", uid);
    DOM.setElementProperty(labelElem, "htmlFor", uid);

    // Accessibility: setting tab index to be 0 by default, ensuring element
    // appears in tab sequence. FocusWidget's setElement method already
    // calls setTabIndex, which is overridden below. However, at the time
    // that this call is made, inputElem has not been created. So, we have
    // to call setTabIndex again, once inputElem has been created.
    setTabIndex(0);
  }

  public HandlerRegistration addValueChangeHandler(
      ValueChangeHandler<Boolean> handler) {
    // Is this the first value change handler? If so, time to listen to clicks
    // on the checkbox
    if (!valueChangeHandlerInitialized) {
      valueChangeHandlerInitialized = true;
      this.addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent event) {
          // No need to compare old value and new value--click handler
          // only fires on real click, and value always toggles
          ValueChangeEvent.fire(CheckBox.this, isChecked());
        }
      });
    }
    return addHandler(handler, ValueChangeEvent.getType());
  }

  @Override
  public String getHTML() {
    return DOM.getInnerHTML(labelElem);
  }

  public String getName() {
    return DOM.getElementProperty(inputElem, "name");
  }

  @Override
  public int getTabIndex() {
    return getFocusImpl().getTabIndex(inputElem);
  }

  @Override
  public String getText() {
    return DOM.getInnerText(labelElem);
  }

  /**
   * Determines whether this check box is currently checked.
   * 
   * @return <code>true</code> if the check box is checked
   */
  public Boolean getValue() {
    return isChecked();
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

  @Override
  public boolean isEnabled() {
    return !DOM.getElementPropertyBoolean(inputElem, "disabled");
  }

  @Override
  public void setAccessKey(char key) {
    DOM.setElementProperty(inputElem, "accessKey", "" + key);
  }

  /**
   * Checks or unchecks this check box. Does not fire {@link ValueChangeEvent}.
   * (If you want the event to fire, use {@link #setValue(Boolean, boolean)})
   * 
   * @param checked <code>true</code> to check the check box.
   */
  public void setChecked(boolean checked) {
    DOM.setElementPropertyBoolean(inputElem, "checked", checked);
    DOM.setElementPropertyBoolean(inputElem, "defaultChecked", checked);
  }

  @Override
  public void setEnabled(boolean enabled) {
    DOM.setElementPropertyBoolean(inputElem, "disabled", !enabled);
    if (enabled) {
      removeStyleDependentName("disabled");
    } else {
      addStyleDependentName("disabled");
    }
  }

  @Override
  public void setFocus(boolean focused) {
    if (focused) {
      getFocusImpl().focus(inputElem);
    } else {
      getFocusImpl().blur(inputElem);
    }
  }

  @Override
  public void setHTML(String html) {
    DOM.setInnerHTML(labelElem, html);
  }

  public void setName(String name) {
    DOM.setElementProperty(inputElem, "name", name);
  }

  @Override
  public void setTabIndex(int index) {
    // Need to guard against call to setTabIndex before inputElem is
    // initialized. This happens because FocusWidget's (a superclass of
    // CheckBox) setElement method calls setTabIndex before inputElem is
    // initialized. See CheckBox's protected constructor for more information.
    if (inputElem != null) {
      getFocusImpl().setTabIndex(inputElem, index);
    }
  }

  @Override
  public void setText(String text) {
    DOM.setInnerText(labelElem, text);
  }

  /**
   * Checks or unchecks the text box.
   * 
   * @param value true to check, false to uncheck. Must not be null.
   * @thows IllegalArgumentException if value is null
   */
  public void setValue(Boolean value) {
    setValue(value, false);
  }

  /**
   * Checks or unchecks the text box, firing {@link ValueChangeEvent} if
   * appropriate.
   * 
   * @param value true to check, false to uncheck. Must not be null.
   * @param fireEvents If true, and value has changed, fire a
   *          {@link ValueChangeEvent}
   * @thows IllegalArgumentException if value is null
   */
  public void setValue(Boolean value, boolean fireEvents) {
    if (value == null) {
      throw new IllegalArgumentException("value must not be null");
    }

    if (isChecked() == value) {
      return;
    }
    setChecked(value);
    if (fireEvents) {
      ValueChangeEvent.fire(this, value);
    }
  }

  // Unlike other widgets the CheckBox sinks on its input element, not its
  // wrapper element.
  @Override
  public void sinkEvents(int eventBitsToAdd) {
    if (isOrWasAttached()) {
      DOM.sinkEvents(inputElem, eventBitsToAdd | DOM.getEventsSunk(inputElem));
    } else {
      super.sinkEvents(eventBitsToAdd);
    }
  }

  /**
   * <b>Affected Elements:</b>
   * <ul>
   * <li>-label = label next to checkbox.</li>
   * </ul>
   * 
   * @see UIObject#onEnsureDebugId(String)
   */
  @Override
  protected void onEnsureDebugId(String baseID) {
    super.onEnsureDebugId(baseID);
    ensureDebugId(labelElem, baseID, "label");
    ensureDebugId(inputElem, baseID, "input");
    DOM.setElementProperty(labelElem, "htmlFor", inputElem.getId());
  }

  /**
   * This method is called when a widget is attached to the browser's document.
   * onAttach needs special handling for the CheckBox case. Must still call
   * {@link Widget#onAttach()} to preserve the <code>onAttach</code> contract.
   */
  @Override
  protected void onLoad() {
    // Sets the event listener on the inputElem, as in this case that's the
    // element we want so input on.
    DOM.setEventListener(inputElem, this);
  }

  /**
   * This method is called when a widget is detached from the browser's
   * document. Overridden because of IE bug that throws away checked state and
   * in order to clear the event listener off of the <code>inputElem</code>.
   */
  @Override
  protected void onUnload() {
    // Clear out the inputElem's event listener (breaking the circular
    // reference between it and the widget).
    DOM.setEventListener(inputElem, null);
    setChecked(isChecked());
  }

  /**
   * Replace the current input element with a new one.
   * 
   * @param elem the new input element
   */
  protected void replaceInputElement(Element elem) {
    // Collect information we need to set
    int tabIndex = getTabIndex();
    boolean checked = isChecked();
    boolean enabled = isEnabled();
    String uid = DOM.getElementProperty(inputElem, "id");
    String accessKey = DOM.getElementProperty(inputElem, "accessKey");
    int sunkEvents = DOM.getEventsSunk(inputElem);

    // Clear out the old input element
    DOM.setEventListener(inputElem, null);
    DOM.setEventListener(inputElem, null);

    DOM.removeChild(getElement(), inputElem);
    DOM.insertChild(getElement(), elem, 0);

    // Sink events on the new element
    DOM.sinkEvents(elem, DOM.getEventsSunk(inputElem));
    DOM.sinkEvents(inputElem, 0);
    inputElem = elem;

    // Setup the new element
    DOM.sinkEvents(inputElem, sunkEvents);
    DOM.setElementProperty(inputElem, "id", uid);
    if (!accessKey.equals("")) {
      DOM.setElementProperty(inputElem, "accessKey", accessKey);
    }
    setTabIndex(tabIndex);
    setChecked(checked);
    setEnabled(enabled);

    // Set the event listener
    if (isAttached()) {
      DOM.setEventListener(inputElem, this);
    }
  }
}
