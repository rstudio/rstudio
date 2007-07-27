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

import com.google.gwt.user.client.Element;

import java.util.Iterator;

/**
 * Abstract base class for panels that can contain multiple child widgets.
 */
public abstract class ComplexPanel extends Panel implements IndexedPanel {

  private WidgetCollection children = new WidgetCollection(this);

  public Widget getWidget(int index) {
    return getChildren().get(index);
  }

  public int getWidgetCount() {
    return getChildren().size();
  }

  public int getWidgetIndex(Widget child) {
    return getChildren().indexOf(child);
  }

  public Iterator iterator() {
    return getChildren().iterator();
  }

  public boolean remove(int index) {
    return remove(getWidget(index));
  }

  public boolean remove(Widget w) {
    // Make sure this panel actually contains the child widget.
    if (!getChildren().contains(w)) {
      return false;
    }

    // Disown it.
    disown(w);

    getChildren().remove(w);
    return true;
  }

  /**
   * Adds a new child widget to the panel.
   * 
   * @param w the child widget to be added
   * @param container the element within which the child will be contained
   */
  protected void add(Widget w, Element container) {
    insert(w, container, getChildren().size());
  }

  /**
   * Adjusts beforeIndex to account for the possibility that the given widget is
   * already a child of this panel.
   * 
   * @param w the widget to be removed
   * @param beforeIndex the index at which it will be added to this panel
   * @return the modified index
   */
  protected int adjustIndex(Widget w, int beforeIndex) {
    checkIndexBoundsForInsertion(beforeIndex);

    // Check to see if this widget is already a direct child.
    if (w.getParent() == this) {
      // If the Widget's previous position was left of the desired new position
      // shift the desired position left to reflect the removal
      int idx = getWidgetIndex(w);
      if (idx < beforeIndex) {
        beforeIndex--;
      }
    }

    return beforeIndex;
  }

  /**
   * Checks that <code>index</code> is in the range [0, getWidgetCount()),
   * which is the valid range on accessible indexes.
   * 
   * @param index the index being accessed
   */
  protected void checkIndexBoundsForAccess(int index) {
    if (index < 0 || index >= getWidgetCount()) {
      throw new IndexOutOfBoundsException();
    }
  }

  /**
   * Checks that <code>index</code> is in the range [0, getWidgetCount()],
   * which is the valid range for indexes on an insertion.
   * 
   * @param index the index where insertion will occur
   */
  protected void checkIndexBoundsForInsertion(int index) {
    if (index < 0 || index > getWidgetCount()) {
      throw new IndexOutOfBoundsException();
    }
  }

  /**
   * Gets the list of children contained in this panel.
   * 
   * @return a collection of child widgets
   */
  protected WidgetCollection getChildren() {
    return children;
  }

  /**
   * Inserts a new child widget into the panel.
   * 
   * @param w the child widget to be added
   * @param container the element within which the child will be contained
   * @param beforeIndex the index before which the widget will be added
   */
  protected void insert(Widget w, Element container, int beforeIndex) {
    // Adjust beforeIndex, in case the widget is already a child of this panel.
    beforeIndex = adjustIndex(w, beforeIndex);
    adopt(w, container);
    getChildren().insert(w, beforeIndex);
  }
}
