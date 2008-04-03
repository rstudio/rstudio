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
 * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#edef-A
 */
public class AnchorElement extends Element {

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static AnchorElement as(Element elem) {
    assert elem.getTagName().equalsIgnoreCase("a");
    return (AnchorElement) elem;
  }

  protected AnchorElement() {
  }

  /**
   * Removes keyboard focus from this element.
   */
  public final native void blur() /*-{
    this.blur();
  }-*/;

  /**
   * Gives keyboard focus to this element.
   */
  public final native void focus() /*-{
    this.focus();
  }-*/;

  /**
   * A single character access key to give access to the form control.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-accesskey
   */
  public final native String getAccessKey() /*-{
    return this.accessKey;
  }-*/;

  /**
   * The absolute URI of the linked resource.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-href
   */
  public final native String getHref() /*-{
    return this.href;
  }-*/;

  /**
   * Language code of the linked resource.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-hreflang
   */
  public final native String getHreflang() /*-{
     return this.hreflang;
   }-*/;

  /**
   * Anchor name.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-name-A
   */
  public final native String getName() /*-{
     return this.name;
   }-*/;

  /**
   * Forward link type.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-rel
   */
  public final native String getRel() /*-{
     return this.name;
   }-*/;

  /**
   * Index that represents the element's position in the tabbing order.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-tabindex
   */
  public final native int getTabIndex() /*-{
    return this.tabIndex;
  }-*/;

  /**
   * Frame to render the resource in.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-target
   */
  public final native String getTarget() /*-{
    return this.target;
  }-*/;

  /**
   * Advisory content type.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-type-A
   */
  public final native String getType() /*-{
     return this.type;
   }-*/;

  /**
   * A single character access key to give access to the form control.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-accesskey
   */
  public final native void setAccessKey(String accessKey) /*-{
    this.accessKey = accessKey;
  }-*/;

  /**
   * The absolute URI of the linked resource.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-href
   */
  public final native void setHref(String href) /*-{
    this.href = href;
  }-*/;

  /**
   * Language code of the linked resource.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-hreflang
   */
  public final native void setHreflang(String hreflang) /*-{
     this.hreflang = hreflang;
   }-*/;

  /**
   * Anchor name.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-name-A
   */
  public final native void setName(String name) /*-{
     this.name = name;
   }-*/;

  /**
   * Forward link type.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-rel
   */
  public final native void setRel(String name) /*-{
     this.name = name;
   }-*/;

  /**
   * Index that represents the element's position in the tabbing order.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-tabindex
   */
  public final native void setTabIndex(int tabIndex) /*-{
    this.tabIndex = tabIndex;
  }-*/;

  /**
   * Frame to render the resource in.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-target
   */
  public final native void setTarget(String target) /*-{
    this.target = target;
  }-*/;

  /**
   * Advisory content type.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-type-A
   */
  public final native void setType(String type) /*-{
     this.type = type;
   }-*/;
}
