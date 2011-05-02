/*
 * Copyright 2011 Google Inc.
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
 * Video element.
 * 
 * <p>
 * <span style="color:red">Experimental API: This API is still under development
 * and is subject to change.
 * </span>
 * </p>
 * 
 * @see <a href="http://www.w3.org/TR/html5/video.html#video">W3C HTML 5 Specification</a>
 */
@TagName(VideoElement.TAG)
public class VideoElement extends MediaElement {

  /**
   * The tag for this element.
   */
  public static final String TAG = "video";

  /**
   * The audio type of MP4 encoded video.
   */
  public static final String TYPE_MP4 = "video/mp4";

  /**
   * The audio type of Ogg encoded video.
   */
  public static final String TYPE_OGG = "video/ogg";

  /**
   * The audio type of WebM encoded audio.
   */
  public static final String TYPE_WEBM = "video/webm";

  protected VideoElement() {
  }

  /**
   * Gets the height of the element.
   * 
   * @return the height, in pixels
   * @see #setHeight(int)
   */
  public final native int getHeight() /*-{
    return this.height;
  }-*/;

  /**
   * Returns a poster URL.
   * 
   * @return a URL containing a poster image
   *
   * @see #setPoster(String)
   */
  public final native String getPoster() /*-{
    return this.poster;
  }-*/;

  /**
   * Gets the intrinsic height of video within the element.
   * 
   * @return the height, in pixels
   * @see #setHeight(int)
   */
  public final native int getVideoHeight() /*-{
    return this.videoHeight;
  }-*/;

  /**
   * Gets the instrinsic width of the video within the element.
   * 
   * @return the width, in pixels
   * @see #setWidth(int)
   */
  public final native int getVideoWidth() /*-{
    return this.videoWidth;
  }-*/;
  
  /**
   * Gets the width of the element.
   * 
   * @return the width, in pixels
   * @see #setWidth(int)
   */
  public final native int getWidth() /*-{
    return this.width;
  }-*/;

  /**
   * Sets the height of the element.
   * 
   * @param height the height, in pixels
   * @see #getHeight()
   */
  public final native void setHeight(int height) /*-{
    this.height = height;
  }-*/;

  /**
   * Sets the poster URL.
   * 
   * @param url the poster image URL
   * @see #getPoster
   */
  public final native void setPoster(String url) /*-{
    this.poster = url;
  }-*/;

  /**
   * Sets the width of the element.
   * 
   * @param width the width, in pixels
   * @see #getWidth()
   */
  public final native void setWidth(int width) /*-{
    this.width = width;
  }-*/;
}
