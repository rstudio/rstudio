/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.user.client.ui;

import java.util.EventListener;

/**
 * Event handler interface for {@link SuggestionEvent}.
 * 
 * @see SuggestBox
 * 
 * @deprecated use {@link com.google.gwt.event.logical.shared.SelectionHandler}
 *             instead
 */
@Deprecated
public interface SuggestionHandler extends EventListener {

  /**
   * Fired when a suggestion is selected. Users can select a suggestion from the
   * SuggestBox by clicking on one of the suggestions, or by pressing the ENTER
   * key to select the suggestion that is currently highlighted.
   * 
   * @param event the object containing information about this event deprecated
   *          use
   *          {@link com.google.gwt.event.logical.shared.SelectionHandler#onSelection(com.google.gwt.event.logical.shared.SelectionEvent)}
   *          instead
   */

  void onSuggestionSelected(SuggestionEvent event);
}
