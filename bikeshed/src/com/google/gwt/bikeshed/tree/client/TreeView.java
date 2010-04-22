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

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A view of a tree.
 */
public abstract class TreeView extends Composite {

  /**
   * A ClientBundle that provides images for this widget.
   */
  public static interface Resources extends ClientBundle {

    /**
     * An image indicating a closed branch.
     */
    ImageResource treeClosed();

    /**
     * An image indicating an open branch.
     */
    ImageResource treeOpen();
  }

  private static final Resources DEFAULT_RESOURCES = GWT.create(Resources.class);

  /**
   * The {@link Resources} used in the tree.
   */
  private Resources resources = DEFAULT_RESOURCES;

  /**
   * The {@link TreeViewModel} that backs the tree.
   */
  private TreeViewModel viewModel;

  /**
   * Construct a new {@link TreeView}.
   * 
   * @param viewModel the {@link TreeViewModel} that backs the tree
   */
  // TODO(jlabanca): Should we nuke this class?
  public TreeView(TreeViewModel viewModel) {
    this(viewModel, new SimplePanel());
  }

  protected TreeView(TreeViewModel viewModel, Widget widget) {
    this.viewModel = viewModel;
    initWidget(widget);
  }

  public TreeViewModel getTreeViewModel() {
    return viewModel;
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
   * Get the {@link Resources} used in the tree.
   * 
   * @return the resources
   */
  protected Resources getResources() {
    return resources;
  }
}
