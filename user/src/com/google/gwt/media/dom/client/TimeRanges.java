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
 * A {@link JavaScriptObject} representing a time range returned from a
 * {@link com.google.gwt.dom.client.MediaElement MediaElement}.
 * 
 * <p>
 * <span style="color:red">Experimental API: This API is still under development
 * and is subject to change.
 * </span>
 * </p>
 * 
 * @see com.google.gwt.dom.client.MediaElement#getBuffered()
 * @see com.google.gwt.dom.client.MediaElement#getPlayed()
 * @see com.google.gwt.dom.client.MediaElement#getSeekable()
 */
public final class TimeRanges extends JavaScriptObject {

  protected TimeRanges() {
  }

  /**
   * Returns the end time of the range indexed by {@code index}.
   * 
   * @param index the range index, between 0 (inclusive) and {@link #length()}
   *          (exclusive)
   * @return a double indicating the end time in seconds
   * 
   * @see #start(int)
   */
  public native double end(int index) /*-{
    return this.end(index);
  }-*/;

  /**
   * Returns the number of distinct ranges contained in this object.
   *
   * @return an integer number of ranges
   */
  public native int length() /*-{
    return this.length;
  }-*/;
  
  /**
   * Returns the start time of the range indexed by {@code index}.
   * 
   * @param index the range index, between 0 (inclusive) and {@link #length()}
   *          (exclusive)
   * @return a double indicating the start time in seconds
   * 
   * @see #end(int)
   */
  public native double start(int index) /*-{
    return this.start(index);
  }-*/;
}
