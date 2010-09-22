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

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.event.logical.shared.HasOpenHandlers;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.view.client.TreeViewModel;
import com.google.gwt.view.client.TreeViewModel.NodeInfo;

/**
 * An abstract representation of a tree widget that renders items using
 * {@link com.google.gwt.cell.client.Cell}s.
 */
public abstract class AbstractCellTree extends Composite implements
    HasOpenHandlers<TreeNode>, HasCloseHandlers<TreeNode>,
    HasKeyboardSelectionPolicy {

  private KeyboardSelectionPolicy keyboardSelectionPolicy = KeyboardSelectionPolicy.ENABLED;

  /**
   * The {@link TreeViewModel} that backs the tree.
   */
  private final TreeViewModel viewModel;

  /**
   * Construct a new {@link CellTree} with the specified {@link TreeViewModel}
   * and root value.
   *
   * @param viewModel the {@link TreeViewModel} that backs the tree
   */
  public AbstractCellTree(TreeViewModel viewModel) {
    this.viewModel = viewModel;
  }

  public HandlerRegistration addCloseHandler(CloseHandler<TreeNode> handler) {
    return addHandler(handler, CloseEvent.getType());
  }

  public HandlerRegistration addOpenHandler(OpenHandler<TreeNode> handler) {
    return addHandler(handler, OpenEvent.getType());
  }

  public KeyboardSelectionPolicy getKeyboardSelectionPolicy() {
    return keyboardSelectionPolicy;
  }

  /**
   * Get the root {@link TreeNode}.
   *
   * @return the {@link TreeNode} at the root of the tree
   */
  public abstract TreeNode getRootTreeNode();

  /**
   * Get the {@link TreeViewModel} that backs this tree.
   *
   * @return the {@link TreeViewModel}
   */
  public TreeViewModel getTreeViewModel() {
    return viewModel;
  }

  public void setKeyboardSelectionPolicy(KeyboardSelectionPolicy policy) {
    if (policy == null) {
      throw new NullPointerException("KeyboardSelectionPolicy cannot be null");
    }
    this.keyboardSelectionPolicy = policy;
  }

  /**
   * Get the {@link NodeInfo} that will provide the information to retrieve and
   * display the children of the specified value.
   *
   * @param value the value in the parent node
   * @return the {@link NodeInfo}
   */
  protected <T> NodeInfo<?> getNodeInfo(T value) {
    return viewModel.getNodeInfo(value);
  }

  /**
   * Check if keyboard selection is disabled.
   *
   * @return true if disabled, false if enabled.
   */
  protected boolean isKeyboardSelectionDisabled() {
    return KeyboardSelectionPolicy.DISABLED == keyboardSelectionPolicy;
  }

  /**
   * Check if the value is known to be a leaf node.
   *
   * @param value the value at the node
   *
   * @return true if the node is known to be a leaf node, false otherwise
   */
  protected boolean isLeaf(Object value) {
    return viewModel.isLeaf(value);
  }
}
