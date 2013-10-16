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
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JRunAsync;
import com.google.gwt.dev.jjs.impl.ControlFlowAnalyzer;
import com.google.gwt.thirdparty.guava.common.collect.HashMultiset;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multiset;

import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Maps an atom to a set of runAsyncs that can be live (NOT necessary exclusively) when that
 * runAsync is activated. The runAsyncs are represented by a bit set where S[i] is set if the atom
 * needs to be live when runAsync i is live.<br />
 * 
 * In this class "payload size" is the size of the atoms that will be loaded (beyond the set of
 * atoms already loaded in the initial sequence) as part of a particular exclusive fragment.
 * 
 * "Subset" refers to an arbitrary combination of runAsync ids.
 */
class LiveAtomsByRunAsyncSets {

  LiveAtomsByRunAsyncSets(TreeLogger logger) {
    this.logger = logger;
  }

  private static class SubsetWithSize implements Comparable<SubsetWithSize> {
    private final int size;
    private final BitSet subset;

    public SubsetWithSize(BitSet subset, int size) {
      this.subset = subset;
      this.size = size;
    }

    @Override
    public int compareTo(SubsetWithSize o) {
      return Integer.valueOf(o.size).compareTo(Integer.valueOf(size));
    }
  }

  private static final int AVERAGE_METHOD_SIZE = 40;
  private static final int AVERAGE_NAME_SIZE = 2;
  private static final int FUNCTION_DEFINITION_CONSTANT_SIZE = "function".length() + "()".length();

  private static BitSet computeComplement(BitSet bitSet, int count) {
    BitSet notMergedSubset = (BitSet) bitSet.clone();
    notMergedSubset.flip(0, count);
    return notMergedSubset;
  }

  private static BitSet computeIntersection(BitSet thisSet, BitSet thatSet) {
    BitSet intersectionBitSet = (BitSet) thisSet.clone();
    intersectionBitSet.and(thatSet);
    return intersectionBitSet;
  }

  private static int getSizeEstimate(JDeclaredType type) {
    int defineSeedSize = AVERAGE_NAME_SIZE + 50;
    int methodsSize = (3 + AVERAGE_NAME_SIZE) * type.getMethods().size();
    return defineSeedSize + methodsSize;
  }

  private static int getSizeEstimate(JField field) {
    return AVERAGE_NAME_SIZE;
  }

  private static int getSizeEstimate(JMethod method) {
    int methodSize = FUNCTION_DEFINITION_CONSTANT_SIZE + (AVERAGE_NAME_SIZE + /* , */1
        + AVERAGE_METHOD_SIZE) * method.getParams().size();
    return methodSize;
  }

  private static int getSizeEstimate(Object obj) {
    if (obj instanceof JField) {
      return getSizeEstimate((JField) obj);
    } else if (obj instanceof JMethod) {
      return getSizeEstimate((JMethod) obj);
    } else if (obj instanceof String) {
      return getSizeEstimate((String) obj);
    } else if (obj instanceof JDeclaredType) {
      return getSizeEstimate((JDeclaredType) obj);
    }
    throw new UnsupportedOperationException(
        "estimateSize unsupported for type " + obj.getClass().getName());
  }

  private static int getSizeEstimate(String string) {
    return string.length();
  }

  private static boolean isSubset(BitSet smallerSet, BitSet biggerSet) {
    return computeIntersection(smallerSet, biggerSet).equals(smallerSet);
  }

  private final Map<JRunAsync, Integer> idForRunAsync = Maps.newHashMap();
  private Map<JField, BitSet> liveSubsetForField = Maps.newHashMap();
  private Map<JMethod, BitSet> liveSubsetForMethod = Maps.newHashMap();
  private Map<String, BitSet> liveSubsetForString = Maps.newHashMap();
  private Map<JDeclaredType, BitSet> liveSubsetForType = Maps.newHashMap();
  private int nextRunAsyncId = 0;
  private final Multiset<BitSet> payloadSizeBySubset = HashMultiset.create();
  private final Map<Integer, JRunAsync> runAsyncForId = Maps.newHashMap();
  private final TreeLogger logger;
  private Collection<Collection<JRunAsync>> groupedRunAsyncs;

  public int getRunAsyncCount() {
    return idForRunAsync.size();
  }

