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
 * Mozilla-specific implementation of a Storage.
 *
 * <p>
 * Implementation of StorageEvents is incomplete for Mozilla. This class amends
 * the properties consistently with W3C's StorageEvent.
 * </p>
 */
class StorageImplMozilla extends StorageImplNonNativeEvents {
  /*
   * Firefox incorrectly handles indices outside the range of 
   * 0 to storage.length(). See bugzilla.mozilla.org/show_bug.cgi?id=50924
   */
  @Override
  public native String key(String storage, int index) /*-{
    return (index >= 0 && index < $wnd[storage].length) ? 
      $wnd[storage].key(index) : null;
  }-*/;
}
