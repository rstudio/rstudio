/*
 * Copyright 2015 Google Inc.
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
package com.google.gwt.emultest.java8.util;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.StringJoiner;

/**
 * Tests StringJoiner.
 */
public class StringJoinerTest extends GWTTestCase {

  private StringJoiner joiner;

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    joiner = new StringJoiner("|", "[", "]");
  }

  public void testConstructor() {
    try {
      new StringJoiner(null, null, null);
      fail("NullPointerException must be thrown if any constructor parameter is null");
    } catch (NullPointerException e) {
      // expected
    }
  }

  public void testAdd() throws Exception {
    joiner.add("0").add(null);
    assertEquals("[0|null]", joiner.toString());
  }

  public void testLength() throws Exception {
    assertEquals(joiner.toString().length(), joiner.length());

    joiner.setEmptyValue("empty");
    assertEquals(joiner.toString().length(), joiner.length());

    joiner.add("0").add("1");
    assertEquals(joiner.toString().length(), joiner.length());
  }

  public void testMerge() throws Exception {
    joiner.add("0").add("1");
    joiner.merge(new StringJoiner(",", "(", ")").add("2").add("3"));
    joiner.add("4").add("5");
    assertEquals("[0|1|2,3|4|5]", joiner.toString());

    joiner.merge(joiner);
    assertEquals("[0|1|2,3|4|5|0|1|2,3|4|5]", joiner.toString());

    try {
      joiner.merge(null);
      fail("NullPointerException must be thrown if other joiner is null");
    } catch (NullPointerException | JavaScriptException e) {
      // expected
    }
  }

  public void testSetEmptyValue() throws Exception {
    joiner.setEmptyValue("empty");
    assertEquals("empty", joiner.toString());

    try {
      joiner.setEmptyValue(null);
      fail("NullPointerException must be thrown if emptyValue is null");
    } catch (NullPointerException e) {
      // expected
    }
  }
}