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
 * Tests for {@link PageSizePager}.
 */
public class PageSizePagerTest extends AbstractPagerTest {

  private static final int PAGE_INCREMENT = 100;

  public void testNextButtonsDisabled() {
    PageSizePager pager = createPager();

    // Buttons are hidden by default.
    assertFalse(pager.isShowLessButtonVisible());
    assertFalse(pager.isShowMoreButtonVisible());

    // Set the display.
    HasRows display = new MockHasData<String>();
    display.setRowCount(1000);
    pager.setDisplay(display);

    // Show number of rows equal to increment.
    display.setVisibleRange(0, PAGE_INCREMENT);
    assertFalse(pager.isShowLessButtonVisible());
    assertTrue(pager.isShowMoreButtonVisible());

    // Show more than increment.
    display.setVisibleRange(0, PAGE_INCREMENT + 1);
    assertTrue(pager.isShowLessButtonVisible());
    assertTrue(pager.isShowMoreButtonVisible());

    // Show all rows.
    display.setVisibleRange(0, 1000);
    assertTrue(pager.isShowLessButtonVisible());
    assertFalse(pager.isShowMoreButtonVisible());
  }

  @Override
  protected PageSizePager createPager() {
    return new PageSizePager(PAGE_INCREMENT);
  }
}
