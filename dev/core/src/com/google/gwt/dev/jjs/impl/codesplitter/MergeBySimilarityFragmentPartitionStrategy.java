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
package com.google.gwt.dev.jjs.impl.codesplitter;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jjs.ast.JRunAsync;
import com.google.gwt.dev.jjs.impl.ControlFlowAnalyzer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This strategy implements the fragment merge by similarity strategy.
 * 
 * <p>
 * The fragment merge by similarity computes the set of live atoms starting for the splitpoints in
 * the fragment. This is a rough under-approximation as it does not consider atoms that would be
 * reachable by the splitpoints in this fragment if a different fragment is loaded first.
 * 
 * <p>
 * A similarity graph is constructed (represented by a similarity matrix) from the results of the
 * control flow analysis, and fragments linked by the highest weights are merged.
 * 
 * <p>
 * Finally, fragments that might result very small are also merged into some related fragment.
 * 
 * <p>
 * The control flow analysis that is computed by this strategy is not used to determine exclusivity.
 */
class MergeBySimilarityFragmentPartitionStrategy implements FragmentPartitionStrategy {
  private final int minSize;
  private final int targetNumberOfFragments;

  public MergeBySimilarityFragmentPartitionStrategy(int targetNumberOfFragments, int minSize) {
    this.targetNumberOfFragments = targetNumberOfFragments;
    this.minSize = minSize;
  }

  @Override
  public List<Fragment> partitionIntoFragments(TreeLogger logger,
      ControlFlowAnalyzer initialSequenceCfa, Collection<JRunAsync> nonInitialRunAsyncs) {
    List<List<JRunAsync>> fragmentRunAsyncLists =
        mergeRunAsyncs(logger, initialSequenceCfa, nonInitialRunAsyncs);

    List<Fragment> fragments = new ArrayList<Fragment>();
    for (List<JRunAsync> fragmentRunAsyncs : fragmentRunAsyncLists) {
      Fragment fragment = new Fragment(Fragment.Type.EXCLUSIVE);
      fragment.addRunAsyncs(fragmentRunAsyncs);
      fragments.add(fragment);
    }
    return fragments;
  }

  private List<List<JRunAsync>> mergeRunAsyncs(TreeLogger logger,
      ControlFlowAnalyzer initialSequenceCfa, Collection<JRunAsync> runAsyncs) {
    LiveAtomsByRunAsyncSets liveAtomsByRunAsyncSets = new LiveAtomsByRunAsyncSets(logger);

    // Compute the under-approximate liveset map.
    liveAtomsByRunAsyncSets.recordLiveSubsetsAndEstimateTheirSizes(initialSequenceCfa, runAsyncs);

    // Merge by similarity.
    int mergeCount = liveAtomsByRunAsyncSets.getRunAsyncCount() - targetNumberOfFragments;
    List<List<JRunAsync>> fragmentRunAsyncLists =
        liveAtomsByRunAsyncSets.mergeSimilarPairs(mergeCount);

    // Merge by size if specified
    if (minSize > 0) {
      liveAtomsByRunAsyncSets.mergeSmallFragments(fragmentRunAsyncLists, minSize);
    }

    return fragmentRunAsyncLists;
  }
}
