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
 * Push button.
 * 
 * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#edef-BUTTON
 */
public class ButtonElement extends Element {

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static ButtonElement as(Element elem) {
    assert elem.getTagName().equalsIgnoreCase("button");
    return (ButtonElement) elem;
  }

  protected ButtonElement() {
  }

  /**
   * Simulate a mouse-click.
   */
  public final native void click() /*-{
    this.click();
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
   * The control is unavailable in this context.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-disabled
   */
  public final native String getDisabled() /*-{
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
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-name-BUTTON
   */
  public final native String getName() /*-{
     return this.name;
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
   * The type of button (all lower case).
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-type-BUTTON
   */
  public final native String getType() /*-{
     return this.type;
   }-*/;

  /**
   * The current form control value.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-value-BUTTON
   */
  public final native String getValue() /*-{
     return this.value;
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
   * The control is unavailable in this context.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-disabled
   */
  public final native void setDisabled(String disabled) /*-{
     this.disabled = disabled;
   }-*/;

  /**
   * Form control or object name when submitted with a form.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-name-BUTTON
   */
  public final native void setName(String name) /*-{
     this.name = name;
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
   * The current form control value.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-value-BUTTON
   */
  public final native void setValue(String value) /*-{
     this.value = value;
   }-*/;
}
