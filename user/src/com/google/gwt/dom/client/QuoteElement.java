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
 * For the Q and BLOCKQUOTE elements.
 * 
 * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/text.html#edef-Q">W3C HTML Specification</a>
 */
@TagName({QuoteElement.TAG_BLOCKQUOTE, QuoteElement.TAG_Q})
public class QuoteElement extends Element {

  public static final String TAG_BLOCKQUOTE = "blockquote";
  public static final String TAG_Q = "q";

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static QuoteElement as(Element elem) {
    assert elem.getTagName().equalsIgnoreCase(TAG_Q)
        || elem.getTagName().equalsIgnoreCase(TAG_BLOCKQUOTE);
    return (QuoteElement) elem;
  }

  protected QuoteElement() {
  }

  /**
   * A URI designating a source document or message.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/text.html#adef-cite-Q">W3C HTML Specification</a>
   */
  public final native String getCite() /*-{
    return this.cite;
  }-*/;

  /**
   * A URI designating a source document or message.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/text.html#adef-cite-Q">W3C HTML Specification</a>
   */
  public final native void setCite(String cite) /*-{
    this.cite = cite;
  }-*/;
}
