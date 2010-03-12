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
import com.google.gwt.dev.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * A solver to solve all kinds of analyses defined in the package.
 * Uses iterative worklist algorithm. 
 * 
 * Solver might be forward or backwards working. Both directions will always 
 * produce a valid fixed point, which depends on direction. As a rule, 
 * forward analysis benefits from forward direction, backwards - from the 
 * opposite.
 *
 * @param <N> graph node type.
 * @param <E> graph edge type.
 * @param <T> graph transformer type.
 * @param <G> graph type.
 * @param <A> assumption type.
 */
public class AnalysisSolver<N, E, T, G extends Graph<N, E, T>, 
                            A extends Assumption<A>> {
  /**
   * Adapter from IntegratedFlowFunction to FlowFunction. If integrated function
   * decides to perform transformation, replacement graph is recursively 
   * analyzed and result return without actually performing transformation,  
   */
  private final class IntegratedFlowFunctionAdapter
      implements FlowFunction<N, E, G, A> {
    private IntegratedFlowFunction<N, E, T, G, A> flowFunction;

    private IntegratedFlowFunctionAdapter(
        IntegratedAnalysis<N, E, T, G, A> analysis) {
      flowFunction = analysis.getIntegratedFlowFunction();
    }

    public void interpret(final N node, G graph, 
        final AssumptionMap<E, A> assumptionMap) {
      final boolean[] mapWasModified = new boolean[1];
      Transformation<T, G> transformation = flowFunction.interpretOrReplace(
          node, graph, new AssumptionMap<E, A>() {
            public A getAssumption(E edge) {
              return assumptionMap.getAssumption(edge);
            }

            public void setAssumption(E edge, A assumption) {
              mapWasModified[0] = true;
              assumptionMap.setAssumption(edge, assumption);
            }
          });

      if (transformation == null) {
        return;
      }
      
      Preconditions.checkArgument(!mapWasModified[0]);
      
      final G newSubgraph = transformation.getNewSubgraph();

      if (debug) {
        System.err.println("Applying transformation: " + transformation);
        System.err.println("Replacing");
        System.err.println(node);
        System.err.println("With graph:");
        System.err.println(newSubgraph);
      }

      final List<E> inEdges = graph.getInEdges(node);
      final List<E> outEdges = graph.getOutEdges(node);

      Preconditions.checkArgument(newSubgraph.getGraphInEdges().size() == 
        inEdges.size());

      Preconditions.checkArgument(newSubgraph.getGraphOutEdges().size() == 
        outEdges.size());

      iterate(newSubgraph,
          new IntegratedAnalysis<N, E, T, G, A>() {
            public IntegratedFlowFunction<N, E, T, G, A> 
            getIntegratedFlowFunction() {
              return flowFunction;
            }

            public void setInitialGraphAssumptions(G graph,
                AssumptionMap<E, A> newAssumptionMap) {
              for (int i = 0; i < inEdges.size(); ++i) {
                newAssumptionMap.setAssumption(newSubgraph.getGraphInEdges().get(i),
                    assumptionMap.getAssumption(inEdges.get(i))); 
              }
              
              for (int i = 0; i < outEdges.size(); ++i) {
                newAssumptionMap.setAssumption(newSubgraph.getGraphOutEdges().get(i),
                    assumptionMap.getAssumption(outEdges.get(i))); 
              }
            }

          });

      for (int i = 0; i < inEdges.size(); ++i) {
        assumptionMap.setAssumption(inEdges.get(i), 
            getEdgeAssumption(newSubgraph, newSubgraph.getGraphInEdges().get(i)));
      }
      
      for (int i = 0; i < outEdges.size(); ++i) {
        assumptionMap.setAssumption(outEdges.get(i), 
            getEdgeAssumption(newSubgraph, newSubgraph.getGraphOutEdges().get(i)));
      }
    }
  }
  
  public static boolean debug = false;

  /**
   * Solve a non-integrated analysis.
   * 
   * @param <N> graph node type.
   * @param <E> graph edge type.
   * @param <T> graph transformer type.
   * @param <G> graph type.
   * @param <A> assumption type.
   */
  public static <N, E, T, G extends Graph<N, E, T>, A extends Assumption<A>> 
  Map<E, A> solve(G g, Analysis<N, E, G, A> analysis, boolean forward) {
    return new AnalysisSolver<N, E, T, G, A>(forward).solve(g, analysis);
  }

  /**
   * Solve a integrated analysis.
   * 
   * @param <N> graph node type.
   * @param <E> graph edge type.
   * @param <T> graph transformer type.
   * @param <G> graph type.
   * @param <A> assumption type.
   */
  public static <N, E, T, G extends Graph<N, E, T>, A extends Assumption<A>> 
  boolean solveIntegrated(G g, IntegratedAnalysis<N, E, T, G, A> analysis, 
      boolean forward) {
    return new AnalysisSolver<N, E, T, G, A>(forward).solveIntegrated(g, 
        analysis);
  }
  
  /**
   * If <code>true</code>, then we are moving forward. Moving backwards 
   * otherwise.
   */
  private final boolean forward;

  /**
   * @param forward <code>true</code> if solvers moves forward.
   */
  private AnalysisSolver(boolean forward) {
    this.forward = forward;
  }
  
  /**
   * Apply all transformations based on a found fixed point.
   */
  private boolean actualize(G graph, 
      final IntegratedAnalysis<N, E, T, G, A> analysis) {
    TransformationFunction<N, E, T, G, A> function = 
      new TransformationFunction<N, E, T, G, A>() {
      public Transformation<T, G> transform(final N node, final G graph,
          AssumptionMap<E, A> assumptionMap) {
        final boolean[] didAssumptionChange = new boolean[1];
        Transformation<T, G> transformation = analysis.getIntegratedFlowFunction().interpretOrReplace(
              node, graph, new AssumptionMap<E, A>() {
                public A getAssumption(E edge) {
                  Preconditions.checkArgument(graph.getStart(edge) == node
                      || graph.getEnd(edge) == node);
                  return getEdgeAssumption(graph, edge);
                }

                public void setAssumption(E edge, A assumption) {
                  Preconditions.checkArgument(graph.getStart(edge) == node
                      || graph.getEnd(edge) == node);
                 didAssumptionChange[0] = true;
                }
              });
        Preconditions.checkArgument(transformation == null ||
            !didAssumptionChange[0]);
        return transformation;
      }
    };
    return applyTransformation(graph, function);
  }

  private boolean applyTransformation(final G graph, 
      TransformationFunction<N, E, T, G, A> transformationFunction) {
    boolean didChange = false;

    for (final N node : graph.getNodes()) {
      Transformation<T, G> transformation = transformationFunction.transform(
          node, graph, new AssumptionMap<E, A>() {
            public A getAssumption(E edge) {
              Preconditions.checkArgument(graph.getStart(edge) == node
                  || graph.getEnd(edge) == node);
              return getEdgeAssumption(graph, edge);
            }

            public void setAssumption(E edge, A assumption) {
              throw new IllegalStateException(
                  "Transformations should not change assumptions");
            }
          });
      if (transformation != null) {
        T actualizer = transformation.getGraphTransformer();
        Preconditions.checkNotNull(actualizer, "Null actualizer from: %s",
            transformationFunction);
        didChange = graph.transform(node, actualizer) || didChange;
      }
    }

    return didChange;
  }

  private LinkedHashSet<N> buildInitialWorklist(G g) {
    ArrayList<N> nodes = new ArrayList<N>(g.getNodes());
    LinkedHashSet<N> worklist = new LinkedHashSet<N>(nodes.size());
    if (!forward) {
      Collections.reverse(nodes);
    } 
    worklist.addAll(nodes);
    return worklist;
  }

  @SuppressWarnings("unchecked")
  private A getEdgeAssumption(G graph, E edge) {
    return (A) graph.getEdgeData(edge);
  }

  private void initGraphAssumptions(Analysis<N, E, G, A> analysis, final G graph) {
    analysis.setInitialGraphAssumptions(graph, new AssumptionMap<E, A>() {
      public A getAssumption(E edge) {
        return getEdgeAssumption(graph, edge);
      }

      public void setAssumption(E edge, A assumption) {
        setEdgeAssumption(graph, edge, assumption);
      }
    });
  }
  
  /**
   * Find a fixed point of integrated analysis by wrapping it with 
   * IntegratedFlowFunctionAdapter and calling
   * {@link #solveImpl(Graph, Analysis)}. 
   */
  private void iterate(G graph,
      final IntegratedAnalysis<N, E, T, G, A> integratedAnalysis) {
    if (debug) {
      System.err.println("-----------------------------------------");
      System.err.println("Iterate started on:");
      System.err.println(graph);
      System.err.println("-----------------------------------------");
    }
    final IntegratedFlowFunctionAdapter adapter = 
      new IntegratedFlowFunctionAdapter(integratedAnalysis);

    Analysis<N, E, G, A> analysis = new Analysis<N, E, G, A>() {
      public FlowFunction<N, E, G, A> getFlowFunction() {
        return adapter;
      }

      public void setInitialGraphAssumptions(G graph,
          AssumptionMap<E, A> assumptionMap) {
        integratedAnalysis.setInitialGraphAssumptions(graph, assumptionMap);
      }
    };
    
    solveImpl(graph, analysis);
  }

  private void resetEdgeData(G graph) {
    for (N node : graph.getNodes()) {
      for (E e : graph.getInEdges(node)) {
        graph.setEdgeData(e, null);
      }
      for (E e : graph.getOutEdges(node)) {
        graph.setEdgeData(e, null);
      }
    }
    for (E e : graph.getGraphOutEdges()) {
      graph.setEdgeData(e, null);
    }
    for (E e : graph.getGraphInEdges()) {
      graph.setEdgeData(e, null);
    }
  }

  private void setEdgeAssumption(G graph, E edge, A assumption) {
    graph.setEdgeData(edge, assumption);
  }

  /**
   * Solve a non-integrated analysis.
   */
  private Map<E, A> solve(G g, Analysis<N, E, G, A> analysis) {
    solveImpl(g, analysis);

    Map<E, A> result = new HashMap<E, A>();
    
    for (N n : g.getNodes()) {
      for (E e : g.getInEdges(n)) {
        result.put(e, getEdgeAssumption(g, e));
      }
      for (E e : g.getOutEdges(n)) {
        result.put(e, getEdgeAssumption(g, e));
      }
    }
    for (E e : g.getGraphInEdges()) {
      result.put(e, getEdgeAssumption(g, e));
    }
    for (E e : g.getGraphOutEdges()) {
      result.put(e, getEdgeAssumption(g, e));
    }

    return result;
  }

  /**
   * Solve a non-integrated analysis.
   */
  private void solveImpl(final G graph, Analysis<N, E, G, A> analysis) {
    FlowFunction<N, E, G, A> flowFunction = analysis.getFlowFunction();

    final LinkedHashSet<N> worklist = buildInitialWorklist(graph);
    resetEdgeData(graph);
    initGraphAssumptions(analysis, graph);

    while (!worklist.isEmpty()) {
      Iterator<N> iterator = worklist.iterator();
      final N node = iterator.next();
      iterator.remove();

      flowFunction.interpret(node, graph, new AssumptionMap<E, A>() {
        public A getAssumption(E edge) {
          Preconditions.checkArgument(graph.getStart(edge) == node
              || graph.getEnd(edge) == node);
          return getEdgeAssumption(graph, edge);
        }

        public void setAssumption(E edge, A assumption) {
          N start = graph.getStart(edge);
          N end = graph.getEnd(edge);
          Preconditions.checkArgument(start == node || end == node);

          if (!AssumptionUtil.equals(getEdgeAssumption(graph, edge), assumption)) {
            setEdgeAssumption(graph, edge, assumption);

            if (start == node) {
              if (end != null) {
                worklist.add(end);
              }
            } else if (end == node) {
              if (start != null) {
                worklist.add(start);
              }
            } else {
              throw new IllegalStateException();
            }
          }
        }
      });
    }
  }

  /**
   * Solve an integrated analysis.
   * 
   * Finds a fixed point by using an IntegratedFlowFunctionAdapter and 
   * recursing into {@link #solve(Graph, Analysis)}. Applies analysis 
   * transformations based on the found fixed point.
   */
  private boolean solveIntegrated(G g, IntegratedAnalysis<N, E, T, G, A> analysis) {
    iterate(g, analysis);
    return actualize(g, analysis);
  }
}
