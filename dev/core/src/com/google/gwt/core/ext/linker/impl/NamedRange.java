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

/**
 * A starting and ending byte range/line number with associated name.
 */
public class NamedRange {

  private int endLineNumber;
  private int endPosition;
  private final String name;
  private int startLineNumber;
  private int startPosition;

  public NamedRange(String name) {
    this.name = name;
  }

  public NamedRange(String name, int startPosition, int endPosition, int startLineNumber,
      int endLineNumber) {
    this.name = name;
    this.startPosition = startPosition;
    this.endPosition = endPosition;
    this.startLineNumber = startLineNumber;
    this.endLineNumber = endLineNumber;
  }

  public int getEndLineNumber() {
    return endLineNumber;
  }

  public int getEndPosition() {
    return endPosition;
  }

  public String getName() {
    return name;
  }

  public int getStartLineNumber() {
    return startLineNumber;
  }

  public int getStartPosition() {
    return startPosition;
  }

  public void setEndLineNumber(int endLineNumber) {
    this.endLineNumber = endLineNumber;
  }

  public void setEndPosition(int endPosition) {
    this.endPosition = endPosition;
  }

  public void setStartLineNumber(int startLineNumber) {
    this.startLineNumber = startLineNumber;
  }

  public void setStartPosition(int startPosition) {
    this.startPosition = startPosition;
  }
}