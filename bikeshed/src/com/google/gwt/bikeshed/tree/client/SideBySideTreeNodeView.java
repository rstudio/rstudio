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

import com.google.gwt.bikeshed.list.client.HasCell;
import com.google.gwt.bikeshed.tree.client.TreeViewModel.NodeInfo;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.RequiresResize;

import java.util.List;

/**
 * A tree view that displays each level in a side-by-side manner.
 *
 * @param <T> the type that this {@link TreeNodeView} contains
 */
public class SideBySideTreeNodeView<T> extends TreeNodeView<T> {

  private int columnWidth;

  private final int imageLeft;

  private int level;

  private int maxColumns;

  private String path;

  /**
   * Construct a {@link TreeNodeView}.
   *
   * @param tree the parent {@link TreeView}
   * @param parent the parent {@link TreeNodeView}
   * @param parentNodeInfo the {@link NodeInfo} of the parent
   * @param elem the outer element of this {@link TreeNodeView}
   * @param value the value of this node
   * @param maxColumns the maximum number of columns to display
   */
  SideBySideTreeNodeView(final TreeView tree, final SideBySideTreeNodeView<?> parent,
      NodeInfo<T> parentNodeInfo, Element elem, T value, int level, String path,
      int columnWidth, int maxColumns) {
    super(tree, parent, parentNodeInfo, value);
    this.imageLeft = columnWidth - 16 - tree.getImageWidth();
    this.level = level;
    this.path = path;
    this.columnWidth = columnWidth;
    this.maxColumns = maxColumns;

    setElement(elem);
  }

  @Override
  protected <C> TreeNodeView<C> createTreeNodeView(NodeInfo<C> nodeInfo,
      Element childElem, C childValue, Void viewData, int idx) {
    return new SideBySideTreeNodeView<C>(getTree(), this, nodeInfo, childElem,
        childValue, level + 1, path + "-" + idx, columnWidth, maxColumns);
  }

  @Override
  protected <C> void emitHtml(StringBuilder sb, List<C> childValues,
      List<HasCell<C, ?, Void>> hasCells, List<TreeNodeView<?>> savedViews) {
    TreeView tree = getTree();
    TreeViewModel model = tree.getTreeViewModel();
    int imageWidth = tree.getImageWidth();

    int idx = 0;
    for (C childValue : childValues) {
      sb.append("<div id=\"" + path + "-" + idx +
          "\" class=\"gwt-sstree-unselectedItem gwt-sstree-" +
          ((idx % 2) == 0 ? "even" : "odd") + "Row\"" +
          " style=\"position:relative;padding-right:");
      sb.append(imageWidth);
      sb.append("px;\">");
      if (savedViews.get(idx) != null) {
        sb.append(tree.getOpenImageHtml(imageLeft));
      } else if (model.isLeaf(childValue, this)) {
        sb.append(LEAF_IMAGE);
      } else {
        sb.append(tree.getClosedImageHtml(imageLeft));
      }
      sb.append("<div class=\"gwt-sstree-cell\">");
      for (int i = 0; i < hasCells.size(); i++) {
        sb.append("<span __idx='");
        sb.append(i);
        sb.append("'>");
        render(sb, childValue, hasCells.get(i));
        sb.append("</span>");
      }
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
    if (getChildContainer() == null) {
      // Create the container within the top-level widget element.
      Element container = createContainer(level);
      container.setInnerHTML("");
      Element animFrame = container.appendChild(
          Document.get().createDivElement());
      animFrame.getStyle().setPosition(Position.RELATIVE);
      animFrame.setId("animFrame");
      setChildContainer(animFrame.appendChild(Document.get().createDivElement()));
    }

    // TODO(jgw): Kind of a hack. We should probably be propagating onResize()
    // down from the TreeView, but this is simpler for the moment.
    TreeView tree = getTree();
    if (tree instanceof RequiresResize) {
      ((RequiresResize) tree).onResize();
    }

    return getChildContainer();
  }

  /**
   * @return the element that contains the rendered cell
   */
  @Override
  protected Element getCellParent() {
    return getElement().getChild(1).cast();
  }

  @Override
  protected Element getContainer() {
    return getTree().getElement().getChild(level).cast();
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
          container.setClassName("gwt-sstree-selectedItem");
        } else {
          if (sibling.getState()) {
            sibling.setState(false);
          }

          container.setClassName("gwt-sstree-unselectedItem " +
              (((i % 2) == 0) ? "gwt-sstree-evenRow" : "gwt-sstree-oddRow"));
        }
      }
    }
  }

  /**
   * Returns the container for child nodes at the given level.
   */
  private Element createContainer(int level) {
    // Resize the root element
    Element rootElement = getTree().getElement();
    rootElement.getStyle().setWidth(Math.min(maxColumns, level + 1) * columnWidth, Unit.PX);

    // Create children of the root container as needed.
    int childCount = rootElement.getChildCount();
    while (childCount <= level) {
      Element div = rootElement.appendChild(Document.get().createDivElement());
      div.setClassName("gwt-sstree-column");
      Style style = div.getStyle();
      style.setPosition(Position.ABSOLUTE);
      style.setTop(0, Unit.PX);
      style.setWidth(columnWidth, Unit.PX);

      childCount++;
    }

    Element child = rootElement.getFirstChild().cast();

    int x = Math.min(0, maxColumns - (level + 1));
    while (child != null) {
      Style style = child.getStyle();
      style.setLeft(x * columnWidth, Unit.PX);
      if (x < 0) {
        style.setDisplay(Display.NONE);
      } else {
        style.clearDisplay();
      }
      child = child.getNextSibling().cast();
      x++;
    }

    return rootElement.getChild(level).cast();
  }

  /**
   * Destroys the containers for child nodes at the given level and all
   * subsequent levels.
   */
  private void destroyContainer(int level) {
    // Resize the root element
    Element rootElement = getTree().getElement();
    rootElement.getStyle().setWidth((level + 1) * columnWidth, Unit.PX);

    // Create children of the root container as needed.
    int childCount = rootElement.getChildCount();
    while (childCount > level) {
      rootElement.removeChild(rootElement.getLastChild());
      childCount--;
    }

    setChildContainer(null);
  }

  private <C, X> void render(StringBuilder sb, C childValue,
      HasCell<C, X, Void> hc) {
    hc.getCell().render(hc.getValue(childValue), null, sb);
  }
}
