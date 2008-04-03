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
 */
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

/**
 * A bundle containing the RTL versions of the images for Tree. Right now, there is no
 * need to override any of the methods in TreeImages and specify a different image in
 * the RTL context, because the images that we're currently using are direction-agnostic.
 * 
 * Notice that this interface is package protected. This interface need not be
 * publicly exposed, as it is only used by the Tree class to provide RTL
 * versions of the images in the case the the user does not pass in their own
 * bundle. However, we cannot make this class private, because the generated
 * class needs to be able to extend this class.
 */
interface TreeImagesRTL extends TreeImages {
}
