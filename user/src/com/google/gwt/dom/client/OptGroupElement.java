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
 * Group options together in logical subdivisions.
 * 
 * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#edef-OPTGROUP">W3C HTML Specification</a>
 */
@TagName(OptGroupElement.TAG)
public class OptGroupElement extends Element {

  public static final String TAG = "optgroup";

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static OptGroupElement as(Element elem) {
    assert elem.getTagName().equalsIgnoreCase(TAG);
    return (OptGroupElement) elem;
  }

  protected OptGroupElement() {
  }

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
   * Assigns a label to this option group.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-label-OPTGROUP">W3C HTML Specification</a>
   */
  public final native String getLabel() /*-{
     return this.label;
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
   */
  public final native void setDisabled(String disabled) /*-{
     this.disabled = disabled;
   }-*/;

  /**
   * Assigns a label to this option group.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-label-OPTGROUP">W3C HTML Specification</a>
   */
  public final native void setLabel(String label) /*-{
     this.label = label;
   }-*/;
}
