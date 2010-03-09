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
 * A flow function receives node assumptions and transforms them according to
 * node semantics. Typical flow functions update either outgoing assumptions 
 * (forward flow)   or incoming assumptions (backward flow) but not both.
 *  
 * @param <N> graph node type.
 * @param <E> edge type.
 * @param <G> graph type.
 * @param <A> analysis assumption type.
 */
public interface FlowFunction<N, E, G extends Graph<N, E, ?>, 
    A extends Assumption<A>> {
  /**
   * Interpret node by computing new node assumptions from current ones. 
   */
  void interpret(N node, G g, 
      AssumptionMap<E, A> assumptionMAp);
}
