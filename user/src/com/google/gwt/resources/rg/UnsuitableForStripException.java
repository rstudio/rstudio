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
package com.google.gwt.resources.rg;

/**
 * Indicates that an image is not suitable for being added to an image strip.
 */
class UnsuitableForStripException extends Exception {
  
  private static final long serialVersionUID = -1;
  private final ImageBundleBuilder.ImageRect rect;
  
  public UnsuitableForStripException(ImageBundleBuilder.ImageRect rect) {
    this.rect = rect;
  }
  
  public UnsuitableForStripException(ImageBundleBuilder.ImageRect rect, String msg) {
    super(msg);
    this.rect = rect;
  }
  
  public UnsuitableForStripException(String msg, Throwable cause) {
    super(msg, cause);
    this.rect = null;
  }
  
  public ImageBundleBuilder.ImageRect getImageRect() {
    return rect;
  }
}
