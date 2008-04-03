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
 * A selectable choice.
 * 
 * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#edef-OPTION
 */
public class OptionElement extends Element {

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static OptionElement as(Element elem) {
    assert elem.getTagName().equalsIgnoreCase("option");
    return (OptionElement) elem;
  }

  protected OptionElement() {
  }

  /**
   * Returns the FORM element containing this control. Returns null if this
   * control is not within the context of a form.
   */
  public native final FormElement getForm() /*-{
    return form;
  }-*/;

  /**
   * The index of this OPTION in its parent SELECT, starting from 0.
   */
  public native final int getIndex() /*-{
    return this.index;
  }-*/;

  /**
   * Option label for use in hierarchical menus.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-label-OPTION
   */
  public native final String getLabel() /*-{
    return this.label;
  }-*/;

  /**
   * The text contained within the option element.
   */
  public native final String getText() /*-{
    return this.text;
  }-*/;

  /**
   * The current form control value.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-value-OPTION
   */
  public native final String getValue() /*-{
    return this.value;
  }-*/;

  /**
   * Represents the value of the HTML selected attribute. The value of this
   * attribute does not change if the state of the corresponding form control,
   * in an interactive user agent, changes.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-selected
   */
  public native final boolean isDefaultSelected() /*-{
    return this.defaultSelected;
  }-*/;

  /**
   * The control is unavailable in this context.
   * 
   * @param see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-disabled
   */
  public native final boolean isDisabled() /*-{
    return this.disabled;
  }-*/;

  /**
   * Represents the current state of the corresponding form control, in an
   * interactive user agent. Changing this attribute changes the state of the
   * form control, but does not change the value of the HTML selected attribute
   * of the element.
   */
  public native final boolean isSelected() /*-{
    return this.selected;
  }-*/;

  /**
   * Represents the value of the HTML selected attribute. The value of this
   * attribute does not change if the state of the corresponding form control,
   * in an interactive user agent, changes.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-selected
   */
  public native final void setDefaultSelected(boolean selected) /*-{
    this.defaultSelected = selected;
  }-*/;

  /**
   * The control is unavailable in this context.
   * 
   * @param see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-disabled
   */
  public native final void setDisabled(boolean disabled) /*-{
    return this.disabled = disabled;
  }-*/;

  /**
   * Option label for use in hierarchical menus.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-label-OPTION
   */
  public native final void setLabel(String label) /*-{
    return this.label = label;
  }-*/;

  /**
   * Represents the current state of the corresponding form control, in an
   * interactive user agent. Changing this attribute changes the state of the
   * form control, but does not change the value of the HTML selected attribute
   * of the element.
   */
  public native final void setSelected(boolean selected) /*-{
    this.selected = selected;
  }-*/;

  /**
   * The text contained within the option element.
   */
  public native final void setText(String text) /*-{
    this.text = text;
  }-*/;

  /**
   * The current form control value.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-value-OPTION
   */
  public native final void setValue(String value) /*-{
    this.value = value;
  }-*/;
}
