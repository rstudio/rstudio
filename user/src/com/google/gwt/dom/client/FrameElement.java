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
 * Create a frame.
 * 
 * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#edef-FRAME">W3C HTML Specification</a>
 */
@TagName(FrameElement.TAG)
public class FrameElement extends Element {

  public static final String TAG = "frame";

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static FrameElement as(Element elem) {
    assert elem.getTagName().equalsIgnoreCase(TAG);
    return (FrameElement) elem;
  }

  protected FrameElement() {
  }

  /**
   * The document this frame contains, if there is any and it is available, or
   * null otherwise.
   */
  public final native Document getContentDocument() /*-{
     // This is known to work on all modern browsers.
     return this.contentWindow.document;
   }-*/;

  /**
   * Request frame borders.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-frameborder">W3C HTML Specification</a>
   */
  public final native int getFrameBorder() /*-{
     return this.frameBorder;
   }-*/;

  /**
   * URI designating a long description of this image or frame.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-longdesc-FRAME">W3C HTML Specification</a>
   */
  public final native String getLongDesc() /*-{
     return this.longDesc;
   }-*/;

  /**
   * Frame margin height, in pixels.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-marginheight">W3C HTML Specification</a>
   */
  public final native int getMarginHeight() /*-{
     return this.marginHeight;
   }-*/;

  /**
   * Frame margin width, in pixels.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-marginwidth">W3C HTML Specification</a>
   */
  public final native int getMarginWidth() /*-{
     return this.marginWidth;
   }-*/;

  /**
   * The frame name (object of the target attribute).
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-name-FRAME">W3C HTML Specification</a>
   */
  public final native String getName() /*-{
     return this.name;
   }-*/;

  /**
   * Specify whether or not the frame should have scrollbars.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-scrolling">W3C HTML Specification</a>
   */
  public final native String getScrolling() /*-{
     return this.scrolling;
   }-*/;

  /**
   * A URI designating the initial frame contents.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-src-FRAME">W3C HTML Specification</a>
   */
  public final native String getSrc() /*-{
     return this.src;
   }-*/;

  /**
   * When true, forbid user from resizing frame.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-noresize">W3C HTML Specification</a>
   */
  public final native boolean isNoResize() /*-{
     return !!this.noResize;
   }-*/;

  /**
   * Request frame borders.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-frameborder">W3C HTML Specification</a>
   */
  public final native void setFrameBorder(int frameBorder) /*-{
     this.frameBorder = frameBorder;
   }-*/;

  /**
   * URI designating a long description of this image or frame.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-longdesc-FRAME">W3C HTML Specification</a>
   */
  public final native void setLongDesc(String longDesc) /*-{
     this.longDesc = longDesc;
   }-*/;

  /**
   * Frame margin height, in pixels.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-marginheight">W3C HTML Specification</a>
   */
  public final native void setMarginHeight(int marginHeight) /*-{
     this.marginHeight = marginHeight;
   }-*/;

  /**
   * Frame margin width, in pixels.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-marginwidth">W3C HTML Specification</a>
   */
  public final native void setMarginWidth(int marginWidth) /*-{
     this.marginWidth = marginWidth;
   }-*/;

  /**
   * The frame name (object of the target attribute).
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-name-FRAME">W3C HTML Specification</a>
   */
  public final native void setName(String name) /*-{
     this.name = name;
   }-*/;

  /**
   * When true, forbid user from resizing frame.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-noresize">W3C HTML Specification</a>
   */
  public final native void setNoResize(boolean noResize) /*-{
     this.noResize = noResize;
   }-*/;

  /**
   * Specify whether or not the frame should have scrollbars.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-scrolling">W3C HTML Specification</a>
   */
  public final native void setScrolling(String scrolling) /*-{
     this.scrolling = scrolling;
   }-*/;

  /**
   * A URI designating the initial frame contents.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-src-FRAME">W3C HTML Specification</a>
   */
  public final native void setSrc(String src) /*-{
     this.src = src;
   }-*/;
}
