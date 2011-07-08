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
 * Provides a caption for a FIELDSET grouping.
 * 
 * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#edef-LEGEND">W3C HTML Specification</a>
 */
@TagName(LegendElement.TAG)
public class LegendElement extends Element {

  public static final String TAG = "legend";

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static LegendElement as(Element elem) {
    assert elem.getTagName().equalsIgnoreCase(TAG);
    return (LegendElement) elem;
  }

  protected LegendElement() {
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
   * Returns the FORM element containing this control. Returns null if this
   * control is not within the context of a form.
   */
  public final native FormElement getForm() /*-{
     return this.form;
   }-*/;

  /**
   * A single character access key to give access to the form control.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-accesskey">W3C HTML Specification</a>
   */
  public final native void setAccessKey(String accessKey) /*-{
    this.accessKey = accessKey;
  }-*/;
}
