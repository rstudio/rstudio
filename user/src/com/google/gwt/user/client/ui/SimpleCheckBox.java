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
import com.google.gwt.editor.client.IsEditor;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.editor.client.adapters.TakesValueEditor;
import com.google.gwt.user.client.TakesValue;

/**
 * A simple checkbox widget, with no label.
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-SimpleCheckBox { }</li>
 * <li>.gwt-SimpleCheckBox-disabled { Applied when checkbox is disabled }</li>
 * </ul>
 */
public class SimpleCheckBox extends FocusWidget implements HasName,
    TakesValue<Boolean>, IsEditor<LeafValueEditor<Boolean>> {

  /**
   * Creates a SimpleCheckBox widget that wraps an existing &lt;input
   * type='checkbox'&gt; element.
   * 
   * This element must already be attached to the document. If the element is
   * removed from the document, you must call
   * {@link RootPanel#detachNow(Widget)}.
   * 
   * @param element the element to be wrapped
   */
  public static SimpleCheckBox wrap(Element element) {
    // Assert that the element is attached.
    assert Document.get().getBody().isOrHasChild(element);

    SimpleCheckBox checkBox = new SimpleCheckBox(element);

    // Mark it attached and remember it for cleanup.
    checkBox.onAttach();
    RootPanel.detachOnWindowClose(checkBox);

    return checkBox;
  }

  private LeafValueEditor<Boolean> editor;

  /**
   * Creates a new simple checkbox.
   */
  public SimpleCheckBox() {
    this(Document.get().createCheckInputElement(), "gwt-SimpleCheckBox");
  }

  /**
   * This constructor may be used by subclasses to explicitly use an existing
   * element. This element must be an &lt;input&gt; element whose type is either
   * 'checkbox'.
   * 
   * @param element the element to be used
   */
  protected SimpleCheckBox(Element element) {
    assert InputElement.as(element).getType().equalsIgnoreCase("checkbox");
    setElement(element);
  }

  SimpleCheckBox(Element element, String styleName) {
    setElement(element);
    if (styleName != null) {
      setStyleName(styleName);
    }
  }

  public LeafValueEditor<Boolean> asEditor() {
    if (editor == null) {
      editor = TakesValueEditor.of(this);
    }
    return editor;
  }

  /**
   * Returns the value property of the input element that backs this widget.
   * This is the value that will be associated with the check box name and
   * submitted to the server if a {@link FormPanel} that holds it is submitted
   * and the box is checked.
   * <p>
   * Don't confuse this with {@link #getValue}, which returns true or false if
   * the widget is checked.
   */
  public String getFormValue() {
    return getInputElement().getValue();
  }

  public String getName() {
    return getInputElement().getName();
  }

  /**
   * Determines whether this check box is currently checked.
   * <p>
   * Note that this <em>does not</em> return the value property of the checkbox
   * input element wrapped by this widget. For access to that property, see
   * {@link #getFormValue()}
   * 
   * @return <code>true</code> if the check box is checked, false otherwise.
   *         Will not return null
   */
  public Boolean getValue() {
    String propName = isAttached() ? "checked" : "defaultChecked";
    return getInputElement().getPropertyBoolean(propName);
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

  /**
   * Checks or unchecks this check box.
   * 
   * @param checked <code>true</code> to check the check box
   * @deprecated Use {@link #setValue(Boolean)} instead
   */
  @Deprecated
  public void setChecked(boolean checked) {
    setValue(checked);
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    if (enabled) {
      removeStyleDependentName("disabled");
    } else {
      addStyleDependentName("disabled");
    }
  }

  /**
   * Set the value property on the input element that backs this widget. This is
   * the value that will be associated with the check box's name and submitted
   * to the server if a {@link FormPanel} that holds it is submitted and the box
   * is checked.
   * <p>
   * Don't confuse this with {@link #setValue}, which actually checks and
   * unchecks the box.
   * 
   * @param value
   */
  public void setFormValue(String value) {
    getInputElement().setAttribute("value", value);
  }

  public void setName(String name) {
    getInputElement().setName(name);
  }

  /**
   * Checks or unchecks the check box.
   * <p>
   * Note that this <em>does not</em> set the value property of the checkbox
   * input element wrapped by this widget. For access to that property, see
   * {@link #setFormValue(String)}
   * 
   * @param value true to check, false to uncheck; null value implies false
   */
  public void setValue(Boolean value) {
    if (value == null) {
      value = Boolean.FALSE;
    }

    getInputElement().setChecked(value);
    getInputElement().setDefaultChecked(value);
  }

  /**
   * This method is called when a widget is detached from the browser's
   * document. Overridden because of IE bug that throws away checked state.
   */
  @Override
  protected void onUnload() {
    setValue(getValue());
  }

  private InputElement getInputElement() {
    return InputElement.as(getElement());
  }
}
