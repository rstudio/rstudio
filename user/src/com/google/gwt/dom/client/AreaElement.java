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
 * Client-side image map area definition.
 * 
 * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#edef-AREA">W3C HTML Specification</a>
 */
@TagName(AreaElement.TAG)
public class AreaElement extends Element {

  public static final String TAG = "area";

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static AreaElement as(Element elem) {
    assert elem.getTagName().equalsIgnoreCase(TAG);
    return (AreaElement) elem;
  }

  protected AreaElement() {
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
   * Alternate text for user agents not rendering the normal content of this
   * element.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-alt">W3C HTML Specification</a>
   */
  public final native String getAlt() /*-{
     return this.alt;
   }-*/;

  /**
   * Comma-separated list of lengths, defining an active region geometry. See
   * also shape for the shape of the region.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-coords">W3C HTML Specification</a>
   */
  public final native String getCoords() /*-{
     return this.coords;
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
   * The shape of the active area. The coordinates are given by coords.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-shape">W3C HTML Specification</a>
   */
  public final native String getShape() /*-{
     return this.shape;
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
   * A single character access key to give access to the form control.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-accesskey">W3C HTML Specification</a>
   */
  public final native void setAccessKey(String accessKey) /*-{
     this.accessKey = accessKey;
   }-*/;

  /**
   * Alternate text for user agents not rendering the normal content of this
   * element.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-alt">W3C HTML Specification</a>
   */
  public final native void setAlt(String alt) /*-{
     this.alt = alt;
   }-*/;

  /**
   * Comma-separated list of lengths, defining an active region geometry. See
   * also shape for the shape of the region.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-coords">W3C HTML Specification</a>
   */
  public final native void setCoords(String coords) /*-{
     this.coords = coords;
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
   * The shape of the active area. The coordinates are given by coords.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-shape">W3C HTML Specification</a>
   */
  public final native void setShape(String shape) /*-{
     this.shape = shape;
   }-*/;

  /**
   * Frame to render the resource in.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-target">W3C HTML Specification</a>
   */
  public final native void setTarget(String target) /*-{
     this.target = target;
   }-*/;
}
