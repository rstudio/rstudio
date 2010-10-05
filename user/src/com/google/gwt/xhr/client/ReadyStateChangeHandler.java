/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.xhr.client;

/**
 * A ready-state callback for an {@link XMLHttpRequest} object.
 */
public interface ReadyStateChangeHandler {

  /**
   * This is called whenever the state of the XMLHttpRequest changes. See
   * {@link XMLHttpRequest#setOnReadyStateChange}.
   * 
   * @param xhr the object whose state has changed.
   */
  void onReadyStateChange(XMLHttpRequest xhr);
}
