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

import com.google.gwt.user.client.ui.SuggestBox.SuggestionCallback;
import com.google.gwt.user.client.ui.SuggestBox.SuggestionDisplay;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

import java.util.ArrayList;
import java.util.List;

/**
 * Base tests for {@link SuggestionDisplay}.
 */
public abstract class SuggestionDisplayTestBase extends WidgetTestBase {

  /**
   * A no-op callback used for testing.
   */
  protected static final SuggestionCallback NULL_CALLBACK = new SuggestionCallback() {
    public void onSuggestionSelected(Suggestion suggestion) {
    }
  };

  /**
   * A simple {@link Suggestion} implementation that uses a single string for
   * both the display and replacement string.
   */
  private static class SimpleSuggestion implements Suggestion {

    public String text;

    public SimpleSuggestion(String text) {
      this.text = text;
    }

    public String getDisplayString() {
      return text;
    }

    public String getReplacementString() {
      return text;
    }
  }

  public void testMoveSelectionUpAndDown() {
    SuggestBox box = new SuggestBox();
    SuggestionDisplay display = box.getSuggestionDisplay();
    SuggestOracle oracle = box.getSuggestOracle();

    // Show some suggestions.
    List<Suggestion> suggestions = createSuggestions("test0", "test1", "test2",
        "test3");
    display.showSuggestions(box, suggestions, false, false, NULL_CALLBACK);
    assertNull(display.getCurrentSelection());

    display.moveSelectionDown();
    assertEquals(suggestions.get(0), display.getCurrentSelection());
    display.moveSelectionDown();
    assertEquals(suggestions.get(1), display.getCurrentSelection());
    display.moveSelectionDown();
    assertEquals(suggestions.get(2), display.getCurrentSelection());
    display.moveSelectionUp();
    assertEquals(suggestions.get(1), display.getCurrentSelection());
    display.moveSelectionUp();
    assertEquals(suggestions.get(0), display.getCurrentSelection());
  }

  public void testShowSuggestionsAutoSelectDisabled() {
    SuggestBox box = new SuggestBox();
    SuggestionDisplay display = box.getSuggestionDisplay();
    SuggestOracle oracle = box.getSuggestOracle();

    // Show some suggestions with auto select disabled.
    List<Suggestion> suggestions = createSuggestions("test0", "test1", "test2");
    display.showSuggestions(box, suggestions, false, false, NULL_CALLBACK);

    // Nothing should be selected.
    assertNull(display.getCurrentSelection());
  }

  public void testShowSuggestionsAutoSelectEnabled() {
    SuggestBox box = new SuggestBox();
    SuggestionDisplay display = box.getSuggestionDisplay();
    SuggestOracle oracle = box.getSuggestOracle();

    // Show some suggestions with auto select enabled.
    List<Suggestion> suggestions = createSuggestions("test0", "test1", "test2");
    display.showSuggestions(box, suggestions, false, true, NULL_CALLBACK);

    // First item should be selected.
    assertEquals(suggestions.get(0), display.getCurrentSelection());
  }

  /**
   * Create a list of {@link Suggestion}.
   * 
   * @param items the items to add to the list
   * @return the list of suggestions
   */
  protected List<Suggestion> createSuggestions(String... items) {
    List<Suggestion> suggestions = new ArrayList<Suggestion>();
    for (String item : items) {
      suggestions.add(new SimpleSuggestion(item));
    }
    return suggestions;
  }

  /**
   * Create a new {@link SuggestionDisplay} to test.
   * 
   * @return the {@link SuggestionDisplay}
   */
  protected abstract SuggestionDisplay createSuggestionDisplay();

  /**
   * Create a new {@link SuggestBox}.
   * 
   * @return the {@link SuggestBox}
   */
  protected SuggestBox createSuggestBox() {
    MultiWordSuggestOracle oracle = createOracle();
    return new SuggestBox(oracle, new TextBox(), createSuggestionDisplay());
  }

  private MultiWordSuggestOracle createOracle() {
    MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
    oracle.add("test");
    oracle.add("test1");
    oracle.add("test2");
    oracle.add("test3");
    oracle.add("test4");
    oracle.add("john");
    return oracle;
  }
}
