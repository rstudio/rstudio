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

import java.util.ArrayList;
import java.util.List;

/**
 * An item that can be contained within a
 * {@link com.google.gwt.user.client.ui.Tree}.
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.TreeExample}
 * </p>
 */
public class TreeItem extends UIObject implements HasHTML {

  private ArrayList<TreeItem> children = new ArrayList<TreeItem>();
  private Element itemTable, contentElem, childSpanElem;
  private final Image statusImage = new Image();
  private boolean open;
  private TreeItem parent;
  private boolean selected;
  private Object userObject;
  private Tree tree;
  private Widget widget;

  /**
   * Creates an empty tree item.
   */
  public TreeItem() {
    setElement(DOM.createDiv());
    itemTable = DOM.createTable();
    contentElem = DOM.createSpan();
    childSpanElem = DOM.createSpan();

    // Uses the following Element hierarchy:
    // <div (handle)>
    //  <table (itemElem)>
    //   <tr>
    //    <td><img (imgElem)/></td>
    //    <td><span (contents)/></td>
    //   </tr>
    //  </table>
    //  <span (childSpanElem)> children </span>
    // </div>

    Element tbody = DOM.createTBody(), tr = DOM.createTR();
    Element tdImg = DOM.createTD(), tdContent = DOM.createTD();
    DOM.appendChild(itemTable, tbody);
    DOM.appendChild(tbody, tr);
    DOM.appendChild(tr, tdImg);
    DOM.appendChild(tr, tdContent);
    DOM.setStyleAttribute(tdImg, "verticalAlign", "middle");
    DOM.setStyleAttribute(tdContent, "verticalAlign", "middle");

    DOM.appendChild(getElement(), itemTable);
    DOM.appendChild(getElement(), childSpanElem);
    DOM.appendChild(tdImg, statusImage.getElement());
    DOM.appendChild(tdContent, contentElem);

    DOM.setStyleAttribute(contentElem, "display", "inline");
    DOM.setStyleAttribute(getElement(), "whiteSpace", "nowrap");
    DOM.setStyleAttribute(childSpanElem, "whiteSpace", "nowrap");
    setStyleName(contentElem, "gwt-TreeItem", true);
  }

  /**
   * Constructs a tree item with the given HTML.
   * 
   * @param html the item's HTML
   */
  public TreeItem(String html) {
    this();
    setHTML(html);
  }

  /**
   * Constructs a tree item with the given <code>Widget</code>.
   * 
   * @param widget the item's widget
   */
  public TreeItem(Widget widget) {
    this();
    setWidget(widget);
  }

  /**
   * Adds a child tree item containing the specified text.
   * 
   * @param itemText the text to be added
   * @return the item that was added
   */
  public TreeItem addItem(String itemText) {
    TreeItem ret = new TreeItem(itemText);
    addItem(ret);
    return ret;
  }

  /**
   * Adds another item as a child to this one.
   * 
   * @param item the item to be added
   */

  public void addItem(TreeItem item) {
    // Detach item from existing parent.
    if ((item.getParentItem() != null) || (item.getTree() != null)) {
      item.remove();
    }

    // Logical attach.
    item.setParentItem(this);
    children.add(item);

    // Physical attach.
    DOM.setStyleAttribute(item.getElement(), "marginLeft", "16px");
    DOM.appendChild(childSpanElem, item.getElement());

    // Adopt.
    item.setTree(tree);

    if (children.size() == 1) {
      updateState();
    }
  }

  /**
   * Adds a child tree item containing the specified widget.
   * 
   * @param widget the widget to be added
   * @return the item that was added
   */
  public TreeItem addItem(Widget widget) {
    TreeItem ret = new TreeItem(widget);
    addItem(ret);
    return ret;
  }

  /**
   * Gets the child at the specified index.
   * 
   * @param index the index to be retrieved
   * @return the item at that index
   */

  public TreeItem getChild(int index) {
    if ((index < 0) || (index >= children.size())) {
      return null;
    }

    return children.get(index);
  }

  /**
   * Gets the number of children contained in this item.
   * 
   * @return this item's child count.
   */

  public int getChildCount() {
    return children.size();
  }

  /**
   * Gets the index of the specified child item.
   * 
   * @param child the child item to be found
   * @return the child's index, or <code>-1</code> if none is found
   */

  public int getChildIndex(TreeItem child) {
    return children.indexOf(child);
  }

  public String getHTML() {
    return DOM.getInnerHTML(contentElem);
  }

  /**
   * Gets this item's parent.
   * 
   * @return the parent item
   */
  public TreeItem getParentItem() {
    return parent;
  }

  /**
   * Gets whether this item's children are displayed.
   * 
   * @return <code>true</code> if the item is open
   */
  public boolean getState() {
    return open;
  }

  public String getText() {
    return DOM.getInnerText(contentElem);
  }

  /**
   * Gets the tree that contains this item.
   * 
   * @return the containing tree
   */
  public final Tree getTree() {
    return tree;
  }

  /**
   * Gets the user-defined object associated with this item.
   * 
   * @return the item's user-defined object
   */
  public Object getUserObject() {
    return userObject;
  }

  /**
   * Gets the <code>Widget</code> associated with this tree item.
   * 
   * @return the widget
   */
  public Widget getWidget() {
    return widget;
  }

  /**
   * Determines whether this item is currently selected.
   * 
   * @return <code>true</code> if it is selected
   */
  public boolean isSelected() {
    return selected;
  }

