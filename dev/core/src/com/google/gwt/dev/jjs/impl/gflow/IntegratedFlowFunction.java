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
 * Integrated flow function should either interpret the node, or produce
 * node transformation based on already computed assumptions.
 *
 * @param <N> graph node type.
 * @param <E> edge type.
 * @param <T> graph transformation type.
 * @param <G> graph type.
 * @param <A> assumption type.
 *
 */
public interface IntegratedFlowFunction<N, E, T, G extends Graph<N, E, T>,
    A extends Assumption<A>> {
  /**
   * Either interpret a node by computing new assumptions, or produce
   * node transformation.
   */
  TransformationFunction.Transformation<T, G>
  interpretOrReplace(N node, G graph, AssumptionMap<E, A> assumptionMap);
}
