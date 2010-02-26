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

import com.google.gwt.animation.client.Animation;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.HasAnimation;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;

/**
 * A view of a tree.
 */
public class TreeView extends Widget implements HasAnimation {

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
   * The animation used for {@link TreeNodeView}.
   */
  public abstract static class TreeNodeAnimation extends Animation {

    /**
     * The default animation delay in milliseconds.
     */
    private static final int DEFAULT_ANIMATION_DURATION = 450;

    /**
     * The duration of the animation.
     */
    private int duration = DEFAULT_ANIMATION_DURATION;

    /**
     * Not instantiable.
     */
    private TreeNodeAnimation() {
    }

    /**
     * Get the duration of animations in milliseconds.
     * 
     * @return the animation duration
     */
    public int getDuration() {
      return duration;
    }

    /**
     * Set the animation duration in milliseconds.
     * 
     * @param duration the duration
     */
    public void setDuration(int duration) {
      this.duration = duration;
    }

    /**
     * Animate a {@link TreeNodeView} into its new state.
     * 
     * @param node the {@link TreeNodeView} to animate
     * @param isAnimationEnabled true to animate
     */
    abstract void animate(TreeNodeView<?> node, boolean isAnimationEnabled);
  }

  /**
   * A {@link TreeNodeAnimation} that reveals the contents of child nodes.
   */
  public static class RevealAnimation extends TreeNodeAnimation {

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
     * Animate a {@link TreeNodeView} into its new state.
     * 
     * @param node the {@link TreeNodeView} to animate
     * @param isAnimationEnabled true to animate
     */
    @Override
    void animate(TreeNodeView<?> node, boolean isAnimationEnabled) {
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
   * A {@link TreeNodeAnimation} that slides children into view.
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
   * We use one animation for the entire {@link TreeView}.
   */
  private TreeNodeAnimation animation = SlideAnimation.create();

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
  // TODO(jlabanca): I18N this, or remove the text
  private String loadingHtml = "Loading...";

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
    setStyleName("gwt-TreeView");

    // Add event handlers.
    sinkEvents(Event.ONCLICK | Event.ONMOUSEDOWN | Event.ONMOUSEUP);

    // Associate a view with the item.
    rootNode = new TreeNodeView<T>(this, null, null, getElement(), rootValue);
    rootNode.setState(true);
  }

  /**
   * Get the animation used to open and close nodes in this tree if animations
   * are enabled.
   * 
   * @return the animation
   * @see #isAnimationEnabled()
   */
  public TreeNodeAnimation getAnimation() {
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

  public TreeViewModel getTreeViewModel() {
    return model;
  }

  public boolean isAnimationEnabled() {
    return isAnimationEnabled;
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
   * Set the animation used to open and close nodes in this tree. You must call
   * {@link #setAnimationEnabled(boolean)} to enable or disable animation.
   * 
   * @param animation the animation
   * @see #setAnimationEnabled(boolean)
   */
  public void setAnimation(TreeNodeAnimation animation) {
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
   * Animate the current state of a {@link TreeNodeView} in this tree.
   * 
   * @param node the node to animate
   */
  void maybeAnimateTreeNode(TreeNodeView<?> node) {
    animation.animate(node, isAnimationEnabled);
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
