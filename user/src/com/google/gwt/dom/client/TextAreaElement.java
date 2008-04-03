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
package com.google.gwt.dom.client;

/**
 * Multi-line text field.
 * 
 * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#edef-TEXTAREA
 */
public class TextAreaElement extends Element {

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static TextAreaElement as(Element elem) {
    assert elem.getTagName().equalsIgnoreCase("textarea");
    return (TextAreaElement) elem;
  }

  protected TextAreaElement() {
  }

  /**
   * Removes keyboard focus from this element.
   */
  public final native void blur() /*-{
    this.blur();
  }-*/;

  /**
   * Gives keyboard focus to this element.
   */
  public final native void focus() /*-{
    this.focus();
  }-*/;

  /**
   * A single character access key to give access to the form control.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-accesskey
   */
  public final native String getAccessKey() /*-{
    return this.accessKey;
  }-*/;

  /**
   * Width of control (in characters).
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-cols-TEXTAREA
   */
  public final native int getCols() /*-{
    return this.cols;
  }-*/;

  /**
   * Represents the contents of the element. The value of this attribute does
   * not change if the contents of the corresponding form control, in an
   * interactive user agent, changes.
   */
  public final native String getDefaultValue() /*-{
    return this.defaultValue;
  }-*/;

  public final native boolean getDisabled() /*-{
    return this.disabled;
  }-*/;

  /**
   * Returns the FORM element containing this control. Returns null if this
   * control is not within the context of a form.
   */
  public final native FormElement getForm() /*-{
    return this.form;
  }-*/;

  /**
   * Form control or object name when submitted with a form.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-name-TEXTAREA
   */
  public final native String getName() /*-{
    return this.name;
  }-*/;

  public final native boolean getReadOnly() /*-{
    return this.readOnly;
  }-*/;

  /**
   * Number of text rows.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-rows-TEXTAREA
   */
  public final native int getRows() /*-{
    return this.rows;
  }-*/;

  /**
   * Index that represents the element's position in the tabbing order.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-tabindex
   */
  public final native int getTabIndex() /*-{
    return this.tabIndex;
  }-*/;

  /**
   * The type of this form control. This the string "textarea".
   */
  public final native String getType() /*-{
    return this.type;
  }-*/;

  /**
   * Represents the current contents of the corresponding form control, in an
   * interactive user agent. Changing this attribute changes the contents of the
   * form control, but does not change the contents of the element. If the
   * entirety of the data can not fit into a single string, the implementation
   * may truncate the data.
   */
  public final native String getValue() /*-{
    return this.value;
  }-*/;

  /**
   * Select the contents of the TEXTAREA.
   */
  public final native void select() /*-{
    this.select();
  }-*/;

  /**
   * A single character access key to give access to the form control.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-accesskey
   */
  public final native void setAccessKey(String accessKey) /*-{
    this.accessKey = accessKey;
  }-*/;

  /**
   * Width of control (in characters).
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-cols-TEXTAREA
   */
  public final native void setCols(int cols) /*-{
    this.cols = cols;
  }-*/;

  /**
   * Represents the contents of the element. The value of this attribute does
   * not change if the contents of the corresponding form control, in an
   * interactive user agent, changes.
   */
  public final native void setDefaultValue(String defaultValue) /*-{
    this.defaultValue = defaultValue;
  }-*/;

  /**
   * The control is unavailable in this context.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-disabled
   */
  public final native boolean isDisabled() /*-{
    return this.disabled;
  }-*/;

  /**
   * The control is unavailable in this context.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-disabled
   */
  public final native void setDisabled(boolean disabled) /*-{
    this.disabled = disabled;
  }-*/;

  /**
   * Form control or object name when submitted with a form.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-name-TEXTAREA
   */
  public final native void setName(String name) /*-{
    this.name = name;
  }-*/;

  /**
   * This control is read-only.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-readonly
   */
  public final native boolean isReadOnly() /*-{
    return this.readOnly;
  }-*/;

  /**
   * This control is read-only.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-readonly
   */
  public final native void setReadOnly(boolean readOnly) /*-{
    this.readOnly = readOnly;
  }-*/;

  /**
   * Number of text rows.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-rows-TEXTAREA
   */
  public final native void setRows(int rows) /*-{
    this.rows = rows;
  }-*/;

  /**
   * Index that represents the element's position in the tabbing order.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-tabindex
   */
  public final native void setTabIndex(int tabIndex) /*-{
    this.tabIndex = tabIndex;
  }-*/;

  /**
   * Represents the current contents of the corresponding form control, in an
   * interactive user agent. Changing this attribute changes the contents of the
   * form control, but does not change the contents of the element. If the
   * entirety of the data can not fit into a single string, the implementation
   * may truncate the data.
   */
  public final native void setValue(String value) /*-{
    this.value = value;
  }-*/;
}
