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
 * Embedded image.
 * 
 * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#edef-IMG">W3C HTML Specification</a>
 */
@TagName(ImageElement.TAG)
public class ImageElement extends Element {

  public static final String TAG = "img";

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static ImageElement as(Element elem) {
    assert elem.getTagName().equalsIgnoreCase(TAG);
    return (ImageElement) elem;
  }

  protected ImageElement() {
  }

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
   * Height of the image in pixels.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-height-IMG">W3C HTML Specification</a>
   */
  public final native int getHeight() /*-{
    return this.height;
  }-*/;

  /**
   * URI designating the source of this image.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-src-IMG">W3C HTML Specification</a>
   */
  public final String getSrc() {
    return DOMImpl.impl.imgGetSrc(this);
  }

  /**
   * The width of the image in pixels.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-width-IMG">W3C HTML Specification</a>
   */
  public final native int getWidth() /*-{
    return this.width;
  }-*/;

  /**
   * Use server-side image map.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-ismap">W3C HTML Specification</a>
   */
  public final native boolean isMap() /*-{
     return !!this.isMap;
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
   * Height of the image in pixels.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-height-IMG">W3C HTML Specification</a>
   */
  public final native void setHeight(int height) /*-{
    this.height = height;
  }-*/;

  /**
   * Use server-side image map.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-ismap">W3C HTML Specification</a>
   */
  public final native void setIsMap(boolean isMap) /*-{
     this.isMap = isMap;
   }-*/;

  /**
   * <p>
   * URI designating the source of this image.
   * </p>
   * <p>
   * If you {@link #cloneNode(boolean)} an {@link ImageElement}, or an element
   * that contains an {@link ImageElement}, then you must call
   * {@link #setSrc(String)} on the cloned element to ensure it is loaded
   * properly in IE6. Failure to do so may cause performance problems, or your
   * image may not load due to an IE6 specific workaround.
   * </p>
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-src-IMG">W3C HTML Specification</a>
   */
  public final void setSrc(String src) {
    DOMImpl.impl.imgSetSrc(this, src);
  }

  /**
   * Use client-side image map.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-usemap">W3C HTML Specification</a>
   */
  public final native void setUseMap(boolean useMap) /*-{
     this.useMap = useMap;
   }-*/;

  /**
   * The width of the image in pixels.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-width-IMG">W3C HTML Specification</a>
   */
  public final native void setWidth(int width) /*-{
      this.width = width;
  }-*/;

  /**
   * Use client-side image map.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/objects.html#adef-usemap">W3C HTML Specification</a>
   */
  public final native boolean useMap() /*-{
     return !!this.useMap;
   }-*/;
}
