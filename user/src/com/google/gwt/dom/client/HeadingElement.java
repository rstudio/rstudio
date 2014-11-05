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

import java.util.Locale;

/**
 * For the H1 to H6 elements.
 * 
 * @see <a
 *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/global.html#edef-H1">W3C
 *      HTML Specification</a>
 */
@TagName({HeadingElement.TAG_H1, HeadingElement.TAG_H2, HeadingElement.TAG_H3,
    HeadingElement.TAG_H4, HeadingElement.TAG_H5, HeadingElement.TAG_H6})
public class HeadingElement extends Element {

  static final String[] TAGS = {
      HeadingElement.TAG_H1, HeadingElement.TAG_H2, HeadingElement.TAG_H3,
      HeadingElement.TAG_H4, HeadingElement.TAG_H5, HeadingElement.TAG_H6};

  public static final String TAG_H1 = "h1";
  public static final String TAG_H2 = "h2";
  public static final String TAG_H3 = "h3";
  public static final String TAG_H4 = "h4";
  public static final String TAG_H5 = "h5";
  public static final String TAG_H6 = "h6";

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static HeadingElement as(Element elem) {
    if (HeadingElement.class.desiredAssertionStatus()) {
      assert is(elem);
    }

    return (HeadingElement) elem;
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
    
    if (elem == null) {
      return false;
    }
    
    String tag = elem.getTagName().toLowerCase(Locale.ROOT);
    
    if (tag.length() != 2) {
      return false;
    }
    
    if (tag.charAt(0) != 'h') {
      return false;
    }

    int n = Integer.parseInt(tag.substring(1, 2));
    if (n < 1 || n > 6) {
      return false;
    }
    
    return true;
  }

  protected HeadingElement() {
  }
}
