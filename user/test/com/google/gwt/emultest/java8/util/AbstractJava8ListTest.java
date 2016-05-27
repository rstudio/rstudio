/*
 * Copyright 2016 Google Inc.
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

import static java.util.Arrays.asList;

import com.google.gwt.emultest.java.util.EmulTestBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Tests for java.util.List implementing classes Java 8 API emulation.
 */
abstract class AbstractJava8ListTest extends EmulTestBase {

  public void testForeach() {
    List<String> list = createEmptyList();

    try {
      list.forEach(null);
      fail();
    } catch (NullPointerException expected) {
    }

    list.forEach(e -> fail());

    list = createEmptyList();
    list.addAll(asList("a", "b", "c"));
    ArrayList<String> visited = new ArrayList<>();
    list.forEach(visited::add);
    assertEquals(asList("a", "b", "c"), visited);
  }

  public void testRemoveIf() {
    List<String> list = createEmptyList();

    try {
      list.removeIf(null);
      fail();
    } catch (NullPointerException expected) {
    }

    list = createEmptyList();
    list.addAll(asList("a", "b", "c"));
    assertFalse(list.removeIf(e -> false));
    assertEquals(asList("a", "b", "c"), list);

    assertFalse(list.removeIf(Predicate.isEqual("")));
    assertEquals(asList("a", "b", "c"), list);

    assertTrue(list.removeIf(Predicate.isEqual("b")));
    assertEquals(asList("a", "c"), list);

    list.add("d");
    assertTrue(list.removeIf(e -> e.equals("a") || e.equals("c")));
    assertEquals(asList("d"), list);

    assertTrue(list.removeIf(Predicate.isEqual("d")));
    assertFalse(list.removeIf(Predicate.isEqual("d")));
    assertTrue(list.isEmpty());

    Collections.addAll(list, "a", "b");
    assertFalse(list.removeIf(Objects::isNull));
    assertEquals(asList("a", "b"), list);
  }

  public void testReplaceAll() {
    List<String> list = createEmptyList();

    try {
      list.replaceAll(null);
      fail();
    } catch (NullPointerException expected) {
    }

    list.replaceAll(UnaryOperator.identity());
    assertTrue(list.isEmpty());

    Collections.addAll(list, "a", "b");
    list.replaceAll(UnaryOperator.identity());
    assertEquals(asList("a", "b"), list);

    list.replaceAll(e -> e + "0");
    assertEquals(asList("a0", "b0"), list);

    list.add("c");
    list.replaceAll(e -> e + "1");
    assertEquals(asList("a01", "b01", "c1"), list);
  }

  public void testSort() {
    List<String> list = createEmptyList();
    list.sort(null);

    Collections.addAll(list, "b", "a", "c");
    list.sort(null);
    assertEquals(asList("a", "b", "c"), list);

    list = createEmptyList();
    Collections.addAll(list, "b", "a", "c");
    list.sort(Collections.reverseOrder());
    assertEquals(asList("c", "b", "a"), list);
  }

  protected abstract List<String> createEmptyList();

}
