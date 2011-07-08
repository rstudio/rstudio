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
 * The anchor element.
 * 
 * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#edef-A">W3C HTML Specification</a>
 */
@TagName(AnchorElement.TAG)
public class AnchorElement extends Element {

  public static final String TAG = "a";

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static AnchorElement as(Element elem) {
    assert elem.getTagName().equalsIgnoreCase(TAG);
    return (AnchorElement) elem;
  }

  protected AnchorElement() {
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
   * The absolute URI of the linked resource.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-href">W3C HTML Specification</a>
   */
  public final native String getHref() /*-{
    return this.href;
  }-*/;

  /**
   * Language code of the linked resource.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-hreflang">W3C HTML Specification</a>
   */
  public final native String getHreflang() /*-{
     return this.hreflang;
   }-*/;

  /**
   * Anchor name.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-name-A">W3C HTML Specification</a>
   */
  public final native String getName() /*-{
     return this.name;
   }-*/;

  /**
   * Forward link type.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-rel">W3C HTML Specification</a>
   */
  public final native String getRel() /*-{
     return this.rel;
   }-*/;

  /**
   * Frame to render the resource in.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-target">W3C HTML Specification</a>
   */
  public final native String getTarget() /*-{
    return this.target;
  }-*/;

  /**
   * Advisory content type.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-type-A">W3C HTML Specification</a>
   */
  public final native String getType() /*-{
     return this.type;
   }-*/;

  /**
   * A single character access key to give access to the form control.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-accesskey">W3C HTML Specification</a>
   */
  public final native void setAccessKey(String accessKey) /*-{
    this.accessKey = accessKey;
  }-*/;

  /**
   * The absolute URI of the linked resource.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-href">W3C HTML Specification</a>
   */
  public final native void setHref(String href) /*-{
    this.href = href;
  }-*/;

  /**
   * Language code of the linked resource.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-hreflang">W3C HTML Specification</a>
   */
  public final native void setHreflang(String hreflang) /*-{
     this.hreflang = hreflang;
   }-*/;

  /**
   * Anchor name.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-name-A">W3C HTML Specification</a>
   */
  public final native void setName(String name) /*-{
     this.name = name;
   }-*/;

  /**
   * Forward link type.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-rel">W3C HTML Specification</a>
   */
  public final native void setRel(String rel) /*-{
     this.rel = rel;
   }-*/;

  /**
   * Frame to render the resource in.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-target">W3C HTML Specification</a>
   */
  public final native void setTarget(String target) /*-{
    this.target = target;
  }-*/;

  /**
   * Advisory content type.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-type-A">W3C HTML Specification</a>
   */
  public final native void setType(String type) /*-{
     this.type = type;
   }-*/;
}
