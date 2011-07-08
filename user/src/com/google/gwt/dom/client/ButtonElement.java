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
 * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#edef-BUTTON">W3C HTML Specification</a>
 */
@TagName(ButtonElement.TAG)
public class ButtonElement extends Element {

  public static final String TAG = "button";

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static ButtonElement as(Element elem) {
    assert elem.getTagName().equalsIgnoreCase(TAG);
    return (ButtonElement) elem;
  }

  protected ButtonElement() {
  }

  /**
   * Simulate a mouse-click.
   */
  public final void click() {
    DOMImpl.impl.buttonClick(this);
  }

  /**
   * A single character access key to give access to the form control.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-accesskey">W3C HTML Specification</a>
   */
  public final native String getAccessKey() /*-{
     return this.accessKey;
   }-*/;

  /**
   * The control is unavailable in this context.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-disabled">W3C HTML Specification</a>
   * @deprecated use {@link #isDisabled()} instead.
   */
  @Deprecated
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
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-name-BUTTON">W3C HTML Specification</a>
   */
  public final native String getName() /*-{
     return this.name;
   }-*/;

  /**
   * The type of button (all lower case).
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-type-BUTTON">W3C HTML Specification</a>
   */
  public final native String getType() /*-{
     return this.type;
   }-*/;

  /**
   * The current form control value.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-value-BUTTON">W3C HTML Specification</a>
   */
  public final native String getValue() /*-{
     return this.value;
   }-*/;

  /**
   * The control is unavailable in this context.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-disabled">W3C HTML Specification</a>
   */
  public final native boolean isDisabled() /*-{
     return !!this.disabled;
   }-*/;

  /**
   * A single character access key to give access to the form control.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-accesskey">W3C HTML Specification</a>
   */
  public final native void setAccessKey(String accessKey) /*-{
     this.accessKey = accessKey;
   }-*/;

  /**
   * The control is unavailable in this context.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-disabled">W3C HTML Specification</a>
   */
  public final native void setDisabled(boolean disabled) /*-{
     this.disabled = disabled;
   }-*/;

  /**
   * The control is unavailable in this context.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-disabled">W3C HTML Specification</a>
   * @deprecated use {@link #setDisabled(boolean)} instead
   */
  @Deprecated
  public final native void setDisabled(String disabled) /*-{
     this.disabled = disabled;
   }-*/;

  /**
   * Form control or object name when submitted with a form.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-name-BUTTON">W3C HTML Specification</a>
   */
  public final native void setName(String name) /*-{
     this.name = name;
   }-*/;

  /**
   * The current form control value.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-value-BUTTON">W3C HTML Specification</a>
   */
  public final native void setValue(String value) /*-{
     this.value = value;
   }-*/;
}
