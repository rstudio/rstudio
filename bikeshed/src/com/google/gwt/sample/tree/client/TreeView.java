/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.sample.tree.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;

/**
 * A view of a tree.
 */
public class TreeView extends Widget {

  private static final Resources DEFAULT_RESOURCES = GWT.create(Resources.class);

  /**
   * A ClientBundle that provides images for this widget.
   */
  public interface Resources extends ClientBundle {

    /**
     * An image indicating a closed branch.
     */
    ImageResource treeClosed();

    /**
     * An image indicating an open branch.
     */
    ImageResource treeOpen();
  }

  /**
   * The HTML used to generate the closed image.
   */
  private String closedImageHtml;

  /**
   * The {@link TreeViewModel} that backs the tree.
   */
  private TreeViewModel model;

  /**
   * The HTML used to generate the open image.
   */
  private String openImageHtml;

  /**
   * The Resources used by this tree.
   */
  private Resources resources;

  /**
   * The hidden root node in the tree.
   */
  private TreeNodeView<?> rootNode;

  /**
   * Construct a new {@link TreeView}.
   * 
   * @param <T> the type of data in the root node
   * @param viewModel the {@link TreeViewModel} that backs the tree
   * @param rootValue the hidden root value of the tree
   */
  public <T> TreeView(TreeViewModel viewModel, T rootValue) {
    this.model = viewModel;
    this.resources = DEFAULT_RESOURCES;
    setElement(Document.get().createDivElement());
    getElement().getStyle().setPosition(Position.RELATIVE);
    setStyleName("gwt-TreeView");

    // Add event handlers.
    sinkEvents(Event.ONCLICK | Event.ONMOUSEDOWN | Event.ONMOUSEUP);

    // Associate a view with the item.
    rootNode = new TreeNodeView<T>(this, null, null, getElement(), rootValue);
    rootNode.initChildContainer(getElement());
    rootNode.setState(true);
  }

  public TreeViewModel getTreeViewModel() {
    return model;
  }

  @Override
  public void onBrowserEvent(Event event) {
    super.onBrowserEvent(event);

    int eventType = DOM.eventGetType(event);
    switch (eventType) {
      case Event.ONMOUSEUP:
        Element currentTarget = event.getCurrentEventTarget().cast();
        if (currentTarget == getElement()) {
          Element target = event.getEventTarget().cast();
          elementClicked(target, event);
        }
        break;
    }
  }

  /**
   * @return the HTML to render the closed image.
   */
  String getClosedImageHtml() {
    if (closedImageHtml == null) {
      AbstractImagePrototype proto = AbstractImagePrototype.create(resources.treeClosed());
      closedImageHtml = proto.getHTML().replace("style='",
          "style='position:absolute;left:0px;top:0px;");
    }
    return closedImageHtml;
  }

  /**
   * Get the width required for the images.
   * 
   * @return the maximum width required for images.
   */
  int getImageWidth() {
    return Math.max(resources.treeClosed().getWidth(),
        resources.treeOpen().getWidth());
  }

  /**
   * @return the HTML to render the open image.
   */
  String getOpenImageHtml() {
    if (openImageHtml == null) {
      AbstractImagePrototype proto = AbstractImagePrototype.create(resources.treeOpen());
      openImageHtml = proto.getHTML().replace("style='",
          "style='position:absolute;left:0px;top:0px;");
    }
    return openImageHtml;
  }

  /**
   * Collects parents going up the element tree, terminated at the tree root.
   */
  private void collectElementChain(ArrayList<Element> chain, Element hRoot,
      Element hElem) {
    if ((hElem == null) || (hElem == hRoot)) {
      return;
    }

    collectElementChain(chain, hRoot, hElem.getParentElement());
    chain.add(hElem);
  }

  private boolean elementClicked(Element hElem, NativeEvent event) {
    ArrayList<Element> chain = new ArrayList<Element>();
    collectElementChain(chain, getElement(), hElem);

    TreeNodeView<?> nodeView = findItemByChain(chain, 0, rootNode);
    if (nodeView != null && nodeView != rootNode) {
      if (nodeView.getImageElement().isOrHasChild(hElem)) {
        nodeView.setState(!nodeView.getState(), true);
        return true;
      } else if (nodeView.getCellParent().isOrHasChild(hElem)) {
        nodeView.fireEventToCell(event);
        return true;
      }
    }

    return false;
  }

  private TreeNodeView<?> findItemByChain(ArrayList<Element> chain, int idx,
      TreeNodeView<?> parent) {
    if (idx == chain.size()) {
      return parent;
    }

    Element hCurElem = chain.get(idx);
    for (int i = 0, n = parent.getChildCount(); i < n; ++i) {
      TreeNodeView<?> child = parent.getChild(i);
      if (child.getElement() == hCurElem) {
        TreeNodeView<?> retItem = findItemByChain(chain, idx + 1, child);
        if (retItem == null) {
          return child;
        }
        return retItem;
      }
    }

    return findItemByChain(chain, idx + 1, parent);
  }
}
