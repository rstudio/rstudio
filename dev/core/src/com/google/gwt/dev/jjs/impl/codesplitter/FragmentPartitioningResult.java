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

import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JRunAsync;

import java.util.Collection;

/**
 * A read-only class that holds some information about the result of the
 * partition process.
 *
 * Unlike the original code splitter where information about the fragments and
 * be deduced from the JProgram, certain compiler passes needs to know what
 * happened here in order to do their job correctly.
 */
public class FragmentPartitioningResult {
  private final int[] fragmentToRunAsyncId;
  private final int[] runAsyncIdToFragment;

  FragmentPartitioningResult(Collection<Fragment> fragments, JProgram jprogram) {
    this.runAsyncIdToFragment = new int[jprogram.getRunAsyncs().size() + 1];
    fragmentToRunAsyncId = new int[fragments.size()];

    for (Fragment fragment : fragments) {
      for (JRunAsync runAsync : fragment.getRunAsyncs()) {
        runAsyncIdToFragment[runAsync.getRunAsyncId()] = fragment.getFragmentId();
        // If the fragment contains more than one runAsync, it will be set to -1 next.
        fragmentToRunAsyncId[fragment.getFragmentId()] = runAsync.getRunAsyncId();
      }
      if (fragment.getRunAsyncs().size() > 1) {
        fragmentToRunAsyncId[fragment.getFragmentId()] = -1;
      }
    }
  }

  /**
   * @return Fragment index from a splitpoint number.
   */
  public int getFragmentForRunAsync(int splitpoint) {
    return runAsyncIdToFragment[splitpoint];
  }

  /**
   * @return Fragment number of the left over fragment.
   */
  public int getLeftoverFragmentIndex() {
    return getNumFragments() - 1;
  }

  /**
   * @return Total number of code fragments in the compilation (initial + exclusives + leftovers).
   */
  public int getNumFragments() {
    return fragmentToRunAsyncId.length;
  }

  /**
   * @return One of the split point number in a given fragment. If there
   *     are more than one splitpoints in the a fragment, -1 is returned.
   */
  public int getRunAsyncIdForFragment(int fragment) {
    return fragmentToRunAsyncId[fragment];
  }
}
