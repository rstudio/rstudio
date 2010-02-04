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
package com.google.gwt.user.client.ui;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.SuggestBox.DefaultSuggestionDisplay;
import com.google.gwt.user.client.ui.SuggestBox.SuggestionCallback;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Tests for {@link SuggestBoxTest}.
 */
public class SuggestBoxTest extends WidgetTestBase {

  /**
   * A SuggestionDisplay used for testing.
   */
  private static class TestSuggestionDisplay extends DefaultSuggestionDisplay {

    private List<? extends Suggestion> suggestions;

    @Override
    protected void showSuggestions(SuggestBox suggestBox,
        Collection<? extends Suggestion> suggestions,
        boolean isDisplayStringHTML, boolean isAutoSelectEnabled,
        SuggestionCallback callback) {
      super.showSuggestions(suggestBox, suggestions, isDisplayStringHTML,
          isAutoSelectEnabled, callback);
      this.suggestions = new ArrayList<Suggestion>(suggestions);
    }

    /**
     * Get the suggestion at the specified index.
     * 
     * @param index the index
     * @return the {@link Suggestion} at the index
     */
    public Suggestion getSuggestion(int index) {
      return suggestions.get(index);
    }

    /**
     * Get the number of suggestions that are currently showing. Used for
     * testing.
     * 
     * @return the number of suggestions currently showing, 0 if there are none
     */
    public int getSuggestionCount() {
      return suggestions.size();
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  /**
   * Test the basic accessors.
   */
  @SuppressWarnings("deprecation")
  public void testAccessors() {
    SuggestBox box = createSuggestBox();

    // setLimit
    box.setLimit(5);
    assertEquals(5, box.getLimit());

    // setSelectsFirstItem
    box.setAutoSelectEnabled(true);
    assertTrue(box.isAutoSelectEnabled());
    box.setAutoSelectEnabled(false);
    assertFalse(box.isAutoSelectEnabled());

    // isSuggestionListShowing
    assertFalse(box.isSuggestionListShowing());
    box.showSuggestions("test");
    assertTrue(box.isSuggestionListShowing());
  }

  public void testGettextShouldBeCalledWhenOverrided() {

    // Verify that the query matches the overridden getText.
    SuggestOracle oracle = new SuggestOracle() {
      @Override
      public void requestSuggestions(Request request, Callback callback) {
        if ("override".equals(request.getQuery())) {
          finishTest();
        } else {
          fail("Expected query: override");
        }
      }
    };

    // Create a customized SuggestBox which overrides getText.
    SuggestBox box = new SuggestBox(oracle) {
      @Override
      public String getText() {
        return "override";
      }
    };

    // Attach the box.
    RootPanel.get().add(box);

    // showSuggestionList should call the overridden method.
    delayTestFinish(1000);
    box.showSuggestionList();
  }

  @SuppressWarnings("deprecation")
  public void testShowAndHide() {
    SuggestBox box = createSuggestBox();
    TestSuggestionDisplay display = (TestSuggestionDisplay) box.getSuggestionDisplay();
    assertFalse(display.isSuggestionListShowing());

    // should do nothing, box is not attached.
    box.showSuggestionList();
    assertFalse(display.isSuggestionListShowing());

    // Adds the suggest box to the root panel.
    RootPanel.get().add(box);
    assertFalse(display.isSuggestionListShowing());

    // Hides the list of suggestions, should be a no-op.
    box.hideSuggestionList();

    // Should try to show, but still fail, as there are no default suggestions.
    box.showSuggestionList();
    assertFalse(display.isSuggestionListShowing());

    // Now, finally, should be true
    box.setText("t");
    box.showSuggestionList();
    assertTrue(display.isSuggestionListShowing());

    // Hides it for real this time.
    box.hideSuggestionList();
    assertFalse(display.isSuggestionListShowing());
  }

  public void testDefaults() {
    MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
    oracle.setDefaultSuggestionsFromText(Arrays.asList("A", "B"));
    TestSuggestionDisplay display = new TestSuggestionDisplay();
    SuggestBox box = new SuggestBox(oracle, new TextBox(), display);
    RootPanel.get().add(box);
    box.showSuggestionList();
    assertTrue(display.isSuggestionListShowing());
    assertEquals(2, display.getSuggestionCount());
    assertEquals("A", display.getSuggestion(0).getReplacementString());
    assertEquals("B", display.getSuggestion(1).getReplacementString());
  }

  public void testShowFirst() {
    SuggestBox box = createSuggestBox();
    assertTrue(box.isAutoSelectEnabled());
    SuggestBox box2 = createSuggestBox();
    assertTrue(box2.isAutoSelectEnabled());
    box.setAutoSelectEnabled(false);
    assertFalse(box.isAutoSelectEnabled());
    box.setText("t");
    box.showSuggestionList();
    // Todo(ecc) once event triggering is enabled, submit a return key to the
    // text box and ensure that we see the correct behavior.
  }

  public void testWrapUsingStaticWrapMethod() {
    Element wrapper = Document.get().createTextInputElement();
    RootPanel.get().getElement().appendChild(wrapper);

    // Use direct wrap method from suggest box.
    SuggestBox box = SuggestBox.wrap(createOracle(), wrapper);
    assertTrue(box.isAttached());
    assertTrue(box.getWidget().getParent() == box);
  }

  public void testWrapUsingComposite() {
    // Ensure we can use this with normal composites
    Element wrapper = Document.get().createTextInputElement();
    RootPanel.get().getElement().appendChild(wrapper);
    TextBox b = TextBox.wrap(wrapper);
    SuggestBox box = new SuggestBox(createOracle(), b);
    assertTrue(b.getParent() == box);
  }

  protected SuggestBox createSuggestBox() {
    MultiWordSuggestOracle oracle = createOracle();
    return new SuggestBox(oracle, new TextBox(), new TestSuggestionDisplay());
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
