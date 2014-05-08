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
package com.google.gwt.dev.util.transitiveclosure;

import com.google.gwt.thirdparty.guava.common.collect.LinkedHashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * A transitive closure solver that lazily calculates reachability for a requested node by
 * recursively walking the tree and then performing a fixup pass on nodes that were seen to be
 * participating in cycles.
 *
 * @param <T> the graph node type.
 */
public class TransitiveClosureSolver<T> {

  /**
   * Builds the reachability for a particular node.
   * <p>
   * Building requires a recursive walk and following fixup of partial views caused by cycles.
   * Temporary state is contained here to facilitate fixup.
   */
  private class ReachabilityBuilder {

    private boolean processed;
    private final Set<T> reachableNodes = Sets.newHashSet();
    private int sawPartialViewCount;
    private final Queue<ReachabilityBuilder> showedPartialViewToBuilders = Lists.newLinkedList();

    private void build(T node) {
      Collection<T> childNodes = childNodesByNode.get(node);
      reachableNodes.add(node);
      reachableNodes.addAll(childNodes);

      for (T childNode : childNodes) {
        ReachabilityBuilder childBuilder = getReachabilityBuilder(childNode);

        // If we're looking at the contents of builder before it is done with its own build. (Can
        // only happen when traversing cycles).
        if (!childBuilder.processed) {
          // Then remember that this partial view occurred so that it can be fixed up later.
          sawPartialViewCount++;
          childBuilder.showedPartialViewToBuilders.add(this);
        }

        reachableNodes.addAll(childBuilder.reachableNodes);
      }

      processed = true;
      maybeFixPartialViewers();
    }

    private boolean hasOthersToFix() {
      return !showedPartialViewToBuilders.isEmpty();
    }

    private void maybeFixPartialViewers() {
      if (!resultIsAccurate()) {
        return;
      }

      while (hasOthersToFix()) {
        ReachabilityBuilder builderWithPartialView = showedPartialViewToBuilders.remove();

        // Fix the partial view of me that had been previously shown to this other builder.
        builderWithPartialView.reachableNodes.addAll(reachableNodes);
        builderWithPartialView.sawPartialViewCount--;

        // This other builder might now be complete, give it a chance to cascade to fix up any other
        // builders that saw a partial view of it.
        builderWithPartialView.maybeFixPartialViewers();
      }
    }

    private boolean resultIsAccurate() {
      return sawPartialViewCount == 0;
    }
  }

  private final LinkedHashMultimap<T, T> childNodesByNode = LinkedHashMultimap.create();
  private final Map<T, ReachabilityBuilder> reachabilityBuildersByNode = Maps.newHashMap();

  public void addConnection(T node, T childNode) {
    childNodesByNode.put(node, childNode);
  }

  public Set<T> getReachableNodes(T node) {
    return getReachabilityBuilder(node).reachableNodes;
  }

  private ReachabilityBuilder getReachabilityBuilder(T node) {
    if (!reachabilityBuildersByNode.containsKey(node)) {
      ReachabilityBuilder reachabilityBuilder = new ReachabilityBuilder();
      // Store the builder (even before it's been expanded) to short-circuit circular dependencies.
      reachabilityBuildersByNode.put(node, reachabilityBuilder);
      reachabilityBuilder.build(node);
      return reachabilityBuilder;
    }
    return reachabilityBuildersByNode.get(node);
  }
}
