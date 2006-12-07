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

import java.util.List;
import java.util.Vector;

/**
 * An item that can be contained within a
 * {@link com.google.gwt.user.client.ui.Tree}.
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.TreeExample}
 * </p>
 */
public class TreeItem extends UIObject implements HasHTML {

  /**
   * <code>Panel</code> containing the <code>Widget</code> associated with
   * the current tree item.
   */
  class ContentPanel extends SimplePanel {
    private ContentPanel(Element e) {
      super(e);
    }

    /**
     * Gets the <code>TreeItem</code> associated with this
     * <code>ContentPanel</code> for testing purposes.
     * 
     * @return the tree item
     */
    TreeItem getTreeItem() {
      return TreeItem.this;
    }

    /**
     * Prevent anyone from stealing a tree item's content pane.
     * 
     * @see com.google.gwt.user.client.ui.Widget#setParent(com.google.gwt.user.client.ui.Widget)
     */
    void setParent(Widget widget) {
      throw new UnsupportedOperationException(
        "Cannot directly setParent on a WidgetTreeItem's ContentPanel");
    }

    void treeSetParent(Widget widget) {
      super.setParent(widget);
    }
  }

  private Vector children = new Vector();
  private ContentPanel contentPanel;
  private Element itemTable, contentElem, imgElem, childSpanElem;
  private boolean open;
  private TreeItem parent;
  private boolean selected;
  private Object userObject;
  private Tree tree;

  /**
   * Creates an empty tree item.
   */
  public TreeItem() {
    setElement(DOM.createDiv());
    itemTable = DOM.createTable();
    contentElem = DOM.createSpan();
    childSpanElem = DOM.createSpan();
    imgElem = DOM.createImg();

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
    DOM.appendChild(tdImg, imgElem);
    DOM.appendChild(tdContent, contentElem);

    DOM.setAttribute(getElement(), "position", "relative");
    DOM.setStyleAttribute(contentElem, "display", "inline");
    DOM.setStyleAttribute(getElement(), "whiteSpace", "nowrap");
    DOM.setAttribute(itemTable, "whiteSpace", "nowrap");
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
    // If this element already belongs to a tree or tree item, it should be
    // removed.
    if ((item.getParentItem() != null) || (item.getTree() != null)) {
      item.remove();
    }
    item.setTree(tree);
    item.setParentItem(this);
    children.add(item);
    DOM.setStyleAttribute(item.getElement(), "marginLeft", "16px");
    DOM.appendChild(childSpanElem, item.getElement());
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

    return (TreeItem) children.get(index);
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
  public Tree getTree() {
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
    if (contentPanel == null) {
      return null;
    }
    return contentPanel.getWidget();
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
    if (!children.contains(item)) {
      return;
    }
    // Update Item state.
    item.setTree(null);
    item.setParentItem(null);
    
    children.remove(item);
    DOM.removeChild(childSpanElem, item.getElement());

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
    clearContentPanel();
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

    if (fireEvents) {
      tree.fireStateChanged(this);
    }
  }

  public void setText(String text) {
    clearContentPanel();
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
   * @param widget Widget to set
   */
  public void setWidget(Widget widget) {
    ensureContentPanel();
    contentPanel.setWidget(widget);
  }

  /**
   * Returns the widget, if any, that should be focused on if this TreeItem is
   * selected.
   * 
   * @return widget to be focused.
   */
  protected HasFocus getFocusableWidget() {
    Widget widget = getWidget();
    if (widget instanceof HasFocus) {
      return (HasFocus)widget;
    } else {
      return null;
    }
   }

  void addTreeItems(List accum) {
    for (int i = 0; i < children.size(); i++) {
      TreeItem item = (TreeItem) children.get(i);
      accum.add(item);
      item.addTreeItems(accum);
    }
  }

  Vector getChildren() {
    return children;
  }

  Element getContentElem() {
    return contentElem;
  }

  int getContentHeight() {
    return DOM.getIntAttribute(itemTable, "offsetHeight");
  }

  Element getImageElement() {
    return imgElem;
  }

  int getTreeTop() {
    TreeItem item = this;
    int ret = 0;

    while (item != null) {
      ret += DOM.getIntAttribute(item.getElement(), "offsetTop");
      item = item.getParentItem();
    }

    return ret;
  }

  String imgSrc(String img) {
    if (tree == null) {
      return img;
    }
    String src = tree.getImageBase() + img;
    return src;
  }

  void setParentItem(TreeItem parent) {
    this.parent = parent;
  }

  void setTree(Tree tree) {
    if (this.tree == tree) {
      return;
    }

    if (this.tree != null) {
      if (this.tree.getSelectedItem() == this) {
        this.tree.setSelectedItem(null);
      }
 
      // Detach contentPanel from old tree.
      if (contentPanel != null) {
        this.tree.disown(contentPanel);
      }
    }
    this.tree = tree;
    for (int i = 0, n = children.size(); i < n; ++i) {
      ((TreeItem) children.get(i)).setTree(tree);
    }
    updateState();
    if (tree != null) {
      if (contentPanel != null) {
        tree.adopt(contentPanel);
      }
    }
  }

  void updateState() {
    if (children.size() == 0) {
      UIObject.setVisible(childSpanElem, false);
      DOM.setAttribute(imgElem, "src", imgSrc("tree_white.gif"));
      return;
    }

    // We must use 'display' rather than 'visibility' here,
    // or the children will always take up space.
    if (open) {
      UIObject.setVisible(childSpanElem, true);
      DOM.setAttribute(imgElem, "src", imgSrc("tree_open.gif"));
    } else {
      UIObject.setVisible(childSpanElem, false);
      DOM.setAttribute(imgElem, "src", imgSrc("tree_closed.gif"));
    }
  }

  void updateStateRecursive() {
    updateState();
    for (int i = 0, n = children.size(); i < n; ++i) {
      ((TreeItem) children.get(i)).updateStateRecursive();
    }
  }

  private void clearContentPanel() {
    if (contentPanel != null) {
      // Child should not be owned by anyone anymore.
      Widget child = contentPanel.getWidget();
      if (contentPanel.getWidget() != null) {
        contentPanel.remove(child);
      }

      // Tree should no longer own contentPanel.
      if (tree != null) {
        tree.disown(contentPanel);
        contentPanel = null;
      }
    }
  }

  private void ensureContentPanel() {
    if (contentPanel == null) {
      // Ensure contentElem is empty.
      DOM.setInnerHTML(contentElem, "");
      contentPanel = new ContentPanel(contentElem);
      if (getTree() != null) {
        tree.adopt(contentPanel);
      }
    }
  }

}
