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
 * Style information.
 * 
 * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/styles.html#edef-STYLE">W3C HTML Specification</a>
 * @see <a href="http://www.w3.org/TR/DOM-Level-2-HTML/references.html#DOMStyle">W3C HTML Specification</a>
 * @see <a href="http://www.w3.org/TR/DOM-Level-2-HTML/references.html#DOMStyle-inf">W3C HTML Specification</a>
 */
@TagName(StyleElement.TAG)
public class StyleElement extends Element {

  public static final String TAG = "style";

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static StyleElement as(Element elem) {
    assert elem.getTagName().equalsIgnoreCase(TAG);
    return (StyleElement) elem;
  }

  protected StyleElement() {
  }

  /**
   * The CSS text.
   */
  public final native String getCssText() /*-{
    return this.cssText;
  }-*/;

  /**
   * Enables/disables the style sheet.
   * @deprecated use {@link #isDisabled()} instead
   */
  @Deprecated
  public final native boolean getDisabled() /*-{
     return !!this.disabled;
   }-*/;

  /**
   * Designed for use with one or more target media.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/styles.html#adef-media">W3C HTML Specification</a>
   */
  public final native String getMedia() /*-{
     return this.media;
   }-*/;

  /**
   * The content type of the style sheet language.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/styles.html#adef-type-STYLE">W3C HTML Specification</a>
   */
  public final native String getType() /*-{
     return this.type;
   }-*/;

  /**
   * Enables/disables the style sheet.
   */
  public final native boolean isDisabled() /*-{
     return !!this.disabled;
   }-*/;

  /**
   * Sets the CSS text.
   */
  public final native void setCssText(String cssText) /*-{
    this.cssText = cssText;
  }-*/;

  /**
   * Enables/disables the style sheet.
   */
  public final native void setDisabled(boolean disabled) /*-{
     this.disabled = disabled;
   }-*/;

  /**
   * Designed for use with one or more target media.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/styles.html#adef-media">W3C HTML Specification</a>
   */
  public final native void setMedia(String media) /*-{
     this.media = media;
   }-*/;

  /**
   * The content type of the style sheet language.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/styles.html#adef-type-STYLE">W3C HTML Specification</a>
   */
  public final native void setType(String type) /*-{
     this.type = type;
   }-*/;
}
