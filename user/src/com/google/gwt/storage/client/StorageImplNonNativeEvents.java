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

/**
 * Implementation of Storage with non-native events.
 *
 * <p>
 * Implementation of StorageEvents is incomplete for many browsers. This class 
 * amends the properties consistently with W3C's StorageEvent.
 * </p>
 */
class StorageImplNonNativeEvents extends StorageImpl {
  private static native StorageEvent createStorageEvent(
      String key, String oldValue, String newValue, String storage) /*-{
    return {
      key: key,
      oldValue: oldValue,
      newValue: newValue,
      storage: storage,
      storageArea: $wnd[storage],
      url: $wnd.location.href
    };
  }-*/;

  private static void fireStorageEvent(
      String key, String oldValue, String newValue, String storage) {
    if (hasStorageEventHandlers()) {
      StorageEvent se = createStorageEvent(key, oldValue, newValue, storage);
      handleStorageEvent(se);
    }
  }

  @Override
  public void clear(String storage) {
    super.clear(storage);
    fireStorageEvent(null, null, null, storage);
  }

  @Override
  public void removeItem(String storage, String key) {
    String oldValue = getItem(storage, key);
    super.removeItem(storage, key);
    fireStorageEvent(key, oldValue, null, storage);
  }

  @Override
  public void setItem(String storage, String key, String data) {
    String oldValue = getItem(storage, key);
    super.setItem(storage, key, data);
    fireStorageEvent(key, oldValue, data, storage);
  }

  @Override
  protected void addStorageEventHandler0() {
    // no-op
  }

  @Override
  protected void removeStorageEventHandler0() {
    // no-op
  }
}
