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
 * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#edef-AREA
 */
public class AreaElement extends Element {

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static AreaElement as(Element elem) {
    assert elem.getTagName().equalsIgnoreCase("area");
    return (AreaElement) elem;
  }

  protected AreaElement() {
  }

  /**
   * A single character access key to give access to the form control.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-accesskey
   */
  public final native String getAccessKey() /*-{
     return this.accessKey;
   }-*/;

  /**
   * Alternate text for user agents not rendering the normal content of this
   * element.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-alt
   */
  public final native String getAlt() /*-{
     return this.alt;
   }-*/;

  /**
   * Comma-separated list of lengths, defining an active region geometry. See
   * also shape for the shape of the region.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-coords
   */
  public final native String getCoords() /*-{
     return this.coords;
   }-*/;

  /**
   * The URI of the linked resource.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-href
   */
  public final native String getHref() /*-{
     return this.href;
   }-*/;

  /**
   * The shape of the active area. The coordinates are given by coords.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-shape
   */
  public final native String getShape() /*-{
     return this.shape;
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
   * A single character access key to give access to the form control.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-accesskey
   */
  public final native void setAccessKey(String accessKey) /*-{
     this.accessKey = accessKey;
   }-*/;

  /**
   * Alternate text for user agents not rendering the normal content of this
   * element.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-alt
   */
  public final native void setAlt(String alt) /*-{
     this.alt = alt;
   }-*/;

  /**
   * Comma-separated list of lengths, defining an active region geometry. See
   * also shape for the shape of the region.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-coords
   */
  public final native void setCoords(String coords) /*-{
     this.coords = coords;
   }-*/;

  /**
   * The URI of the linked resource.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/links.html#adef-href
   */
  public final native void setHref(String href) /*-{
     this.href = href;
   }-*/;

  /**
   * The shape of the active area. The coordinates are given by coords.
   * 
   * @see http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-shape
   */
  public final native void setShape(String shape) /*-{
     this.shape = shape;
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
}
