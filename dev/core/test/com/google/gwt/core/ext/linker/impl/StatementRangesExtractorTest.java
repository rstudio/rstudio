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

import com.google.gwt.thirdparty.guava.common.collect.Lists;

import junit.framework.TestCase;

import java.util.Arrays;

/**
 * Tests for StatementRangesExtractor.
 */
public class StatementRangesExtractorTest extends TestCase {

  public void testExtract() {
    // Initializes program statement ranges.
    StandardStatementRanges programStatementRanges = new StandardStatementRanges(
        Lists.newArrayList(0, 10, 20, 30, 1000, 1005, 1010, 1015, 10000, 10005, 10010, 10015),
        Lists.newArrayList(10, 20, 30, 40, 1005, 1010, 1015, 1020, 10005, 10010, 10015, 10020));

    // Prepares the extractor.
    StatementRangesExtractor programStatementRangesExtractor =
        new StatementRangesExtractor(programStatementRanges);

    // Verifies extraction of class foo.
    StandardStatementRanges classFooStatementRanges =
        (StandardStatementRanges) programStatementRangesExtractor.extract(0, 1000);
    assertTrue(Arrays.equals(new int[] {0, 10, 20, 30}, classFooStatementRanges.starts));
    assertTrue(Arrays.equals(new int[] {10, 20, 30, 40}, classFooStatementRanges.ends));

    // Verifies extraction of class bar.
    StandardStatementRanges classBarStatementRanges =
        (StandardStatementRanges) programStatementRangesExtractor.extract(1000, 10000);
    assertTrue(Arrays.equals(new int[] {1000, 1005, 1010, 1015}, classBarStatementRanges.starts));
    assertTrue(Arrays.equals(new int[] {1005, 1010, 1015, 1020}, classBarStatementRanges.ends));

    // Verifies extraction of class baz.
    StandardStatementRanges classBazStatementRanges =
        (StandardStatementRanges) programStatementRangesExtractor.extract(10000, 10020);
    assertTrue(
        Arrays.equals(new int[] {10000, 10005, 10010, 10015}, classBazStatementRanges.starts));
    assertTrue(Arrays.equals(new int[] {10005, 10010, 10015, 10020}, classBazStatementRanges.ends));
  }
}
