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
package com.google.gwt.core.client.impl;

/**
 * A {@link StringBufferImpl} that uses += for appending strings. This appears
 * to be the fastest implementation everywhere except IE, where it's terrible.
 */
public class StringBufferImplAppend extends StringBufferImpl {
  private String string = "";

  @Override
  public void append(Object data, boolean x) {
    string += x;
  }

  @Override
  public void append(Object data, double x) {
    string += x;
  }

  @Override
  public void append(Object data, float x) {
    string += x;
  }

  @Override
  public void append(Object data, int x) {
    string += x;
  }

  @Override
  public void append(Object data, Object x) {
    string += x;
  }

  @Override
  public void append(Object data, String x) {
    string += x;
  }

  @Override
  public void appendNonNull(Object data, String x) {
    string += x;
  }

  @Override
  public Object createData() {
    return null;
  }

  @Override
  public int length(Object data) {
    return string.length();
  }

  @Override
  public void replace(Object data, int start, int end, String toInsert) {
    string = string.substring(0, start) + toInsert + string.substring(end);
  }

  @Override
  public String toString(Object data) {
    return string;
  }
}
