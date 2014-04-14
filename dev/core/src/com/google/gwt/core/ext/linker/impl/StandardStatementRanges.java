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
package com.google.gwt.core.ext.linker.impl;

import com.google.gwt.core.ext.linker.StatementRanges;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.io.Serializable;
import java.util.List;

/**
 * The standard implementation of {@link StatementRanges}.
 */
public class StandardStatementRanges implements StatementRanges, Serializable {

  /**
   * Combines multiple StatementRanges into a single StatementRanges by assuming
   * that the offsets inside each subsequent StatementRanges instance begin
   * after the ending offset of the previous instance.
   */
  public static StatementRanges combine(List<StatementRanges> statementRangesList) {
    List<Integer> combinedStarts = Lists.newArrayList();
    List<Integer> combinedEnds = Lists.newArrayList();

    int lastEndingOffset = 0;
    for (StatementRanges statementRanges : statementRangesList) {
      // Append all the start and end indexes + the current offset.
      for (int i = 0; i < statementRanges.numStatements(); i++) {
        combinedStarts.add(lastEndingOffset + statementRanges.start(i));
        combinedEnds.add(lastEndingOffset + statementRanges.end(i));
      }
      // Move the offset forward to the end of the just finished StatementRanges
      // instance.
      if (statementRanges.numStatements() > 0) {
        lastEndingOffset += statementRanges.end(statementRanges.numStatements() - 1);
      }
    }

    return new StandardStatementRanges(combinedStarts, combinedEnds);
  }

  private static int[] toArray(List<Integer> list) {
    int[] ary = new int[list.size()];
    for (int i = 0; i < list.size(); i++) {
      ary[i] = list.get(i);
    }
    return ary;
  }

  // VisibleForTesting
  final int[] ends;
  final int[] starts;

  public StandardStatementRanges(List<Integer> starts, List<Integer> ends) {
    assert starts.size() == ends.size();
    this.starts = toArray(starts);
    this.ends = toArray(ends);
  }

  @Override
  public int end(int i) {
    return ends[i];
  }

  @Override
  public int numStatements() {
    return starts.length;
  }

  @Override
  public int start(int i) {
    return starts[i];
  }
}
