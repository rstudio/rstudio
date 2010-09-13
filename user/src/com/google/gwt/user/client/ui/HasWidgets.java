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
package com.google.gwt.user.client.ui;

import java.util.Iterator;

/**
 * A widget that implements this interface contains
 * {@link com.google.gwt.user.client.ui.Widget widgets} and can enumerate them.
 */
public interface HasWidgets extends Iterable<Widget> {

  /**
   * Extends this interface with convenience methods to handle {@link IsWidget}.
   */
  interface ForIsWidget extends HasWidgets {
    void add(IsWidget w);

    boolean remove(IsWidget w);
  }

  /**
   * Adds a child widget.
   * 
   * @param w the widget to be added
   * @throws UnsupportedOperationException if this method is not supported (most
   *           often this means that a specific overload must be called)
   */
  void add(Widget w);

  /**
   * Removes all child widgets.
   */
  void clear();

  /**
   * Gets an iterator for the contained widgets. This iterator is required to
   * implement {@link Iterator#remove()}.
   */
  Iterator<Widget> iterator();

  /**
   * Removes a child widget.
   * 
   * @param w the widget to be removed
   * @return <code>true</code> if the widget was present
   */
  boolean remove(Widget w);
}
