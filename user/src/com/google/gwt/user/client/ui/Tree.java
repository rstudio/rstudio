/*
 * Copyright 2008 Google Inc.
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
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasAllFocusHandlers;
import com.google.gwt.event.dom.client.HasAllKeyHandlers;
import com.google.gwt.event.dom.client.HasAllMouseHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.MouseWheelEvent;
import com.google.gwt.event.dom.client.MouseWheelHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.event.logical.shared.HasOpenHandlers;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.AbstractImagePrototype.ImagePrototypeElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A standard hierarchical tree widget. The tree contains a hierarchy of
 * {@link com.google.gwt.user.client.ui.TreeItem TreeItems} that the user can
 * open, close, and select.
 * <p>
 * <img class='gallery' src='Tree.png'/>
 * </p>
 * <h3>CSS Style Rules</h3>
 * <dl>
 * <dt>.gwt-Tree</dt>
 * <dd>the tree itself</dd>
 * <dt>.gwt-Tree .gwt-TreeItem</dt>
 * <dd>a tree item</dd>
 * <dt>.gwt-Tree .gwt-TreeItem-selected</dt>
 * <dd>a selected tree item</dd>
 * </dl>
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.TreeExample}
 * </p>
 */
@SuppressWarnings("deprecation")
public class Tree extends Widget implements HasWidgets, SourcesTreeEvents,
    HasFocus, HasAnimation, HasAllKeyHandlers, HasAllFocusHandlers,
    HasSelectionHandlers<TreeItem>, HasOpenHandlers<TreeItem>,
    HasCloseHandlers<TreeItem>, SourcesMouseEvents, HasAllMouseHandlers {

  /**
   * Provides images to support the the deprecated case where a url prefix is
   * passed in through {@link Tree#setImageBase(String)}. This class is used in
   * such a way that it will be completely removed by the compiler if the
   * deprecated methods, {@link Tree#setImageBase(String)} and
   * {@link Tree#getImageBase()}, are not called.
   */
  private static class ImagesFromImageBase implements TreeImages {

    /**
     * A convenience image prototype that implements
     * {@link AbstractImagePrototype#applyTo(Image)} for a specified image name.
     */
    private class Prototype extends AbstractImagePrototype {
      private final String imageUrl;

      Prototype(String url) {
        imageUrl = url;
      }

      @Override
      public void applyTo(Image image) {
        image.setUrl(baseUrl + imageUrl);
      }

      @Override
      public void applyTo(ImagePrototypeElement img) {
        DOM.setImgSrc(img.<Element> cast(), imageUrl);
      }

      @Override
      public ImagePrototypeElement createElement() {
        Element img = DOM.createImg();
        applyTo(img.<ImagePrototypeElement> cast());
        return img.cast();
      }

      @Override
      public Image createImage() {
        // NOTE: This class is only used internally and, therefore only needs
        // to support applyTo(Image).
        throw new UnsupportedOperationException("createImage is unsupported.");
      }

      @Override
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

  private static final int OTHER_KEY_DOWN = 63233;
  private static final int OTHER_KEY_LEFT = 63234;
  private static final int OTHER_KEY_RIGHT = 63235;
  private static final int OTHER_KEY_UP = 63232;

  static native boolean shouldTreeDelegateFocusToElement(Element elem) /*-{
    var name = elem.nodeName;
    return ((name == "SELECT") ||
        (name == "INPUT")  ||
        (name == "TEXTAREA") ||
        (name == "OPTION") ||
        (name == "BUTTON") ||
        (name == "LABEL"));
  }-*/;

  private static boolean isArrowKey(int code) {
    switch (code) {
      case OTHER_KEY_DOWN:
      case OTHER_KEY_RIGHT:
      case OTHER_KEY_UP:
      case OTHER_KEY_LEFT:
      case KeyCodes.KEY_DOWN:
      case KeyCodes.KEY_RIGHT:
      case KeyCodes.KEY_UP:
      case KeyCodes.KEY_LEFT:
        return true;
      default:
        return false;
    }
  }

  /**
   * Normalized key codes. Also switches KEY_RIGHT and KEY_LEFT in RTL
   * languages.
   */
  private static int standardizeKeycode(int code) {
    switch (code) {
      case OTHER_KEY_DOWN:
        code = KeyCodes.KEY_DOWN;
        break;
      case OTHER_KEY_RIGHT:
        code = KeyCodes.KEY_RIGHT;
        break;
      case OTHER_KEY_UP:
        code = KeyCodes.KEY_UP;
        break;
      case OTHER_KEY_LEFT:
        code = KeyCodes.KEY_LEFT;
        break;
    }
    if (LocaleInfo.getCurrentLocale().isRTL()) {
      if (code == KeyCodes.KEY_RIGHT) {
        code = KeyCodes.KEY_LEFT;
      } else if (code == KeyCodes.KEY_LEFT) {
        code = KeyCodes.KEY_RIGHT;
      }
    }
    return code;
  }

  /**
   * Map of TreeItem.widget -> TreeItem.
   */
  private final Map<Widget, TreeItem> childWidgets = new HashMap<Widget, TreeItem>();

  private TreeItem curSelection;

  private Element focusable;

  private TreeImages images;

  private String indentValue;

  private boolean isAnimationEnabled = false;

  private boolean lastWasKeyDown;

  private TreeItem root;

  private boolean useLeafImages;

  /**
   * Constructs an empty tree.
   */
  public Tree() {
    if (LocaleInfo.getCurrentLocale().isRTL()) {
      init(GWT.<TreeImagesRTL> create(TreeImagesRTL.class), false);
    } else {
      init(GWT.<TreeImages> create(TreeImages.class), false);
    }
  }

  /**
   * Constructs a tree that uses the specified image bundle for images.
   * 
   * @param images a bundle that provides tree specific images
   */
  public Tree(TreeImages images) {
    init(images, true);
  }

  /**
   * Constructs a tree that uses the specified image bundle for images. If this
   * tree does not use leaf images, the width of the TreeImage's leaf image will
   * control the leaf indent.
   * 
   * @param images a bundle that provides tree specific images
   * @param useLeafImages use leaf images from bundle
   */
  public Tree(TreeImages images, boolean useLeafImages) {
    init(images, useLeafImages);
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

  public HandlerRegistration addBlurHandler(BlurHandler handler) {
    return addDomHandler(handler, BlurEvent.getType());
  }

  public HandlerRegistration addCloseHandler(CloseHandler<TreeItem> handler) {
    return addHandler(handler, CloseEvent.getType());
  }

  public HandlerRegistration addFocusHandler(FocusHandler handler) {
    return addDomHandler(handler, FocusEvent.getType());
  }

  /**
   * @deprecated Use {@link #addFocusHandler} instead
   */
  @Deprecated
  public void addFocusListener(FocusListener listener) {
    ListenerWrapper.WrappedFocusListener.add(this, listener);
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
  }

  /**
   * Adds a new tree item containing the specified widget.
   * 
   * @param widget the widget to be added
   * @return the new item
   */
  public TreeItem addItem(Widget widget) {
    return root.addItem(widget);
  }

  /**
   * @deprecated Use {@link #addKeyDownHandler}, {@link
   * #addKeyUpHandler} and {@link #addKeyPressHandler} instead
   */
  @Deprecated
  public void addKeyboardListener(KeyboardListener listener) {
    ListenerWrapper.WrappedKeyboardListener.add(this, listener);
  }

  public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
    return addDomHandler(handler, KeyDownEvent.getType());
  }

  public HandlerRegistration addKeyPressHandler(KeyPressHandler handler) {
    return addDomHandler(handler, KeyPressEvent.getType());
  }

  public HandlerRegistration addKeyUpHandler(KeyUpHandler handler) {
    return addDomHandler(handler, KeyUpEvent.getType());
  }

  public HandlerRegistration addMouseDownHandler(MouseDownHandler handler) {
    return addHandler(handler, MouseDownEvent.getType());
  }

  /**
   * @deprecated Use {@link #addMouseOverHandler} {@link
   * #addMouseMoveHandler}, {@link #addMouseDownHandler}, {@link
   * #addMouseUpHandler} and {@link #addMouseOutHandler} instead
   */
  @Deprecated
  public void addMouseListener(MouseListener listener) {
    ListenerWrapper.WrappedMouseListener.add(this, listener);
  }

  public HandlerRegistration addMouseMoveHandler(MouseMoveHandler handler) {
    return addDomHandler(handler, MouseMoveEvent.getType());
  }

  public HandlerRegistration addMouseOutHandler(MouseOutHandler handler) {
    return addDomHandler(handler, MouseOutEvent.getType());
  }

  public HandlerRegistration addMouseOverHandler(MouseOverHandler handler) {
    return addDomHandler(handler, MouseOverEvent.getType());
  }

  public HandlerRegistration addMouseUpHandler(MouseUpHandler handler) {
    return addDomHandler(handler, MouseUpEvent.getType());
  }

  public HandlerRegistration addMouseWheelHandler(MouseWheelHandler handler) {
    return addDomHandler(handler, MouseWheelEvent.getType());
  }

  public final HandlerRegistration addOpenHandler(OpenHandler<TreeItem> handler) {
    return addHandler(handler, OpenEvent.getType());
  }

  public HandlerRegistration addSelectionHandler(
      SelectionHandler<TreeItem> handler) {
    return addHandler(handler, SelectionEvent.getType());
  }

  /**
   * @deprecated Use {@link #addSelectionHandler}, {@link
   * #addOpenHandler}, and {@link #addCloseHandler} instead
   */
  @Deprecated
  public void addTreeListener(TreeListener listener) {
    ListenerWrapper.WrappedTreeListener.add(this, listener);
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
   * @deprecated Use {@link #Tree(TreeImages)} as it provides a more efficient
   *             and manageable way to supply a set of images to be used within
   *             a tree
   */
  @Deprecated
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

  public boolean isAnimationEnabled() {
    return isAnimationEnabled;
  }

  public Iterator<Widget> iterator() {
    final Widget[] widgets = new Widget[childWidgets.size()];
    childWidgets.keySet().toArray(widgets);
    return WidgetIterators.createWidgetIterator(this, widgets);
  }

  @Override
  @SuppressWarnings("fallthrough")
  public void onBrowserEvent(Event event) {
    int eventType = DOM.eventGetType(event);

    switch (eventType) {
      case Event.ONKEYDOWN: {
        // If nothing's selected, select the first item.
        if (curSelection == null) {
          if (root.getChildCount() > 0) {
            onSelection(root.getChild(0), true, true);
          }
          super.onBrowserEvent(event);
          return;
        }
      }

        // Intentional fallthrough.
      case Event.ONKEYPRESS:
      case Event.ONKEYUP:
        // Issue 1890: Do not block history navigation via alt+left/right
        if (DOM.eventGetAltKey(event) || DOM.eventGetMetaKey(event)) {
          super.onBrowserEvent(event);
          return;
        }
        break;
    }

    switch (eventType) {
      case Event.ONCLICK: {
        Element e = DOM.eventGetTarget(event);
        if (shouldTreeDelegateFocusToElement(e)) {
          // The click event should have given focus to this element already.
          // Avoid moving focus back up to the tree (so that focusable widgets
          // attached to TreeItems can receive keyboard events).
        } else if (curSelection != null) {
          setFocus(true);
        }
        break;
      }

      case Event.ONMOUSEDOWN: {
        // Currently, the way we're using image bundles causes extraneous events
        // to be sunk on individual items' open/close images. This leads to an
        // extra event reaching the Tree, which we will ignore here.
        if (DOM.eventGetCurrentTarget(event) == getElement()) {
          elementClicked(DOM.eventGetTarget(event));
        }
        break;
      }
      case Event.ONKEYDOWN: {
        keyboardNavigation(event);
        lastWasKeyDown = true;
        break;
      }

      case Event.ONKEYPRESS: {
        if (!lastWasKeyDown) {
          keyboardNavigation(event);
        }
        lastWasKeyDown = false;
        break;
      }

      case Event.ONKEYUP: {
        if (DOM.eventGetKeyCode(event) == KeyCodes.KEY_TAB) {
          ArrayList<Element> chain = new ArrayList<Element>();
          collectElementChain(chain, getElement(), DOM.eventGetTarget(event));
          TreeItem item = findItemByChain(chain, 0, root);
          if (item != getSelectedItem()) {
            setSelectedItem(item, true);
          }
        }
        lastWasKeyDown = false;
        break;
      }
    }

    switch (eventType) {
      case Event.ONKEYDOWN:
      case Event.ONKEYUP: {
        if (isArrowKey(DOM.eventGetKeyCode(event))) {
          DOM.eventCancelBubble(event, true);
          DOM.eventPreventDefault(event);
          return;
        }
      }
    }

    // We must call super for all handlers.
    super.onBrowserEvent(event);
  }

  public boolean remove(Widget w) {
    // Validate.
    TreeItem item = childWidgets.get(w);
    if (item == null) {
      return false;
    }

    // Delegate to TreeItem.setWidget, which performs correct removal.
    item.setWidget(null);
    return true;
  }

  /**
   * @deprecated Use the {@link HandlerRegistration#removeHandler} method on 
   * the object returned by {@link #addFocusHandler} instead
   */
  @Deprecated
  public void removeFocusListener(FocusListener listener) {
    ListenerWrapper.WrappedFocusListener.remove(this, listener);
  }

  /**
   * Removes an item from the root level of this tree.
   * 
   * @param item the item to be removed
   */
  public void removeItem(TreeItem item) {
    root.removeItem(item);
  }

  /**
   * Removes all items from the root level of this tree.
   */
  public void removeItems() {
    while (getItemCount() > 0) {
      removeItem(getItem(0));
    }
  }

  /**
   * @deprecated Use the {@link HandlerRegistration#removeHandler}
   * method on the object returned by an add*Handler method instead
   */
  @Deprecated
  public void removeKeyboardListener(KeyboardListener listener) {
    ListenerWrapper.WrappedKeyboardListener.remove(this, listener);
  }

  /**
   * @deprecated Use the {@link HandlerRegistration#removeHandler}
   * method on the object returned by an add*Handler method instead
   */
  @Deprecated
  public void removeMouseListener(MouseListener listener) {
    ListenerWrapper.WrappedMouseListener.remove(this, listener);
  }

  /**
   * @deprecated Use the {@link HandlerRegistration#removeHandler}
   * method on the object returned by an add*Handler method instead
   */
  @Deprecated
  public void removeTreeListener(TreeListener listener) {
    ListenerWrapper.WrappedTreeListener.remove(this, listener);
  }

  public void setAccessKey(char key) {
    FocusPanel.impl.setAccessKey(focusable, key);
  }

  public void setAnimationEnabled(boolean enable) {
    isAnimationEnabled = enable;
  }

  public void setFocus(boolean focus) {
    if (focus) {
      FocusPanel.impl.focus(focusable);
    } else {
      FocusPanel.impl.blur(focusable);
    }
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
   * 
   * @return the iterator
   */
  public Iterator<TreeItem> treeItemIterator() {
    List<TreeItem> accum = new ArrayList<TreeItem>();
    root.addTreeItems(accum);
    return accum.iterator();
  }

  @Override
  protected void doAttachChildren() {
    try {
      AttachDetachException.tryCommand(this,
          AttachDetachException.attachCommand);
    } finally {
      DOM.setEventListener(focusable, this);
    }
  }

  @Override
  protected void doDetachChildren() {
    try {
      AttachDetachException.tryCommand(this,
          AttachDetachException.detachCommand);
    } finally {
      DOM.setEventListener(focusable, null);
    }
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

  /**
   * <b>Affected Elements:</b>
   * <ul>
   * <li>-root = The root {@link TreeItem}.</li>
   * </ul>
   * 
   * @see UIObject#onEnsureDebugId(String)
   */
  @Override
  protected void onEnsureDebugId(String baseID) {
    super.onEnsureDebugId(baseID);
    root.ensureDebugId(baseID + "-root");
  }

  @Override
  protected void onLoad() {
    root.updateStateRecursive();
  }

  void adopt(Widget widget, TreeItem treeItem) {
    assert (!childWidgets.containsKey(widget));
    childWidgets.put(widget, treeItem);
    widget.setParent(this);
  }

  void fireStateChanged(TreeItem item, boolean open) {
    if (open) {
      OpenEvent.fire(this, item);
    } else {
      CloseEvent.fire(this, item);
    }
  }

  /*
   * This method exists solely to support unit tests.
   */
  Map<Widget, TreeItem> getChildWidgets() {
    return childWidgets;
  }

  TreeImages getImages() {
    return images;
  }

  void maybeUpdateSelection(TreeItem itemThatChangedState, boolean isItemOpening) {
    /**
     * If we just closed the item, let's check to see if this item is the parent
     * of the currently selected item. If so, we should make this item the
     * currently selected selected item.
     */
    if (!isItemOpening) {
      TreeItem tempItem = curSelection;
      while (tempItem != null) {
        if (tempItem == itemThatChangedState) {
          setSelectedItem(itemThatChangedState);
          return;
        }
        tempItem = tempItem.getParentItem();
      }
    }
  }

  void orphan(Widget widget) {
    // Validation should already be done.
    assert (widget.getParent() == this);

    // Orphan.
    try {
      widget.setParent(null);
    } finally {
      // Logical detach.
      childWidgets.remove(widget);
    }
  }

  /**
   * Called only from {@link TreeItem}: Shows the closed image on that tree
   * item.
   * 
   * @param treeItem the tree item
   */
  void showClosedImage(TreeItem treeItem) {
    showImage(treeItem, images.treeClosed());
  }

  /**
   * Called only from {@link TreeItem}: Shows the leaf image on a tree item.
   * 
   * @param treeItem the tree item
   */
  void showLeafImage(TreeItem treeItem) {
    if (useLeafImages || treeItem.isFullNode()) {
      showImage(treeItem, images.treeLeaf());
    } else if (LocaleInfo.getCurrentLocale().isRTL()) {
      DOM.setStyleAttribute(treeItem.getElement(), "paddingRight", indentValue);
    } else {
      DOM.setStyleAttribute(treeItem.getElement(), "paddingLeft", indentValue);
    }
  }

  /**
   * Called only from {@link TreeItem}: Shows the open image on a tree item.
   * 
   * @param treeItem the tree item
   */
  void showOpenImage(TreeItem treeItem) {
    showImage(treeItem, images.treeOpen());
  }

  /**
   * Collects parents going up the element tree, terminated at the tree root.
   */
  private void collectElementChain(ArrayList<Element> chain, Element hRoot,
      Element hElem) {
    if ((hElem == null) || (hElem == hRoot)) {
      return;
    }

    collectElementChain(chain, hRoot, DOM.getParent(hElem));
    chain.add(hElem);
  }

  private boolean elementClicked(Element hElem) {
    ArrayList<Element> chain = new ArrayList<Element>();
    collectElementChain(chain, getElement(), hElem);

    TreeItem item = findItemByChain(chain, 0, root);
    if (item != null && item != root) {
      if (item.getChildCount() > 0
          && DOM.isOrHasChild(item.getImageElement(), hElem)) {
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

  private TreeItem findItemByChain(ArrayList<Element> chain, int idx,
      TreeItem root) {
    if (idx == chain.size()) {
      return root;
    }

    Element hCurElem = chain.get(idx);
    for (int i = 0, n = root.getChildCount(); i < n; ++i) {
      TreeItem child = root.getChild(i);
      if (child.getElement() == hCurElem) {
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
   * Get the top parent above this {@link TreeItem} that is in closed state. In
   * other words, get the parent that is guaranteed to be visible.
   * 
   * @param item
   * @return the closed parent, or null if all parents are opened
   */
  private TreeItem getTopClosedParent(TreeItem item) {
    TreeItem topClosedParent = null;
    TreeItem parent = item.getParentItem();
    while (parent != null && parent != root) {
      if (!parent.getState()) {
        topClosedParent = parent;
      }
      parent = parent.getParentItem();
    }
    return topClosedParent;
  }

  private void init(TreeImages images, boolean useLeafImages) {
    setImages(images, useLeafImages);
    setElement(DOM.createDiv());

    DOM.setStyleAttribute(getElement(), "position", "relative");

    // Fix rendering problem with relatively-positioned elements and their
    // children by
    // forcing the element that is positioned relatively to 'have layout'
    DOM.setStyleAttribute(getElement(), "zoom", "1");

    focusable = FocusPanel.impl.createFocusable();
    DOM.setStyleAttribute(focusable, "fontSize", "0");
    DOM.setStyleAttribute(focusable, "position", "absolute");

    // Hide focus outline in Mozilla/Webkit/Opera
    DOM.setStyleAttribute(focusable, "outline", "0px");

    // Hide focus outline in IE 6/7
    DOM.setElementAttribute(focusable, "hideFocus", "true");

    DOM.setIntStyleAttribute(focusable, "zIndex", -1);
    DOM.appendChild(getElement(), focusable);

    sinkEvents(Event.ONMOUSEDOWN | Event.ONCLICK | Event.KEYEVENTS);
    DOM.sinkEvents(focusable, Event.FOCUSEVENTS);

    // The 'root' item is invisible and serves only as a container
    // for all top-level items.
    root = new TreeItem() {
      @Override
      public void addItem(TreeItem item) {
        // If this element already belongs to a tree or tree item, remove it.
        if ((item.getParentItem() != null) || (item.getTree() != null)) {
          item.remove();
        }
        DOM.appendChild(Tree.this.getElement(), item.getElement());

        item.setTree(this.getTree());

        // Explicitly set top-level items' parents to null.
        item.setParentItem(null);
        getChildren().add(item);

        // Use no margin on top-most items.
        if (LocaleInfo.getCurrentLocale().isRTL()) {
          DOM.setIntStyleAttribute(item.getElement(), "marginRight", 0);
        } else {
          DOM.setIntStyleAttribute(item.getElement(), "marginLeft", 0);
        }
      }

      @Override
      public void removeItem(TreeItem item) {
        if (!getChildren().contains(item)) {
          return;
        }

        // Update Item state.
        item.setTree(null);
        item.setParentItem(null);
        getChildren().remove(item);

        DOM.removeChild(Tree.this.getElement(), item.getElement());
      }
    };
    root.initChildren();
    root.setTree(this);
    setStyleName("gwt-Tree");

    // Add a11y role "tree"
    Accessibility.setRole(getElement(), Accessibility.ROLE_TREE);
    Accessibility.setRole(focusable, Accessibility.ROLE_TREEITEM);
  }

  private void keyboardNavigation(Event event) {
    // Handle keyboard events if keyboard navigation is enabled
    if (isKeyboardNavigationEnabled(curSelection)) {
      int code = DOM.eventGetKeyCode(event);

      switch (standardizeKeycode(code)) {
        case KeyCodes.KEY_UP: {
          moveSelectionUp(curSelection);
          break;
        }
        case KeyCodes.KEY_DOWN: {
          moveSelectionDown(curSelection, true);
          break;
        }
        case KeyCodes.KEY_LEFT: {
          maybeCollapseTreeItem();
          break;
        }
        case KeyCodes.KEY_RIGHT: {
          maybeExpandTreeItem();
          break;
        }
        default: {
          return;
        }
      }
    }
  }

  private void maybeCollapseTreeItem() {

    TreeItem topClosedParent = getTopClosedParent(curSelection);
    if (topClosedParent != null) {
      // Select the first visible parent if curSelection is hidden
      setSelectedItem(topClosedParent);
    } else if (curSelection.getState()) {
      curSelection.setState(false);
    } else {
      TreeItem parent = curSelection.getParentItem();
      if (parent != null) {
        setSelectedItem(parent);
      }
    }
  }

  private void maybeExpandTreeItem() {

    TreeItem topClosedParent = getTopClosedParent(curSelection);
    if (topClosedParent != null) {
      // Select the first visible parent if curSelection is hidden
      setSelectedItem(topClosedParent);
    } else if (!curSelection.getState()) {
      curSelection.setState(true);
    } else if (curSelection.getChildCount() > 0) {
      setSelectedItem(curSelection.getChild(0));
    }
  }

  /**
   * Move the tree focus to the specified selected item.
   */
  private void moveFocus() {
    Focusable focusableWidget = curSelection.getFocusable();
    if (focusableWidget != null) {
      focusableWidget.setFocus(true);
      DOM.scrollIntoView(((Widget) focusableWidget).getElement());
    } else {
      // Get the location and size of the given item's content element relative
      // to the tree.
      Element selectedElem = curSelection.getContentElem();
      int containerLeft = getAbsoluteLeft();
      int containerTop = getAbsoluteTop();

      int left = DOM.getAbsoluteLeft(selectedElem) - containerLeft;
      int top = DOM.getAbsoluteTop(selectedElem) - containerTop;
      int width = DOM.getElementPropertyInt(selectedElem, "offsetWidth");
      int height = DOM.getElementPropertyInt(selectedElem, "offsetHeight");

      // If the item is not visible, quite here
      if (width == 0 || height == 0) {
        DOM.setIntStyleAttribute(focusable, "left", 0);
        DOM.setIntStyleAttribute(focusable, "top", 0);
        return;
      }

      // Set the focusable element's position and size to exactly underlap the
      // item's content element.
      DOM.setStyleAttribute(focusable, "left", left + "px");
      DOM.setStyleAttribute(focusable, "top", top + "px");
      DOM.setStyleAttribute(focusable, "width", width + "px");
      DOM.setStyleAttribute(focusable, "height", height + "px");

      // Scroll it into view.
      DOM.scrollIntoView(focusable);

      // Update ARIA attributes to reflect the information from the
      // newly-selected item.
      updateAriaAttributes();

      // Ensure Focus is set, as focus may have been previously delegated by
      // tree.
      setFocus(true);
    }
  }

  /**
   * Moves to the next item, going into children as if dig is enabled.
   */
  private void moveSelectionDown(TreeItem sel, boolean dig) {
    if (sel == root) {
      return;
    }

    // Find a parent that is visible
    TreeItem topClosedParent = getTopClosedParent(sel);
    if (topClosedParent != null) {
      moveSelectionDown(topClosedParent, false);
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
    // Find a parent that is visible
    TreeItem topClosedParent = getTopClosedParent(sel);
    if (topClosedParent != null) {
      onSelection(topClosedParent, true, true);
      return;
    }

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

    if (curSelection != null) {
      if (moveFocus) {
        moveFocus();
      }
      // Select the item and fire the selection event.
      curSelection.setSelected(true);
      if (fireEvents) {
        SelectionEvent.fire(this, curSelection);
      }
    }
  }

  private void setImages(TreeImages images, boolean useLeafImages) {
    this.images = images;
    this.useLeafImages = useLeafImages;

    if (!useLeafImages) {
      Image image = images.treeLeaf().createImage();
      DOM.setStyleAttribute(image.getElement(), "visibility", "hidden");
      RootPanel.get().add(image);
      int size = image.getWidth() + TreeItem.IMAGE_PAD;
      image.removeFromParent();
      indentValue = (size) + "px";
    }
  }

  private void showImage(TreeItem treeItem, AbstractImagePrototype proto) {
    Element holder = treeItem.getImageHolderElement();
    Element child = DOM.getFirstChild(holder);
    if (child == null) {
      // If no image element has been created yet, create one from the
      // prototype.
      DOM.appendChild(holder, proto.createElement().<Element> cast());
    } else {
      // Otherwise, simply apply the prototype to the existing element.
      proto.applyTo(child.<ImagePrototypeElement> cast());
    }
  }

  private void updateAriaAttributes() {

    Element curSelectionContentElem = curSelection.getContentElem();

    // Set the 'aria-level' state. To do this, we need to compute the level of
    // the currently selected item.

    // We initialize itemLevel to -1 because the level value is zero-based.
    // Note that the root node is not a part of the TreeItem hierachy, and we
    // do not consider the root node to have a designated level. The level of
    // the root's children is level 0, its children's children is level 1, etc.

    int curSelectionLevel = -1;
    TreeItem tempItem = curSelection;

    while (tempItem != null) {
      tempItem = tempItem.getParentItem();
      ++curSelectionLevel;
    }

    Accessibility.setState(curSelectionContentElem, Accessibility.STATE_LEVEL,
        String.valueOf(curSelectionLevel + 1));

    // Set the 'aria-setsize' and 'aria-posinset' states. To do this, we need to
    // compute the the number of siblings that the currently selected item has,
    // and the item's position among its siblings.

    TreeItem curSelectionParent = curSelection.getParentItem();
    if (curSelectionParent == null) {
      curSelectionParent = root;
    }

    Accessibility.setState(curSelectionContentElem,
        Accessibility.STATE_SETSIZE,
        String.valueOf(curSelectionParent.getChildCount()));

    int curSelectionIndex = curSelectionParent.getChildIndex(curSelection);

    Accessibility.setState(curSelectionContentElem,
        Accessibility.STATE_POSINSET, String.valueOf(curSelectionIndex + 1));

    // Set the 'aria-expanded' state. This depends on the state of the currently
    // selected item.
    // If the item has no children, we remove the 'aria-expanded' state.

    if (curSelection.getChildCount() == 0) {
      Accessibility.removeState(curSelectionContentElem,
          Accessibility.STATE_EXPANDED);
    } else {
      if (curSelection.getState()) {
        Accessibility.setState(curSelectionContentElem,
            Accessibility.STATE_EXPANDED, "true");
      } else {
        Accessibility.setState(curSelectionContentElem,
            Accessibility.STATE_EXPANDED, "false");
      }
    }

    // Make sure that 'aria-selected' is true.

    Accessibility.setState(curSelectionContentElem,
        Accessibility.STATE_SELECTED, "true");

    // Update the 'aria-activedescendant' state for the focusable element to
    // match the id of the currently selected item

    Accessibility.setState(focusable, Accessibility.STATE_ACTIVEDESCENDANT,
        DOM.getElementAttribute(curSelectionContentElem, "id"));
  }
}
