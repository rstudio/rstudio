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
   * @return the index at which the widget was added
   */
  protected int add(Widget w, Element container) {
    return insert(w, container, getChildren().size());
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
   * @return the index at which the widget was added
   */
  protected int insert(Widget w, Element container, int beforeIndex) {
    if ((beforeIndex < 0) || (beforeIndex > getWidgetCount())) {
      throw new IndexOutOfBoundsException();
    }
    // Call this early to ensure that the table doesn't end up partially
    // constructed when an exception is thrown from adopt().
    int idx = getWidgetIndex(w);
    if (idx == -1) {
      w.removeFromParent();
    } else {
      remove(w);
      // If the Widget's previous position was left of the desired new position
      // shift the desired position left to reflect the removal
      if (idx < beforeIndex) {
        beforeIndex--;
      }
    }
    adopt(w, container);
    getChildren().insert(w, beforeIndex);
    return beforeIndex;
  }
}
