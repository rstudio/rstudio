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

import com.google.gwt.dev.jjs.impl.gflow.TransformationFunction.Transformation;
import com.google.gwt.thirdparty.guava.common.base.Preconditions;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Integrated analysis, which combines several other integrated analyses into
 * one. It does so be defining combined assumption, which is vector of original
 * assumptions, and applies each analysis to its own component.
 *
 * If any analysis decides to rewrite the node, combined analysis returns
 * produced transformation. If more than one analyses decide to transform, the
 * first one wins.
 *
 * @param <N> graph node type.
 * @param <E> graph edge type.
 * @param <T> graph transformer type.
 * @param <G> graph type.
 *
 */
public class CombinedIntegratedAnalysis<N, E, T, G extends Graph<N, E, T>>
    implements
    IntegratedAnalysis<N, E, T, G, CombinedIntegratedAnalysis.CombinedAssumption> {

  /**
   * Combined assumption which holds vector of original assumptions.
   */
  public static class CombinedAssumption implements
      Assumption<CombinedAssumption> {

    private static class CopyOnWrite {
      private final int size;
      private CombinedAssumption assumption;
      private boolean copied = false;

      private CopyOnWrite(CombinedAssumption assumption, int size) {
        this.assumption = assumption;
        this.size = size;
      }

      public boolean isCopied() {
        return copied;
      }

      public void set(int slice, Assumption<?> assumption) {
        copyIfNeeded();
        this.assumption.set(slice, assumption);
      }

      public CombinedAssumption unwrap() {
        return assumption;
      }

      private void copyIfNeeded() {
        if (!copied) {
          copied = true;
          if (assumption == null) {
            assumption = new CombinedAssumption(size);
          } else {
            assumption = new CombinedAssumption(assumption);
          }
        }
      }
    }

    /**
     * Individual assumptions vector.
     */
    private final List<Assumption<?>> assumptions;

    public CombinedAssumption(CombinedAssumption assumption) {
      this.assumptions = new ArrayList<Assumption<?>>(assumption.assumptions);
    }

    public CombinedAssumption(int size) {
      this.assumptions = new ArrayList<Assumption<?>>(size);
      for (int i = 0; i < size; ++i) {
        this.assumptions.add(null);
      }
    }

    public CombinedAssumption(List<Assumption<?>> assumptions) {
      this.assumptions = assumptions;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }

      CombinedAssumption other = (CombinedAssumption) obj;

      // We do not implement equals in our zipped lists. Do it here.
      if (other.assumptions.size() != assumptions.size()) {
        return false;
      }

      for (int i = 0; i < assumptions.size(); ++i) {
        Assumption<?> a1 = assumptions.get(i);
        Assumption<?> a2 = other.assumptions.get(i);
        if (a1 == null) {
          if (a1 != a2) {
            return false;
          }
        } else {
          if (a2 == null) {
            return false;
          }
          if (!a1.equals(a2)) {
            return false;
          }
        }
      }

      return true;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
          + ((assumptions == null) ? 0 : assumptions.hashCode());
      return result;
    }

    /**
     * Joins combined assumption by joining all individual components.
     */
    @Override
    @SuppressWarnings("unchecked")
    public CombinedAssumption join(CombinedAssumption value) {
      if (value == null) {
        return this;
      }
      Preconditions.checkArgument(value.assumptions.size() ==
        assumptions.size());

      List<Assumption<?>> newAssumptions = new ArrayList<Assumption<?>>();
      for (int i = 0; i < assumptions.size(); ++i) {
        Assumption a1 = assumptions.get(i);
        Assumption a2 = value.assumptions.get(i);
        newAssumptions.add(AssumptionUtil.join(a1, a2));
      }

      return new CombinedAssumption(newAssumptions);
    }

    @Override
    public String toString() {
      return assumptions.toString();
    }

    /**
     * Gets nth assumption component.
     */
    private Assumption<?> get(int n) {
      return assumptions.get(n);
    }

    private void set(int slice, Assumption<?> assumption) {
      assumptions.set(slice, assumption);
    }
  }

  /**
   * Combined integrated flow function.
   */
  private final class CombinedIntegratedFlowFunction implements
      IntegratedFlowFunction<N, E, T, G, CombinedAssumption> {
    @Override
    @SuppressWarnings("unchecked")
    public Transformation interpretOrReplace(final N node, final G graph,
        final AssumptionMap<E, CombinedAssumption> assumptionMap) {

      final Map<E, CombinedAssumption.CopyOnWrite> newAssumptions = new IdentityHashMap<E, CombinedAssumption.CopyOnWrite>();

      final int size = functions.size();
      for (int i = 0; i < size; ++i) {
        final int slice = i;
        IntegratedFlowFunction function = functions.get(i);

        Transformation transformation =
          function.interpretOrReplace(node, graph,
              new AssumptionMap() {
                @Override
                public Assumption getAssumption(Object edge) {
                  CombinedAssumption combinedAssumption = assumptionMap.getAssumption((E) edge);
                  if (combinedAssumption == null) {
                    return null;
                  }
                  return combinedAssumption.get(slice);
                }

                @Override
                public void setAssumption(Object edge, Assumption assumption) {
                  CombinedAssumption.CopyOnWrite newAssumption = newAssumptions.get(edge);
                  if (newAssumption == null) {
                    newAssumption = new CombinedAssumption.CopyOnWrite(assumptionMap.getAssumption((E) edge), size);
                    newAssumptions.put((E) edge, newAssumption);
                  }
                  newAssumption.set(slice, assumption);
                }

                @Override
                public String toString() {
                  return AssumptionUtil.toString(graph.getInEdges(node),
                      graph.getOutEdges(node), this);
                }
              });

        if (transformation != null) {
          return transformation;
        }
      }

      for (E e : newAssumptions.keySet()) {
        CombinedAssumption.CopyOnWrite newAssumption = newAssumptions.get(e);
        if (newAssumption.isCopied()) {
          assumptionMap.setAssumption(e, newAssumption.unwrap());
        }
      }

      return null;
    }
  }

  /**
   * Factory method.
   */
  public static <N, E, T, G extends Graph<N, E, T>>
  CombinedIntegratedAnalysis<N, E, T, G> createAnalysis() {
    return new CombinedIntegratedAnalysis<N, E, T, G>();
  }
  /**
   * Individual analyses.
   */
  List<IntegratedAnalysis<N, E, T, G, ?>> analyses =
    new ArrayList<IntegratedAnalysis<N, E, T, G, ?>>();

  /**
   * Their flow functions.
   */
  List<IntegratedFlowFunction<N, E, T, G, ?>> functions =
    new ArrayList<IntegratedFlowFunction<N, E, T, G, ?>>();

  /**
   * Adds analysis to the combined one.
   */
  public void addAnalysis(IntegratedAnalysis<N, E, T, G, ?> analysis) {
    analyses.add(analysis);
    functions.add(analysis.getIntegratedFlowFunction());
  }

  @Override
  public IntegratedFlowFunction<N, E, T, G, CombinedAssumption>
  getIntegratedFlowFunction() {
    return new CombinedIntegratedFlowFunction();
  }

  @Override
  @SuppressWarnings("unchecked")
  public void setInitialGraphAssumptions(G graph,
      final AssumptionMap<E, CombinedAssumption> assumptionMap) {
    for (int i = 0; i < functions.size(); ++i) {
      final int slice = i;
      IntegratedAnalysis<N, E, T, G, ?> analysis = analyses.get(slice);
      analysis.setInitialGraphAssumptions(graph, new AssumptionMap() {
        @Override
        public Assumption getAssumption(Object edge) {
          throw new UnsupportedOperationException();
        }

        @Override
        public void setAssumption(Object edge,Assumption assumption) {
          CombinedAssumption combinedAssumption = assumptionMap.getAssumption((E) edge);
          if (combinedAssumption == null) {
            combinedAssumption = new CombinedAssumption(functions.size());
            combinedAssumption.set(slice, assumption);
            assumptionMap.setAssumption((E) edge, combinedAssumption);
          } else {
            combinedAssumption.set(slice, assumption);
          }
        }
      });
    }
  }
}
