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
package com.google.gwt.emultest.java8.lang;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.Arrays;

/**
 * Java8 String tests.
 */
public class StringTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testJoin() {
    assertEquals("", String.join("", ""));
    assertEquals("", String.join(",", ""));
    assertEquals("", String.join(",", Arrays.<String>asList()));

    assertEquals("a", String.join("", "a"));
    assertEquals("a", String.join(",", "a"));
    assertEquals("a", String.join(",", Arrays.asList("a")));

    assertEquals("ab", String.join("", "a", "b"));
    assertEquals("a,b", String.join(",", "a", "b"));
    assertEquals("a,b", String.join(",", Arrays.asList("a", "b")));

    assertEquals("abc", String.join("", "a", "b", "c"));
    assertEquals("a,b,c", String.join(",", "a", "b", "c"));
    assertEquals("a,b,c", String.join(",", Arrays.asList("a", "b", "c")));
  }

}
