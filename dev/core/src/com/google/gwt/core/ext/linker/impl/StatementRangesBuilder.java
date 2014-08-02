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

import java.util.LinkedList;

/**
 * A builder for combining existing statement ranges.
 * <p>
 * Takes care of rebasing the offset of appended StatementRanges onto the end of the previously
 * accumulated ranges.
 */
public class StatementRangesBuilder {

  private LinkedList<Integer> builderEndPositions = Lists.newLinkedList();
  private LinkedList<Integer> builderStartPositions = Lists.newLinkedList();

  public void addEndPosition(Integer endPosition) {
    builderEndPositions.add(endPosition);
  }

  public void addStartPosition(Integer startPosition) {
    builderStartPositions.add(startPosition);
  }

  public void append(StatementRanges newStatementRanges) {
    if (newStatementRanges.numStatements() == 0) {
      return;
    }

    int oldBaseOffset = newStatementRanges.start(0);
    int newBaseOffset = builderEndPositions.isEmpty() ? 0 : builderEndPositions.getLast();
    int baseOffsetDelta = newBaseOffset - oldBaseOffset;

    for (int i = 0; i < newStatementRanges.numStatements(); i++) {
      builderStartPositions.add(newStatementRanges.start(i) + baseOffsetDelta);
      builderEndPositions.add(newStatementRanges.end(i) + baseOffsetDelta);
    }
  }

  public StatementRanges build() {
    return new StandardStatementRanges(builderStartPositions, builderEndPositions);
  }
}
