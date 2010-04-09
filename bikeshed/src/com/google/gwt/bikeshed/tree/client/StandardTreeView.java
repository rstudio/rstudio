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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.HasAnimation;

import java.util.ArrayList;

/**
 * A view of a tree.
 */
public class StandardTreeView extends TreeView implements HasAnimation {

  /**
   * A {@link TreeView.TreeNodeViewAnimation} that reveals the contents of child
   * nodes.
   */
  public static class RevealAnimation extends TreeNodeViewAnimation {

    /**
     * Create a new {@link RevealAnimation}.
     *
     * @return the new animation
     */
    public static RevealAnimation create() {
      return new RevealAnimation();
    }

    /**
     * The container that holds the child container.
     */
    Element animFrame;
    /**
     * The container that holds the children.
     */
    Element childContainer;

    /**
     * The target height when opening, the start height when closing.
     */
    int height;
    /**
     * True if the node is opening, false if closing.
     */
    boolean opening;

    /**
     * Not instantiable.
     */
    private RevealAnimation() {
    }

    /**
     * Animate a {@link TreeNodeView} into its new state.
     *
     * @param node the {@link TreeNodeView} to animate
     * @param isAnimationEnabled true to animate
     */
    @Override
    public void animate(TreeNodeView<?> node, boolean isAnimationEnabled) {
      // Cancel any pending animations.
      cancel();

      // Initialize the fields.
      this.opening = node.getState();
      childContainer = node.getChildContainer();
      animFrame = childContainer.getParentElement();

      if (isAnimationEnabled) {
        // Animated.
        int duration = getDuration();
        int childCount = childContainer.getChildCount();
        if (childCount < 4) {
          // Reduce the duration if there are less than four items or it will
          // look really slow.
          duration = (int) ((childCount / 4.0) * duration);
        }
        run(duration);
      } else {
        // Non animated.
        cleanup();
      }
    }

    @Override
    protected void onComplete() {
      cleanup();
    }

    @Override
    protected void onStart() {
      if (opening) {
        animFrame.getStyle().setHeight(1.0, Unit.PX);
        animFrame.getStyle().clearDisplay();
        height = childContainer.getScrollHeight();
      } else {
        height = childContainer.getOffsetHeight();
      }
    }

    @Override
    protected void onUpdate(double progress) {
      if (opening) {
        double curHeight = progress * height;
        animFrame.getStyle().setHeight(curHeight, Unit.PX);
      } else {
        double curHeight = (1.0 - progress) * height;
        animFrame.getStyle().setHeight(curHeight, Unit.PX);
      }
    }

    /**
     * Put the node back into a clean state and clear fields.
     */
    private void cleanup() {
      if (opening) {
        animFrame.getStyle().clearDisplay();
      } else {
        animFrame.getStyle().setDisplay(Display.NONE);
        childContainer.setInnerHTML("");
      }
      animFrame.getStyle().clearHeight();
      this.childContainer = null;
      this.animFrame = null;
    }
  }

  /**
   * A {@link TreeView.TreeNodeViewAnimation} that slides children into view.
   */
  public static class SlideAnimation extends RevealAnimation {
    /**
     * Create a new {@link RevealAnimation}.
     *
     * @return the new animation
     */
    public static SlideAnimation create() {
      return new SlideAnimation();
    }

    /**
     * Not instantiable.
     */
    private SlideAnimation() {
    }

    @Override
    protected void onComplete() {
      childContainer.getStyle().clearPosition();
      childContainer.getStyle().clearTop();
      childContainer.getStyle().clearWidth();
      super.onComplete();
    }

    @Override
    protected void onStart() {
      super.onStart();
      if (opening) {
        childContainer.getStyle().setTop(-height, Unit.PX);
      } else {
        childContainer.getStyle().setTop(0, Unit.PX);
      }
      childContainer.getStyle().setPosition(Position.RELATIVE);
    }

    @Override
    protected void onUpdate(double progress) {
      super.onUpdate(progress);
      if (opening) {
        double curTop = (1.0 - progress) * -height;
        childContainer.getStyle().setTop(curTop, Unit.PX);
      } else {
        double curTop = progress * -height;
        childContainer.getStyle().setTop(curTop, Unit.PX);
      }
    }
  }

  /**
   * Construct a new {@link TreeView}.
   *
   * @param <T> the type of data in the root node
   * @param viewModel the {@link TreeViewModel} that backs the tree
   * @param rootValue the hidden root value of the tree
   */
  public <T> StandardTreeView(TreeViewModel viewModel, T rootValue) {
    super(viewModel);
    setElement(Document.get().createDivElement());
    setStyleName("gwt-TreeView");

    // We use one animation for the entire tree.
    setAnimation(SlideAnimation.create());

    // Add event handlers.
    sinkEvents(Event.ONCLICK | Event.ONMOUSEDOWN | Event.ONMOUSEUP
        | Event.ONCHANGE);

    // Associate a view with the item.
    TreeNodeView<T> root = new StandardTreeNodeView<T>(this, null, null,
        getElement(), rootValue);
    setRootNode(root);
    root.setState(true);
  }

  @Override
  public void onBrowserEvent(Event event) {
    super.onBrowserEvent(event);

    Element target = event.getEventTarget().cast();
    TreeNodeView<?> rootNode = getRootTreeNodeView();

    ArrayList<Element> chain = new ArrayList<Element>();
    ArrayList<String> ids = new ArrayList<String>();
    collectElementChain(chain, ids, getElement(), target);

    TreeNodeView<?> nodeView = findItemByChain(chain, 0, rootNode);
    if (nodeView != null && nodeView != rootNode) {
      if (nodeView.getImageElement().isOrHasChild(target)) {
        if ("click".equals(event.getType())) {
          nodeView.setState(!nodeView.getState());
        }
      } else if (nodeView.getCellParent().isOrHasChild(target)) {
        nodeView.fireEventToCell(event);

        // TODO(jgw): Kind of a hacky way to set selection. Need to generalize
        // this to some sort of keyboard/mouse->selection controller.
        if (getSelectionModel() != null) {
          if ("click".equals(event.getType())) {
            getSelectionModel().setSelected(nodeView.getValue(), true);
          }
        }
      }
    }
  }

  /**
   * Collects parents going up the element tree, terminated at the tree root.
   */
  private void collectElementChain(ArrayList<Element> chain,
      ArrayList<String> ids, Element hRoot, Element hElem) {
    if ((hElem == null) || (hElem == hRoot)) {
      return;
    }

    collectElementChain(chain, ids, hRoot, hElem.getParentElement());
    chain.add(hElem);
    ids.add(hElem.getId());
  }

  private TreeNodeView<?> findItemByChain(ArrayList<Element> chain, int idx,
      TreeNodeView<?> parent) {
    if (idx == chain.size()) {
      return parent;
    }

    Element hCurElem = chain.get(idx);
    for (int i = 0, n = parent.getChildCount(); i < n; ++i) {
      TreeNodeView<?> child = parent.getChildTreeNodeView(i);
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
