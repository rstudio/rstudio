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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jjs.ast.JRunAsync;
import com.google.gwt.dev.jjs.impl.ControlFlowAnalyzer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The simplest fragmentation strategy, each non initial runAsync is assigned to a different
 * exclusive fragment.
*/
class OneToOneFragmentPartitionStrategy implements FragmentPartitionStrategy {
  @Override
  public List<Fragment> partitionIntoFragments(TreeLogger logger, ControlFlowAnalyzer initiallyLive,
      Collection<JRunAsync> nonInitialRunAsyncs) {
    /**
     * Create the exclusive fragments according to a one-to-one assignment.
     */
    List<Fragment> fragments = new ArrayList<Fragment>();
    for (JRunAsync runAsync : nonInitialRunAsyncs) {
      Fragment fragment =
          new Fragment(Fragment.Type.EXCLUSIVE);
      fragment.addRunAsync(runAsync);
      fragments.add(fragment);
    }
    return fragments;
  }
}
