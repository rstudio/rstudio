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
import com.google.gwt.junit.client.GWTTestCase;

import java.util.Arrays;

/**
 * Tests for {@link SuggestBoxTest}.
 */
public class SuggestBoxTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  /**
   * Test the basic accessors.
   */
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

  public void testShowAndHide() {
    SuggestBox box = createSuggestBox();
    assertFalse(box.isSuggestionListShowing());
    // should do nothing, box is not attached.
    box.showSuggestionList();
    assertFalse(box.isSuggestionListShowing());

    // Adds the suggest box to the root panel.
    RootPanel.get().add(box);
    assertFalse(box.isSuggestionListShowing());

    // Hides the list of suggestions, should be a no-op.
    box.hideSuggestionList();

    // Should try to show, but still fail, as there are no default suggestions.
    box.showSuggestionList();
    assertFalse(box.isSuggestionListShowing());

    // Now, finally, should be true
    box.setText("t");
    box.showSuggestionList();
    assertTrue(box.isSuggestionListShowing());

    // Hides it for real this time.
    box.hideSuggestionList();
    assertFalse(box.isSuggestionListShowing());
  }

  public void testDefaults() {
    MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
    oracle.setDefaultSuggestionsFromText(Arrays.asList("A", "B"));
    SuggestBox box = new SuggestBox(oracle);
    RootPanel.get().add(box);
    box.showSuggestionList();
    assertTrue(box.isSuggestionListShowing());
    assertEquals(2, box.getSuggestionCount());
    assertEquals("A", box.getSuggestion(0).getReplacementString());
    assertEquals("B", box.getSuggestion(1).getReplacementString());
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

  @Override
  public void gwtTearDown() throws Exception {
    super.gwtTearDown();
    RootPanel.get().clear();
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
    return new SuggestBox(oracle);
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
