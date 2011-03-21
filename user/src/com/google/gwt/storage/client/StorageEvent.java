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

package com.google.gwt.storage.client;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Represents a Storage Event.
 * 
 * <p>
 * <span style="color:red">Experimental API: This API is still under development
 * and is subject to change. </span>
 * </p>
 * 
 * <p>
 * A Storage Event is fired when a storage area changes, as described in these
 * two sections (for <a
 * href="http://www.w3.org/TR/webstorage/#sessionStorageEvent">session
 * storage</a>, for <a
 * href="http://www.w3.org/TR/webstorage/#localStorageEvent">local storage</a>).
 * </p>
 * 
 * @see Handler
 * @see <a href="http://www.w3.org/TR/webstorage/#event-definition">W3C Web
 *      Storage - StorageEvent</a>
 * @see <a
 *      href="https://developer.apple.com/safari/library/documentation/AppleApplications/Reference/WebKitDOMRef/StorageEvent_idl/Classes/StorageEvent/index.html">Safari
 *      StorageEvent reference</a>
 */
public final class StorageEvent extends JavaScriptObject {
  /**
   * Represents an Event handler for {@link StorageEvent}s.
   * 
   * <p>
   * Apply your StorageEventHandler using
   * {@link Storage#addStorageEventHandler(StorageEvent.Handler)}.
   * </p>
   * 
   * @see StorageEvent
   */
  public interface Handler {
    /**
     * Invoked when a StorageEvent is fired.
     * 
     * @param event the fired StorageEvent
     * @see <a href="http://www.w3.org/TR/webstorage/#event-storage">W3C Web
     *      Storage - Storage Event</a>
     */
    void onStorageChange(StorageEvent event);
  }

  protected StorageEvent() {
  }

  /**
   * Returns the key being changed.
   * 
   * @return the key being changed
   * @see <a href="http://www.w3.org/TR/webstorage/#dom-storageevent-key">W3C
   *      Web Storage - StorageEvent.key</a>
   */
  public native String getKey() /*-{
    return this.key;
  }-*/;

  /**
   * Returns the new value of the key being changed.
   * 
   * @return the new value of the key being changed
   * @see <a
   *      href="http://www.w3.org/TR/webstorage/#dom-storageevent-newvalue">W3C
   *      Web Storage - StorageEvent.newValue</a>
   */
  public native String getNewValue() /*-{
    return this.newValue;
  }-*/;

  /**
   * Returns the old value of the key being changed.
   * 
   * @return the old value of the key being changed
   * @see <a
   *      href="http://www.w3.org/TR/webstorage/#dom-storageevent-oldvalue">W3C
   *      Web Storage - StorageEvent.oldValue</a>
   */
  public native String getOldValue() /*-{
    return this.oldValue;
  }-*/;

  /**
   * Returns the {@link Storage} object that was affected.
   * 
   * @return the {@link Storage} object that was affected
   * @see <a
   *      href="http://www.w3.org/TR/webstorage/#dom-storageevent-storagearea">W3C
   *      Web Storage - StorageEvent.storageArea</a>
   */
  public Storage getStorageArea() {
    return Storage.impl.getStorageFromEvent(this);
  }

  /**
   * Returns the address of the document whose key changed.
   * 
   * @return the address of the document whose key changed
   * @see <a href="http://www.w3.org/TR/webstorage/#dom-storageevent-url">W3C
   *      Web Storage - StorageEvent.url</a>
   */
  public native String getUrl() /*-{
    return this.url || this.uri;  // Older Safari browsers have 'uri' instead of 'url'
  }-*/;
}
