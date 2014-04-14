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
package com.google.gwt.dev.jjs.impl.gflow;

/**
 * Transformation function defines an optional transformation of a graph node
 * based on node assumptions.
 *
 * @param <N> graph node type.
 * @param <E> graph edge type.
 * @param <T> graph transformer type.
 * @param <G> graph type.
 * @param <A> assumption type.
 *
 */
public interface TransformationFunction<N, E, T, G extends Graph<N, E, T>,
    A extends Assumption<A>> {
  /**
   * Gets node transformation for a given node.
   * @return node transformation or <code>null</code> if no transformation is
   * necessary.
   */
  Transformation<T, G> transform(N node, G graph,
      AssumptionMap<E, A> assumptionMap);

  /**
   * Transformation defines new subgraph replacement for a node, and
   * transformation which will be applied during the last (actualizing) step
   * of analysis.
   *
   * @param <T> graph transformer type
   * @param <G> graph type
   */
  interface Transformation<T, G extends Graph<?, ?, T>> {
    G getNewSubgraph();
    T getGraphTransformer();
  }
}
