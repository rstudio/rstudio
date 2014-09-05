/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.dev.jjs;

import com.google.gwt.core.ext.linker.impl.JsSourceMapExtractor;
import com.google.gwt.core.ext.soyc.Range;

import java.util.List;

/**
 * An unmodifiable container of Ranges that map from JavaScript to the Java it came from.
 */
public class JsSourceMap {

  private final int bytes;
  private final int lines;

  /**
   * Maps JS ranges to Java ranges. The mapping is sparse thus the need for separately tracking
   * total bytes and lines.
   */
  private final List<Range> ranges;

  public JsSourceMap(List<Range> ranges, int bytes, int lines) {
    this.ranges = ranges;
    this.bytes = bytes;
    this.lines = lines;
  }

  public JsSourceMapExtractor createExtractor() {
    return new JsSourceMapExtractor(ranges);
  }

  public int getBytes() {
    return bytes;
  }

  public List<Range> getRanges() {
    return ranges;
  }

  public int getLines() {
    return lines;
  }

  public int size() {
    return ranges.size();
  }
}
