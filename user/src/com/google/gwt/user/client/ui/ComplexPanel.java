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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

import java.util.Iterator;

/**
 * Abstract base class for panels that can contain multiple child widgets.
 */
public abstract class ComplexPanel extends Panel implements IndexedPanel.ForIsWidget {

  private WidgetCollection children = new WidgetCollection(this);

  /**
   * The command used to orphan children. 
   */
  private AttachDetachException.Command orphanCommand;

  public Widget getWidget(int index) {
    return getChildren().get(index);
  }

  public int getWidgetCount() {
    return getChildren().size();
  }

  public int getWidgetIndex(Widget child) {
    return getChildren().indexOf(child);
  }
  
  public int getWidgetIndex(IsWidget child) {
    return getWidgetIndex(asWidgetOrNull(child));
  }

  public Iterator<Widget> iterator() {
    return getChildren().iterator();
  }

  public boolean remove(int index) {
    return remove(getWidget(index));
  }

  @Override
  public boolean remove(Widget w) {
    // Validate.
    if (w.getParent() != this) {
      return false;
    }
    // Orphan.
    try {
      orphan(w);
    } finally {
      // Physical detach.
      Element elem = w.getElement();
      DOM.removeChild(DOM.getParent(elem), elem);
  
      // Logical detach.
      getChildren().remove(w);
    }
    return true;
  }

  /**
   * Adds a new child widget to the panel, attaching its Element to the
   * specified container Element.
   * 
   * @param child the child widget to be added
   * @param container the element within which the child will be contained
   */
  protected void add(Widget child, Element container) {
    // Detach new child.
    child.removeFromParent();

    // Logical attach.
    getChildren().add(child);

    // Physical attach.
    DOM.appendChild(container, child.getElement());

    // Adopt.
    adopt(child);
  }

  /**
   * Adjusts beforeIndex to account for the possibility that the given widget is
   * already a child of this panel.
   * 
   * @param child the widget that might be an existing child
   * @param beforeIndex the index at which it will be added to this panel
   * @return the modified index
   */
  protected int adjustIndex(Widget child, int beforeIndex) {
    checkIndexBoundsForInsertion(beforeIndex);

    // Check to see if this widget is already a direct child.
    if (child.getParent() == this) {
      // If the Widget's previous position was left of the desired new position
      // shift the desired position left to reflect the removal
      int idx = getWidgetIndex(child);
      if (idx < beforeIndex) {
        beforeIndex--;
      }
    }

    return beforeIndex;
  }

  /**
   * Checks that <code>index</code> is in the range [0, getWidgetCount()), which
   * is the valid range on accessible indexes.
   * 
   * @param index the index being accessed
   */
  protected void checkIndexBoundsForAccess(int index) {
    if (index < 0 || index >= getWidgetCount()) {
      throw new IndexOutOfBoundsException();
    }
  }

  /**
   * Checks that <code>index</code> is in the range [0, getWidgetCount()], which
   * is the valid range for indexes on an insertion.
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
   * This method was used by subclasses to insert a new child Widget. It is now
   * deprecated because it was ambiguous whether the <code>child</code> should
   * be appended to <code>container</code> element versus inserted into
   * <code>container</code> at <code>beforeIndex</code>. Use
   * {@link #insert(Widget, Element, int, boolean)}, which clarifies this
   * ambiguity.
   * 
   * @deprecated Use {@link #insert(Widget, Element, int, boolean)} instead
   */
  @Deprecated
  protected void insert(Widget child, Element container, int beforeIndex) {
    if (container == null) {
      throw new NullPointerException("container may not be null");
    }
    insert(child, container, beforeIndex, false);
  }

  /**
   * Insert a new child Widget into this Panel at a specified index, attaching
   * its Element to the specified container Element. The child Element will
   * either be attached to the container at the same index, or simply appended
   * to the container, depending on the value of <code>domInsert</code>.
   * 
   * @param child the child Widget to be added
   * @param container the Element within which <code>child</code> will be
   *          contained
   * @param beforeIndex the index before which <code>child</code> will be
   *          inserted
   * @param domInsert if <code>true</code>, insert <code>child</code> into
   *          <code>container</code> at <code>beforeIndex</code>; otherwise
   *          append <code>child</code> to the end of <code>container</code>.
   */
  protected void insert(Widget child, Element container, int beforeIndex,
      boolean domInsert) {
    // Validate index; adjust if the widget is already a child of this panel.
    beforeIndex = adjustIndex(child, beforeIndex);

    // Detach new child.
    child.removeFromParent();

    // Logical attach.
    getChildren().insert(child, beforeIndex);

    // Physical attach.
    if (domInsert) {
      DOM.insertChild(container, child.getElement(), beforeIndex);
    } else {
      DOM.appendChild(container, child.getElement());
    }

    // Adopt.
    adopt(child);
  }

  void doLogicalClear() {
    // TODO(jgw): When Layout work has landed, deprecate FlowPanel (the only
    // caller of this method in our code), and deprecate this method with an eye
    // to making it private down the road.

    // Only use one orphan command per panel to avoid object creation.
    if (orphanCommand == null) {
      orphanCommand = new AttachDetachException.Command() {
        public void execute(Widget w) {
          orphan(w);
        }
      };
    }
    try {
      AttachDetachException.tryCommand(this, orphanCommand);
    } finally {
      children = new WidgetCollection(this);
    }
  }
}