  /**
   * Removes this item from its tree.
   */
  public void remove() {
    if (parent != null) {
      // If this item has a parent, remove self from it.
      parent.removeItem(this);
    } else if (tree != null) {
      // If the item has no parent, but is in the Tree, it must be a top-level
      // element.
      tree.removeItem(this);
    }
  }

  /**
   * Removes one of this item's children.
   * 
   * @param item the item to be removed
   */

  public void removeItem(TreeItem item) {
    // Validate.
    if (!children.contains(item)) {
      return;
    }

    // Orphan.
    item.setTree(null);

    // Physical detach.
    DOM.removeChild(childSpanElem, item.getElement());

    // Logical detach.
    item.setParentItem(null);
    children.remove(item);

    if (children.size() == 0) {
      updateState();
    }
  }

  /**
   * Removes all of this item's children.
   */
  public void removeItems() {
    while (getChildCount() > 0) {
      removeItem(getChild(0));
    }
  }

  public void setHTML(String html) {
    setWidget(null);
    DOM.setInnerHTML(contentElem, html);
  }

  /**
   * Selects or deselects this item.
   * 
   * @param selected <code>true</code> to select the item, <code>false</code>
   *          to deselect it
   */
  public void setSelected(boolean selected) {
    if (this.selected == selected) {
      return;
    }
    this.selected = selected;
    setStyleName(contentElem, "gwt-TreeItem-selected", selected);
  }

  /**
   * Sets whether this item's children are displayed.
   * 
   * @param open whether the item is open
   */
  public void setState(boolean open) {
    setState(open, true);
  }

  /**
   * Sets whether this item's children are displayed.
   * 
   * @param open whether the item is open
   * @param fireEvents <code>true</code> to allow open/close events to be
   *          fired
   */
  public void setState(boolean open, boolean fireEvents) {
    if (open && children.size() == 0) {
      return;
    }

    this.open = open;
    updateState();

    if (fireEvents && tree != null) {
      tree.fireStateChanged(this);
    }
  }

  public void setText(String text) {
    setWidget(null);
    DOM.setInnerText(contentElem, text);
  }

  /**
   * Sets the user-defined object associated with this item.
   * 
   * @param userObj the item's user-defined object
   */
  public void setUserObject(Object userObj) {
    userObject = userObj;
  }

  /**
   * Sets the current widget. Any existing child widget will be removed.
   * 
   * @param newWidget Widget to set
   */
  public void setWidget(Widget newWidget) {
    // Detach new child from old parent.
    if (newWidget != null) {
      newWidget.removeFromParent();
    }

    // Detach old child from tree.
    if (widget != null && tree != null) {
      tree.orphan(widget);
    }

    // Physical detach old from self.
    // Clear out any existing content before adding a widget.
    DOM.setInnerHTML(contentElem, "");

    // Logical detach old/attach new.
    widget = newWidget;

    if (newWidget != null) {
      // Physical attach new.
      DOM.appendChild(contentElem, newWidget.getElement());

      // Attach child to tree.
      if (tree != null) {
        tree.adopt(widget, this);
      }
    }
  }

  /**
   * Returns the widget, if any, that should be focused on if this TreeItem is
   * selected.
   * 
   * @return widget to be focused.
   */
  protected HasFocus getFocusableWidget() {
    Widget w = getWidget();
    if (w instanceof HasFocus) {
      return (HasFocus) w;
    } else {
      return null;
    }
  }

  void addTreeItems(List<TreeItem> accum) {
    for (int i = 0; i < children.size(); i++) {
      TreeItem item = children.get(i);
      accum.add(item);
      item.addTreeItems(accum);
    }
  }

  ArrayList<TreeItem> getChildren() {
    return children;
  }

  Element getContentElem() {
    return contentElem;
  }

  int getContentHeight() {
    return DOM.getElementPropertyInt(itemTable, "offsetHeight");
  }

  Element getImageElement() {
    return statusImage.getElement();
  }

  int getTreeTop() {
    TreeItem item = this;
    int ret = 0;

    while (item != null) {
      ret += DOM.getElementPropertyInt(item.getElement(), "offsetTop");
      item = item.getParentItem();
    }

    return ret;
  }

  void setParentItem(TreeItem parent) {
    this.parent = parent;
  }

  void setTree(Tree newTree) {
    // Early out.
    if (tree == newTree) {
      return;
    }

    // Remove this item from existing tree.
    if (tree != null) {
      if (tree.getSelectedItem() == this) {
        tree.setSelectedItem(null);
      }

      if (widget != null) {
        tree.orphan(widget);
      }
    }

    tree = newTree;
    for (int i = 0, n = children.size(); i < n; ++i) {
      children.get(i).setTree(newTree);
    }
    updateState();

    if (newTree != null) {
      if (widget != null) {
        // Add my widget to the new tree.
        newTree.adopt(widget, this);
      }
    }
  }

  void updateState() {
    // If the tree hasn't been set, there is no visual state to update.
    if (tree == null) {
      return;
    }

    TreeImages images = tree.getImages();

    if (children.size() == 0) {
      UIObject.setVisible(childSpanElem, false);
      images.treeLeaf().applyTo(statusImage);
      return;
    }

    // We must use 'display' rather than 'visibility' here,
    // or the children will always take up space.
    if (open) {
      UIObject.setVisible(childSpanElem, true);
      images.treeOpen().applyTo(statusImage);
    } else {
      UIObject.setVisible(childSpanElem, false);
      images.treeClosed().applyTo(statusImage);
    }
  }

  void updateStateRecursive() {
    updateState();
    for (int i = 0, n = children.size(); i < n; ++i) {
      children.get(i).updateStateRecursive();
    }
  }
}
