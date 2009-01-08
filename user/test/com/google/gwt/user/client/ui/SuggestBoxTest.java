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

import com.google.gwt.junit.client.GWTTestCase;

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
    box.setSelectsFirstItem(true);
    assertTrue(box.getSelectsFirstItem());
    box.setSelectsFirstItem(false);
    assertFalse(box.getSelectsFirstItem());

    // isSuggestionListShowing
    assertFalse(box.isSuggestionListShowing());
    box.showSuggestions("test");
    assertTrue(box.isSuggestionListShowing());
  }

  protected SuggestBox createSuggestBox() {
    MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
    oracle.add("test");
    oracle.add("test1");
    oracle.add("test2");
    oracle.add("test3");
    oracle.add("test4");
    oracle.add("john");
    return new SuggestBox(oracle);
  }
}
