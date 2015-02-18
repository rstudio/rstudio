/*
 * Copyright 2013 Google Inc.
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

import com.google.gwt.core.ext.linker.StatementRanges;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import junit.framework.TestCase;

import java.util.Arrays;

/**
 * Tests for {@link StandardStatementRanges}.
 */
public class StandardStatementRangesTest extends TestCase {

  public void testCombine() {
    StandardStatementRanges smallRanges = new StandardStatementRanges(
        Lists.newArrayList(0, 4, 9, 15), Lists.newArrayList(3, 8, 14, 22));
    StandardStatementRanges biggerRanges = new StandardStatementRanges(
        Lists.newArrayList(5, 24, 33, 100), Lists.newArrayList(22, 31, 95, 120));
    StandardStatementRanges emptyRanges =
        new StandardStatementRanges(Lists.<Integer> newArrayList(), Lists.<Integer> newArrayList());

    StandardStatementRanges combinedRanges = (StandardStatementRanges) StandardStatementRanges
        .combine(Lists.<StatementRanges> newArrayList(smallRanges, emptyRanges, biggerRanges));

    assertTrue(Arrays.equals(new int[] {0, 4, 9, 15, 27, 46, 55, 122}, combinedRanges.starts));
    assertTrue(Arrays.equals(new int[] {3, 8, 14, 22, 44, 53, 117, 142}, combinedRanges.ends));
  }
}
