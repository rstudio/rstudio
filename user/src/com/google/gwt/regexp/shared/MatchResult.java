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
package com.google.gwt.regexp.shared;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure Java implementation of a regular expression match result.
 */
public class MatchResult {

  private final List<String> groups;
  private final int index;
  private final String input;

  public MatchResult(int index, String input, List<String> groups) {
    this.index = index;
    this.input = input;
    this.groups = new ArrayList<String>(groups);
  }

  /**
   * Retrieves the matched string or the given matched group.
   *
   * @param index the index of the group to return, 0 to return the whole
   *        matched string; must be between 0 and {@code getGroupCount() - 1}
   *        included
   * @return The matched string if {@code index} is zero, else the given matched
   *         group. If the given group was optional and did not match, the
   *         behavior is browser-dependent: this method will return {@code null}
   *         or an empty string.
   */
  public String getGroup(int index) {
    return groups.get(index);
  }

  /**
   * Returns the number of groups, including the matched string hence greater or
   * equal than 1.
   */
  public int getGroupCount() {
    return groups.size();
  }

  /**
   * Returns the zero-based index of the match in the input string.
   */
  public int getIndex() {
    return index;
  }

  /**
   * Returns the original input string.
   */
  public String getInput() {
    return input;
  }
}
