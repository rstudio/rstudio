/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.dev.jjs.impl.codesplitter;

import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.dev.jjs.ast.JRunAsync;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import junit.framework.TestCase;

import java.util.List;

/**
 * Tests {@link FragmentPartitioningResult#getCommonAncestorFragmentId(int, int)}.
 */
public class FragmentPartitioningResultTest extends TestCase {

  private final int NUM_RUNASYNCS = 10;
  private final JRunAsync[] runAsyncs = new JRunAsync[NUM_RUNASYNCS + 1];

  @Override
  public void setUp() {
    // Create some runAsyncs.
    for (int i = 1; i <= NUM_RUNASYNCS; i++) {
      runAsyncs[i] = new JRunAsync(SourceOrigin.UNKNOWN, i, "runAsync" + i, false,
          JNullLiteral.INSTANCE, JNullLiteral.INSTANCE);
    }
  }

  public void testBasics() {
    // TODO(rluble): replace all this setup with mocks once mockito is in.

    // Begin setup
    List<JRunAsync> initialSequence = Lists.newArrayList(runAsyncs[4], runAsyncs[3], runAsyncs[2]);

    List<Fragment> fragments = Lists.newArrayList();
    // Create fragment 0 that is not associated with any runAsync and represent the code live
    // from the entry point.
    Fragment entryFragment = new Fragment(Fragment.Type.INITIAL);
    entryFragment.setFragmentId(0);
    fragments.add(entryFragment);
    Fragment lastInitialFragment = entryFragment;
    int nextId = 1;
    for (JRunAsync runAsync : initialSequence) {
      Fragment initialFragment = new Fragment(Fragment.Type.INITIAL, lastInitialFragment);
      initialFragment.addRunAsync(runAsync);
      initialFragment.setFragmentId(nextId++);
      lastInitialFragment = initialFragment;
      fragments.add(initialFragment);
    }

    // create leftover fragment
    final int LEFTOVERS_ID = NUM_RUNASYNCS + 1;
    Fragment leftoversFragment = new Fragment(Fragment.Type.NOT_EXCLUSIVE, lastInitialFragment);
    leftoversFragment.setFragmentId(LEFTOVERS_ID);

    // Create exclusive fragments.
    for (JRunAsync runAsync : runAsyncs) {
      if (runAsync == null || initialSequence.contains(runAsync)) {
        continue;
      }
      Fragment exclusiveFragment = new Fragment(Fragment.Type.EXCLUSIVE, leftoversFragment);
      exclusiveFragment.addRunAsync(runAsync);
      assert nextId < LEFTOVERS_ID;
      exclusiveFragment.setFragmentId(nextId++);
      fragments.add(exclusiveFragment);
    }

    // leftover fragment is the last one.
    // TODO(rluble): make fragment numbering independent to the load partial ordering.
    fragments.add(leftoversFragment);

    // End setup.

    FragmentPartitioningResult fragmentPartitioningResult =
        new FragmentPartitioningResult(fragments, NUM_RUNASYNCS);

    // The closest common ancestor is itself.
    assertEquals(0, fragmentPartitioningResult.getCommonAncestorFragmentId(0, 0));
    assertEquals(1, fragmentPartitioningResult.getCommonAncestorFragmentId(1, 1));
    assertEquals(3, fragmentPartitioningResult.getCommonAncestorFragmentId(3, 3));
    assertEquals(6, fragmentPartitioningResult.getCommonAncestorFragmentId(6, 6));
    assertEquals(11, fragmentPartitioningResult.getCommonAncestorFragmentId(11, 11));

    // Lowest number in the initial sequence is the closest common ancestor.
    assertEquals(0, fragmentPartitioningResult.getCommonAncestorFragmentId(11, 0));
    assertEquals(0, fragmentPartitioningResult.getCommonAncestorFragmentId(10, 0));
    assertEquals(0, fragmentPartitioningResult.getCommonAncestorFragmentId(3, 0));

    // Initial sequence fragments load before all others
    assertEquals(3, fragmentPartitioningResult.getCommonAncestorFragmentId(3, 10));
    assertEquals(3, fragmentPartitioningResult.getCommonAncestorFragmentId(10, 3));

    // Earlier initial sequence fragments load before the others
    assertEquals(1, fragmentPartitioningResult.getCommonAncestorFragmentId(1, 2));
    assertEquals(1, fragmentPartitioningResult.getCommonAncestorFragmentId(2, 1));
    assertEquals(2, fragmentPartitioningResult.getCommonAncestorFragmentId(2, 3));

    // For non-equal exclusive fragments, leftovers is the common predecessor
    assertEquals(LEFTOVERS_ID, fragmentPartitioningResult.getCommonAncestorFragmentId(6, 8));
    assertEquals(LEFTOVERS_ID, fragmentPartitioningResult.getCommonAncestorFragmentId(9, 8));

    // Leftovers is before any exclusive
    assertEquals(LEFTOVERS_ID, fragmentPartitioningResult.getCommonAncestorFragmentId(LEFTOVERS_ID, 8));

    // Inintial are before leftover
    assertEquals(1, fragmentPartitioningResult.getCommonAncestorFragmentId(LEFTOVERS_ID, 1));
  }


