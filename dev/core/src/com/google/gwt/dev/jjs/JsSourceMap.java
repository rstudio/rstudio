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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * An unmodifiable container of Ranges that map from JavaScript to the Java it came from.
 */
public class JsSourceMap implements Serializable {

  private int bytes;
  private int lines;

  /**
   * Maps JS ranges to Java ranges. The mapping is sparse thus the need for separately tracking
   * total bytes and lines.
   */
  private List<Range> ranges;

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

  public int getLines() {
    return lines;
  }

  public List<Range> getRanges() {
    return ranges;
  }

  private void readObject(ObjectInputStream inStream) throws IOException, ClassNotFoundException {
    bytes = inStream.readInt();
    lines = inStream.readInt();

    int rangeCount = inStream.readInt();
    ranges = new ArrayList<Range>(rangeCount);
    for (int i = 0; i < rangeCount; i++) {
      int start = inStream.readInt();
      int end = inStream.readInt();
      int startLine = inStream.readInt();
      int startColumn = inStream.readInt();
      int endLine = inStream.readInt();
      int endColumn = inStream.readInt();
      SourceInfo sourceInfo = (SourceInfo) inStream.readObject();

      ranges.add(new Range(start, end, startLine, startColumn, endLine, endColumn, sourceInfo));
    }
  }

  private void writeObject(ObjectOutputStream outStream) throws IOException {
    outStream.writeInt(bytes);
    outStream.writeInt(lines);

    outStream.writeInt(ranges.size());
    for (Range range : ranges) {
      outStream.writeInt(range.getStart());
      outStream.writeInt(range.getEnd());
      outStream.writeInt(range.getStartLine());
      outStream.writeInt(range.getStartColumn());
      outStream.writeInt(range.getEndLine());
      outStream.writeInt(range.getEndColumn());
      outStream.writeObject(range.getSourceInfo());
    }
  }
}
