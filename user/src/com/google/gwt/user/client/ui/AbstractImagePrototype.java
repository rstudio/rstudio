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

import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.impl.ClippedImagePrototype;

/**
 * An opaque representation of a particular image such that the image can be
 * accessed either as an HTML fragment or as an {@link Image} object. An image
 * prototype can be thought of as an abstract image factory with additional
 * capabilities.
 * 
 * <p>
 * The {@link #applyTo(Image)} method provides an efficient way to replace the
 * contents of an existing <code>Image</code>. This is useful in cases where an
 * image changes its appearance based on a user's action. Instead of creating
 * two <code>Image</code> objects then alternately hiding/showing them, one can
 * use the {@link #applyTo(Image)} method of two
 * <code>AbstractImagePrototype</code> objects to transform a single
 * <code>Image</code> object between two (or more) visual representations. The
 * use of <code>AbstractImagePrototypes</code> results in an cleaner and more
 * efficient implementation.
 * </p>
 * 
 * <p>
 * This class also provide methods for working with raw elements, using
 * {@link #createElement()} and {@link #applyTo(ImagePrototypeElement)}.
 * </p>
 * 
 * <p>
 * This class is also a useful way to encapsulate complex HTML that represents
 * an image without actually instantiating <code>Image</code> objects. When
 * constructing large HTML fragments, especially those that contain many images,
 * {@link #getHTML()} can be much more efficient.
 * </p>
 */
public abstract class AbstractImagePrototype {

  /**
   * This corresponds to the top Element of the DOM structure created by
   * {@link #createElement()}.
   */
  public static class ImagePrototypeElement extends Element {
    protected ImagePrototypeElement() {
    }
  }

  /**
   * Create an AbstractImagePrototype backed by a ClientBundle ImageResource.
   * This method provides an API compatibility mapping for the new ImageResource
   * API.
   * 
   * @param resource an ImageResource produced by a ClientBundle
   * @return an AbstractImagePrototype that displays the contents of the
   *         ImageResource
   */
  public static AbstractImagePrototype create(ImageResource resource) {
    return new ClippedImagePrototype(resource.getSafeUri(), resource.getLeft(), resource.getTop(),
        resource.getWidth(), resource.getHeight());
  }

  /**
   * Transforms an existing {@link Image} into the image represented by this
   * prototype.
   * 
   * @param image the instance to be transformed to match this prototype
   */
  public abstract void applyTo(Image image);

  /**
   * Transforms an existing {@link ImagePrototypeElement} into the image
   * represented by this prototype.
   * 
   * @param imageElement an <code>ImagePrototypeElement</code> created by
   *          {@link #createElement()}
   */
  public void applyTo(ImagePrototypeElement imageElement) {
    // Because this is a new method on an existing base class, we need to throw
    // UnsupportedOperationException to avoid static errors.
    throw new UnsupportedOperationException();
  }

  /**
   * Creates a new {@link Element} based on the image represented by this
   * prototype. The DOM structure may not necessarily a simple
   * <code>&lt;img&gt;</code> element. It may be a more complex structure that
   * should be treated opaquely.
   * 
   * @return the <code>ImagePrototypeElement</code> corresponding to the image
   *         represented by this prototype
   */
  public ImagePrototypeElement createElement() {
    // Because this is a new method on an existing base class, we need to throw
    // UnsupportedOperationException to avoid static errors.
    throw new UnsupportedOperationException();
  }

  /**
   * Creates a new {@link Image} instance based on the image represented by this
   * prototype.
   * 
   * @return a new {@link Image} based on this prototype
   */
  public abstract Image createImage();

  /**
   * Gets an HTML fragment that displays the image represented by this
   * prototype. The HTML returned is not necessarily a simple
   * <code>&lt;img&gt;</code> element. It may be a more complex structure that
   * should be treated opaquely.
   * <p>
   * The default implementation calls {@link #getSafeHtml()}.
   * 
   * @return the HTML representation of this prototype
   */
  public String getHTML() {
    return getSafeHtml().asString();
  }

  /**
   * Gets an HTML fragment that displays the image represented by this
   * prototype. The HTML returned is not necessarily a simple
   * <code>&lt;img&gt;</code> element. It may be a more complex structure that
   * should be treated opaquely.
   * <p>
   * The default implementation throws an {@link UnsupportedOperationException}.
   * 
   * @return the HTML representation of this prototype
   */
  public SafeHtml getSafeHtml() {
    // Because this is a new method on an existing base class, we need to throw
    // UnsupportedOperationException to avoid static errors.
    throw new UnsupportedOperationException();
  }
}
