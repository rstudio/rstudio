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
 * An immutable GWT safe implementation of {@link Node}
 */
class NodeImpl implements Node, Serializable {

  private static final long serialVersionUID = 1L;
  public static final Node ROOT_NODE = new NodeImpl(null);

  private final String name;
  private final Integer index;
  private final Object key;

  /**
   * Create a non iterable node.
   *
   * @param name the possibly <code>null</code> name.
   */
  public NodeImpl(String name) {
    this.name = name;
    this.index = null;
    this.key = null;
  }

  /**
   * Create an iterable node with an index.
   *
   * @param name the possibly <code>null</code> name.
   * @param index the zero based index.
   */
  public NodeImpl(String name, int index) {
    if (index < 0) {
      throw new IllegalArgumentException("Index can not be negative.");
    }
    this.name = name;
    this.index = Integer.valueOf(index);
    this.key = null;
  }

  /**
   * Create an iterable node with a key.
   *
   * @param name the possibly <code>null</code> name.
   * @param key the lookup key for this node.
   */
  public NodeImpl(String name, Object key) {
    if (key == null) {
      throw new NullPointerException();
    }
    this.name = name;
    this.index = null;
    this.key = key;
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
    return (this.name == null ? that.name == null : this.name == that.name)
        && (this.index == null ? that.index == null : this.index == that.index)
        && (this.key == null ? that.key == null : this.key == that.key);
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
    return result;
  }

  public boolean isInIterable() {
    return index != null || key != null;
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
      } else {
        sb.append(index);
      }
      sb.append(']');
    }
    return sb.toString();
  }
}
