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
package com.google.gwt.event.dom.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DataTransfer;
import com.google.gwt.dom.client.PartialSupport;
import com.google.gwt.event.shared.EventHandler;

/**
 * Base class for drag and drop events.
 * 
 * @param <H> handler type
 */
@PartialSupport
public abstract class DragDropEventBase<H extends EventHandler> extends DomEvent<H> {

  /**
   * Detector for browser support of drag events.
   */
  static class DragSupportDetector {

    private final boolean isSupported = detectDragSupport();

    /**
     * Using a run-time check, return true if drag events are supported.
     * 
     * @return true if supported, false otherwise.
     */
    public boolean isSupported() {
      return isSupported;
    }

    private native boolean detectDragSupport() /*-{
      var elem = document.createElement('div');
      elem.setAttribute('ondragstart', 'return;');
      return (typeof elem.ondragstart) == "function";
    }-*/;
  }

  /**
   * Detector for permutations that do not support drag events.
   */
  static class DragSupportDetectorNo extends DragSupportDetector {
    @Override
    public boolean isSupported() {
      return false;
    }
  }

  /**
   * The implementation singleton.
   */
  private static DragSupportDetector impl;

  /**
   * Runtime check for whether drag events are supported in this browser.
   * 
   * @return true if supported, false if not
   */
  public static boolean isSupported() {
    if (impl == null) {
      impl = GWT.create(DragSupportDetector.class);
    }
    return impl.isSupported();
  }

  /**
   * Get the data for the specified format from the {@link DataTransfer} object.
   * 
   * @param format the format
   * @return the data for the specified format
   */
  public String getData(String format) {
    DataTransfer dt = getDataTransfer();
    return getDataTransfer().getData(format);
  }

  /**
   * Get the {@link DataTransfer} associated with the current drag event.
   * 
   * @return the {@link DataTransfer} object
   */
  public DataTransfer getDataTransfer() {
    return getNativeEvent().getDataTransfer();
  }

  /**
   * Set the data in the {@link DataTransfer} object for the specified format.
   * 
   * @param format the format
   * @param data the data to associate with the format
   */
  public void setData(String format, String data) {
    getDataTransfer().setData(format, data);
  }
}