  public void testWithEmptyInitial() {
    // TODO(rluble): replace all this setup with mocks once mockito is in.

    // Begin setup
    List<Fragment> fragments = Lists.newArrayList();
    // Create fragment 0 that is not associated with any runAsync and represent the code live
    // from the entry point.
    Fragment entryFragment = new Fragment(Fragment.Type.INITIAL);
    entryFragment.setFragmentId(0);
    fragments.add(entryFragment);
    Fragment lastInitialFragment = entryFragment;
    int nextId = 1;

    // create leftover fragment
    final int LEFTOVERS_ID = NUM_RUNASYNCS + 1;
    Fragment leftoversFragment = new Fragment(Fragment.Type.NOT_EXCLUSIVE, lastInitialFragment);
    leftoversFragment.setFragmentId(LEFTOVERS_ID);

    // Create exclusive fragments.
    for (JRunAsync runAsync : runAsyncs) {
      if (runAsync == null) {
        continue;
      }
      Fragment exclusiveFragment = new Fragment(Fragment.Type.EXCLUSIVE, leftoversFragment);
      exclusiveFragment.addRunAsync(runAsync);
      assert nextId < LEFTOVERS_ID;
      exclusiveFragment.setFragmentId(nextId++);
      fragments.add(exclusiveFragment);
    }

    // leftover fragment is the last one.
    // TODO(rluble): make fragment numbering independent to the load partial ordering.
    fragments.add(leftoversFragment);

    // End setup.

    FragmentPartitioningResult fragmentPartitioningResult =
        new FragmentPartitioningResult(fragments, NUM_RUNASYNCS);

    // The closest common ancestor is itself.
    assertEquals(0, fragmentPartitioningResult.getCommonAncestorFragmentId(0, 0));
    assertEquals(1, fragmentPartitioningResult.getCommonAncestorFragmentId(1, 1));
    assertEquals(3, fragmentPartitioningResult.getCommonAncestorFragmentId(3, 3));
    assertEquals(6, fragmentPartitioningResult.getCommonAncestorFragmentId(6, 6));
    assertEquals(11, fragmentPartitioningResult.getCommonAncestorFragmentId(11, 11));

    // Lowest number in the initial sequence is the closest common ancestor.
    assertEquals(0, fragmentPartitioningResult.getCommonAncestorFragmentId(11, 0));
    assertEquals(0, fragmentPartitioningResult.getCommonAncestorFragmentId(10, 0));
    assertEquals(0, fragmentPartitioningResult.getCommonAncestorFragmentId(3, 0));

    // For non-equal exclusive fragments, leftovers is the common predecessor
    assertEquals(LEFTOVERS_ID, fragmentPartitioningResult.getCommonAncestorFragmentId(6, 8));
    assertEquals(LEFTOVERS_ID, fragmentPartitioningResult.getCommonAncestorFragmentId(9, 8));

    // Leftovers is before any exclusive
    assertEquals(LEFTOVERS_ID, fragmentPartitioningResult.getCommonAncestorFragmentId(LEFTOVERS_ID, 8));

    // Inintial are before leftover
    assertEquals(0, fragmentPartitioningResult.getCommonAncestorFragmentId(LEFTOVERS_ID, 0));
  }
}
