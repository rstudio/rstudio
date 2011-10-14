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
import com.google.gwt.user.client.ui.MultiWordSuggestOracle.MultiWordSuggestion;
import com.google.gwt.user.client.ui.SuggestBox.DefaultSuggestionDisplay;
import com.google.gwt.user.client.ui.SuggestBox.SuggestionCallback;
import com.google.gwt.user.client.ui.SuggestOracle.Response;
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
   * A SuggestOracle used for testing.
   */
  private static class TestOracle extends SuggestOracle {
    private Request request;
    private Callback callback;

    @Override
    public void requestSuggestions(Request request, Callback callback) {
      this.request = request;
      this.callback = callback;
    }
  }

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

  public void testLargerMatchShows() {
    MultiWordSuggestOracle oracle = new MultiWordSuggestOracle(" ");
    oracle.add("He'll help me wont he");

    TestSuggestionDisplay display = new TestSuggestionDisplay();
    SuggestBox box = new SuggestBox(oracle, new TextBox(), display);
    RootPanel.get().add(box);
    box.setText("He help");
    box.showSuggestionList();
    assertTrue(display.isSuggestionListShowing());
    assertEquals(1, display.getSuggestionCount());
    assertEquals("<strong>He</strong>&#39;ll <strong>help</strong> me wont <strong>he</strong>",
        display.getSuggestion(0).getDisplayString());
  }

  public void testMultipleWordMatchesShow() {
    MultiWordSuggestOracle oracle = new MultiWordSuggestOracle(",! ");
    oracle.add("Hark, Shark and Herald");
    oracle.add("Hark! The Herald Angels Sing");
    oracle.add("Heraldings and Harkings");
    oracle.add("Send my regards to Herald");

    TestSuggestionDisplay display = new TestSuggestionDisplay();
    SuggestBox box = new SuggestBox(oracle, new TextBox(), display);
    RootPanel.get().add(box);
    box.setText("Herald! Hark");
    box.showSuggestionList();
    assertTrue(display.isSuggestionListShowing());
    assertEquals(3, display.getSuggestionCount());
    assertEquals("<strong>Hark</strong>, Shark and <strong>Herald</strong>", 
        display.getSuggestion(0).getDisplayString());
    assertEquals("<strong>Hark</strong>! The <strong>Herald</strong> Angels Sing",
        display.getSuggestion(1).getDisplayString());
    assertEquals("<strong>Herald</strong>ings and <strong>Hark</strong>ings",
        display.getSuggestion(2).getDisplayString());
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

  public void testEnabled() {
    SuggestBox box = createSuggestBox();
    TestSuggestionDisplay display = (TestSuggestionDisplay) box.getSuggestionDisplay();
    assertFalse(display.isSuggestionListShowing());

    // Adds suggest box with suggestion showing
    RootPanel.get().add(box);
    box.setText("t");
    box.showSuggestionList();
    assertTrue(display.isSuggestionListShowing());

    // When the box is disabled
    box.setEnabled(false);

    // The suggestion list was hidden and the inner text box disabled
    assertFalse(display.isSuggestionListShowing());
    assertFalse(box.getTextBox().isEnabled());

    // When the box is re-enabled
    box.setEnabled(true);

    // The box is re-enabled, but the suggestion list not re-shown
    assertTrue(box.getTextBox().isEnabled());
    assertFalse(display.isSuggestionListShowing());
  }

  public void testDisabledIgnoresLateResponses() {
    TestOracle oracle = new TestOracle();
    SuggestBox box = new SuggestBox(oracle, new TextBox(), new TestSuggestionDisplay());
    TestSuggestionDisplay display = (TestSuggestionDisplay) box.getSuggestionDisplay();
    assertFalse(display.isSuggestionListShowing());

    // Adds suggest box with suggestion showing
    RootPanel.get().add(box);
    box.setText("t");
    box.showSuggestionList();

    // Waiting for response
    assertFalse(display.isSuggestionListShowing());

    // The box becomes disabled
    box.setEnabled(false);

    // Response comes back after that
    Collection<MultiWordSuggestion> suggestions = new ArrayList<MultiWordSuggestion>();
    suggestions.add(new MultiWordSuggestion("one", "one"));
    oracle.callback.onSuggestionsReady(oracle.request, new Response(suggestions));

    // The suggestion list stays hidden
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
  
  public void testSuggestionSelection() {
    MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
    oracle.setDefaultSuggestionsFromText(Arrays.asList("A", "B"));
    TestSuggestionDisplay display = new TestSuggestionDisplay();
    SuggestBox box = new SuggestBox(oracle, new TextBox(), display);
    box.setAutoSelectEnabled(false);
    RootPanel.get().add(box);
    box.showSuggestionList();

    // If nothing is selected, moving down will select the first item
    assertNull(display.getCurrentSelection());
    display.moveSelectionDown();
    assertEquals("A", display.getCurrentSelection().getReplacementString());
    
    // Once something is selected, selections are made as expected, but we do
    // not move outside the box
    display.moveSelectionDown();
    assertEquals("B", display.getCurrentSelection().getReplacementString());
    display.moveSelectionDown();
    assertEquals("B", display.getCurrentSelection().getReplacementString());
    display.moveSelectionUp();
    assertEquals("A", display.getCurrentSelection().getReplacementString());
    display.moveSelectionUp();
    assertEquals("A", display.getCurrentSelection().getReplacementString());
    
    // Reset the suggestions so that nothing is selected again
    display.hideSuggestions();
    box.showSuggestionList();
    assertNull(display.getCurrentSelection());
    
    // If nothing is selected, moving up will select the last item
    display.moveSelectionUp();
    assertEquals("B", display.getCurrentSelection().getReplacementString());
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
