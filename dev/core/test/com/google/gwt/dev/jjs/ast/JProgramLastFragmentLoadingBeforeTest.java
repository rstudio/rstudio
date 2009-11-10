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
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.dev.util.collect.Lists;

import junit.framework.TestCase;

import java.util.List;

/**
 * Tests {@link JProgram#lastFragmentLoadingBefore(int, int...)}.
 */
public class JProgramLastFragmentLoadingBeforeTest extends TestCase {

  public void testBasics() {
    List<Integer> initialSeq = Lists.create(4, 3, 2);
    int numSps = 10;

    // Very simple
    assertEquals(0, JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 0,
        1, 2, 3));

    // Equal fragments load at the same time
    assertEquals(0,
        JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 0, 0));
    assertEquals(1,
        JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 1, 1));
    assertEquals(3,
        JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 3, 3));
    assertEquals(6,
        JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 6, 6));
    assertEquals(11, JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 11,
        11));

    // Zero loads first
    assertEquals(0, JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 11,
        0));
    assertEquals(0, JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 10,
        0));
    assertEquals(0,
        JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 3, 0));

    // Initial sequence fragments load before all others
    assertEquals(3, JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 3,
        10));
    assertEquals(3, JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 10,
        3));

    // Earlier initial sequence fragments load before the others
    assertEquals(4, JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 2,
        3, 4));
    assertEquals(3,
        JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 2, 3));
    assertEquals(4, JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 4,
        3, 2));

    // For non-equal exclusive fragments, leftovers is the common predecessor
    assertEquals(11, JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 1,
        7));

    // Leftovers is before any exclusive
    assertEquals(11, JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 7,
        11));
  }

  public void testWithEmptyInitial() {
    List<Integer> initialSeq = Lists.create();
    int numSps = 10;

    // Simple case
    assertEquals(0, JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 0,
        1, 2, 3));

    // Equal fragments load at the same time
    assertEquals(0,
        JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 0, 0));
    assertEquals(6,
        JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 6, 6));
    assertEquals(11, JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 11,
        11));

    // Zero loads first
    assertEquals(0, JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 11,
        0));
    assertEquals(0, JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 10,
        0));
    assertEquals(0,
        JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 3, 0));

    // For non-equal exclusive fragments, leftovers is the common predecessor
    assertEquals(11, JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 1,
        7));

    // Leftovers is before any exclusive
    assertEquals(11, JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 7,
        11));

    // With just one argument, return it
    assertEquals(0, JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 0));
    assertEquals(3, JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 3));
    assertEquals(11, JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 11));
  }

  public void testWithNoSplitPoints() {
    List<Integer> initialSeq = Lists.create();
    int numSps = 0;

    assertEquals(0,
        JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 0, 0));
    assertEquals(0, JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 0,
        0, 0));
    assertEquals(0, JProgram.lastFragmentLoadingBefore(initialSeq, numSps, 0));
  }
}
