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
 * The HTML document body. This element is always present in the DOM API, even
 * if the tags are not present in the source document.
 * 
 * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/global.html#edef-BODY">W3C HTML Specification</a>
 */
@TagName(BodyElement.TAG)
public class BodyElement extends Element {

  public static final String TAG = "body";

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static BodyElement as(Element elem) {
    assert elem.getTagName().equalsIgnoreCase(TAG);
    return (BodyElement) elem;
  }

  protected BodyElement() {
  }
}
