/*
 * Copyright 2009 Google Inc.
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
 * Implemented by {@link IndexedPanel}s that also allow insertions.
 */
public interface InsertPanel extends IndexedPanel {

  /**
   * Extends this interface with convenience methods to handle {@link IsWidget}.
   */
  interface ForIsWidget extends InsertPanel, IndexedPanel.ForIsWidget {
    void add(IsWidget w);

    void insert(IsWidget w, int beforeIndex);
  }

  /**
   * Adds a child widget to this panel.
   * 
   * @param w the child widget to be added
   */
  void add(Widget w);

  /**
   * Inserts a child widget before the specified index. If the widget is already
   * a child of this panel, it will be moved to the specified index.
   * 
   * @param w the child widget to be inserted
   * @param beforeIndex the index before which it will be inserted
   * @throws IndexOutOfBoundsException if <code>beforeIndex</code> is out of
   *           range
   */
  void insert(Widget w, int beforeIndex);
}
