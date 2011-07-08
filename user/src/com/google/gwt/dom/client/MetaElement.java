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
 * This contains generic meta-information about the document.
 * 
 * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/global.html#edef-META">W3C HTML Specification</a>
 */
@TagName(MetaElement.TAG)
public class MetaElement extends Element {

  public static final String TAG = "meta";

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static MetaElement as(Element elem) {
    assert elem.getTagName().equalsIgnoreCase(TAG);
    return (MetaElement) elem;
  }

  protected MetaElement() {
  }

  /**
   * Associated information.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/global.html#adef-content">W3C HTML Specification</a>
   */
  public final native String getContent() /*-{
     return this.content;
   }-*/;

  /**
   * HTTP response header name [IETF RFC 2616].
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/global.html#adef-http-equiv">W3C HTML Specification</a>
   */
  public final native String getHttpEquiv() /*-{
     return this.httpEquiv;
   }-*/;

  /**
   * Meta information name.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/global.html#adef-name-META">W3C HTML Specification</a>
   */
  public final native String getName() /*-{
     return this.name;
   }-*/;

  /**
   * Associated information.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/global.html#adef-content">W3C HTML Specification</a>
   */
  public final native void setContent(String content) /*-{
     this.content = content;
   }-*/;

  /**
   * HTTP response header name [IETF RFC 2616].
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/global.html#adef-http-equiv">W3C HTML Specification</a>
   */
  public final native void setHttpEquiv(String httpEquiv) /*-{
     this.httpEquiv = httpEquiv;
   }-*/;

  /**
   * Meta information name.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/global.html#adef-name-META">W3C HTML Specification</a>
   */
  public final native void setName(String name) /*-{
     this.name = name;
   }-*/;
}
