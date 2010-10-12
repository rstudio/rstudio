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
package com.google.gwt.user.cellview.client;

/**
 * A representation of a node in a tree.
 */
public interface TreeNode {

  /**
   * Get the number of children of the node.
   *
   * @return the child count
   */
  int getChildCount();

  /**
   * Get the value associated with a child node.
   *
   * @param index the child index
   * @return the value
   */
  Object getChildValue(int index);

  /**
   * Get the index of the current node relative to its parent.
   *
   * @return the index of the current node
   */
  int getIndex();

  /**
   * Get the parent node of this node.
   *
   * @return the parent node, or null if this node is the root node
   */
  TreeNode getParent();

  /**
   * Get the value associated with this node. This method can be called on
   * destroyed nodes.
   *
   * @return the value
   */
  Object getValue();

  /**
   * Check whether or not a child node is a leaf node.
   *
   * @param index the index of the child
   * @return true if a leaf node, false if not
   */
  boolean isChildLeaf(int index);

  /**
   * Check whether or not a child node is open.
   *
   * @param index the index of the child
   * @return true if open, false if closed
   */
  boolean isChildOpen(int index);

  /**
   * Check whether or not the current node is destroyed. The node is destroyed
   * when it is closed, even if it still appears in the tree as an unopened
   * non-leaf node. Once a node is destroyed, calling most methods on the node
   * results in an {@link IllegalStateException}.
   *
   * @return true if destroyed, false if active
   */
  boolean isDestroyed();

  /**
   * Open or close a child node and fire an event. If <code>open</code> is true
   * and the {@link TreeNode} successfully opens, returns the child
   * {@link TreeNode}. Delegates to {@link #setChildOpen(int,boolean, boolean)}.
   *
   * @param index the index of the child
   * @param open true to open, false to close
   * @return the {@link TreeNode} that was opened, or null if the node was
   *         closed or could not be opened
   */
  TreeNode setChildOpen(int index, boolean open);

  /**
   * Open or close the node, optionally firing an event. If <code>open</code> is
   * true and the {@link TreeNode} successfully opens, returns the child
   * {@link TreeNode}.
   *
   * @param index the index of the child
   * @param open true to open, false to flose
   * @param fireEvents true to fire an event, false not to
   * @return the {@link TreeNode} that was opened, or null if the node was
   *         closed or could not be opened
   */
  TreeNode setChildOpen(int index, boolean open, boolean fireEvents);
}
