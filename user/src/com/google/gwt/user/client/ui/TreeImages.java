/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.user.client.ui;

/**
 * An {@link ImageBundle} that provides images for
 * {@link com.google.gwt.user.client.ui.Tree}.
 * 
 * <p>
 * <h3>Example</h3> {@example com.google.gwt.examples.TreeImagesExample}
 * </p>
 * 
 * @deprecated replaced by {@link Tree.Resources}.
 */
@Deprecated
public interface TreeImages extends ImageBundle {

  /**
   * An image indicating an open branch.
   * 
   * @return a prototype of this image
   */
  AbstractImagePrototype treeOpen();

  /**
   * An image indicating a closed branch.
   * 
   * @return a prototype of this image
   */
  AbstractImagePrototype treeClosed();

  /**
   * An image indicating a leaf.
   * 
   * @return a prototype of this image
   */
  AbstractImagePrototype treeLeaf();
}
