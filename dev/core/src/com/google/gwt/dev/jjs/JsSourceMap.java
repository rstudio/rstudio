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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

/**
 * An unmodifiable container of mappings from one JavaScript file to the Java code it came from.
 *
 * (This class doesn't implement Map because we only use a few methods.)
 */
public class JsSourceMap {

  private final int bytes;
  private final int lines;
  /**
   * Maps JS ranges to Java ranges. The mapping is sparse thus the need for separately tracking
   * total bytes and lines. Entries are ordered so that it is possible to extract and separate
   * chunks in an efficient way.
   */
  private final LinkedHashMap<Range, SourceInfo> sourceInfosByRange;

  public JsSourceMap(LinkedHashMap<Range, SourceInfo> sourceInfosByRange, int bytes, int lines) {
    this.sourceInfosByRange = sourceInfosByRange;
    this.bytes = bytes;
    this.lines = lines;
  }

  public JsSourceMapExtractor createExtractor() {
    return new JsSourceMapExtractor(sourceInfosByRange);
  }

  public SourceInfo get(Range key) {
    return sourceInfosByRange.get(key);
  }

  public int getBytes() {
    return bytes;
  }

  public Collection<Entry<Range, SourceInfo>> getEntries() {
    return sourceInfosByRange.entrySet();
  }

  public int getLines() {
    return lines;
  }

  public Set<Range> keySet() {
    return Collections.unmodifiableSet(sourceInfosByRange.keySet());
  }

  public int size() {
    return sourceInfosByRange.size();
  }
}
