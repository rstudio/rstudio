/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.dev.jjs.impl;

import junit.framework.TestCase;

import java.util.List;

/**
 * Tests the {@link OptimizerStats} class.
 */
public class OptimizerStatsTest extends TestCase {

  public void testOptimizerStats() {
    OptimizerStats stats = new OptimizerStats("foo");
    assertEquals("foo", stats.getName());
    assertEquals(0, stats.getNumMods());
    assertEquals(0, stats.getNumVisits());
    assertFalse(stats.didChange());
    List<OptimizerStats> children = stats.getChildren();
    assertNotNull(children);
    assertEquals(0, children.size());

    stats.recordModified();
    assertEquals(1, stats.getNumMods());
    assertEquals(0, stats.getNumVisits());
    assertNotNull(stats.prettyPrint());
    assertTrue(stats.didChange());

    stats.recordVisit();
    assertEquals(1, stats.getNumVisits());
    assertEquals(1, stats.getNumMods());

    stats.recordModified(10);
    assertEquals(11, stats.getNumMods());
    assertEquals(1, stats.getNumVisits());

    stats.recordVisits(5);
    assertEquals(6, stats.getNumVisits());
    assertEquals(11, stats.getNumMods());

    OptimizerStats childStats = new OptimizerStats("bar");
    childStats.recordModified(9).recordVisits(24);
    assertEquals(9, childStats.getNumMods());
    assertEquals(24, childStats.getNumVisits());

    stats.add(childStats);
    children = stats.getChildren();
    assertNotNull(children);
    assertEquals(1, children.size());

    // Child stats should be added to parent object's tally
    assertEquals(20, stats.getNumMods());
    assertEquals(30, stats.getNumVisits());
  }

  public void testOptimizerStatsChangeChildOnly() {
    OptimizerStats stats = new OptimizerStats("foo");
    OptimizerStats childStats = new OptimizerStats("bar");
    stats.add(childStats);
    assertFalse(stats.didChange());
    childStats.recordModified();
    assertTrue(stats.didChange());
  }
}
