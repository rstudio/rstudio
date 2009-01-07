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
package com.google.gwt.user.client.ui;

import java.util.EventObject;

/**
 * Event object containing information about the selection of a
 * {@link SuggestOracle.Suggestion} displayed by a {@link SuggestBox}.
 * 
 * @see SuggestBox#addEventHandler(SuggestionHandler)
 */
public class SuggestionEvent extends EventObject {

  private SuggestOracle.Suggestion selectedSuggestion;

  public SuggestionEvent(SuggestBox sender,
      SuggestOracle.Suggestion selectedSuggestion) {
    super(sender);
    this.selectedSuggestion = selectedSuggestion;
  }

  /**
   * Gets the <code>Suggestion</code> object for the suggestion chosen by the
   * user.
   * 
   * @return the <code>Suggestion</code> object for the selected suggestion
   */
  public SuggestOracle.Suggestion getSelectedSuggestion() {
    return selectedSuggestion;
  }

  /**
   * Returns the string representation of this event object. The string contains
   * the string representation of the SuggestBox from which the event originated
   * (the source), and the string representation of the Suggestion that was
   * selected.
   * 
   * @return the string representation of this event object containing the
   *         source SuggestBox and the selected Suggestion
   */
  @Override
  public String toString() {
    return "[source=" + getSource() + ", selectedSuggestion="
        + getSelectedSuggestion() + "]";
  }
}
