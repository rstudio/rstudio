/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.user.client;

/**
 * Implement this interface to receive notification of changes to the browser
 * history state. It is used with {@link com.google.gwt.user.client.History}.
 * 
 * @deprecated use
 *             {@link History#addValueChangeHandler(com.google.gwt.event.logical.shared.ValueChangeHandler)} instead
 */
@Deprecated
public interface HistoryListener extends java.util.EventListener {

  /**
   * Fired when the user clicks the browser's 'back' or 'forward' buttons.
   * 
   * @param historyToken the token representing the current history state
   */
  @Deprecated
  void onHistoryChanged(String historyToken);
}
