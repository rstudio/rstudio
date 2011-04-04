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
 * IE8-specific implementation of the Web Storage.
 *
 * @see <a
 *      href="http://msdn.microsoft.com/en-us/library/cc197062(VS.85).aspx">MSDN
 *      - Introduction to DOM Storage</a>
 */
class StorageImplIE8 extends StorageImplNonNativeEvents {
  /*
   * IE8 will throw "Class doesn't support Automation" error when comparing
   * $wnd["localStorage"] === $wnd["localStorage"]. In this impl method, we
   * work around it by using an attribute on the StorageEvent.
   */
  @Override
  protected native Storage getStorageFromEvent(StorageEvent event) /*-{
    if (event.storage == "localStorage") {
      return @com.google.gwt.storage.client.Storage::getLocalStorageIfSupported()();
    } else {
      return @com.google.gwt.storage.client.Storage::getSessionStorageIfSupported()();
    }
  }-*/;
}
