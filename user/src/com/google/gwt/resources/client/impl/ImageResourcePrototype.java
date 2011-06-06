/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.resources.client.impl;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeUri;

/**
 * This is part of an implementation of the ImageBundle optimization implemented
 * with ClientBundle.
 */
public class ImageResourcePrototype implements ImageResource {

  private final boolean animated;
  private final boolean lossy;
  private final String name;
  private final SafeUri url;
  private final int left;
  private final int top;
  private final int width;
  private final int height;

  /**
   * Only called by generated code.
   */
  public ImageResourcePrototype(String name, SafeUri url, int left, int top, int width, int height,
      boolean animated, boolean lossy) {
    this.name = name;
    this.left = left;
    this.top = top;
    this.height = height;
    this.width = width;
    this.url = url;
    this.animated = animated;
    this.lossy = lossy;
  }

  /**
   * Exists for testing purposes, not part of the ImageResource interface.
   */
  public int getHeight() {
    return height;
  }

  /**
   * Exists for testing purposes, not part of the ImageResource interface.
   */
  public int getLeft() {
    return left;
  }

  public String getName() {
    return name;
  }

  public SafeUri getSafeUri() {
    return url;
  }

  public int getTop() {
    return top;
  }

  public String getURL() {
    return url.asString();
  }

  /**
   * Exists for testing purposes, not part of the ImageResource interface.
   */
  public int getWidth() {
    return width;
  }

  public boolean isAnimated() {
    return animated;
  }

  /**
   * Exists for testing purposes, not part of the ImageResource interface.
   */
  public boolean isLossy() {
    return lossy;
  }
}
