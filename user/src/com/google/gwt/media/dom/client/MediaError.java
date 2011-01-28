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
package com.google.gwt.media.dom.client;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * <p>
 * A {@link JavaScriptObject} indicating the type of error encountered by a
 * {@link com.google.gwt.dom.client.MediaElement MediaElement}.
 * 
 * <p>
 * <span style="color:red">Experimental API: This API is still under development
 * and is subject to change.
 * </span>
 * </p>
 * 
 * @see com.google.gwt.dom.client.MediaElement#getError()
 */
public final class MediaError extends JavaScriptObject {

  /**
   * A constant returned by {@link #getCode} indicating that playback
   * was aborted at the user's request. 
   */
  public static final int MEDIA_ERR_ABORTED = 1;

  /**
   * A constant returned by {@link #getCode} indicating that playback
   * was aborted due to a network error. 
   */
  public static final int MEDIA_ERR_NETWORK = 2;

  /**
   * A constant returned by {@link #getCode} indicating that playback
   * was aborted due to an error in decoding. 
   */
  public static final int MEDIA_ERR_DECODE = 3;

  /**
   * A constant returned by {@link #getCode} indicating that the format
   * of the source stream was unsuitable for playback. 
   */
  public static final int MEDIA_ERR_SRC_NOT_SUPPORTED = 4;

  protected MediaError() {
  }

  /**
   * Returns an error code indicating the reason for the error. 
   *
   * @return one of {@link MediaError#MEDIA_ERR_ABORTED},
   * {@link MediaError#MEDIA_ERR_NETWORK}, {@link MediaError#MEDIA_ERR_DECODE},
   * or {@link MediaError#MEDIA_ERR_SRC_NOT_SUPPORTED}
   */
  public native int getCode() /*-{
    return this.code;
  }-*/;
}
