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
 * Notice of modification to part of a document.
 * 
 * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/text.html#edef-ins">W3C HTML Specification</a>
 * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/text.html#edef-del">W3C HTML Specification</a>
 */
@TagName({ModElement.TAG_INS, ModElement.TAG_DEL})
public class ModElement extends Element {

  public static final String TAG_INS = "ins";
  public static final String TAG_DEL = "del";

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static ModElement as(Element elem) {
    assert elem.getTagName().equalsIgnoreCase(TAG_INS)
        || elem.getTagName().equalsIgnoreCase(TAG_DEL);
    return (ModElement) elem;
  }

  protected ModElement() {
  }

  /**
   * A URI designating a document that describes the reason for the change.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/">W3C HTML Specification</a>
   */
  public final native String getCite() /*-{
    return this.cite;
  }-*/;

  /**
   * The date and time of the change.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/text.html#adef-datetime">W3C HTML Specification</a>
   */
  public final native String getDateTime() /*-{
    return this.dateTime;
  }-*/;

  /**
   * A URI designating a document that describes the reason for the change.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/">W3C HTML Specification</a>
   */
  public final native void setCite(String cite) /*-{
    this.cite = cite;
  }-*/;

  /**
   * The date and time of the change.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/text.html#adef-datetime">W3C HTML Specification</a>
   */
  public final native void setDateTime(String dateTime) /*-{
    this.dateTime = dateTime;
  }-*/;
}
