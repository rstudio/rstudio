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
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

/**
 * An efficient JS source map chunk extractor.
 * <p>
 * Efficient extraction comes with the restriction that extracted ranges must be consecutive to
 * avoid having to seek within the subject JS source map data.
 */
public class JsSourceMapExtractor {

  private int lastTypeEndPosition;
  private LinkedList<Entry<Range, SourceInfo>> rangeToSourceInfoMappings = Lists.newLinkedList();

  public JsSourceMapExtractor(Map<Range, SourceInfo> sourceInfosByRange) {
    rangeToSourceInfoMappings.addAll(sourceInfosByRange.entrySet());
  }

  public JsSourceMap extract(int typeStartPosition, int typeEndPosition, int typeStartLineNumber,
      int typeEndLineNumber) {
    assert !rangeToSourceInfoMappings
        .isEmpty() : "Source mappings can't be extracted past the end.";
    skipTo(typeStartPosition);

    lastTypeEndPosition = typeEndPosition;

    LinkedHashMap<Range, SourceInfo> typeSourceMappings = Maps.newLinkedHashMap();

    do {
      Entry<Range, SourceInfo> rangeAndSourceInfo = rangeToSourceInfoMappings.getFirst();
      Range range = rangeAndSourceInfo.getKey();
      SourceInfo sourceInfo = rangeAndSourceInfo.getValue();

      if (range.getStart() < typeStartPosition || range.getStart() >= typeEndPosition) {
        break;
      }
      if (range.getEnd() <= typeStartPosition || range.getEnd() > typeEndPosition) {
        break;
      }

      rangeToSourceInfoMappings.removeFirst();

      // Normalize range position relative to the beginning of the type that contains it.
      Range typeOffsetNormalizedRange =
          range.createNormalizedCopy(typeStartPosition, typeStartLineNumber);
      typeSourceMappings.put(typeOffsetNormalizedRange, sourceInfo);
    } while (!rangeToSourceInfoMappings.isEmpty());

    int typeBytes = typeEndPosition - typeStartPosition;
    int typeLines = typeEndLineNumber - typeStartLineNumber;
    return new JsSourceMap(typeSourceMappings, typeBytes, typeLines);
  }

  private void skipTo(int rangeEndPosition) {
    assert lastTypeEndPosition <= rangeEndPosition : "You can only skip forward.";

    do {
      Range range = rangeToSourceInfoMappings.getFirst().getKey();

      if (range.getStart() >= rangeEndPosition) {
        break;
      }
      if (range.getEnd() > rangeEndPosition) {
        break;
      }

      rangeToSourceInfoMappings.removeFirst();
    } while (!rangeToSourceInfoMappings.isEmpty());
  }
}
