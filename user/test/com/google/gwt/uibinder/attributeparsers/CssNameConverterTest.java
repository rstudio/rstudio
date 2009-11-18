/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.uibinder.attributeparsers;

import junit.framework.TestCase;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Eponymous test class.
 */
public class CssNameConverterTest extends TestCase {
  static class Pair {
    final String before;
    final String after;

    Pair(String before, String after) {
      this.before = before;
      this.after = after;
    }
  }

  private final CssNameConverter converter = new CssNameConverter();

  public void testCollision() {
    Set<String> in = makeOrderedSet("charlie-delta", "baker", "charlieDelta",
        "echo");

    try {
      converter.convertSet(in);
      fail();
    } catch (CssNameConverter.Failure e) {
      assertContains(e.getMessage(), "charlie-delta");
      assertContains(e.getMessage(), "charlieDelta");
    }
  }

  public void testNameConversion() {
    Pair[] beforeAndAfter = {
        new Pair("able", "able"), new Pair("-baker-", "baker"),
        new Pair("charlie-delta", "charlieDelta"), new Pair("echo", "echo"),
        new Pair("foxtrot-Tango", "foxtrotTango")};
    
    for (Pair pair : beforeAndAfter) {
      assertEquals(pair.after, converter.convertName(pair.before));
    }
  }

  public void testNoOp() throws CssNameConverter.Failure {
    Set<String> in = makeOrderedSet("able", "baker", "charlieDelta", "echo");
    Map<String, String> out = converter.convertSet(in);

    for (Map.Entry<String, String> entry : out.entrySet()) {
      String key = entry.getKey();
      assertTrue(in.remove(key));
      assertEquals(key, entry.getValue());
    }
    assertTrue(in.isEmpty());
  }

  public void testReverseCollision() {
    Set<String> in = makeOrderedSet("charlieDelta", "baker", "charlie-delta",
        "echo");

    try {
      converter.convertSet(in);
      fail();
    } catch (CssNameConverter.Failure e) {
      assertContains(e.getMessage(), "charlie-delta");
      assertContains(e.getMessage(), "charlieDelta");
    }
  }

  public void testSomeOp() throws CssNameConverter.Failure {
    Set<String> in = makeOrderedSet("able", "-baker-", "charlie-delta", "echo",
        "foxtrot-Tango");
    Pair[] expected = {
        new Pair("able", "able"), new Pair("-baker-", "baker"),
        new Pair("charlie-delta", "charlieDelta"), new Pair("echo", "echo"),
        new Pair("foxtrot-Tango", "foxtrotTango")};

    Map<String, String> out = converter.convertSet(in);

    for (Pair pair : expected) {
      String convert = out.remove(pair.before);
      assertEquals(pair.after, convert);
    }

    assertTrue(out.isEmpty());
  }

  private void assertContains(String string, String fragment) {
    assertTrue(String.format("%s should contain %s", string, fragment),
        string.contains(fragment));
  }

  private Set<String> makeOrderedSet(String... strings) {
    LinkedHashSet<String> set = new LinkedHashSet<String>();
    for (String string : strings) {
      set.add(string);
    }
    return set;
  }
}
