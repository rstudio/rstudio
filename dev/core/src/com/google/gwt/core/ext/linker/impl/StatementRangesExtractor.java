/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.core.ext.linker.impl;

import com.google.gwt.core.ext.linker.StatementRanges;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.util.List;

/**
 * An efficient StatementRanges extractor.
 * <p>
 * Efficient extraction comes with the restriction that extracted ranges must be consecutive to
 * avoid having the seek within the subject StatementRanges instance.
 */
public class StatementRangesExtractor {

  private int lastRangeEndPosition;
  private int statementRangeIndex;
  private final StatementRanges statementRanges;

  public StatementRangesExtractor(StatementRanges statementRanges) {
    this.statementRanges = statementRanges;
  }

  public StatementRanges extract(int rangeStartPosition, int rangeEndPosition) {
    assert statementRangeIndex
        < statementRanges.numStatements() : "Ranges can't be extracted past the end.";
    skipTo(rangeStartPosition);

    lastRangeEndPosition = rangeEndPosition;

    List<Integer> statementStartPositions = Lists.newArrayList();
    List<Integer> statementEndPositions = Lists.newArrayList();

    do {
      int statementStartPosition = statementRanges.start(statementRangeIndex);
      if (statementStartPosition < rangeStartPosition
          || statementStartPosition >= rangeEndPosition) {
        break;
      }

      int statementEndPosition = statementRanges.end(statementRangeIndex);
      if (statementEndPosition <= rangeStartPosition || statementEndPosition > rangeEndPosition) {
        break;
      }

      statementStartPositions.add(statementStartPosition);
      statementEndPositions.add(statementEndPosition);

    } while (++statementRangeIndex < statementRanges.numStatements());

    return new StandardStatementRanges(statementStartPositions, statementEndPositions);
  }

  private void skipTo(int rangeEndPosition) {
    assert statementRangeIndex <= statementRanges.numStatements() : "You can't skip past the end.";
    assert lastRangeEndPosition <= rangeEndPosition : "You can only skip forward.";

    do {
      int statementStartPosition = statementRanges.start(statementRangeIndex);
      if (statementStartPosition >= rangeEndPosition) {
        break;
      }

      int statementEndPosition = statementRanges.end(statementRangeIndex);
      if (statementEndPosition > rangeEndPosition) {
        break;
      }

    } while (++statementRangeIndex < statementRanges.numStatements());
  }
}
