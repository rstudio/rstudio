/*
 * Copyright 2011 Google Inc.
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
 * Implemented by objects that have the visibility trait.
 */
public interface HasVisibility {

  /**
   * Determines whether or not this object is visible. Note that this does not
   * necessarily take into account whether or not the receiver's parent is
   * visible, or even if it is attached to the
   * {@link com.google.gwt.dom.client.Document Document}. The default
   * implementation of this trait in {@link UIObject} is based on the value of a
   * dom element's style object's display attribute.
   * 
   * @return <code>true</code> if the object is visible
   */
  boolean isVisible();

  /**
   * Sets whether this object is visible.
   * 
   * @param visible <code>true</code> to show the object, <code>false</code> to
   *          hide it
   */
  void setVisible(boolean visible);

}