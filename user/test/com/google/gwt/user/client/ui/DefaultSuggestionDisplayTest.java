/*
 * Copyright 2010 Google Inc.
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

import com.google.gwt.user.client.ui.SuggestBox.DefaultSuggestionDisplay;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

import java.util.List;

/**
 * Tests for {@link DefaultSuggestionDisplay}.
 */
public class DefaultSuggestionDisplayTest extends SuggestionDisplayTestBase {

  public void testAccessors() {
    SuggestBox box = createSuggestBox();
    DefaultSuggestionDisplay display = (DefaultSuggestionDisplay) box.getSuggestionDisplay();
    PopupPanel popup = display.getPopupPanel();

    // isAnimationEnabled.
    assertFalse(display.isAnimationEnabled());
    assertFalse(popup.isAnimationEnabled());
    display.setAnimationEnabled(true);
    assertTrue(display.isAnimationEnabled());
    assertTrue(popup.isAnimationEnabled());

    // isSuggestListShowing.
    List<Suggestion> suggestions = createSuggestions("test0", "test1", "test2");
    assertFalse(display.isSuggestionListShowing());
    assertFalse(popup.isShowing());
    display.showSuggestions(box, suggestions, false, false, NULL_CALLBACK);
    assertTrue(display.isSuggestionListShowing());
    assertTrue(popup.isShowing());
    display.hideSuggestions();
    assertFalse(display.isSuggestionListShowing());
    assertFalse(popup.isShowing());
  }

  public void testGetCurrentSelectionWhenHidden() {
    SuggestBox box = createSuggestBox();
    DefaultSuggestionDisplay display = (DefaultSuggestionDisplay) box.getSuggestionDisplay();

    // Show the suggestions and select the first item.
    List<Suggestion> suggestions = createSuggestions("test0", "test1", "test2");
    display.showSuggestions(box, suggestions, false, true, NULL_CALLBACK);
    assertTrue(display.isSuggestionListShowing());
    assertEquals(suggestions.get(0), display.getCurrentSelection());

    // Hide the list and ensure that nothing is selected.
    display.hideSuggestions();
    assertNull(display.getCurrentSelection());
  }

  public void testShowSuggestionsEmpty() {
    SuggestBox box = createSuggestBox();
    DefaultSuggestionDisplay display = (DefaultSuggestionDisplay) box.getSuggestionDisplay();

    // Show null suggestions.
    display.showSuggestions(box, null, false, true, NULL_CALLBACK);
    assertFalse(display.isSuggestionListShowing());

    // Show empty suggestions.
    List<Suggestion> suggestions = createSuggestions();
    display.showSuggestions(box, suggestions, false, true, NULL_CALLBACK);
    assertFalse(display.isSuggestionListShowing());
  }

  @Override
  protected DefaultSuggestionDisplay createSuggestionDisplay() {
    return new DefaultSuggestionDisplay();
  }
}
