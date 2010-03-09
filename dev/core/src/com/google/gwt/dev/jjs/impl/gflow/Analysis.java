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
 * A simple, non-transforming flow analysis.
 * 
 * @param <N> graph node type.
 * @param <E> edge type.
 * @param <G> graph type.
 * @param <A> assumption type.
 */
public interface Analysis<N, E, G extends Graph<N, E, ?>, 
                          A extends Assumption<A>> {
  /**
   * Gets analysis flow function. 
   */
  FlowFunction<N, E, G, A> getFlowFunction();
  
  /**
   * Gets assumptions for graph to start approximation from.
   */
  void setInitialGraphAssumptions(G graph, AssumptionMap<E, A> assumptionMap);
}
