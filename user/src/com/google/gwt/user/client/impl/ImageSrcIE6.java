/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.user.client.impl;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Element;

/**
 * Works around an IE problem where multiple images trying to load at the same
 * time will generate a request per image. We fix this by only allowing the
 * first image of a given URL to set its source immediately, but simultaneous
 * requests for the same URL don't actually get their source set until the
 * original load is complete.
 */
class ImageSrcIE6 {

  /**
   * A native map of image source URL strings to Image objects. All Image
   * objects with values in this map are waiting on an asynchronous load to
   * complete and have event handlers hooked. The moment the image finishes
   * loading, it will be removed from this map.
   */
  private static JavaScriptObject srcImgMap;
  
  static {
    executeBackgroundImageCacheCommand();
  }
  
  /**
   * Returns the src of the image, or the pending src if the image is pending. 
   */
  public static native String getImgSrc(Element img) /*-{
    return img.__pendingSrc || img.src;
  }-*/;

  /**
   * Sets the src of the image, queuing up with other requests for the same
   * URL if necessary.
   */
  public static void setImgSrc(Element img, String src) {
    // Early out for no-op prop set.
    if (getImgSrc(img).equals(src)) {
      return;
    }
    // Lazy init the map
    if (srcImgMap == null) {
      srcImgMap = JavaScriptObject.createObject();
    }
    // See if this element is already pending.
    String oldSrc = getPendingSrc(img);
    if (oldSrc != null) {
      // The element is pending; there must be a top node for this src.
      Element top = getTop(srcImgMap, oldSrc);
      assert (top != null);
      if (top.equals(img)) {
        // It's a pending parent.
        removeTop(srcImgMap, top);
      } else {
        // It's a pending child.
        removeChild(top, img);
      }
    }

    // Now load the new src URL.
    Element top = getTop(srcImgMap, src);
    if (top == null) {
      // There is no existing pending parent.
      addTop(srcImgMap, img, src);
    } else {
      // There is an existing pending parent.
      addChild(top, img);
    }
  }

  /**
   * Adds an image as a child to a pending parent.
   */
  private static native void addChild(Element parent, Element child) /*-{
    parent.__kids.push(child);
    child.__pendingSrc = parent.__pendingSrc;
  }-*/;

  /**
   * Sets an image as the pending parent for the specified URL.
   */
  private static native void addTop(JavaScriptObject srcImgMap, Element img, String src) /*-{
    // No outstanding requests; load the image.
    img.src = src;
    
    // If the image was in cache, the load may have just happened synchronously.
    if (img.complete) {
      // We're done
      return;
    }

    // Image is loading asynchronously; put in map for chaining.
    img.__kids = [];
    img.__pendingSrc = src;
    srcImgMap[src] = img;
    
    var _onload = img.onload, _onerror = img.onerror, _onabort = img.onabort;
    
    // Same cleanup code matter what state we end up in.
    function finish(_originalHandler) {
      // Grab a copy of the kids.
      var kids = img.__kids;
      img.__cleanup();

      // Set the src for all kids in a timer to ensure caching has happened.
      window.setTimeout(function() {
        for (var i = 0; i < kids.length; ++i) {
          var kid = kids[i];
          if (kid.__pendingSrc == src) {
            kid.src = src;
            kid.__pendingSrc = null;
          }
        }
      }, 0);      

      // Call the original handler, if any.
      _originalHandler && _originalHandler.call(img);
    }

    img.onload = function() {
      finish(_onload);
    }
    img.onerror = function() {
      finish(_onerror);
    }
    img.onabort = function() {
      finish(_onabort);
    }
    
    img.__cleanup = function() {
      img.onload = _onload;
      img.onerror = _onerror;
      img.onabort = _onabort;
      img.__cleanup = img.__pendingSrc = img.__kids = null;
      delete srcImgMap[src];
    }
  }-*/;

  private static native void executeBackgroundImageCacheCommand() /*-{
    // Fix IE background image refresh bug, present through IE6
    // see http://www.mister-pixel.com/#Content__state=is_that_simple
    // this only works with IE6 SP1+
    try {
      $doc.execCommand("BackgroundImageCache", false, true);
    } catch (e) {
      // ignore error on other browsers
    }
  }-*/;

  /**
   * Returns the pending src URL of an image, or <code>null</code> if the image
   * has no pending src URL.
   */
  private static native String getPendingSrc(Element img) /*-{
    return img.__pendingSrc || null;
  }-*/;

  /**
   * Returns the pending parent for the specified URL, or <code>null</code> if
   * there is no pending parent for the specified URL.
   */
  private static native Element getTop(JavaScriptObject srcImgMap, String src) /*-{
    return srcImgMap[src] || null;
  }-*/;

  /**
   * Removes a child image from its pending parent.
   */
  private static native void removeChild(Element parent, Element child) /*-{
    var kids = parent.__kids;
    for (var i = 0, c = kids.length; i < c; ++i) {
      if (kids[i] === child) {
        kids.splice(i, 1);
        child.__pendingSrc = null;
        return;
      } 
    }
  }-*/;

  /**
   * Removes a pending parent's pending status, in preparation for changing its
   * URL to something else.
   */
  private static native void removeTop(JavaScriptObject srcImgMap, Element img) /*-{
    var src = img.__pendingSrc;
    var kids = img.__kids;
    img.__cleanup();

    // Restructure the kids, if any.
    if (img = kids[0]) {
      // Try to elect a new top node.
      img.__pendingSrc = null;
      @com.google.gwt.user.client.impl.ImageSrcIE6::addTop(Lcom/google/gwt/core/client/JavaScriptObject;Lcom/google/gwt/user/client/Element;Ljava/lang/String;)(srcImgMap, img, src);
      if (img.__pendingSrc) {
        // It became a top node, add the rest as children.
        kids.splice(0, 1);
        img.__kids = kids;
      } else {
        // It loaded immediately; just finish the rest.
        // This is an extremely unlikely case, but could theoretically happen
        // depending on how a browser's network and UI threads are synchronized.
        for (var i = 1, c = kids.length; i < c; ++i) {
          kids[i].src = src;
          kids[i].__pendingSrc = null;
        }
      }
    }
  }-*/;
}
