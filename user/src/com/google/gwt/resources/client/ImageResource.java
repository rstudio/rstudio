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
package com.google.gwt.resources.client;

import com.google.gwt.resources.ext.DefaultExtensions;
import com.google.gwt.resources.ext.ResourceGeneratorType;
import com.google.gwt.resources.rg.ImageResourceGenerator;
import com.google.gwt.safehtml.shared.SafeUri;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides access to image resources at runtime.
 */
@DefaultExtensions(value = {".png", ".jpg", ".gif", ".bmp"})
@ResourceGeneratorType(ImageResourceGenerator.class)
public interface ImageResource extends ResourcePrototype {

  /**
   * Specifies additional options to control how an image is bundled.
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface ImageOptions {
    /**
     * If <code>true</code>, the image will be flipped about the y-axis when
     * {@link com.google.gwt.i18n.client.LocaleInfo#isRTL()} returns
     * <code>true</code>. This is intended to be used by graphics that are
     * sensitive to layout direction, such as arrows and disclosure indicators.
     */
    boolean flipRtl() default false;

    /**
     * Set to a positive value to override the image's intrinsic height. The
     * image bundling code will scale the image to the desired height. If only
     * one of <code>width</code> or <code>height</code> are set, the aspect
     * ratio of the image will be maintained.
     */
    int height() default -1;

    /**
     * Set to {@code true} to require the ImageResource to be downloaded as a
     * separate resource at runtime. Specifically, this will disable the use of
     * {@code data:} URLs or other bundling optimizations for the image. This
     * can be used for infrequently-displayed images.
     */
    boolean preventInlining() default false;

    /**
     * This option affects the image bundling optimization to allow the image to
     * be used with the {@link CssResource} {@code @sprite} rule where
     * repetition of the image is desired.
     * 
     * @see "CssResource documentation"
     */
    // http://bugs.sun.com/view_bug.do?bug_id=6512707
    RepeatStyle repeatStyle() default com.google.gwt.resources.client.ImageResource.RepeatStyle.None;

    /**
     * Set to a positive value to override the image's intrinsic width. The
     * image bundling code will scale the image to the desired width. If only
     * one of <code>width</code> or <code>height</code> are set, the aspect
     * ratio of the image will be maintained.
     */
    int width() default -1;
  }

  /**
   * Indicates that an ImageResource should be bundled in such a way as to
   * support horizontal or vertical repetition.
   */
  public enum RepeatStyle {
    /**
     * The image is not intended to be tiled.
     */
    None,

    /**
     * The image is intended to be tiled horizontally.
     */
    Horizontal,

    /**
     * The image is intended to be tiled vertically.
     */
    Vertical,

    /**
     * The image is intended to be tiled both horizontally and vertically. Note
     * that this will prevent compositing of the particular image in most cases.
     */
    Both
  }

  /**
   * Returns the height of the image.
   */
  int getHeight();

  /**
   * Returns the horizontal position of the image within the composite image.
   */
  int getLeft();

  /**
   * Returns the URL for the composite image that contains the ImageResource.
   */
  SafeUri getSafeUri();

  /**
   * Returns the vertical position of the image within the composite image.
   */
  int getTop();

  /**
   * Returns the URL for the composite image that contains the ImageResource.
   * 
   * @deprecated Use {@link #getSafeUri()} instead.
   */
  @Deprecated
  String getURL();

  /**
   * Returns the width of the image.
   */
  int getWidth();

  /**
   * Return <code>true</code> if the image contains multiple frames.
   */
  boolean isAnimated();
}
