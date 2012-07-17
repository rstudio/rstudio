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
package com.google.gwt.user.client.ui.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Image;

/**
 * Implementation of {@link AbstractImagePrototype} for a clipped image. This
 * class is used internally by the image bundle generator and is not intended
 * for general use. It is subject to change without warning.
 */
public class ClippedImagePrototype extends AbstractImagePrototype {

  private static final ClippedImageImpl impl = GWT.create(ClippedImageImpl.class);

  private int height = 0;
  private int left = 0;
  private int top = 0;
  private SafeUri url = null;
  private int width = 0;
  private boolean isDraggable = false;

  public ClippedImagePrototype(SafeUri url, int left, int top, int width, int height) {
    this.url = url;
    this.left = left;
    this.top = top;
    this.width = width;
    this.height = height;
  }

  @Deprecated
  public ClippedImagePrototype(String url, int left, int top, int width, int height) {
    this(UriUtils.unsafeCastFromUntrustedString(url), left, top, width, height);
  }

  @Override
  public void applyTo(Image image) {
    image.setUrlAndVisibleRect(url, left, top, width, height);
  }

  @Override
  public void applyTo(ImagePrototypeElement imageElement) {
    impl.adjust(imageElement, url, left, top, width, height);
  }

  @Override
  public ImagePrototypeElement createElement() {
    return (ImagePrototypeElement) impl.createStructure(url, left, top, width, height);
  }

  @Override
  public Image createImage() {
    return new Image(url, left, top, width, height);
  }

  @Override
  public SafeHtml getSafeHtml() {
    return impl.getSafeHtml(url, left, top, width, height, isDraggable);
  }

  public void setDraggable(boolean isDraggable) {
    this.isDraggable = isDraggable;
  }
}
