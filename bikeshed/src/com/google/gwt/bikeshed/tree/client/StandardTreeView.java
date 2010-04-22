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

import com.google.gwt.animation.client.Animation;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.HasAnimation;

import java.util.ArrayList;

/**
 * A view of a tree.
 */
public class StandardTreeView extends TreeView implements HasAnimation {

  /**
   * A node animation.
   */
  public abstract static class NodeAnimation extends Animation {

    /**
     * The default animation delay in milliseconds.
     */
    private static final int DEFAULT_ANIMATION_DURATION = 450;

    /**
     * The duration of the animation.
     */
    private int duration = DEFAULT_ANIMATION_DURATION;

    NodeAnimation() {
    }

    /**
     * Animate a tree node into its new state.
     * 
     * @param node the node to animate
     * @param isAnimationEnabled true to animate
     */
    abstract void animate(StandardTreeNodeView<?> node,
        boolean isAnimationEnabled);

    public int getDuration() {
      return duration;
    }

    public void setDuration(int duration) {
      this.duration = duration;
    }
  }

  /**
   * A {@link NodeAnimation} that reveals the contents of child nodes.
   */
  public static class RevealAnimation extends NodeAnimation {

    /**
     * Create a new {@link RevealAnimation}.
     * 
     * @return the new animation
     */
    public static RevealAnimation create() {
      return new RevealAnimation();
    }

    /**
     * The container that holds the content, includind the children.
     */
    Element contentContainer;

    /**
     * The target height when opening, the start height when closing.
     */
    int height;

    /**
     * True if the node is opening, false if closing.
     */
    boolean opening;

    /**
     * The container that holds the child container.
     */
    private Element animFrame;

    /**
     * The container that holds the children.
     */
    private Element childContainer;

    /**
     * Not instantiable.
     */
    private RevealAnimation() {
    }

    /**
     * Animate a {@link StandardTreeNodeView} into its new state.
     * 
     * @param node the {@link StandardTreeNodeView} to animate
     * @param isAnimationEnabled true to animate
     */
    @Override
    void animate(StandardTreeNodeView<?> node, boolean isAnimationEnabled) {
      // Cancel any pending animations.
      cancel();

      // Initialize the fields.
      this.opening = node.isOpen();
      animFrame = node.ensureAnimationFrame();
      contentContainer = node.ensureContentContainer();
      childContainer = node.ensureChildContainer();

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
        height = contentContainer.getScrollHeight();
      } else {
        height = contentContainer.getOffsetHeight();
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
      this.contentContainer = null;
      this.childContainer = null;
      this.animFrame = null;
    }
  }

  /**
   * A {@link NodeAnimation} that slides children into view.
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
      contentContainer.getStyle().clearPosition();
      contentContainer.getStyle().clearTop();
      contentContainer.getStyle().clearWidth();
      super.onComplete();
    }

    @Override
    protected void onStart() {
      super.onStart();
      if (opening) {
        contentContainer.getStyle().setTop(-height, Unit.PX);
      } else {
        contentContainer.getStyle().setTop(0, Unit.PX);
      }
      contentContainer.getStyle().setPosition(Position.RELATIVE);
    }

    @Override
    protected void onUpdate(double progress) {
      super.onUpdate(progress);
      if (opening) {
        double curTop = (1.0 - progress) * -height;
        contentContainer.getStyle().setTop(curTop, Unit.PX);
      } else {
        double curTop = progress * -height;
        contentContainer.getStyle().setTop(curTop, Unit.PX);
      }
    }
  }

  /**
   * The animation.
   */
  private NodeAnimation animation;

  /**
   * The HTML used to generate the closed image.
   */
  private String closedImageHtml;

  /**
   * Indicates whether or not animations are enabled.
   */
  private boolean isAnimationEnabled;

  /**
   * The message displayed while child nodes are loading.
   */
  // TODO(jlabanca): I18N loading HTML, or remove the text.
  private String loadingHtml = "Loading...";

  /**
   * The HTML used to generate the open image.
   */
  private String openImageHtml;

  /**
   * The hidden root node in the tree.
   */
  private StandardTreeNodeView<?> rootNode;

  /**
   * Construct a new {@link TreeView}.
   * 
   * @param <T> the type of data in the root node
   * @param viewModel the {@link TreeViewModel} that backs the tree
   * @param rootValue the hidden root value of the tree
   */
  public <T> StandardTreeView(TreeViewModel viewModel, T rootValue) {
    super(viewModel);
    setStyleName("gwt-StandardTreeView");

    // We use one animation for the entire tree.
    setAnimation(SlideAnimation.create());

    // Add event handlers.
    sinkEvents(Event.ONCLICK | Event.ONCHANGE | Event.MOUSEEVENTS);

    // Associate a view with the item.
    StandardTreeNodeView<T> root = new StandardTreeNodeView<T>(this, null,
        null, getElement(), rootValue);
    rootNode = root;
    root.setOpen(true);
  }

