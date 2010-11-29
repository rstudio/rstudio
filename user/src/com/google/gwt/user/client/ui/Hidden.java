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
 * Represents a hidden field in an HTML form.
 */
public class Hidden extends Widget implements HasName, TakesValue<String>, IsEditor<LeafValueEditor<String>> {

  /**
   * Creates a Hidden widget that wraps an existing &lt;input type='hidden'&gt;
   * element.
   * 
   * This element must already be attached to the document. If the element is
   * removed from the document, you must call
   * {@link RootPanel#detachNow(Widget)}.
   * 
   * @param element the element to be wrapped
   */
  public static Hidden wrap(Element element) {
    // Assert that the element is attached.
    assert Document.get().getBody().isOrHasChild(element);

    Hidden hidden = new Hidden(element);

    // Mark it attached and remember it for cleanup.
    hidden.onAttach();
    RootPanel.detachOnWindowClose(hidden);

    return hidden;
  }

  private LeafValueEditor<String> editor;

  /**
   * Constructor for <code>Hidden</code>.
   */
  public Hidden() {
    setElement(Document.get().createHiddenInputElement());
  }

  /**
   * Constructor for <code>Hidden</code>.
   * 
   * @param name name of the hidden field
   */
  public Hidden(String name) {
    this();
    setName(name);
  }

  /**
   * Constructor for <code>Hidden</code>.
   * 
   * @param name name of the hidden field
   * @param value value of the hidden field
   */
  public Hidden(String name, String value) {
    this(name);
    setValue(value);
  }

  /**
   * This constructor may be used by subclasses to explicitly use an existing
   * element. This element must be an &lt;input&gt; element whose type is
   * 'hidden'.
   * 
   * @param element the element to be used
   */
  protected Hidden(Element element) {
    assert InputElement.as(element).getType().equalsIgnoreCase("hidden");
    setElement(element);
  }

  public LeafValueEditor<String> asEditor() {
    if (editor == null) {
      editor = TakesValueEditor.of(this);
    }
    return editor;
  }

  /**
   * Gets the default value of the hidden field.
   * 
   * @return the default value
   */
  public String getDefaultValue() {
    return getInputElement().getDefaultValue();
  }

  /**
   * Gets the id of the hidden field.
   * 
   * @return the id
   */
  public String getID() {
    return getElement().getId();
  }

  /**
   * Gets the name of the hidden field.
   * 
   * @return the name
   */

  public String getName() {
    return getInputElement().getName();
  }

  /**
   * Gets the value of the hidden field.
   * 
   * @return the value
   */
  public String getValue() {
    return getInputElement().getValue();
  }

  /**
   * Sets the default value of the hidden field.
   * 
   * @param defaultValue default value to set
   */
  public void setDefaultValue(String defaultValue) {
    getInputElement().setDefaultValue(defaultValue);
  }

  /**
   * Sets the id of the hidden field.
   * 
   * @param id id to set
   */
  public void setID(String id) {
    getElement().setId(id);
  }

  /**
   * Sets the name of the hidden field.
   * 
   * @param name name of the field
   */
  public void setName(String name) {
    if (name == null) {
      throw new NullPointerException("Name cannot be null");
    } else if (name.equals("")) {
      throw new IllegalArgumentException("Name cannot be an empty string.");
    }

    getInputElement().setName(name);
  }

  /**
   * Sets the value of the hidden field.
   * 
   * @param value value to set
   */
  public void setValue(String value) {
    getInputElement().setValue(value);
  }

  private InputElement getInputElement() {
    return getElement().cast();
  }
}
