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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.LabelElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;

/**
 * A standard check box widget.
 * 
 * This class also serves as a base class for
 * {@link com.google.gwt.user.client.ui.RadioButton}.
 * 
 * <p>
 * <img class='gallery' src='doc-files/CheckBox.png'/>
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
public class CheckBox extends ButtonBase implements HasName, HasValue<Boolean>,
    HasWordWrap {
  InputElement inputElem;
  LabelElement labelElem;
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
    inputElem = InputElement.as(elem);
    labelElem = Document.get().createLabelElement();

    getElement().appendChild(inputElem);
    getElement().appendChild(labelElem);

    String uid = DOM.createUniqueId();
    inputElem.setPropertyString("id", uid);
    labelElem.setHtmlFor(uid);

    // Accessibility: setting tab index to be 0 by default, ensuring element
    // appears in tab sequence. FocusWidget's setElement method already
    // calls setTabIndex, which is overridden below. However, at the time
    // that this call is made, inputElem has not been created. So, we have
    // to call setTabIndex again, once inputElem has been created.
    setTabIndex(0);
  }

  public HandlerRegistration addValueChangeHandler(
      ValueChangeHandler<Boolean> handler) {
    // Is this the first value change handler? If so, time to add handlers
    if (!valueChangeHandlerInitialized) {
      ensureDomEventHandlers();
      valueChangeHandlerInitialized = true;
    }
    return addHandler(handler, ValueChangeEvent.getType());
  }

  /**
   * Returns the value property of the input element that backs this widget.
   * This is the value that will be associated with the CheckBox name and
   * submitted to the server if a {@link FormPanel} that holds it is submitted
   * and the box is checked.
   * <p>
   * Don't confuse this with {@link #getValue}, which returns true or false if
   * the widget is checked.
   */
  public String getFormValue() {
    return inputElem.getValue();
  }

  @Override
  public String getHTML() {
    return labelElem.getInnerHTML();
  }

  public String getName() {
    return inputElem.getName();
  }

  @Override
  public int getTabIndex() {
    return inputElem.getTabIndex();
  }

  @Override
  public String getText() {
    return labelElem.getInnerText();
  }

  /**
   * Determines whether this check box is currently checked. 
   * <p>
   * Note that this <em>is not</em> return the value property of the checkbox
   * input element wrapped by this widget. For access to that property, see
   * {@link #getFormValue()}
   * 
   * @return <code>true</code> if the check box is checked, false otherwise.
   * Will not return null
   */
  public Boolean getValue() {
    if (isAttached()) {
      return inputElem.isChecked();
    } else {
      return inputElem.isDefaultChecked();
    }
  }

  public boolean getWordWrap() {
    return !getElement().getStyle().getProperty("whiteSpace").equals("nowrap");
  }

  /**
   * Determines whether this check box is currently checked.
   * 
   * @return <code>true</code> if the check box is checked
   * @deprecated Use {@link #getValue} instead
   */
  @Deprecated
  public boolean isChecked() {
    // Funny comparison b/c getValue could in theory return null
    return getValue() == true; 
  }

  @Override
  public boolean isEnabled() {
    return !inputElem.isDisabled();
  }

  @Override
  public void setAccessKey(char key) {
    inputElem.setAccessKey("" + key);
  }

  /**
   * Checks or unchecks this check box. Does not fire {@link ValueChangeEvent}.
   * (If you want the event to fire, use {@link #setValue(Boolean, boolean)})
   * 
   * @param checked <code>true</code> to check the check box.
   * @deprecated Use {@link #setValue(Boolean)} instead
   */
  @Deprecated
  public void setChecked(boolean checked) {
    setValue(checked);
  }

  @Override
  public void setEnabled(boolean enabled) {
    inputElem.setDisabled(!enabled);
    if (enabled) {
      removeStyleDependentName("disabled");
    } else {
      addStyleDependentName("disabled");
    }
  }

  @Override
  public void setFocus(boolean focused) {
    if (focused) {
      inputElem.focus();
    } else {
      inputElem.blur();
    }
  }

  /**
   * Set the value property on the input element that backs this widget. This is
   * the value that will be associated with the CheckBox's name and submitted to
   * the server if a {@link FormPanel} that holds it is submitted and the box is
   * checked.
   * <p>
   * Don't confuse this with {@link #setValue}, which actually checks and
   * unchecks the box.
   * 
   * @param value
   */
  public void setFormValue(String value) {
    inputElem.setAttribute("value", value);
  }

  @Override
  public void setHTML(String html) {
    labelElem.setInnerHTML(html);
  }

  public void setName(String name) {
    inputElem.setName(name);
  }

  @Override
  public void setTabIndex(int index) {
    // Need to guard against call to setTabIndex before inputElem is
    // initialized. This happens because FocusWidget's (a superclass of
    // CheckBox) setElement method calls setTabIndex before inputElem is
    // initialized. See CheckBox's protected constructor for more information.
    if (inputElem != null) {
      inputElem.setTabIndex(index);
    }
  }

  @Override
  public void setText(String text) {
    labelElem.setInnerText(text);
  }

  /**
   * Checks or unchecks the text box.
   * <p>
   * Note that this <em>does not</em> set the value property of the checkbox
   * input element wrapped by this widget. For access to that property, see
   * {@link #setFormValue(String)}
   * 
   * @param value true to check, false to uncheck; must not be null
   * @throws IllegalArgumentException if value is null
   */
  public void setValue(Boolean value) {
    setValue(value, false);
  }

  /**
   * Checks or unchecks the text box, firing {@link ValueChangeEvent} if
   * appropriate.
   * <p>
   * Note that this <em>does not</em> set the value property of the checkbox
   * input element wrapped by this widget. For access to that property, see
   * {@link #setFormValue(String)}
   *
   * @param value true to check, false to uncheck; must not be null
   * @param fireEvents If true, and value has changed, fire a
   *          {@link ValueChangeEvent}
   * @throws IllegalArgumentException if value is null
   */
  public void setValue(Boolean value, boolean fireEvents) {
    if (value == null) {
      throw new IllegalArgumentException("value must not be null");
    }

    Boolean oldValue = getValue();
    inputElem.setChecked(value);
    inputElem.setDefaultChecked(value);
    if (value.equals(oldValue)) {
      return;
    }
    if (fireEvents) {
      ValueChangeEvent.fire(this, value);
    }
  }

  public void setWordWrap(boolean wrap) {
    getElement().getStyle().setProperty("whiteSpace",
        wrap ? "normal" : "nowrap");
  }

  // Unlike other widgets the CheckBox sinks on its inputElement, not
  // its wrapper
  @Override
  public void sinkEvents(int eventBitsToAdd) {
    if (isOrWasAttached()) {
      Event.sinkEvents(inputElem, 
          eventBitsToAdd | Event.getEventsSunk(inputElem));
    } else {
      super.sinkEvents(eventBitsToAdd);
    }
  }

  protected void ensureDomEventHandlers() {
    addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        // Checkboxes always toggle their value, no need to compare
        // with old value. Radio buttons are not so lucky, see
        // overrides in RadioButton
        ValueChangeEvent.fire(CheckBox.this, getValue());
      }
    });
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
    labelElem.setHtmlFor(inputElem.getId());
  }

  /**
   * This method is called when a widget is attached to the browser's document.
   * onAttach needs special handling for the CheckBox case. Must still call
   * {@link Widget#onAttach()} to preserve the <code>onAttach</code> contract.
   */
  @Override
  protected void onLoad() {
    setEventListener(inputElem, this);
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
    setEventListener(asOld(inputElem), null);
    setValue(getValue());
  }

  /**
   * Replace the current input element with a new one. Preserves
   * all state except for the name property, for nasty reasons
   * related to radio button grouping. (See implementation of 
   * {@link RadioButton#setName}.)
   * 
   * @param elem the new input element
   */
  protected void replaceInputElement(Element elem) {
    InputElement newInputElem = InputElement.as(elem);
    // Collect information we need to set
    int tabIndex = getTabIndex();
    boolean checked = getValue();
    boolean enabled = isEnabled();
    String formValue = getFormValue();
    String uid = inputElem.getId();
    String accessKey = inputElem.getAccessKey();
    int sunkEvents = Event.getEventsSunk(inputElem);   

    // Clear out the old input element
    setEventListener(asOld(inputElem), null);

    getElement().replaceChild(newInputElem, inputElem);

    // Sink events on the new element
    Event.sinkEvents(elem, Event.getEventsSunk(inputElem));
    Event.sinkEvents(inputElem, 0);
    inputElem = newInputElem;

    // Setup the new element
    Event.sinkEvents(inputElem, sunkEvents);
    inputElem.setId(uid);
    if (!"".equals(accessKey)) {
      inputElem.setAccessKey(accessKey);
    }
    setTabIndex(tabIndex);
    setValue(checked);
    setEnabled(enabled);
    setFormValue(formValue);

    // Set the event listener
    if (isAttached()) {
      setEventListener(asOld(inputElem), this);
    }
  }

  private Element asOld(com.google.gwt.dom.client.Element elem) {
    Element oldSchool = elem.cast();
    return oldSchool;
  }

  private void setEventListener(com.google.gwt.dom.client.Element e,
      EventListener listener) {
    DOM.setEventListener(asOld(e), listener);
  }
}
