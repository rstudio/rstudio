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

/**
 * Edge in CFG graph. Edge can be annotated by its role when there are several
 * edges coming from the node, to be able to reason about them separately (e.g.
 * it's important which edge is then/else branch in conditional node).
 */
public class CfgEdge {
  Object data;
  // We do not add setStart/setEnd methods because we'd like to be sure
  // that no one except CfgNode changes these.
  CfgNode<?> end;

  CfgNode<?> start;
  private final String role;

  public CfgEdge() {
    this.role = null;
  }

  public CfgEdge(String role) {
    this.role = role;
  }

  /**
   * Get edge end node.
   */
  public CfgNode<?> getEnd() {
    return end;
  }

  /**
   * Get edge role.
   */
  public String getRole() {
    return role;
  }

  /**
   * Get edge start node.
   */
  public CfgNode<?> getStart() {
    return start;
  }

  @Override
  public String toString() {
    return (start != null ? start.toDebugString() : "*") + "->" +
      (end != null ? end.toDebugString() : "*");
  }
}