  /**
   * Returns a list of lists of runAsyncs where {@code pairCount} lists are the result of greedily
   * accumulating the pairs of runAsyncs that together result in the largest payload, while
   * referencing each runAsync only once.
   */
  public Collection<Collection<JRunAsync>> mergeSimilarPairs(int pairCount) {
    Collection<Collection<JRunAsync>> fragmentRunAsyncLists = Lists.newArrayList();
    BitSet mergedSubset = new BitSet();
    PriorityQueue<SubsetWithSize> subsetsDescending = computeSubsetsDescending();

    // Exclude the groupings prespecified by the user.
    // TODO(rluble): we could do better and treat a prespecified grouping as one runAsync for
    // merging purpose. For now prespecified groupings are excluded from merge by similarity.
    // will be treated better in v3.
    for (Collection<JRunAsync> runAsyncGroup : groupedRunAsyncs) {
      if (runAsyncGroup.size() <=  1) {
        continue;
      }
      // Premptively add them to the output.
      fragmentRunAsyncLists.add(runAsyncGroup);
      // And mark them as used.
      mergedSubset.or(asBitSet(runAsyncGroup));
    }

    // While there are still combinations to examine
    while (fragmentRunAsyncLists.size() < pairCount && !subsetsDescending.isEmpty()) {
      BitSet largestSubset = subsetsDescending.poll().subset;

      // If one of the runAsyncs in the current set of runAsyncs has already been used.
      if (largestSubset.intersects(mergedSubset)) {
        // Then throw this set out.
        continue;
      }

      logger.log(TreeLogger.Type.DEBUG, "Merging " + largestSubset);

      fragmentRunAsyncLists.add(asRunAsyncList(largestSubset));

      // Remember that the runAsyncs have already been used.
      mergedSubset.or(largestSubset);
    }

    // Get the ones that are not merged
    BitSet notMergedSubset = computeComplement(mergedSubset, getRunAsyncCount());

    fragmentRunAsyncLists.addAll(CodeSplitters.getListOfLists(asRunAsyncList(notMergedSubset)));
    return fragmentRunAsyncLists;
  }

  /**
   * Modifies a provided list of lists of runAsyncs (each of which represents a fragment) by
   * combining sets of runAsyncs whose output size is too small.
   */
  public Collection<Collection<JRunAsync>> mergeSmallFragments(
      Collection<Collection<JRunAsync>> fragmentRunAsyncLists, int minSize) {
    List<JRunAsync> smallFragmentRunAsyncs = Lists.newArrayList();

    Iterator<Collection<JRunAsync>> fragmentIterator = fragmentRunAsyncLists.iterator();
    while (fragmentIterator.hasNext()) {
      Collection<JRunAsync> fragmentRunAsyncs = fragmentIterator.next();
      if (isFragmentTooSmall(fragmentRunAsyncs, minSize)) {
        smallFragmentRunAsyncs.addAll(fragmentRunAsyncs);
        fragmentIterator.remove();
      }
    }

    if (!smallFragmentRunAsyncs.isEmpty() && !isFragmentTooSmall(smallFragmentRunAsyncs, minSize)) {
      // Keep the fragment combining all the small fragments.
      fragmentRunAsyncLists.add(smallFragmentRunAsyncs);
      logger.log(TreeLogger.Type.DEBUG, "Merging small fragments " + smallFragmentRunAsyncs +
          " together ");
    } else if (!smallFragmentRunAsyncs.isEmpty()) {
      // This fragment was still small so it is not added to fragmentRunAsyncLists and consequently
      // its contents will end up in the leftovers.
      logger.log(TreeLogger.Type.DEBUG, "Merging small fragments " + smallFragmentRunAsyncs +
          " into leftovers ");
    }

    return fragmentRunAsyncLists;
  }

  /**
   * Iteratively expand the initial sequence CFA with each runAsync, record the resulting live
   * atoms and finally compute the payload size for each subset.
   */
  public void recordLiveSubsetsAndEstimateTheirSizes(
      ControlFlowAnalyzer initialSequenceCfa, Collection<Collection<JRunAsync>> groupedRunAsyncs) {
    this.groupedRunAsyncs = groupedRunAsyncs;
    for (Collection<JRunAsync> runAsyncGroup : groupedRunAsyncs) {
      for (JRunAsync runAsync : runAsyncGroup) {
        ControlFlowAnalyzer withRunAsyncCfa = new ControlFlowAnalyzer(initialSequenceCfa);
        withRunAsyncCfa.traverseFromRunAsync(runAsync);
        recordLiveSubset(withRunAsyncCfa, runAsync);
      }
    }
    accumulatePayloadSizes();
  }

