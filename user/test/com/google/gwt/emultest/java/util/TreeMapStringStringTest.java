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
package com.google.gwt.emultest.java.util;

/**
 * Tests <code>TreeMap</code> with Strings and the natural comparator.
 */
public class TreeMapStringStringTest extends TreeMapTest<String, String> {

  @Override
  String getGreaterThanMaximumKey() {
    return "z";
  }

  @Override
  String[] getKeys() {
    return convertToStringArray(getSampleKeys());
  }

  private String[] convertToStringArray(Object[] objArray) {
    String[] strArray = new String[objArray.length];
    System.arraycopy(objArray, 0, strArray, 0, objArray.length);
    return strArray;
  }

  @Override
  String[] getKeys2() {
    return convertToStringArray(getOtherKeys());
  }

  @Override
  String getLessThanMinimumKey() {
    return "a";
  }

  @Override
  String[] getValues() {
    return convertToStringArray(getSampleValues());
  }

  @Override
  String[] getValues2() {
    return convertToStringArray(getOtherValues());
  }

  @Override
  protected Object getConflictingKey() {
    return new Integer(1);
  }

  @Override
  protected Object getConflictingValue() {
    return new Long(42);
  }
}
