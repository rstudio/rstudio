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
package com.google.gwt.dev.jjs.impl.gflow.cfg;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Control flow graph printer. Prints all nodes and all their edges.
 */
public class CfgPrinter {
  private final Cfg graph;
  
  public CfgPrinter(Cfg graph) {
    this.graph = graph;
  }
  
  public String print() {
    StringBuffer result = new StringBuffer();
    List<CfgNode<?>> nodes = graph.getNodes();
    
    // Determine nodes which have edges incoming not from previous node.
    Set<CfgNode<?>> targetNodes = new HashSet<CfgNode<?>>();
    for (int i = 1; i < nodes.size(); ++i) {
      CfgNode<?> node = nodes.get(i);
      List<CfgEdge> inEdges = graph.getInEdges(node);
      for (CfgEdge inEdge : inEdges) {
        if (inEdge.getStart() != null && 
            inEdge.getStart() != nodes.get(i - 1)) {
          targetNodes.add(node);
        }
      }
    }
    
    Map<CfgNode<?>, String> labels = new HashMap<CfgNode<?>, String>();
    for (int i = 0, j = 1; i < nodes.size(); ++i) {
      if (targetNodes.contains(nodes.get(i))) {
        labels.put(nodes.get(i), String.valueOf(j));
        ++j;
      }
    }

    for (int i = 0; i < nodes.size(); ++i) {
      CfgNode<?> node = nodes.get(i);
      if (i != 0) {
        result.append("\n");
      }

      if (labels.containsKey(node)) {
        result.append(labels.get(node));
        result.append(": ");
      }
      result.append(node.toDebugString());

      {
        List<CfgEdge> out = graph.getOutEdges(node);
        if (!out.isEmpty()) {
          result.append(" -> [");
          for (int j = 0; j < out.size(); ++j) {
            if (j > 0) {
              result.append(", ");
            }
            CfgEdge edge = out.get(j);
            if (edge.getRole() != null) {
              result.append(edge.getRole());
              result.append("=");
            }
            if (i + 1 < nodes.size() && edge.getEnd() != nodes.get(i + 1)) {
              result.append(labels.get(edge.getEnd()));
            } else {
              result.append("*");
            }
            
            appendEdgeInfo(result, edge);
          }
          result.append("]");
        }
      }
    }

    return result.toString();
  }

  /**
   * Template method to append arbitrary edge information.
   */
  protected void appendEdgeInfo(@SuppressWarnings("unused") StringBuffer result, 
      @SuppressWarnings("unused") CfgEdge edge) {
    // Overridden by ancestors.
  }
}
