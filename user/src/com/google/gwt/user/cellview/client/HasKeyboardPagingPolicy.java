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

/**
 * Implemented by widgets that have a
 * {@link HasKeyboardPagingPolicy.KeyboardPagingPolicy}.
 */
public interface HasKeyboardPagingPolicy extends HasKeyboardSelectionPolicy {

  /**
   * The policy that determines how keyboard paging will work.
   */
  enum KeyboardPagingPolicy {
    /**
     * Users cannot navigate past the current page.
     */
    CURRENT_PAGE(true),

    /**
     * Users can navigate between pages.
     */
    CHANGE_PAGE(false),

    /**
     * If the user navigates to the beginning or end of the current range, the
     * range is increased.
     */
    INCREASE_RANGE(false);

    private final boolean isLimitedToRange;

    private KeyboardPagingPolicy(boolean isLimitedToRange) {
      this.isLimitedToRange = isLimitedToRange;
    }

    boolean isLimitedToRange() {
      return isLimitedToRange;
    }
  }

  /**
   * Get the {@link KeyboardPagingPolicy}.
   *
   * @return the paging policy
   * @see #setKeyboardPagingPolicy(KeyboardPagingPolicy)
   */
  KeyboardPagingPolicy getKeyboardPagingPolicy();

  /**
   * Set the {@link KeyboardPagingPolicy}.
   *
   * @param policy the paging policy
   * @see #getKeyboardPagingPolicy()
   */
  void setKeyboardPagingPolicy(KeyboardPagingPolicy policy);
}
