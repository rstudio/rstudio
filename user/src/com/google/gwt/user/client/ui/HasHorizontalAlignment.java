/*
 * Copyright 2006 Google Inc.
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
 * Characteristic interface which indicates that a widget can be aligned
 * horizontally.
 */
public interface HasHorizontalAlignment {

  /**
   * Horizontal alignment constant.
   */
  public static class HorizontalAlignmentConstant {
    private String textAlignString;

    private HorizontalAlignmentConstant(String textAlignString) {
      this.textAlignString = textAlignString;
    }

    /**
     * Gets the CSS 'text-align' string associated with this constant.
     * 
     * @return the CSS 'text-align' value
     */
    public String getTextAlignString() {
      return textAlignString;
    }
  }

  /**
   * Specifies that the widget's contents should be aligned in the center.
   */
  public static final HorizontalAlignmentConstant ALIGN_CENTER = new HorizontalAlignmentConstant(
    "center");

  /**
   * Specifies that the widget's contents should be aligned to the left.
   */
  public static final HorizontalAlignmentConstant ALIGN_LEFT = new HorizontalAlignmentConstant(
    "left");

  /**
   * Specifies that the widget's contents should be aligned to the right.
   */
  public static final HorizontalAlignmentConstant ALIGN_RIGHT = new HorizontalAlignmentConstant(
    "right");

  /**
   * Gets the horizontal alignment.
   * 
   * @return the current horizontal alignment.
   */
  public HorizontalAlignmentConstant getHorizontalAlignment();

  /**
   * Sets the horizontal alignment.
   * 
   * @param align the horizontal alignment (
   *          {@link HasHorizontalAlignment#ALIGN_LEFT},
   *          {@link HasHorizontalAlignment#ALIGN_CENTER}, or
   *          {@link HasHorizontalAlignment#ALIGN_RIGHT}).
   */
  public void setHorizontalAlignment(HorizontalAlignmentConstant align);
}