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
package com.google.gwt.user.cellview.client;

import com.google.gwt.view.client.HasRows;
import com.google.gwt.view.client.MockHasData;

/**
 * Tests for {@link SimplePager}.
 */
public class SimplePagerTest extends AbstractPagerTest {

  public void testNextButtonsDisabled() {
    SimplePager pager = createPager();

    // Buttons are disabled by default.
    assertTrue(pager.isPreviousButtonDisabled());
    assertTrue(pager.isNextButtonDisabled());

    // Set the view.
    HasRows view = new MockHasData<String>();
    view.setRowCount(1000);
    pager.setView(view);

    // First Page.
    view.setVisibleRange(0, 20);
    assertTrue(pager.isPreviousButtonDisabled());
    assertFalse(pager.isNextButtonDisabled());

    // Middle Page.
    view.setVisibleRange(100, 20);
    assertFalse(pager.isPreviousButtonDisabled());
    assertFalse(pager.isNextButtonDisabled());

    // Last Page.
    view.setVisibleRange(980, 20);
    assertFalse(pager.isPreviousButtonDisabled());
    assertTrue(pager.isNextButtonDisabled());
  }

  @Override
  protected SimplePager createPager() {
    return new SimplePager();
  }
}