  private <T> void accumulatePayloadSizes(Map<T, BitSet> liveSubsetsByAtom) {
    for (Map.Entry<T, BitSet> entry : liveSubsetsByAtom.entrySet()) {
      BitSet liveSubset = entry.getValue();
      T atom = entry.getKey();

      // TODO(rluble): Underestimates the size of fragments resulting of merging more than 2
      // fragments. With the current strategy it can only happen for the set of very small fragments
      // and that is OK.
      if (liveSubset.cardinality() > 2) {
        continue;
      }

      payloadSizeBySubset.add(liveSubset, getSizeEstimate(atom));
    }
  }

  private void addRunAsync(JRunAsync runAsync) {
    int runAsyncId = nextRunAsyncId++;
    idForRunAsync.put(runAsync, runAsyncId);
    runAsyncForId.put(runAsyncId, runAsync);
  }

  private BitSet asBitSet(Collection<JRunAsync> runAsyncs) {
    BitSet result = new BitSet();
    for (JRunAsync runAsync : runAsyncs) {
      result.set(getIdForRunAsync(runAsync));
    }
    return result;
  }

  private List<JRunAsync> asRunAsyncList(BitSet subset) {
    int runAsyncId = -1;
    List<JRunAsync> runAsyncs = Lists.newArrayList();
    while ((runAsyncId = subset.nextSetBit(runAsyncId + 1)) != -1) {
      runAsyncs.add(runAsyncForId.get(runAsyncId));
    }
    return runAsyncs;
  }

  private PriorityQueue<SubsetWithSize> computeSubsetsDescending() {
    PriorityQueue<SubsetWithSize> subsetsDescending = new PriorityQueue<SubsetWithSize>();
    for (BitSet subset : payloadSizeBySubset.elementSet()) {
      if (subset.cardinality() != 2) {
        continue;
      }

      subsetsDescending.add(new SubsetWithSize(subset, payloadSizeBySubset.count(subset)));
    }
    return subsetsDescending;
  }

  /**
   * Accumulate payload sizes.
   */
  private void accumulatePayloadSizes() {
    accumulatePayloadSizes(liveSubsetForField);
    accumulatePayloadSizes(liveSubsetForMethod);
    accumulatePayloadSizes(liveSubsetForString);
    accumulatePayloadSizes(liveSubsetForType);
  }

  private int getIdForRunAsync(JRunAsync runAsync) {
    return idForRunAsync.get(runAsync);
  }

  private boolean isFragmentTooSmall(Collection<JRunAsync> fragmentRunAsyncs, int minSize) {
    BitSet fragmentSubset = asBitSet(fragmentRunAsyncs);

    int size = 0;
    // TODO(rluble): This might be quite inefficient as it compare to all possible (non trivially
    // empty) subsets (bounded by the number of atoms). But is only run at most #runAsyncs times to
    // determine whether to merge small fragments.
    for (BitSet subset : payloadSizeBySubset.elementSet()) {
      if (isSubset(subset, fragmentSubset)) {

        size += payloadSizeBySubset.count(subset);
        if (size >= minSize) {
          return false;
        }
      }
    }
    return true;
  }

  private ControlFlowAnalyzer recordLiveSubset(ControlFlowAnalyzer cfa, JRunAsync runAsync) {
    addRunAsync(runAsync);
    for (JNode node : cfa.getLiveFieldsAndMethods()) {
      if (node instanceof JField) {
        setLive(liveSubsetForField, (JField) node, runAsync);
      }
      if (node instanceof JMethod) {
        setLive(liveSubsetForMethod, (JMethod) node, runAsync);
      }
    }
    for (JField field : cfa.getFieldsWritten()) {
      setLive(liveSubsetForField, field, runAsync);
    }
    for (String string : cfa.getLiveStrings()) {
      setLive(liveSubsetForString, string, runAsync);
    }
    for (JReferenceType type : cfa.getInstantiatedTypes()) {
      if (type instanceof JDeclaredType) {
        setLive(liveSubsetForType, (JDeclaredType) type, runAsync);
      }
    }
    return cfa;
  }

  private <T> boolean setLive(Map<T, BitSet> liveSubsetsByAtom, T atom, JRunAsync runAsync) {
    int runAsyncId = idForRunAsync.get(runAsync);
    BitSet liveSubset = liveSubsetsByAtom.get(atom);
    if (liveSubset == null) {
      liveSubset = new BitSet();
      liveSubset.set(runAsyncId);
      liveSubsetsByAtom.put(atom, liveSubset);
      return true;
    } else {
      if (liveSubset.get(runAsyncId)) {
        return false;
      } else {
        liveSubset.set(runAsyncId);
        return true;
      }
    }
  }
}
