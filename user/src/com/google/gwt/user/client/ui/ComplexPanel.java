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
public abstract class ComplexPanel extends Panel {

  private WidgetCollection children = new WidgetCollection(this);

  public Iterator iterator() {
    return children.iterator();
  }

  public boolean remove(Widget w) {
    // Make sure this panel actually contains the child widget.
    if (!children.contains(w)) {
      return false;
    }

    // Disown it.
    disown(w);

    children.remove(w);
    return true;
  }

  /**
   * Adds a new child widget to the panel.
   * 
   * @param w the child widget to be added
   * @param container the element within which the child will be contained
   */
  protected void add(Widget w, Element container) {
    insert(w, container, children.size());
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
    if (w.getParent() == this) {
      return;
    }

    adopt(w, container);
    children.insert(w, beforeIndex);
  }
}
