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
package com.google.gwt.bikeshed.tree.client;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.bikeshed.list.shared.SelectionModel;
import com.google.gwt.bikeshed.list.shared.SelectionModel.SelectionChangeEvent;
import com.google.gwt.bikeshed.list.shared.SelectionModel.SelectionChangeHandler;
import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Widget;

/**
 * A view of a tree.
 */
public abstract class TreeView extends Widget {

  /**
   * An Animation of a {@link TreeNodeView}.
   */
  public abstract static class TreeNodeViewAnimation extends Animation {

    /**
     * The default animation delay in milliseconds.
     */
    private static final int DEFAULT_ANIMATION_DURATION = 450;

    /**
     * The duration of the animation.
     */
    private int duration = DEFAULT_ANIMATION_DURATION;

    /**
     * Animate a {@link TreeNodeView} into its new state.
     *
     * @param node the {@link TreeNodeView} to animate
     * @param isAnimationEnabled true to animate
     */
    public abstract void animate(TreeNodeView<?> node,
        boolean isAnimationEnabled);

    public int getDuration() {
      return duration;
    }

    public void setDuration(int duration) {
      this.duration = duration;
    }
  }

  /**
   * A ClientBundle that provides images for this widget.
   */
  interface Resources extends ClientBundle {

    /**
     * An image indicating a closed branch.
     */
    ImageResource treeClosed();

    /**
     * An image indicating an open branch.
     */
    ImageResource treeOpen();
  }

  private class TreeSelectionHandler implements SelectionChangeHandler {
    public void onSelectionChange(SelectionChangeEvent event) {
      refreshSelection();
    }
  }

  private static final Resources DEFAULT_RESOURCES = GWT.create(Resources.class);

  /**
   * The animation.
   */
  private TreeNodeViewAnimation animation;

  /**
   * The HTML used to generate the closed image.
   */
  private String closedImageHtml;

  /**
   * Indicates whether or not animations are enabled.
   */
  private boolean isAnimationEnabled;

  /**
   * The message displayed while child nodes are loading.
   */
  // TODO(jlabanca): I18N this, or remove the text
  private String loadingHtml = "Loading...";

  /**
   * The HTML used to generate the open image.
   */
  private String openImageHtml;

  /**
   * The hidden root node in the tree.
   */
  private TreeNodeView<?> rootNode;

  /**
   * The {@link SelectionModel} for the tree.
   */
  private SelectionModel<Object> selectionModel;

  /**
   * The {@link TreeViewModel} that backs the tree.
   */
  private TreeViewModel viewModel;

  /**
   * Construct a new {@link TreeView}.
   *
   * @param viewModel the {@link TreeViewModel} that backs the tree
   */
  public TreeView(TreeViewModel viewModel) {
    this.viewModel = viewModel;
  }

  /**
   * Get the animation used to open and close nodes in this tree if animations
   * are enabled.
   *
   * @return the animation
   * @see #isAnimationEnabled()
   */
  public TreeNodeViewAnimation getAnimation() {
    return animation;
  }

  public TreeNode<?> getRootNode() {
    return rootNode;
  }

  /**
   * Returns the {@link SelectionModel} containing the selection state for
   * this tree, or null if none is present.
   */
  public SelectionModel<Object> getSelectionModel() {
    return selectionModel;
  }

  public TreeViewModel getTreeViewModel() {
    return viewModel;
  }

  public boolean isAnimationEnabled() {
    return isAnimationEnabled;
  }

  /**
   * Refresh any visible cells of this tree that depend on the selection state.
   */
  public void refreshSelection() {
    refreshSelection(rootNode);
  }

  /**
   * Set the animation used to open and close nodes in this tree. You must call
   * {@link #setAnimationEnabled(boolean)} to enable or disable animation.
   *
   * @param animation a {@link TreeNodeViewAnimation}
   * @see #setAnimationEnabled(boolean)
   */
  public void setAnimation(TreeNodeViewAnimation animation) {
    assert animation != null : "animation cannot be null";
    this.animation = animation;
  }

  public void setAnimationEnabled(boolean enable) {
    this.isAnimationEnabled = enable;
    if (!enable && animation != null) {
      animation.cancel();
    }
  }

  public void setSelectionModel(SelectionModel<Object> selectionModel) {
    this.selectionModel = selectionModel;
    // Attach a selection handler.
    if (selectionModel != null) {
      selectionModel.addSelectionChangeHandler(new TreeSelectionHandler());
    }
  }

  /**
   * @return the HTML to render the closed image.
   */
  protected String getClosedImageHtml(int left) {
    if (closedImageHtml == null) {
      AbstractImagePrototype proto =
        AbstractImagePrototype.create(DEFAULT_RESOURCES.treeClosed());
      // CHECKSTYLE_OFF
      closedImageHtml = proto.getHTML().replace("style='",
          "style='position:absolute;left:" + left + "px;top:0px;");
      // CHECKSTYLE_ON
    }
    return closedImageHtml;
  }

  /**
   * Get the width required for the images.
   *
   * @return the maximum width required for images.
   */
  protected int getImageWidth() {
    return Math.max(DEFAULT_RESOURCES.treeClosed().getWidth(),
        DEFAULT_RESOURCES.treeOpen().getWidth());
  }

  /**
   * Get the HTML string that is displayed while nodes wait for their children
   * to load.
   *
   * @return the loading HTML string
   */
  protected String getLoadingHtml() {
    return loadingHtml;
  }

  /**
   * @return the HTML to render the open image.
   */
  protected String getOpenImageHtml(int left) {
    if (openImageHtml == null) {
      AbstractImagePrototype proto =
        AbstractImagePrototype.create(DEFAULT_RESOURCES.treeOpen());
      // CHECKSTYLE_OFF
      openImageHtml = proto.getHTML().replace("style='",
          "style='position:absolute;left:" + left + "px;top:0px;");
      // CHECKSTYLE_ON
    }
    return openImageHtml;
  }

  protected TreeNodeView<?> getRootTreeNodeView() {
    return rootNode;
  }

  /**
   * Animate the current state of a {@link TreeNodeView} in this tree.
   *
   * @param node the node to animate
   */
  protected void maybeAnimateTreeNode(TreeNodeView<?> node) {
    if (animation != null) {
      animation.animate(node, node.consumeAnimate() && isAnimationEnabled());
    }
  }

  /**
   * Set the HTML string that will be displayed when a node is waiting for its
   * child nodes to load.
   *
   * @param loadingHtml the HTML string
   */
  protected void setLoadingHtml(String loadingHtml) {
    this.loadingHtml = loadingHtml;
  }

  protected void setRootNode(TreeNodeView<?> rootNode) {
    this.rootNode = rootNode;
  }

  private void refreshSelection(TreeNodeView<?> node) {
    node.refreshSelection();
    int count = node.getChildCount();
    for (int i = 0; i < count; i++) {
      TreeNodeView<?> child = node.getChildTreeNodeView(i);
      if (child.isOpen()) {
        refreshSelection(child);
      }
    }
  }
}
