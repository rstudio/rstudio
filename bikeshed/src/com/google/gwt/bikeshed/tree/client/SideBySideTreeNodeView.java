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
package com.google.gwt.bikeshed.tree.client;

import static com.google.gwt.bikeshed.tree.client.SideBySideTreeView.COLUMN_WIDTH;

import com.google.gwt.bikeshed.cells.client.Cell;
import com.google.gwt.bikeshed.tree.client.TreeViewModel.NodeInfo;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;

import java.util.List;

/**
 * A tree view that displays each level in a side-by-side manner.
 * 
 * @param <T> the type that this {@link TreeNodeView} contains
 */
public class SideBySideTreeNodeView<T> extends TreeNodeView<T> {

  private final int imageLeft;

  private int level;

  private String path;

  /**
   * Construct a {@link TreeNodeView}.
   * 
   * @param tree the parent {@link TreeView}
   * @param parent the parent {@link TreeNodeView}
   * @param parentNodeInfo the {@link NodeInfo} of the parent
   * @param elem the outer element of this {@link TreeNodeView}.
   * @param value the value of this node
   */
  SideBySideTreeNodeView(final TreeView tree, final SideBySideTreeNodeView<?> parent,
      NodeInfo<T> parentNodeInfo, Element elem, T value, int level, String path) {
    super(tree, parent, parentNodeInfo, value);
    this.imageLeft = 85 - tree.getImageWidth();
    this.level = level;
    this.path = path;
    
    setElement(elem);
  }

  @Override
  protected <C> TreeNodeView<C> createTreeNodeView(NodeInfo<C> nodeInfo,
      Element childElem, C childValue, int idx) {
    return new SideBySideTreeNodeView<C>(tree,
        SideBySideTreeNodeView.this, nodeInfo, childElem, childValue,
        level + 1, path + "-" + idx);
  }
  
  @Override
  protected <C> void emitHtml(StringBuilder sb, NodeInfo<C> nodeInfo,
      List<C> childValues, List<TreeNodeView<?>> savedViews) {
    TreeViewModel model = tree.getTreeViewModel();
    int imageWidth = tree.getImageWidth();
    Cell<C> theCell = nodeInfo.getCell();

    int idx = 0;
    for (C childValue : childValues) {
      sb.append("<div id=\"" + path + "-" + idx + "\" style=\"position:relative;padding-right:");
      sb.append(imageWidth);
      sb.append("px;\">");
      if (savedViews.get(idx) != null) {
        sb.append(tree.getOpenImageHtml(imageLeft));
      } else if (model.isLeaf(childValue)) {
        sb.append(LEAF_IMAGE);
      } else {
        sb.append(tree.getClosedImageHtml(imageLeft));
      }
      sb.append("<div class=\"gwt-sstree-cell\">");
      theCell.render(childValue, sb);
      sb.append("</div></div>");
      
      idx++;
    }
  }

  /**
   * Ensure that the child container exists and return it.
   * 
   * @return the child container
   */
  @Override
  protected Element ensureChildContainer() {
    if (childContainer == null) {
      // Create the container within the top-level widget element.
      Element container = createContainer(level);
      container.setInnerHTML("");
      Element animFrame = container.appendChild(
          Document.get().createDivElement());
      animFrame.getStyle().setPosition(Position.RELATIVE);
      childContainer = animFrame.appendChild(Document.get().createDivElement());
    }

    return childContainer;
  }

  /**
   * @return the element that contains the rendered cell
   */
  @Override
  protected Element getCellParent() {
    return getElement().getChild(1).cast();
  }
  
  /**
   * @return the image element
   */
  @Override
  protected Element getImageElement() {
    return getElement().getFirstChildElement();
  }

  @Override
  protected int getImageLeft() {
    return imageLeft;
  }
  
  @Override
  protected void postClose() {
    destroyContainer(level);
  }

  @Override
  protected void preOpen() {
    // Close siblings of this node
    TreeNodeView<?> parentNode = getParentTreeNodeView();
    if (parentNode != null) {
      int numSiblings = parentNode.getChildCount();
      for (int i = 0; i < numSiblings; i++) {
        Element container = parentNode.getChildContainer().getChild(i).cast();

        TreeNodeView<?> sibling = parentNode.getChildTreeNodeView(i);
        if (sibling == this) {
          container.setClassName("gwt-SideBySideTree-selectedItem");
        } else {
          if (sibling.getState()) {
            sibling.setState(false);
            container.setClassName("gwt-SideBySideTree-unselectedItem");
          }
        }
      }
    }
  }

  /**
   * Returns the container for child nodes at the given level.
   */
  private Element createContainer(int level) {
    // Resize the root element
    Element rootElement = tree.getElement();
    rootElement.getStyle().setWidth((level + 1) * COLUMN_WIDTH, Unit.PX);
    
    // Create children of the root container as needed.
    int childCount = rootElement.getChildCount();
    while (childCount <= level) {
      Element div = rootElement.appendChild(Document.get().createDivElement());
      div.setClassName("gwt-SideBySideTreeColumn");
      Style style = div.getStyle();
      style.setPosition(Position.ABSOLUTE);
      style.setTop(0, Unit.PX);
      style.setLeft(level * COLUMN_WIDTH, Unit.PX);
      
      childCount++;
    }
    
    return rootElement.getChild(level).cast();
  }

  /**
   * Destroys the containers for child nodes at the given level and all
   * subsequent levels.
   */
  private void destroyContainer(int level) {
    // Resize the root element
    Element rootElement = tree.getElement();
    rootElement.getStyle().setWidth((level + 1) * COLUMN_WIDTH, Unit.PX);
    
    // Create children of the root container as needed.
    int childCount = rootElement.getChildCount();
    while (childCount > level) {
      rootElement.removeChild(rootElement.getLastChild());
      childCount--;
    }
    
    childContainer = null;
  }
}
