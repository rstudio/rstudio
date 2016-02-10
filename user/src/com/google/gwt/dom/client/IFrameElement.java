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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.annotations.IsSafeUri;

/**
 * Inline subwindows.
 * 
 * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#edef-IFRAME">W3C HTML Specification</a>
 */
@TagName(IFrameElement.TAG)
public class IFrameElement extends Element {

  public static final String TAG = "iframe";

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static IFrameElement as(Element elem) {
    assert is(elem);
    return (IFrameElement) elem;
  }

  /**
   * Determines whether the given {@link JavaScriptObject} can be cast to
   * this class. A <code>null</code> object will cause this method to
   * return <code>false</code>.
   */
  public static boolean is(JavaScriptObject o) {
    if (Element.is(o)) {
      return is((Element) o);
    }
    return false;
  }

  /**
   * Determine whether the given {@link Node} can be cast to this class.
   * A <code>null</code> node will cause this method to return
   * <code>false</code>.
   */
  public static boolean is(Node node) {
    if (Element.is(node)) {
      return is((Element) node);
    }
    return false;
  }
  
  /**
   * Determine whether the given {@link Element} can be cast to this class.
   * A <code>null</code> node will cause this method to return
   * <code>false</code>.
   */
  public static boolean is(Element elem) {
    return elem != null && elem.hasTagName(TAG);
  }

  protected IFrameElement() {
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
  public final void setSrc(SafeUri src) {
    setSrc(src.asString());
  }

  /**
   * A URI designating the initial frame contents.
   *
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/present/frames.html#adef-src-FRAME">W3C HTML Specification</a>
   */
  public final native void setSrc(@IsSafeUri String src) /*-{
     this.src = src;
   }-*/;
}
