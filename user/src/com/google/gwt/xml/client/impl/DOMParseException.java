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
package com.google.gwt.xml.client.impl;

import com.google.gwt.xml.client.DOMException;

/**
 * Thrown when parse errors occur in the underlying implementation.
 */
public class DOMParseException extends DOMException {

  /**
   * Maximum size of error message in summary.
   */
  private static final int MAX_SUMMARY_LENGTH = 128;

  static String summarize(String text) {
    return text.substring(0, Math.min(text.length(), MAX_SUMMARY_LENGTH));
  }

  private String contents;

  public DOMParseException() {
    super(DOMException.SYNTAX_ERR, "Parse error");
  }

  public DOMParseException(String contents) {
    super(DOMException.SYNTAX_ERR, "Failed to parse: " + summarize(contents));
    this.contents = contents;
  }

  public DOMParseException(String contents, Throwable e) {
    super(DOMException.SYNTAX_ERR, "Failed to parse: " + summarize(contents));
    initCause(e);
    this.contents = contents;
  }

  public String getContents() {
    return contents;
  }
}
