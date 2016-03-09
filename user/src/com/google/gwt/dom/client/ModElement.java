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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.annotations.IsSafeUri;

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
    assert is(elem);
    return (ModElement) elem;
  }
  
  /**
   * Determines whether the given {@link JavaScriptObject} can be cast to
   * this class. A <code>null</code> object will cause this method to
   * return <code>false</code>.
   */
  public static boolean is(JavaScriptObject o) {
    if (Element.is(o)) {
      return is((Element) o);
    }
    return false;
  }

  /**
   * Determine whether the given {@link Node} can be cast to this class.
   * A <code>null</code> node will cause this method to return
   * <code>false</code>.
   */
  public static boolean is(Node node) {
    if (Element.is(node)) {
      return is((Element) node);
    }
    return false;
  }
  
  /**
   * Determine whether the given {@link Element} can be cast to this class.
   * A <code>null</code> node will cause this method to return
   * <code>false</code>.
   */
  public static boolean is(Element elem) {
    return elem != null &&
        ( elem.getTagName().equalsIgnoreCase(TAG_INS) ||
          elem.getTagName().equalsIgnoreCase(TAG_DEL) );
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
  public final void setCite(SafeUri cite) {
    setCite(cite.asString());
  }

  /**
   * A URI designating a document that describes the reason for the change.
   *
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/">W3C HTML Specification</a>
   */
  public final native void setCite(@IsSafeUri String cite) /*-{
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
