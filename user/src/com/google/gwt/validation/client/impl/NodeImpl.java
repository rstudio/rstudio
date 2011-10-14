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

import javax.validation.Path.Node;

/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 * <p>
 * An immutable GWT safe implementation of {@link Node}.
 */
final class NodeImpl implements Node, Serializable {

  private static final long serialVersionUID = 1L;
  public static final Node ROOT_NODE = new NodeImpl(null, null, null, false);

  static Node createIndexedNode(String name, Integer index) {
    return new NodeImpl(name, null, index, true);
  }

  static Node createIterableNode(String name) {
    return new NodeImpl(name, null, null, true);
  }

  static Node createKeyedNode(String name, Object key) {
    return new NodeImpl(name, key, null, true);
  }

  static Node createNode(String name) {
    return new NodeImpl(name, null, null, false);
  }

  private final boolean isInIterable;
  private final String name;
  private final Integer index;

  private final Object key;

  private NodeImpl(String name, Object key, Integer index, boolean iterable) {
    this.name = name;
    this.key = key;
    this.index = index;
    this.isInIterable = iterable;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof NodeImpl)) {
      return false;
    }
    NodeImpl that = (NodeImpl) obj;
    return (this.name == null ? that.name == null : this.name.equals(that.name))
        && (this.index == null ? that.index == null : this.index
            .equals(that.index))
        && (this.key == null ? that.key == null : this.key.equals(that.key))
        && this.isInIterable == that.isInIterable;
  }

  public Integer getIndex() {
    return index;
  }

  public Object getKey() {
    return key;
  }

  public String getName() {
    return name;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((index == null) ? 0 : index.hashCode());
    result = prime * result + ((key == null) ? 0 : key.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + (isInIterable ? 0 : 1);
    return result;
  }

  public boolean isInIterable() {
    return isInIterable;
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    if (name != null) {
      sb.append(name);
    }
    if (isInIterable()) {
      sb.append('[');
      if (key != null) {
        sb.append(key);
      } else if (index != null) {
        sb.append(index);
      }
      sb.append(']');
    }
    return sb.toString();
  }
}
