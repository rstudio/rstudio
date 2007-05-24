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

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A standard hierarchical tree widget. The tree contains a hierarchy of
 * {@link com.google.gwt.user.client.ui.TreeItem TreeItems} that the user can
 * open, close, and select.
 * <p>
 * <img class='gallery' src='Tree.png'/>
 * </p>
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-Tree { the tree itself }</li>
 * <li>.gwt-Tree .gwt-TreeItem { a tree item }</li>
 * <li>.gwt-Tree .gwt-TreeItem-selected { a selected tree item }</li>
 * </ul>
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.TreeExample}
 * </p>
 */
public class Tree extends Widget implements HasWidgets, SourcesTreeEvents,
    HasFocus {

  /**
   * Provides images to support the the deprecated case where a url prefix is
   * passed in through {@link Tree#setImageBase(String)}. This class is used
   * in such a way that it will be completely removed by the compiler if the
   * deprecated methods, {@link Tree#setImageBase(String)} and
   * {@link Tree#getImageBase()}, are not called.
   */
  private static class ImagesFromImageBase implements TreeImages {
    /**
     * A convience image prototype that implements
     * {@link AbstractImagePrototype#applyTo(Image)} for a specified image
     * name.
     */
    private class Prototype extends AbstractImagePrototype {
      private final String imageUrl;

      Prototype(String url) {
        imageUrl = url;
      }

      public void applyTo(Image image) {
        image.setUrl(baseUrl + imageUrl);
      }

      public Image createImage() {
        // NOTE: This class is only used internally and, therefore only needs
        // to support applyTo(Image).
        throw new UnsupportedOperationException("createImage is unsupported.");
      }

      public String getHTML() {
        // NOTE: This class is only used internally and, therefore only needs
        // to support applyTo(Image).
        throw new UnsupportedOperationException("getHTML is unsupported.");
      }
    }

    private final String baseUrl;

    ImagesFromImageBase(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    public AbstractImagePrototype treeClosed() {
      return new Prototype("tree_closed.gif");
    }
    
    public AbstractImagePrototype treeLeaf() {
      return new Prototype("tree_white.gif");
    }

    public AbstractImagePrototype treeOpen() {
      return new Prototype("tree_open.gif");
    }

    String getBaseUrl() {
      return baseUrl;
    }
  }

  /**
   * Map of TreeItem.ContentPanel --> Tree Items.
   */
  private final Set childWidgets = new HashSet();
  private TreeItem curSelection;
  private final Element focusable;
  private FocusListenerCollection focusListeners;
  private TreeImages images;
  private KeyboardListenerCollection keyboardListeners;
  private TreeListenerCollection listeners;
  private MouseListenerCollection mouseListeners = null;
  private final TreeItem root;

  /**
   * Keeps track of the last event type seen. We do this to determine if we have
   * a duplicate key down.
   */
  private int lastEventType;

  /**
   * Constructs an empty tree.
   */
  public Tree() {
    this((TreeImages) GWT.create(TreeImages.class));
  }

  /**
   * Constructs a tree that uses the specified image bundle for images.
   * 
   * @param images a bundle that provides tree specific images
   */
  public Tree(TreeImages images) {
    this.images = images;
    setElement(DOM.createDiv());
    DOM.setStyleAttribute(getElement(), "position", "relative");
    focusable = FocusPanel.impl.createFocusable();
    DOM.setStyleAttribute(focusable, "fontSize", "0");
    DOM.setStyleAttribute(focusable, "position", "absolute");
    DOM.setIntStyleAttribute(focusable, "zIndex", -1);
    DOM.appendChild(getElement(), focusable);

    sinkEvents(Event.MOUSEEVENTS | Event.ONCLICK | Event.KEYEVENTS);
    DOM.sinkEvents(focusable, Event.FOCUSEVENTS);

    // The 'root' item is invisible and serves only as a container
    // for all top-level items.
    root = new TreeItem() {
      public void addItem(TreeItem item) {
        // If this element already belongs to a tree or tree item, remove it.
        if ((item.getParentItem() != null) || (item.getTree() != null)) {
          item.remove();
        }
        item.setTree(this.getTree());

        // Explicitly set top-level items' parents to null.
        item.setParentItem(null);
        getChildren().add(item);

        // Use no margin on top-most items.
        DOM.setIntStyleAttribute(item.getElement(), "marginLeft", 0);
      }

      public void removeItem(TreeItem item) {
        if (!getChildren().contains(item)) {
          return;
        }
        // Update Item state.
        item.setTree(null);
        item.setParentItem(null);
        getChildren().remove(item);
      }
    };
    root.setTree(this);
    setStyleName("gwt-Tree");
  }

  /**
   * Adds the widget as a root tree item.
   * 
   * @see com.google.gwt.user.client.ui.HasWidgets#add(com.google.gwt.user.client.ui.Widget)
   * @param widget widget to add.
   */
  public void add(Widget widget) {
    addItem(widget);
  }

  public void addFocusListener(FocusListener listener) {
    if (focusListeners == null) {
      focusListeners = new FocusListenerCollection();
    }
    focusListeners.add(listener);
  }

  /**
   * Adds a simple tree item containing the specified text.
   * 
   * @param itemText the text of the item to be added
   * @return the item that was added
   */
  public TreeItem addItem(String itemText) {
    TreeItem ret = new TreeItem(itemText);
    addItem(ret);

    return ret;
  }

  /**
   * Adds an item to the root level of this tree.
   * 
   * @param item the item to be added
   */
  public void addItem(TreeItem item) {
    root.addItem(item);
    DOM.appendChild(getElement(), item.getElement());
  }

  /**
   * Adds a new tree item containing the specified widget.
   * 
   * @param widget the widget to be added
   */
  public TreeItem addItem(Widget widget) {
    TreeItem item = root.addItem(widget);
    DOM.appendChild(getElement(), item.getElement());
    return item;
  }

  public void addKeyboardListener(KeyboardListener listener) {
    if (keyboardListeners == null) {
      keyboardListeners = new KeyboardListenerCollection();
    }
    keyboardListeners.add(listener);
  }

  public void addMouseListener(MouseListener listener) {
    if (mouseListeners == null) {
      mouseListeners = new MouseListenerCollection();
    }
    mouseListeners.add(listener);
  }

  public void addTreeListener(TreeListener listener) {
    if (listeners == null) {
      listeners = new TreeListenerCollection();
    }
    listeners.add(listener);
  }

  /**
   * Clears all tree items from the current tree.
   */
  public void clear() {
    int size = root.getChildCount();
    for (int i = size - 1; i >= 0; i--) {
      root.getChild(i).remove();
    }
  }

  /**
   * Ensures that the currently-selected item is visible, opening its parents
   * and scrolling the tree as necessary.
   */
  public void ensureSelectedItemVisible() {
    if (curSelection == null) {
      return;
    }

    TreeItem parent = curSelection.getParentItem();
    while (parent != null) {
      parent.setState(true);
      parent = parent.getParentItem();
    }
  }

  /**
   * Gets this tree's default image package.
   * 
   * @return the tree's image package
   * @see #setImageBase
   * @deprecated Use {@link #Tree(TreeImages)} as it provides a more efficent
   *             and manageable way to supply a set of images to be used within
   *             a tree.
   */
  public String getImageBase() {
    return (images instanceof ImagesFromImageBase)
        ? ((ImagesFromImageBase) images).getBaseUrl() : GWT.getModuleBaseURL();
  }

  /**
   * Gets the top-level tree item at the specified index.
   * 
   * @param index the index to be retrieved
   * @return the item at that index
   */
  public TreeItem getItem(int index) {
    return root.getChild(index);
  }

  /**
   * Gets the number of items contained at the root of this tree.
   * 
   * @return this tree's item count
   */
  public int getItemCount() {
    return root.getChildCount();
  }

  /**
   * Gets the currently selected item.
   * 
   * @return the selected item
   */
  public TreeItem getSelectedItem() {
    return curSelection;
  }

  public int getTabIndex() {
    return FocusPanel.impl.getTabIndex(focusable);
  }

  /**
   * Returns all <code>TreeItem.ContentPanel</code> elements contained within
   * this tree. The existence of the <code>TreeItem.ContentPanel</code> class
   * is an implementation detail that may or may not be preserved in future
   * versions of Tree.
   */
  public Iterator iterator() {
    return getChildWidgets().iterator();
  }

  public void onBrowserEvent(Event event) {
    int eventType = DOM.eventGetType(event);
    switch (eventType) {
      case Event.ONCLICK: {
        Element e = DOM.eventGetTarget(event);
        if (shouldTreeDelegateFocusToElement(e)) {
          // The click event should have given focus to this element already.
          // Avoid moving focus back up to the tree (so that focusable widgets
          // attached to TreeItems can receive keyboard events).
        } else {
          setFocus(true);
        }
        break;
      }
      case Event.ONMOUSEDOWN: {
        if (mouseListeners != null) {
          mouseListeners.fireMouseEvent(this, event);
        }
        elementClicked(root, DOM.eventGetTarget(event));
        break;
      }

      case Event.ONMOUSEUP: {
        if (mouseListeners != null) {
          mouseListeners.fireMouseEvent(this, event);
        }
        break;
      }

      case Event.ONMOUSEMOVE: {
        if (mouseListeners != null) {
          mouseListeners.fireMouseEvent(this, event);
        }
        break;
      }

      case Event.ONMOUSEOVER: {
        if (mouseListeners != null) {
          mouseListeners.fireMouseEvent(this, event);
        }
        break;
      }

      case Event.ONMOUSEOUT: {
        if (mouseListeners != null) {
          mouseListeners.fireMouseEvent(this, event);
        }
        break;
      }

      case Event.ONFOCUS:
        // If we already have focus, ignore the focus event.
        if (focusListeners != null) {
          focusListeners.fireFocusEvent(this, event);
        }
        break;

      case Event.ONBLUR: {
        if (focusListeners != null) {
          focusListeners.fireFocusEvent(this, event);
        }

        break;
      }

      case Event.ONKEYDOWN:
        // If nothing's selected, select the first item.
        if (curSelection == null) {
          if (root.getChildCount() > 0) {
            onSelection(root.getChild(0), true, true);
          }
          super.onBrowserEvent(event);
          return;
        }

        if (lastEventType == Event.ONKEYDOWN) {
          // Two key downs in a row signal a duplicate event. Multiple key
          // downs can be triggered in the current configuration independent
          // of the browser.
          return;
        }

        // Handle keyboard events if keyboard navigation is enabled
        if (isKeyboardNavigationEnabled(curSelection)) {
          switch (DOM.eventGetKeyCode(event)) {
            case KeyboardListener.KEY_UP: {
              moveSelectionUp(curSelection);
              DOM.eventPreventDefault(event);
              break;
            }
            case KeyboardListener.KEY_DOWN: {
              moveSelectionDown(curSelection, true);
              DOM.eventPreventDefault(event);
              break;
            }
            case KeyboardListener.KEY_LEFT: {
              if (curSelection.getState()) {
                curSelection.setState(false);
              } else {
                TreeItem parent = curSelection.getParentItem();
                if (parent != null) {
                  setSelectedItem(parent);
                }
              }
              DOM.eventPreventDefault(event);
              break;
            }
            case KeyboardListener.KEY_RIGHT: {
              if (!curSelection.getState()) {
                curSelection.setState(true);
              } else if (curSelection.getChildCount() > 0) {
                setSelectedItem(curSelection.getChild(0));
              }
              DOM.eventPreventDefault(event);
              break;
            }
          }
        }

        // Intentional fallthrough.
      case Event.ONKEYUP:
        if (eventType == Event.ONKEYUP) {
          // If we got here because of a key tab, then we need to make sure the
          // current tree item is selected.
          if (DOM.eventGetKeyCode(event) == KeyboardListener.KEY_TAB) {
            ArrayList chain = new ArrayList();
            collectElementChain(chain, getElement(), DOM.eventGetTarget(event));
            TreeItem item = findItemByChain(chain, 0, root);
            if (item != getSelectedItem()) {
              setSelectedItem(item, true);
            }
          }
        }

        // Intentional fallthrough.
      case Event.ONKEYPRESS: {
        if (keyboardListeners != null) {
          keyboardListeners.fireKeyboardEvent(this, event);
        }
        break;
      }
    }

    // We must call SynthesizedWidget's implementation for all other events.
    super.onBrowserEvent(event);
    lastEventType = eventType;
  }

  public boolean remove(Widget w) {
    throw new UnsupportedOperationException(
        "Widgets should never be directly removed from a tree");
  }

  public void removeFocusListener(FocusListener listener) {
    if (focusListeners != null) {
      focusListeners.remove(listener);
    }
  }

  /**
   * Removes an item from the root level of this tree.
   * 
   * @param item the item to be removed
   */
  public void removeItem(TreeItem item) {
    root.removeItem(item);
    DOM.removeChild(getElement(), item.getElement());
  }

  /**
   * Removes all items from the root level of this tree.
   */
  public void removeItems() {
    while (getItemCount() > 0) {
      removeItem(getItem(0));
    }
  }

  public void removeKeyboardListener(KeyboardListener listener) {
    if (keyboardListeners != null) {
      keyboardListeners.remove(listener);
    }
  }

  public void removeTreeListener(TreeListener listener) {
    if (listeners != null) {
      listeners.remove(listener);
    }
  }

  public void setAccessKey(char key) {
    FocusPanel.impl.setAccessKey(focusable, key);
  }

  public void setFocus(boolean focus) {
    if (focus) {
      FocusPanel.impl.focus(focusable);
    } else {
      FocusPanel.impl.blur(focusable);
    }
  }

  /**
   * Sets the base URL under which this tree will find its default images. These
   * images must be named "tree_white.gif", "tree_open.gif", and
   * "tree_closed.gif".
   * 
   * @param baseUrl
   * @deprecated Use {@link #Tree(TreeImages)} as it provides a more efficent
   *             and manageable way to supply a set of images to be used within
   *             a tree.
   */
  public void setImageBase(String baseUrl) {
    images = new ImagesFromImageBase(baseUrl);
    root.updateStateRecursive();
  }

  /**
   * Selects a specified item.
   * 
   * @param item the item to be selected, or <code>null</code> to deselect all
   *          items
   */
  public void setSelectedItem(TreeItem item) {
    setSelectedItem(item, true);
  }

  /**
   * Selects a specified item.
   * 
   * @param item the item to be selected, or <code>null</code> to deselect all
   *          items
   * @param fireEvents <code>true</code> to allow selection events to be fired
   */
  public void setSelectedItem(TreeItem item, boolean fireEvents) {
    if (item == null) {
      if (curSelection == null) {
        return;
      }
      curSelection.setSelected(false);
      curSelection = null;
      return;
    }

    onSelection(item, fireEvents, true);
  }

  public void setTabIndex(int index) {
    FocusPanel.impl.setTabIndex(focusable, index);
  }

  /**
   * Iterator of tree items.
   */
  public Iterator treeItemIterator() {
    List accum = new ArrayList();
    root.addTreeItems(accum);
    return accum.iterator();
  }

  /**
   * Indicates if keyboard navigation is enabled for the Tree and for a given
   * TreeItem. Subclasses of Tree can override this function to selectively
   * enable or disable keyboard navigation.
   * 
   * @param currentItem the currently selected TreeItem
   * @return <code>true</code> if the Tree will response to arrow keys by
   *         changing the currently selected item
   */
  protected boolean isKeyboardNavigationEnabled(TreeItem currentItem) {
    return true;
  }

  protected void onAttach() {
    super.onAttach();

    // Ensure that all child widgets are attached.
    for (Iterator it = iterator(); it.hasNext();) {
      Widget child = (Widget) it.next();
      child.onAttach();
    }
    DOM.setEventListener(focusable, this);
  }

  protected void onDetach() {
    super.onDetach();

    // Ensure that all child widgets are detached.
    for (Iterator it = iterator(); it.hasNext();) {
      Widget child = (Widget) it.next();
      child.onDetach();
    }
    DOM.setEventListener(focusable, null);
  }

  protected void onLoad() {
    root.updateStateRecursive();
  }

  void adopt(TreeItem.ContentPanel content) {
    getChildWidgets().add(content);
    content.treeSetParent(this);
  }

  void disown(TreeItem.ContentPanel item) {
    getChildWidgets().remove(item);
    item.treeSetParent(null);
  }

  void fireStateChanged(TreeItem item) {
    if (listeners != null) {
      listeners.fireItemStateChanged(item);
    }
  }

  /**
   * Get the Set of child widgets. Exposed only to allow unit tests to validate
   * tree.
   * 
   * @return the children
   */
  Set getChildWidgets() {
    return childWidgets;
  }

  TreeImages getImages() {
    return images;
  }

  /**
   * Collects parents going up the element tree, terminated at the tree root.
   */
  private void collectElementChain(ArrayList chain, Element hRoot, Element hElem) {
    if ((hElem == null) || DOM.compare(hElem, hRoot)) {
      return;
    }

    collectElementChain(chain, hRoot, DOM.getParent(hElem));
    chain.add(hElem);
  }

  private boolean elementClicked(TreeItem root, Element hElem) {
    ArrayList chain = new ArrayList();
    collectElementChain(chain, getElement(), hElem);

    TreeItem item = findItemByChain(chain, 0, root);
    if (item != null) {
      if (DOM.isOrHasChild(item.getImageElement(), hElem)) {
        item.setState(!item.getState(), true);
        return true;
      } else if (DOM.isOrHasChild(item.getElement(), hElem)) {
        onSelection(item, true, !shouldTreeDelegateFocusToElement(hElem));
        return true;
      }
    }

    return false;
  }

  private TreeItem findDeepestOpenChild(TreeItem item) {
    if (!item.getState()) {
      return item;
    }
    return findDeepestOpenChild(item.getChild(item.getChildCount() - 1));
  }

  private TreeItem findItemByChain(ArrayList chain, int idx, TreeItem root) {
    if (idx == chain.size()) {
      return root;
    }

    Element hCurElem = (Element) chain.get(idx);
    for (int i = 0, n = root.getChildCount(); i < n; ++i) {
      TreeItem child = root.getChild(i);
      if (DOM.compare(child.getElement(), hCurElem)) {
        TreeItem retItem = findItemByChain(chain, idx + 1, root.getChild(i));
        if (retItem == null) {
          return child;
        }
        return retItem;
      }
    }

    return findItemByChain(chain, idx + 1, root);
  }

  /**
   * Move the tree focus to the specified selected item.
   * 
   * @param selection
   */
  private void moveFocus(TreeItem selection) {
    HasFocus focusableWidget = selection.getFocusableWidget();
    if (focusableWidget != null) {
      focusableWidget.setFocus(true);
      DOM.scrollIntoView(((Widget) focusableWidget).getElement());
    } else {
      // Get the location and size of the given item's content element relative
      // to the tree.
      Element selectedElem = selection.getContentElem();
      int containerLeft = getAbsoluteLeft();
      int containerTop = getAbsoluteTop();

      int left = DOM.getAbsoluteLeft(selectedElem) - containerLeft;
      int top = DOM.getAbsoluteTop(selectedElem) - containerTop;
      int width = DOM.getElementPropertyInt(selectedElem, "offsetWidth");
      int height = DOM.getElementPropertyInt(selectedElem, "offsetHeight");

      // Set the focusable element's position and size to exactly underlap the
      // item's content element.
      DOM.setIntStyleAttribute(focusable, "left", left);
      DOM.setIntStyleAttribute(focusable, "top", top);
      DOM.setIntStyleAttribute(focusable, "width", width);
      DOM.setIntStyleAttribute(focusable, "height", height);

      // Scroll it into view.
      DOM.scrollIntoView(focusable);

      // Ensure Focus is set, as focus may have been previously delegated by
      // tree.
      FocusPanel.impl.focus(focusable);
    }
  }

  /**
   * Moves to the next item, going into children as if dig is enabled.
   */
  private void moveSelectionDown(TreeItem sel, boolean dig) {
    if (sel == root) {
      return;
    }

    TreeItem parent = sel.getParentItem();
    if (parent == null) {
      parent = root;
    }
    int idx = parent.getChildIndex(sel);

    if (!dig || !sel.getState()) {
      if (idx < parent.getChildCount() - 1) {
        onSelection(parent.getChild(idx + 1), true, true);
      } else {
        moveSelectionDown(parent, false);
      }
    } else if (sel.getChildCount() > 0) {
      onSelection(sel.getChild(0), true, true);
    }
  }

  /**
   * Moves the selected item up one.
   */
  private void moveSelectionUp(TreeItem sel) {
    TreeItem parent = sel.getParentItem();
    if (parent == null) {
      parent = root;
    }
    int idx = parent.getChildIndex(sel);

    if (idx > 0) {
      TreeItem sibling = parent.getChild(idx - 1);
      onSelection(findDeepestOpenChild(sibling), true, true);
    } else {
      onSelection(parent, true, true);
    }
  }

  private void onSelection(TreeItem item, boolean fireEvents, boolean moveFocus) {

    // 'root' isn't a real item, so don't let it be selected
    // (some cases in the keyboard handler will try to do this)
    if (item == root) {
      return;
    }

    if (curSelection != null) {
      curSelection.setSelected(false);
    }

    curSelection = item;

    if (moveFocus && curSelection != null) {
      moveFocus(curSelection);

      // Select the item and fire the selection event.
      curSelection.setSelected(true);
      if (fireEvents && (listeners != null)) {
        listeners.fireItemSelected(curSelection);
      }
    }
  }

  private native boolean shouldTreeDelegateFocusToElement(Element elem) /*-{
    var name = elem.nodeName;
    return ((name == "SELECT") ||
        (name == "INPUT")  ||
        (name == "TEXTAREA") ||
        (name == "OPTION") ||
        (name == "BUTTON") ||
        (name == "LABEL"));
  }-*/;
}
