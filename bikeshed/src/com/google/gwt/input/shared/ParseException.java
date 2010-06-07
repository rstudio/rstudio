/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.input.shared;

/**
 * Exception class indicating parsing errors.
 */
public class ParseException extends RuntimeException {

  private final String rawInput;
  private final int offset;

  public ParseException(Throwable e) {
    this("", "", 0, e);
  }

  public ParseException(String rawInput) {
    this("", rawInput, 0, null);
  }

  public ParseException(String rawInput, int offset) {
    this("", rawInput, offset, null);
  }

  public ParseException(String message, String rawInput, int offset) {
    this(message, rawInput, offset, null);
  }

  public ParseException(String message, String rawInput, int offset, Throwable e) {
    super(message, e);
    this.rawInput = rawInput;
    this.offset = offset;
  }

  public String getInput() {
    return rawInput;
  }
  
  public int getOffset() {
    return offset;
  }
}