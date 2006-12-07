/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.dev.js;

/**
 * Thrown due to a JavaScript parser exception.
 */
public class JsParserException extends Exception {

  public static class SourceDetail {
    private final int line;

    private final int lineOffset;

    private final String lineSource;

    public SourceDetail(int line, String lineSource, int lineOffset) {
      this.line = line;
      this.lineSource = lineSource;
      this.lineOffset = lineOffset;
    }

    public int getLine() {
      return line;
    }

    public int getLineOffset() {
      return lineOffset;
    }

    public String getLineSource() {
      return lineSource;
    }
  }

  private final SourceDetail sourceDetail;

  public JsParserException(String msg) {
    super(msg);
    sourceDetail = null;
  }

  public JsParserException(String msg, int line, String lineSource,
      int lineOffset) {
    this(msg, line, lineSource, lineOffset, null);
  }

  public JsParserException(String msg, int line, String lineSource,
      int lineOffset, Throwable cause) {
    super(msg, cause);
    sourceDetail = new SourceDetail(line, lineSource, lineOffset);
  }

  public JsParserException(String msg, Throwable cause) {
    super(msg, cause);
    sourceDetail = null;
  }

  public String getDescription() {
    StringBuffer sb = new StringBuffer();

    if (sourceDetail != null) {
      sb.append("Line ");
      sb.append(sourceDetail.getLine());
      sb.append(": ");
      sb.append(getMessage());
      sb.append("\n");
      sb.append("> ");
      sb.append(sourceDetail.getLineSource());
      sb.append("\n> ");
      for (int i = 0, n = sourceDetail.getLineOffset(); i < n; ++i) {
        sb.append('-');
      }
      sb.append('^');
    } else {
      sb.append(getMessage());
    }

    return sb.toString();
  }

  /**
   * Provides additional source detail in some cases.
   * 
   * @return additional detail regarding the error, or <code>null</code> if no
   *         additional detail is available
   */
  public SourceDetail getSourceDetail() {
    return sourceDetail;
  }
}
