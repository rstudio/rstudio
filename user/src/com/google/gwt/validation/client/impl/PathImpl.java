/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.validation.client.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.validation.Path;

/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 * <p>
 * An immutable GWT safe implementation of {@link Path}.
 */
public final class PathImpl implements Path, Serializable {

  private static final long serialVersionUID = 1L;

  private final List<Node> nodes = new ArrayList<Node>();

  /**
   * Creates a new path containing only the root (<code>null</code>)
   * {@link Node}.
   */
  public PathImpl() {
    nodes.add(NodeImpl.ROOT_NODE);
  }

  private PathImpl(PathImpl originalPath, Node node) {
    if (!originalPath.isRoot()) {
      nodes.addAll(originalPath.nodes);
    }
    nodes.add(node);
  }

  /**
   * Create a new path with a node named <code>name</code> appended to the
   * existing path.
   *
   * @param name
   * @return The new path with appended node.
   */
  public PathImpl append(String name) {
    return new PathImpl(this, NodeImpl.createNode(name));
  }

  /**
   * Create a new path with an indexed node named <code>name</code> appended to
   * the existing path.
   *
   * @param name
   * @param key
   * @return The new path with appended node.
   */
  public PathImpl appendIndex(String name, int index) {
    return new PathImpl(this, NodeImpl.createIndexedNode(name, index));
  }

  /**
   * Create a new path with an iterable node named <code>name</code> appended to
   * the existing path.
   *
   * @param name
   * @return The new path with appended node.
   */
  public PathImpl appendIterable(String name) {
    return new PathImpl(this, NodeImpl.createIterableNode(name));
  }

  /**
   * Create a new path with a keyed node named <code>name</code> appended to the
   * existing path.
   *
   * @param name
   * @param key
   * @return The new path with appended node.
   */
  public PathImpl appendKey(String name, Object key) {
    return new PathImpl(this, NodeImpl.createKeyedNode(name, key));
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PathImpl)) {
      return false;
    }
    PathImpl that = (PathImpl) obj;
    return this.nodes.equals(that.nodes);
  }

  @Override
  public int hashCode() {
    return nodes.hashCode();
  }

  public Iterator<Node> iterator() {
    return nodes.iterator();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Node n : nodes) {
      if (sb.length() > 0) {
        sb.append('.');
      }
      sb.append(n);
    }
    return sb.toString();
  }

  private boolean isRoot() {
    return nodes.size() == 1 && nodes.get(0) == NodeImpl.ROOT_NODE;
  }
}
