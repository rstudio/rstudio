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
 * The LINK element specifies a link to an external resource, and defines this
 * document's relationship to that resource (or vice versa).
 * 
 * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#edef-LINK">W3C HTML Specification</a>
 */
@TagName(LinkElement.TAG)
public class LinkElement extends Element {

  public static final String TAG = "link";

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static LinkElement as(Element elem) {
    assert elem.getTagName().equalsIgnoreCase(TAG);
    return (LinkElement) elem;
  }

  protected LinkElement() {
  }

  /**
   * Enables/disables the link. This is currently only used for style sheet
   * links, and may be used to activate or deactivate style sheets.
   * @deprecated use {@link #isDisabled()} instead.
   */
  @Deprecated
  public final native boolean getDisabled() /*-{
    return !!this.disabled;
  }-*/;

  /**
   * The URI of the linked resource.
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
   * Designed for use with one or more target media.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/styles.html#adef-media">W3C HTML Specification</a>
   */
  public final native String getMedia() /*-{
     return this.media;
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
   * Enables/disables the link. This is currently only used for style sheet
   * links, and may be used to activate or deactivate style sheets.
   */
  public final native boolean isDisabled() /*-{
    return !!this.disabled;
  }-*/;

  /**
   * Enables/disables the link. This is currently only used for style sheet
   * links, and may be used to activate or deactivate style sheets.
   */
  public final native void setDisabled(boolean disabled) /*-{
    this.disabled = disabled;
  }-*/;

  /**
   * The URI of the linked resource.
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
   * Designed for use with one or more target media.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/styles.html#adef-media">W3C HTML Specification</a>
   */
  public final native void setMedia(String media) /*-{
     this.media = media;
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
