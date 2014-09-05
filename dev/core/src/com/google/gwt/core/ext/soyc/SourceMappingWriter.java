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
package com.google.gwt.core.ext.soyc;

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.thirdparty.debugging.sourcemap.FilePosition;
import com.google.gwt.thirdparty.debugging.sourcemap.SourceMapGenerator;

/**
 * Writes a sorted stream of mappings to a sourcemap. Automatically merges mappings that have
 * adjacent or overlapping JavaScript ranges and also point to the same Java line.
 */
class SourceMappingWriter {
  private final SourceMapGenerator out;

  // There may be zero or one mappings in the buffer.
  // It's represented as separate fields to avoid unnecessary memory allocation.

  private boolean empty;

  private String javaFile;
  private int javaLine; // one-based

  // the JavaScript range so far (zero-based)
  private int startLine;
  private int startColumn;
  private int endLine;
  private int endColumn;

  SourceMappingWriter(SourceMapGenerator out) {
    this.out = out;
    this.empty = true;
  }

  /**
   * Sends one mapping to the sourcemap.
   *
   * <p>The mappings must be sorted by JavaScript starting position.
   *
   * <p>The output is buffered, so the caller must call {@link #flush} when done.
   */
  void addMapping(Range nextRange, String javaName) {
    SourceInfo nextInfo = nextRange.getSourceInfo();
    if (!canMerge(nextRange, nextInfo, javaName)) {
      flush(null);
    }

    if (empty) {
      // Start a new range.
      javaFile = nextInfo.getFileName();
      javaLine = nextInfo.getStartLine();
      startLine = nextRange.getStartLine();
      startColumn = nextRange.getStartColumn();
      endLine = nextRange.getEndLine();
      endColumn = nextRange.getEndColumn();
      empty = false;

      if (javaName != null) {
        flush(javaName); // Don't merge mappings with Java names.
      }

      return;
    }

    // Merge with the buffer by adjusting the end of the JavaScript range if needed.
    // (It's rarely needed because the range of a Java statement usually comes before
    // any subexpressions within that statement, and there is rarely more than one Java
    // statement per line.)

    int nextEndLine = nextRange.getEndLine();
    if (nextEndLine < endLine) {
      return; // The multi-line range in the buffer already covers it.
    }

    int nextEndColumn = nextRange.getEndColumn();
    if (nextEndLine == endLine && nextEndColumn <= endColumn) {
      return; // The range in the buffer already covers it.
    }

    endLine = nextEndLine;
    endColumn = nextEndColumn;
  }

  /**
   * Writes any buffered mappings to the source map generator.
   */
  void flush() {
    flush(null);
  }

  /**
   * Returns true if there is a mapping in the buffer that we can merge with.
   */
  private boolean canMerge(Range nextRange, SourceInfo nextInfo, String javaName) {
    if (empty) {
      return false; // Nothing in the buffer.
    }

    if (javaName != null) {
      return false; // Don't merge mappings with Java names.
    }

    // The ranges were sorted by starting position. Therefore we only need to to check
    // that our ending position touches or overlaps their starting position.

    if (endLine < nextRange.getStartLine()) {
      return false; // Not adjacent because they're on separate JavaScript lines.
    }

    if (endLine == nextRange.getStartLine() && endColumn < nextRange.getStartColumn()) {
      // Not adjacent due to unmapped characters between JavaScript ranges.
      // (In theory we could relax this check if there is only whitespace between
      // JavaScript ranges due to pretty-printing.)
      return false;
    }

    if (javaLine != nextInfo.getStartLine()) {
      return false; // They don't map to the same Java line.
    }

    return javaFile.equals(nextInfo.getFileName());
  }

  /**
   * Flush the mapping in the buffer and annotate it with the given Java name.
   */
  private void flush(String javaName) {
    if (empty) {
      return;
    }

    // Starting with V3, SourceMap line numbers are zero-based.
    // GWT's line numbers for Java files originally came from the JDT, which is 1-based,
    // so adjust them here to avoid an off-by-one error in debuggers.
    out.addMapping(javaFile, javaName,
        new FilePosition(javaLine - 1, 0),
        new FilePosition(startLine, startColumn),
        new FilePosition(endLine, endColumn));

    empty = true; // don't write it twice.
  }
}
