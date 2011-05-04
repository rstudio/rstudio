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

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Class representing DataTransfer interface.
 * 
 * <p>
 * <span style="color:red">Experimental API: This API is still under development
 * and is subject to change. </span>
 * </p>
 * 
 * @see <a
 *      href="http://www.w3.org/TR/html5/dnd.html#the-datatransfer-interface">W3C
 *      HTML Specification</a>
 */
public class DataTransfer extends JavaScriptObject {

  /**
   * Required constructor for GWT compiler to function.
   */
  protected DataTransfer() {
  }

  /**
   * Remove all data from the current drag sequence.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/html5/dnd.html#dom-datatransfer-cleardata">W3C
   *      Specification</a>
   */
  public final native void clearData() /*-{
    this.clearData();
  }-*/;

  /**
   * Remove the data for the specified format for all drag events in the current
   * drag sequence.
   * 
   * @param format the format, which is usually the mime-type of the associated
   *          data
   * @see #setData(String, String)
   * @see <a
   *      href="http://www.w3.org/TR/html5/dnd.html#dom-datatransfer-cleardata">W3C
   *      Specification</a>
   */
  public final native void clearData(String format) /*-{
    this.clearData(format);
  }-*/;

  /**
   * Get the data for the specified format. The data may have been set in a
   * previous drag event that is part of the current drag sequence.
   * 
   * @param format the format, which is usually the mime-type of the data
   * @return the data for the specified format
   * @see #setData(String, String)
   * @see <a
   *      href="http://www.w3.org/TR/html5/dnd.html#dom-datatransfer-getdata">W3C
   *      Specification</a>
   */
  public final native String getData(String format) /*-{
    return this.getData(format);
  }-*/;

  /**
   * Set the data for the specified format to associate with all drag events in
   * the current drag and drop sequence. The data can be read using
   * {@link #getData(String)} from any subsequent drag events in this sequence
   * (such as the drop event).
   * 
   * <p>
   * The format is usually the mime-type of the data, but can also be
   * <code>text</code>.
   * </p>
   * 
   * @param format the format, which is usually the mime-type of the data
   * @param data the data to associate with the format
   * @see <a
   *      href="http://www.w3.org/TR/html5/dnd.html#dom-datatransfer-setdata">W3C
   *      Specification</a>
   */
  public final native void setData(String format, String data) /*-{
    this.setData(format, data);
  }-*/;

  /**
   * Specify the element to use to update the drag feedback.
   * 
   * @param element the feedback image
   * @param x the x offset of the cursor
   * @param y the y offset of the cursor
   * @see <a
   *      href="http://www.w3.org/TR/html5/dnd.html#dom-datatransfer-setdragimage">W3C
   *      Specification</a>
   */
  public final native void setDragImage(Element element, int x, int y) /*-{
    if (this.setDragImage) {
      this.setDragImage(element, x, y);
    }
  }-*/;
}
