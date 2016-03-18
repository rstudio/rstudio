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

import com.google.gwt.emultest.java.util.EmulTestBase;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Java 8 methods to test in java.util.Comparator.
 */
public class ComparatorTest extends EmulTestBase {

  public void testThenComparing() {
    Supplier<String[]> strings = () -> new String[] {"1,b", "1,a", "2,a"};
    // expected sort results for 1st and 2nd char, each (f)orward or (r)everse
    String[] f1f2 = {"1,a", "1,b", "2,a"};
    String[] f1r2 = {"1,b", "1,a", "2,a"};
    String[] f2f1 = {"1,a", "2,a" ,"1,b"};

    // keyextractor
    assertSortedEquals(f1f2, strings,
        Comparator.<String, String>comparing(s -> s.split(",")[0])
            .thenComparing(s -> s.split(",")[1])
    );
    // keyextractor, keycomparator
    assertSortedEquals(f1r2, strings,
        Comparator.<String, String>comparing(s -> s.split(",")[0])
            .thenComparing(
                s -> s.split(",")[1],
                Comparator.<String>reverseOrder()
            )
    );

    // int key extractor
    assertSortedEquals(f2f1, strings,
        Comparator.<String, String>comparing(s -> s.split(",")[1])
            .thenComparingInt(
                s -> Integer.parseInt(s.split(",")[0])
            )
    );
    // long key extractor
    assertSortedEquals(f2f1, strings,
        Comparator.<String, String>comparing(s -> s.split(",")[1])
            .thenComparingLong(
                s -> Long.parseLong(s.split(",")[0])
            )
    );
    // double key extractor
    assertSortedEquals(f2f1, strings,
        Comparator.<String, String>comparing(s -> s.split(",")[1])
            .thenComparingDouble(
                s -> Double.parseDouble(s.split(",")[0])
            )
    );
  }

  public void testComparing() {
    Supplier<String[]> strings = () -> new String[] {"1b3", "2a1", "3c2"};
    // pre-sorted lists to test against, named for which char (1-indexed) they are sorted on
    String[] first = {"1b3", "2a1", "3c2"};
    String[] second = {"2a1", "1b3", "3c2"};
    String[] secondReversed = {"3c2", "1b3", "2a1"};
    String[] third = {"2a1", "3c2", "1b3"};

    // keyextractor
    assertSortedEquals(first, strings, Comparator.comparing(Function.identity()));
    assertSortedEquals(second, strings, Comparator.comparing(a -> a.substring(1)));
    assertSortedEquals(third, strings, Comparator.comparing(a -> a.substring(2)));

    // keyextractor, keycomparator
    assertSortedEquals(secondReversed, strings, Comparator.comparing(a -> a.substring(1), Comparator.<String>reverseOrder()));

    // double key extractor
    assertSortedEquals(third, strings, Comparator.comparingDouble(a -> Double.parseDouble(a.substring(2))));
    // int key extractor
    assertSortedEquals(third, strings, Comparator.comparingInt(a -> Integer.parseInt(a.substring(2))));
    // long key extractor
    assertSortedEquals(third, strings, Comparator.comparingLong(a -> Long.parseLong(a.substring(2))));
  }

  public void testNullsFirst() {
    Supplier<String[]> strings = () -> new String[]{"a", null, "c", null, "b"};
    assertSortedEquals(
        new String[] {null, null, "a", "b", "c"},
        strings,
        Comparator.nullsFirst(Comparator.naturalOrder())
    );
    assertSortedEquals(
        new String[] {null, null, "c", "b", "a"},
        strings,
        Comparator.nullsFirst(Comparator.reverseOrder())
    );
  }

  public void testNullsLast() {
    Supplier<String[]> strings = () -> new String[]{"a", null, "c", null, "b"};
    assertSortedEquals(
        new String[] {"a", "b", "c", null, null},
        strings,
        Comparator.nullsLast(Comparator.naturalOrder())
    );
    assertSortedEquals(
        new String[] {"c", "b", "a", null, null},
        strings,
        Comparator.nullsLast(Comparator.reverseOrder())
    );
  }

  private static void assertSortedEquals(String[] expected, Supplier<String[]> presort, Comparator<String> comparator) {
    String[] presortedArray = presort.get();
    Arrays.sort(presortedArray, comparator);
    assertEquals(expected, presortedArray);
  }
}
