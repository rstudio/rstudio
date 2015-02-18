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

import com.google.gwt.core.ext.soyc.Range;
import com.google.gwt.dev.jjs.JsSourceMap;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.util.LinkedList;
import java.util.List;

/**
 * An efficient JS source map chunk extractor.
 * <p>
 * Efficient extraction comes with the restriction that extracted ranges must be consecutive to
 * avoid having to seek within the subject JS source map data.
 */
public class JsSourceMapExtractor {

  private int lastTypeEndPosition;
  private LinkedList<Range> ranges = Lists.newLinkedList();

  public JsSourceMapExtractor(List<Range> ranges) {
    this.ranges.addAll(ranges);
  }

  public JsSourceMap extract(int typeStartPosition, int typeEndPosition, int typeStartLineNumber,
      int typeEndLineNumber) {
    assert !ranges.isEmpty() : "Source mappings can't be extracted past the end.";
    skipTo(typeStartPosition);

    lastTypeEndPosition = typeEndPosition;

    List<Range> extractedRanges = Lists.newArrayList();

    while (!ranges.isEmpty()) {
      Range range = ranges.getFirst();
      if (range.getStart() < typeStartPosition || range.getStart() >= typeEndPosition) {
        break;
      }
      if (range.getEnd() <= typeStartPosition || range.getEnd() > typeEndPosition) {
        break;
      }

      ranges.removeFirst();

      // Normalize range position relative to the beginning of the type that contains it.
      Range typeOffsetNormalizedRange =
          range.createNormalizedCopy(typeStartPosition, typeStartLineNumber);
      extractedRanges.add(typeOffsetNormalizedRange);
    }

    int typeBytes = typeEndPosition - typeStartPosition;
    int typeLines = typeEndLineNumber - typeStartLineNumber;
    return new JsSourceMap(extractedRanges, typeBytes, typeLines);
  }

  private void skipTo(int rangeEndPosition) {
    assert lastTypeEndPosition <= rangeEndPosition : "You can only skip forward.";

    while (!ranges.isEmpty()) {
      Range range = ranges.getFirst();

      if (range.getStart() >= rangeEndPosition) {
        break;
      }
      if (range.getEnd() > rangeEndPosition) {
        break;
      }

      ranges.removeFirst();
    }
  }
}
