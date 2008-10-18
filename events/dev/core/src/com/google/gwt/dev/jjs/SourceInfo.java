/*
 * Copyright 2008 Google Inc.
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

import java.io.Serializable;

/**
 * Tracks file and line information for AST nodes.
 */
public class SourceInfo implements Serializable {

  private final int endPos;
  private final String fileName;
  private final int startLine;
  private final int startPos;

  public SourceInfo(int startPos, int endPos, int startLine, String fileName) {
    this.startPos = startPos;
    this.endPos = endPos;
    this.startLine = startLine;
    this.fileName = fileName;
  }

  public int getEndPos() {
    return endPos;
  }

  public String getFileName() {
    return fileName;
  }

  public int getStartLine() {
    return startLine;
  }

  public int getStartPos() {
    return startPos;
  }
}
