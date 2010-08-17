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
package com.google.gwt.user.cellview.client;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasAnimation;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.view.client.TreeViewModel;

import java.util.ArrayList;

/**
 * A view of a tree.
 */
public class CellTree extends Composite implements HasAnimation {

  /**
   * A cleaner version of the table that uses less graphics.
   */
  public static interface CleanResources extends Resources {

    @Source("cellTreeClosedArrow.png")
    ImageResource cellTreeClosedItem();

    @Source("cellTreeLoadingClean.gif")
    ImageResource cellTreeLoading();

    @Source("cellTreeOpenArrow.png")
    ImageResource cellTreeOpenItem();

    @Source("CellTreeClean.css")
    CleanStyle cellTreeStyle();
  }

  /**
   * A cleaner version of the table that uses less graphics.
   */
  public static interface CleanStyle extends Style {
    String topItem();

    String topItemImageValue();
  }

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

    public int getDuration() {
      return duration;
    }

    public void setDuration(int duration) {
      this.duration = duration;
    }

    /**
     * Animate a tree node into its new state.
     *
     * @param node the node to animate
     * @param isAnimationEnabled true to animate
     */
    abstract void animate(CellTreeNodeView<?> node, boolean isAnimationEnabled);
  }

  /**
   * A ClientBundle that provides images for this widget.
   */
  public static interface Resources extends ClientBundle {

    /**
     * An image indicating a closed branch.
     */
    ImageResource cellTreeClosedItem();

    /**
     * An image indicating that a node is loading.
     */
    ImageResource cellTreeLoading();

    /**
     * An image indicating an open branch.
     */
    ImageResource cellTreeOpenItem();

    /**
     * The background used for selected items.
     */
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    ImageResource cellTreeSelectedBackground();

    /**
     * The styles used in this widget.
     */
    @Source("CellTree.css")
    Style cellTreeStyle();
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
     * Animate a {@link CellTreeNodeView} into its new state.
     *
     * @param node the {@link CellTreeNodeView} to animate
     * @param isAnimationEnabled true to animate
     */
    @Override
    void animate(CellTreeNodeView<?> node, boolean isAnimationEnabled) {
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
   * Styles used by this widget.
   */
  public static interface Style extends CssResource {

    /**
     * Applied to the empty message.
     */
    String emptyMessage();

    /**
     * Applied to tree items.
     */
    String item();

    /**
     * Applied to open/close icon.
     */
    String itemImage();

    /**
     * Applied to the wrapper around the image and value.
     */
    String itemImageValue();

    /**
     * Applied to the value in an item.
     */
    String itemValue();

    /**
     * Applied to the keyboard selected item.
     */
    String keyboardSelectedItem();

    /**
     * Applied to open tree items.
     */
    String openItem();

    /**
     * Applied to selected tree items.
     */
    String selectedItem();

    /**
     * Applied to the show more button.
     */
    String showMoreButton();

    /**
     * Applied to top level items.
     */
    String topItem();

    /**
     * Applied to open/close icon at the top level.
     */
    String topItemImage();

    /**
     * Applied to the wrapper around the image and value of top level items.
     */
    String topItemImageValue();
  }

  /**
   * The default number of children to show under a tree node.
   */
  private static final int DEFAULT_LIST_SIZE = 25;

  private static Resources DEFAULT_RESOURCES;

  private static Resources getDefaultResources() {
    if (DEFAULT_RESOURCES == null) {
      DEFAULT_RESOURCES = GWT.create(Resources.class);
    }
    return DEFAULT_RESOURCES;
  }

  /**
   * The animation.
   */
  private NodeAnimation animation;

  private boolean cellIsEditing;

  /**
   * The HTML used to generate the closed image.
   */
  private final String closedImageHtml;

  /**
   * The HTML used to generate the closed image for the top items.
   */
  private final String closedImageTopHtml;

  /**
   * The default number of children to display under each node.
   */
  private int defaultNodeSize = DEFAULT_LIST_SIZE;

  /**
   * The maximum width of the open and closed images.
   */
  private final int imageWidth;

  /**
   * Indicates whether or not animations are enabled.
   */
  private boolean isAnimationEnabled;

  /**
   * The {@link CellTreeNodeView} whose children are currently being selected
   * using the keyboard.
   */
  private CellTreeNodeView<?> keyboardSelectedNode;

  /**
   * The HTML used to generate the loading image.
   */
  private final String loadingImageHtml;

  /**
   * The HTML used to generate the open image.
   */
  private final String openImageHtml;

  /**
   * The HTML used to generate the open image for the top items.
   */
  private final String openImageTopHtml;

  /**
   * The hidden root node in the tree.
   */
  private final CellTreeNodeView<?> rootNode;

  /**
   * The styles used by this widget.
   */
  private final Style style;

  /**
   * The {@link TreeViewModel} that backs the tree.
   */
  private final TreeViewModel viewModel;

  /**
   * Construct a new {@link CellTree}.
   *
   * @param <T> the type of data in the root node
   * @param viewModel the {@link TreeViewModel} that backs the tree
   * @param rootValue the hidden root value of the tree
   */
  public <T> CellTree(TreeViewModel viewModel, T rootValue) {
    this(viewModel, rootValue, getDefaultResources());
  }

  /**
   * Construct a new {@link CellTree}.
   *
   * @param <T> the type of data in the root node
   * @param viewModel the {@link TreeViewModel} that backs the tree
   * @param rootValue the hidden root value of the tree
   * @param resources the resources used to render the tree
   */
  public <T> CellTree(TreeViewModel viewModel, T rootValue,
      Resources resources) {
    this.viewModel = viewModel;
    this.style = resources.cellTreeStyle();
    this.style.ensureInjected();
    initWidget(new SimplePanel());
    setStyleName("gwt-StandardTreeView");

    // Initialize the open and close images strings.
    ImageResource treeOpen = resources.cellTreeOpenItem();
    ImageResource treeClosed = resources.cellTreeClosedItem();
    ImageResource treeLoading = resources.cellTreeLoading();
    openImageHtml = getImageHtml(treeOpen, false);
    closedImageHtml = getImageHtml(treeClosed, false);
    openImageTopHtml = getImageHtml(treeOpen, true);
    closedImageTopHtml = getImageHtml(treeClosed, true);
    loadingImageHtml = getImageHtml(treeLoading, false);
    imageWidth = Math.max(Math.max(treeOpen.getWidth(), treeClosed.getWidth()),
        treeLoading.getWidth());

    // We use one animation for the entire tree.
    setAnimation(SlideAnimation.create());

    // Add event handlers.
    sinkEvents(Event.ONCLICK | Event.ONKEYDOWN | Event.ONKEYUP);

    // Associate a view with the item.
    CellTreeNodeView<T> root = new CellTreeNodeView<T>(this, null, null,
        getElement(), rootValue);
    keyboardSelectedNode = rootNode = root;
    root.setOpen(true);
    keyboardSelectedNode.keyboardEnter(0, false);
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
   * Get the default maximum number of children to display under each tree node.
   *
   * @return the default node size
   */
  public int getDefaultNodeSize() {
    return defaultNodeSize;
  }

  public TreeViewModel getTreeViewModel() {
    return viewModel;
  }

  public boolean isAnimationEnabled() {
    return isAnimationEnabled;
  }

  @Override
  public void onBrowserEvent(Event event) {
    CellBasedWidgetImpl.get().onBrowserEvent(this, event);
    super.onBrowserEvent(event);

    String eventType = event.getType();

    // Keep track of whether the user has focused on the widget
    if ("blur".equals(eventType)) {
      keyboardSelectedNode.keyboardBlur();
      return;
    }
    if ("focus".equals(eventType)) {
      keyboardSelectedNode.keyboardFocus();
      return;
    }

    boolean keyUp = "keyup".equals(eventType);
    boolean keyDown = "keydown".equals(eventType);

    // Ignore keydown events unless the cell is in edit mode
    if (keyDown && !cellIsEditing) {
      return;
    }
    if (keyUp && !cellIsEditing) {
      if (handleKey(event)) {
        return;
      }
    }

    Element target = event.getEventTarget().cast();
    ArrayList<Element> chain = new ArrayList<Element>();
    collectElementChain(chain, getElement(), target);

    CellTreeNodeView<?> nodeView = findItemByChain(chain, 0, rootNode);
    if (nodeView != null && nodeView != rootNode) {
      if ("click".equals(event.getType())) {
        // Open the node when the open image is clicked.
        Element showMoreElem = nodeView.getShowMoreElement();
        if (nodeView.getImageElement().isOrHasChild(target)) {
          nodeView.setOpen(!nodeView.isOpen());
          return;
        } else if (showMoreElem != null && showMoreElem.isOrHasChild(target)) {
          nodeView.showMore();
          return;
        }

        // Move the keyboard focus to the clicked item
        keyboardSelectedNode.keyboardExit();
        keyboardSelectedNode = nodeView.getParentNode();
        keyboardSelectedNode.keyboardEnter(target, true);
      }

      // Forward the event to the cell
      if (nodeView.getCellParent().isOrHasChild(target)
          || (eventType.startsWith("key")
              && nodeView.getCellParent().getParentElement() == target)) {
        nodeView.fireEventToCell(event);
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
   * Set the default number of children to display beneath each child node. If
   * more nodes are available, a button will appear at the end of the list
   * allowing the user to show more items. Changing this value will not affect
   * tree nodes that are already open.
   *
   * @param defaultNodeSize the max
   */
  public void setDefaultNodeSize(int defaultNodeSize) {
    this.defaultNodeSize = defaultNodeSize;
  }

  /**
   * Get the HTML to render the closed image.
   *
   * @param isTop true if the top element, false if not
   * @return the HTML string
   */
  String getClosedImageHtml(boolean isTop) {
    return isTop ? closedImageTopHtml : closedImageHtml;
  }

  /**
   * Get the width required for the images.
   *
   * @return the maximum width required for images.
   */
  int getImageWidth() {
    return imageWidth;
  }

  /**
   * @return the HTML to render the loading image.
   */
  String getLoadingImageHtml() {
    return loadingImageHtml;
  }

  /**
   * Get the HTML to render the open image.
   *
   * @param isTop true if the top element, false if not
   * @return the HTML string
   */
  String getOpenImageHtml(boolean isTop) {
    return isTop ? openImageTopHtml : openImageHtml;
  }

  /**
   * @return the Style used by the tree
   */
  Style getStyle() {
    return style;
  }

  /**
   * Animate the current state of a {@link CellTreeNodeView} in this tree.
   *
   * @param node the node to animate
   */
  void maybeAnimateTreeNode(CellTreeNodeView<?> node) {
    if (animation != null) {
      animation.animate(node, node.consumeAnimate() && isAnimationEnabled()
          && !node.isRootNode());
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

  private CellTreeNodeView<?> findItemByChain(ArrayList<Element> chain,
      int idx, CellTreeNodeView<?> parent) {
    if (idx == chain.size()) {
      return parent;
    }

    Element hCurElem = chain.get(idx);
    for (int i = 0, n = parent.getChildCount(); i < n; ++i) {
      CellTreeNodeView<?> child = parent.getChildNode(i);
      if (child.getElement() == hCurElem) {
        CellTreeNodeView<?> retItem = findItemByChain(chain, idx + 1, child);
        if (retItem == null) {
          return child;
        }
        return retItem;
      }
    }

    return findItemByChain(chain, idx + 1, parent);
  }

  /**
   * Get the HTML representation of an image.
   *
   * @param res the {@link ImageResource} to render as HTML
   * @param isTop true if the image is for a top level element.
   * @return the rendered HTML
   */
  private String getImageHtml(ImageResource res, boolean isTop) {
    StringBuilder sb = new StringBuilder();
    sb.append("<div class='").append(style.itemImage());
    if (isTop) {
      sb.append(" ").append(style.topItemImage());
    }
    sb.append("' ");

    // Add the position and dimensions.
    sb.append("style=\"position:absolute;left:0px;top:0px;");
    sb.append("height:").append(res.getHeight()).append("px;");
    sb.append("width:").append(res.getWidth()).append("px;");

    // Add the background, vertically centered.
    sb.append("background:url('").append(res.getURL()).append("') ");
    sb.append("no-repeat scroll center center transparent;");

    // Close the div and return.
    sb.append("\"></div>");
    return sb.toString();
  }

  /**
   * @return true if the key event was consumed by navigation, false if it
   *         should be passed on to the underlying Cell.
   */
  private boolean handleKey(Event event) {
    int keyCode = event.getKeyCode();

    CellTreeNodeView<?> child = null;
    int keyboardSelectedIndex = keyboardSelectedNode.getKeyboardSelectedIndex();
    if (keyboardSelectedIndex != -1
        && keyboardSelectedNode.getChildCount() > keyboardSelectedIndex) {
      child = keyboardSelectedNode.getChildNode(keyboardSelectedIndex);
    }

    CellTreeNodeView<?> parent = keyboardSelectedNode.getParentNode();
    switch (keyCode) {
      case KeyCodes.KEY_UP:
        if (keyboardSelectedNode.getKeyboardSelectedIndex() == 0) {
          if (!keyboardSelectedNode.isRootNode()) {
            if (parent != null) {
              keyboardSelectedNode.keyboardExit();
              parent.keyboardEnter(parent.indexOf(keyboardSelectedNode), true);
              keyboardSelectedNode = parent;
            }
          }
        } else {
          keyboardSelectedNode.keyboardUp();
          // Descend into open nodes, go to bottom of leaf node
          int index = keyboardSelectedNode.getKeyboardSelectedIndex();
          while ((child = keyboardSelectedNode.getChildNode(index)).isOpen()) {
            keyboardSelectedNode.keyboardExit();
            index = child.getChildCount() - 1;
            child.keyboardEnter(index, true);
            keyboardSelectedNode = child;
          }
        }
        return true;

      case KeyCodes.KEY_DOWN:
        if (child != null && child.isOpen()) {
          keyboardSelectedNode.keyboardExit();
          child.keyboardEnter(0, true);
          keyboardSelectedNode = child;
        } else if (!keyboardSelectedNode.keyboardDown()) {
          if (parent != null) {
            keyboardSelectedNode.keyboardExit();
            parent.keyboardEnter(parent.indexOf(keyboardSelectedNode), true);
            // If already at last node of a given level, go up
            while (!parent.keyboardDown()) {
              CellTreeNodeView<?> newParent = parent.getParentNode();
              if (newParent != null) {
                parent.keyboardExit();
                newParent.keyboardEnter(newParent.indexOf(parent) + 1, true);
                parent = newParent;
              }
            }
            keyboardSelectedNode = parent;
          }
        }
        return true;

      case KeyCodes.KEY_LEFT:
      case KeyCodes.KEY_RIGHT:
      case KeyCodes.KEY_ENTER:
        // TODO(rice) - try different key bahavior mappings such as
        // left=close, right=open, enter=toggle.
        if (child != null && !child.isLeaf()) {
          child.setOpen(!child.isOpen());
          return true;
        }
        break;
    }

    return false;
  }
}