  /**
   * Get the animation used to open and close nodes in this tree if animations
   * are enabled.
   * 
   * @return the animation
   * @see #isAnimationEnabled()
   */
  public NodeAnimation getAnimation() {
    return animation;
  }

  /**
   * Get the HTML string that is displayed while nodes wait for their children
   * to load.
   * 
   * @return the loading HTML string
   */
  public String getLoadingHtml() {
    return loadingHtml;
  }

  public boolean isAnimationEnabled() {
    return isAnimationEnabled;
  }

  @Override
  public void onBrowserEvent(Event event) {
    super.onBrowserEvent(event);

    Element target = event.getEventTarget().cast();

    ArrayList<Element> chain = new ArrayList<Element>();
    collectElementChain(chain, getElement(), target);

    StandardTreeNodeView<?> nodeView = findItemByChain(chain, 0, rootNode);
    if (nodeView != null && nodeView != rootNode) {
      if ("click".equals(event.getType())) {
        // Open the node when the open image is clicked.
        Element showFewerElem = nodeView.getShowFewerElement();
        Element showMoreElem = nodeView.getShowMoreElement();
        if (nodeView.getImageElement().isOrHasChild(target)) {
          nodeView.setOpen(!nodeView.isOpen());
          return;
        } else if (showFewerElem != null && showFewerElem.isOrHasChild(target)) {
          nodeView.showFewer();
          return;
        } else if (showMoreElem != null && showMoreElem.isOrHasChild(target)) {
          nodeView.showMore();
          return;
        }
      }

      // Forward the event to the cell.
      if (nodeView.getCellParent().isOrHasChild(target)) {
        boolean consumesEvent = nodeView.fireEventToCell(event);
        if (!consumesEvent && "click".equals(event.getType())) {
          nodeView.select();
        }
      }
    }
  }

  /**
   * Set the animation used to open and close nodes in this tree. You must call
   * {@link #setAnimationEnabled(boolean)} to enable or disable animation.
   * 
   * @param animation a {@link NodeAnimation}
   * @see #setAnimationEnabled(boolean)
   */
  public void setAnimation(NodeAnimation animation) {
    assert animation != null : "animation cannot be null";
    this.animation = animation;
  }

  public void setAnimationEnabled(boolean enable) {
    this.isAnimationEnabled = enable;
    if (!enable && animation != null) {
      animation.cancel();
    }
  }

  /**
   * Set the HTML string that will be displayed when a node is waiting for its
   * child nodes to load.
   * 
   * @param loadingHtml the HTML string
   */
  public void setLoadingHtml(String loadingHtml) {
    this.loadingHtml = loadingHtml;
  }

  /**
   * @return the HTML to render the closed image.
   */
  String getClosedImageHtml() {
    if (closedImageHtml == null) {
      AbstractImagePrototype proto = AbstractImagePrototype.create(getResources().treeClosed());
      closedImageHtml = proto.getHTML().replace("style='",
          "style='position:absolute;left:0px;top:0px;");
    }
    return closedImageHtml;
  }

  /**
   * @return the HTML to render the open image.
   */
  String getOpenImageHtml() {
    if (openImageHtml == null) {
      AbstractImagePrototype proto = AbstractImagePrototype.create(getResources().treeOpen());
      openImageHtml = proto.getHTML().replace("style='",
          "style='position:absolute;left:0px;top:0px;");
    }
    return openImageHtml;
  }

  /**
   * Animate the current state of a {@link StandardTreeNodeView} in this tree.
   * 
   * @param node the node to animate
   */
  void maybeAnimateTreeNode(StandardTreeNodeView<?> node) {
    if (animation != null) {
      animation.animate(node, node.consumeAnimate() && isAnimationEnabled());
    }
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

  private StandardTreeNodeView<?> findItemByChain(ArrayList<Element> chain,
      int idx, StandardTreeNodeView<?> parent) {
    if (idx == chain.size()) {
      return parent;
    }

    Element hCurElem = chain.get(idx);
    for (int i = 0, n = parent.getChildCount(); i < n; ++i) {
      StandardTreeNodeView<?> child = parent.getChildNode(i);
      if (child.getElement() == hCurElem) {
        StandardTreeNodeView<?> retItem = findItemByChain(chain, idx + 1, child);
        if (retItem == null) {
          return child;
        }
        return retItem;
      }
    }

    return findItemByChain(chain, idx + 1, parent);
  }
}
