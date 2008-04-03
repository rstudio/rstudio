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
package com.google.gwt.i18n.client;

/**
 * A widget that implements this interface has the ability to override 
 * the document directionality for its root element.
 * 
 * Widgets that implement this interface should be leaf widgets. More
 * specifically, they should not implement the 
 * {@link com.google.gwt.user.client.ui.HasWidgets} interface.
 */
public interface HasDirection {

  /**
   * Possible return values for {@link HasDirection#getDirection()} and parameter values for
   * {@link HasDirection#setDirection(Direction)}.Widgets that implement this interface can 
   * either have a direction that is right-to-left (RTL), left-to-right (LTR), or default 
   * (which means that their directionality is inherited from their parent widget). 
   */
  enum Direction { RTL, LTR, DEFAULT }
    
  /**
   * Sets the directionality for a widget.
   *
   * @param direction <code>RTL</code> if the directionality should be set to right-to-left,
   *                  <code>LTR</code> if the directionality should be set to left-to-right
   *                  <code>DEFAULT</code> if the directionality should not be explicitly set
   */
  void setDirection(Direction direction);
  
  /**
   * Gets the directionality of the widget.
   *
   * @return <code>RTL</code> if the directionality is right-to-left,
   *         <code>LTR</code> if the directionality is left-to-right, or
   *         <code>DEFAULT</code> if the directionality is not explicitly specified
   */
  Direction getDirection();  
}
