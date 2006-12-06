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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Abstract base class for panels that contain only one widget.
 */
public class SimplePanel extends Panel {

  private Widget widget;

  /**
   * Creates an empty panel that uses a DIV for its contents.
   */
  public SimplePanel() {
    this(DOM.createDiv());
  }

  /**
   * Creates an empty panel that uses the specified browser element for its
   * contents.
   * 
   * @param elem the browser element to use
   */
  protected SimplePanel(Element elem) {
    setElement(elem);
  }

  /**
   * Adds a widget to this panel.
   * 
   * @param w the child widget to be added
   */
  public void add(Widget w) {
    // Can't add() more than one widget to a SimplePanel.
    if (getWidget() != null) {
      throw new IllegalStateException("SimplePanel can only contain one child widget");
    }
    setWidget(w);
  }

  /**
   * Gets the panel's child widget.
   * 
   * @return the child widget, or <code>null</code> if none is present
   */
  public Widget getWidget() {
    return widget;
  }

  public Iterator iterator() {
    // Return a simple iterator that enumerates the 0 or 1 elements in this
    // panel.
    return new Iterator() {
      boolean hasElement = widget != null;
      Widget returned = null;

      public boolean hasNext() {
        return hasElement;
      }

      public Object next() {
        if (!hasElement || (widget == null)) {
          throw new NoSuchElementException();
        }
        hasElement = false;
        return (returned = widget);
      }

      public void remove() {
        if (returned != null) {
          SimplePanel.this.remove(returned);
        }
      }
    };
  }

  public boolean remove(Widget w) {
    if (widget == w) {
      disown(w);
      widget = null;
      return true;
    }
    return false;
  }

  /**
   * Sets this panel's widget. Any existing child widget will be removed.
   * 
   * @param w the panel's new widget (<code>null</code> will clear the panel)
   */
  public void setWidget(Widget w) {
    // If there is already a widget attached, remove it.
    if (widget != null) {
      disown(widget);
    }

    if (w != null) {
      // Adopt the child.
      adopt(w, getContainerElement());
    }

    widget = w;
  }

  /**
   * Override this method to specify that an element other than the root element
   * be the container for the panel's child widget. This can be useful when you
   * want to create a simple panel that decorates its contents.
   * 
   * @return the element to be used as the panel's container
   */
  protected Element getContainerElement() {
    return getElement();
  }
}
