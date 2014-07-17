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
 * Tests for StatementRangesBuilder.
 */
public class StatementRangesBuilderTest extends TestCase {

  public void testBuild() {
    // Appends Ranges.
    StatementRangesBuilder statementRangesBuilder = new StatementRangesBuilder();
    statementRangesBuilder.append(new StandardStatementRanges(Lists.newArrayList(0, 10, 20, 30),
        Lists.newArrayList(10, 20, 30, 40)));
    statementRangesBuilder.append(new StandardStatementRanges(
        Lists.newArrayList(1000, 1005, 1010, 1015), Lists.newArrayList(1005, 1010, 1015, 1020)));
    statementRangesBuilder.append(new StandardStatementRanges(
        Lists.newArrayList(10000, 10005, 10010, 10015),
        Lists.newArrayList(10005, 10010, 10015, 10020)));

    // Builds the result.
    StandardStatementRanges combinedRanges =
        (StandardStatementRanges) statementRangesBuilder.build();

    // Verifies result structure.
    assertTrue(Arrays.equals(new int[] {0, 10, 20, 30, 40, 45, 50, 55, 60, 65, 70, 75},
        combinedRanges.starts));
    assertTrue(Arrays.equals(new int[] {10, 20, 30, 40, 45, 50, 55, 60, 65, 70, 75, 80},
        combinedRanges.ends));
  }
}
