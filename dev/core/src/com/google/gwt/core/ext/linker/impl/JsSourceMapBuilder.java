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

import java.util.List;

/**
 * A builder for combining existing JS source maps.
 * <p>
 * Takes care of rebasing the offset of appended ranges onto the end of the previously
 * accumulated ranges.
 */
public class JsSourceMapBuilder {

  private int bytes;
  private final List<Range> ranges = Lists.newArrayList();
  private int lines;

  public void append(JsSourceMap jsSourceMap) {
    for (Range normalizedRange : jsSourceMap.getRanges()) {
      Range offsetRange = normalizedRange.createOffsetCopy(bytes, lines);
      ranges.add(offsetRange);
    }
    bytes += jsSourceMap.getBytes();
    lines += jsSourceMap.getLines();
  }

  public JsSourceMap build() {
    return new JsSourceMap(ranges, bytes, lines);
  }
}
