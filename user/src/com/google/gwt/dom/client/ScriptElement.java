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
 * Script statements.
 * 
 * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/scripts.html#edef-SCRIPT">W3C HTML Specification</a>
 */
@TagName(ScriptElement.TAG)
public class ScriptElement extends Element {

  public static final String TAG = "script";

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static ScriptElement as(Element elem) {
    assert elem.getTagName().equalsIgnoreCase(TAG);
    return (ScriptElement) elem;
  }

  protected ScriptElement() {
  }

  /**
   * Indicates that the user agent can defer processing of the script.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/scripts.html#adef-defer">W3C HTML Specification</a>
   */
  public final native String getDefer() /*-{
     return this.defer;
   }-*/;

  /**
   * URI designating an external script.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/scripts.html#adef-src-SCRIPT">W3C HTML Specification</a>
   */
  public final native String getSrc() /*-{
     return this.src;
   }-*/;

  /**
   * The script content of the element.
   */
  public final native String getText() /*-{
     return this.text;
   }-*/;

  /**
   * The content type of the script language.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/scripts.html#adef-type-SCRIPT">W3C HTML Specification</a>
   */
  public final native String getType() /*-{
     return this.type;
   }-*/;

  /**
   * Indicates that the user agent can defer processing of the script.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/scripts.html#adef-defer">W3C HTML Specification</a>
   */
  public final native void setDefer(String defer) /*-{
     this.defer = defer;
   }-*/;

  /**
   * URI designating an external script.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/scripts.html#adef-src-SCRIPT">W3C HTML Specification</a>
   */
  public final native void setSrc(String src) /*-{
     this.src = src;
   }-*/;

  /**
   * The script content of the element.
   */
  public final native void setText(String text) /*-{
     this.text = text;
   }-*/;

  /**
   * The content type of the script language.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/scripts.html#adef-type-SCRIPT">W3C HTML Specification</a>
   */
  public final native void setType(String type) /*-{
     this.type = type;
   }-*/;
}
