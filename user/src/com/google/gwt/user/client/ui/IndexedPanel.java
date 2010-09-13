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
 * Implemented by panels that impose an explicit ordering on their children.
 * 
 * @see InsertPanel
 */
public interface IndexedPanel {

  /**
   * Extends this interface with convenience methods to handle {@link IsWidget}.
   */
  interface ForIsWidget extends IndexedPanel {
    int getWidgetIndex(IsWidget child);
  }

  /**
   * Gets the child widget at the specified index.
   * 
   * @param index the child widget's index
   * @return the child widget
   */
  Widget getWidget(int index);

  /**
   * Gets the number of child widgets in this panel.
   * 
   * @return the number of children
   */
  int getWidgetCount();

  /**
   * Gets the index of the specified child widget.
   * 
   * @param child the widget to be found
   * @return the widget's index, or <code>-1</code> if it is not a child of this
   *         panel
   */
  int getWidgetIndex(Widget child);

  /**
   * Removes the widget at the specified index.
   * 
   * @param index the index of the widget to be removed
   * @return <code>false</code> if the widget is not present
   */
  boolean remove(int index);
}
