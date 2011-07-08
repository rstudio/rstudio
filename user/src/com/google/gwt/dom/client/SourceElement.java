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
 * The SOURCE element specifies one of potentially multiple source file in a
 * media element.
 * 
 * @see <a href="http://www.w3.org/TR/html5/video.html#the-source-element">W3C
 *      HTML Specification</a>
 */
@TagName(SourceElement.TAG)
public class SourceElement extends Element {

  public static final String TAG = "source";

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static SourceElement as(Element elem) {
    assert elem.getTagName().equalsIgnoreCase(TAG);
    return (SourceElement) elem;
  }

  protected SourceElement() {
  }

  /**
   * Returns the source URL for the media, or {@code null} if none is set.
   * 
   * @return a String URL or {@code null}
   * 
   * @see #setSrc(String)
   */
  public final native String getSrc() /*-{
    return this.src;
  }-*/;

  /**
   * Returns the type of media represented by the src, or {@code null} if none
   * is set.
   * 
   * @return a String type, or {@code null}
   * 
   * @see #setType(String)
   */
  public final native String getType() /*-{
    return this.type;
  }-*/;

  /**
   * Sets the source URL for the media.
   * 
   * @param url a String URL
   * 
   * @see #getSrc()
   */
  public final native void setSrc(String url) /*-{
    this.src = url;
  }-*/;

  /**
   * Sets the type of media represented by the src. The browser will look at the
   * type when deciding which source files to request from the server.
   * 
   * <p>
   * The type is the format or encoding of the media represented by the source
   * element. For example, the type of an {@link AudioElement} could be one of
   * {@value AudioElement#TYPE_OGG}, {@link AudioElement#TYPE_MP3}, or
   * {@link AudioElement#TYPE_WAV}.
   * </p>
   * 
   * <p>
   * You can also add the codec information to the type, giving the browser even
   * more information about whether or not it can play the file (Example: "
   * <code>audio/ogg; codec=vorbis</code>");
   * </p>
   * 
   * @param type the media type
   * 
   * @see #getType()
   */
  public final native void setType(String type) /*-{
    this.type = type;
  }-*/;
}
