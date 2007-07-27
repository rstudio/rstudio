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

/**
 * Abstract base class for all panels, which are widgets that can contain other
 * widgets.
 */
public abstract class Panel extends Widget implements HasWidgets {

  public void add(Widget w) {
    throw new UnsupportedOperationException("This panel does not support no-arg add()");
  }

  public void clear() {
    Iterator it = iterator();
    while (it.hasNext()) {
      it.next();
      it.remove();
    }
  }

  /**
   * This method must be called as part of the add method of any panel. It
   * ensures that the Widget's parent is set properly, and that it is removed
   * from any existing parent widget. It also attaches the child widget's
   * DOM element to its new container, ensuring that this process occurs in the
   * right order. 
   * 
   * @param w the widget to be adopted
   * @param container the element within which it will be contained
   */
  protected void adopt(Widget w, Element container) {
    // Remove the widget from its current parent, if any.
    w.removeFromParent();

    // Attach it at the DOM and GWT levels.
    if (container != null) {
      DOM.appendChild(container, w.getElement());
    }
    w.setParent(this);
  }

  /**
   * This method must be called whenever a Widget is removed. It ensures that
   * the Widget's parent is cleared. It also detaches the Widget's DOM element
   * from its container, ensuring that this process occurs in the right order.
   * 
   * @param w the widget to be disowned
   */
  protected void disown(Widget w) {
    // Only disown it if it's actually contained in this panel.
    if (w.getParent() != this) {
      throw new IllegalArgumentException("w is not a child of this panel");
    }

    // setParent() must be called before removeChild() to ensure that the
    // element is still attached when onDetach()/onUnload() are called.
    Element elem = w.getElement();
    w.setParent(null);
    DOM.removeChild(DOM.getParent(elem), elem);
  }

  protected void doAttachChildren() {
    // Ensure that all child widgets are attached.
    for (Iterator it = iterator(); it.hasNext();) {
      Widget child = (Widget) it.next();
      child.onAttach();
    }
  }

  protected void doDetachChildren() {
    // Ensure that all child widgets are detached.
    for (Iterator it = iterator(); it.hasNext();) {
      Widget child = (Widget) it.next();
      child.onDetach();
    }
  }

  /**
   * A Panel's onLoad method will be called after all of its children are
   * attached.
   * 
   * @see Widget#onLoad()
   */
  protected void onLoad() {
  }

  /**
   * A Panel's onUnload method will be called before its children become
   * detached themselves.
   * 
   * @see Widget#onLoad()
   */
  protected void onUnload() {
  }
}
