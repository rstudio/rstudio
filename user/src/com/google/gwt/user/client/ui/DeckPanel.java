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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.animation.WidgetAnimation;

/**
 * A panel that displays all of its child widgets in a 'deck', where only one
 * can be visible at a time. It is used by
 * {@link com.google.gwt.user.client.ui.TabPanel}.
 * 
 * <p>
 * Once a widget has been added to a DeckPanel, its visibility, width, and
 * height attributes will be manipulated. When the widget is removed from the
 * DeckPanel, it will be visible, and its width and height attributes will be
 * cleared.
 * </p>
 */
public class DeckPanel extends ComplexPanel implements HasAnimation {
  /**
   * An {@link WidgetAnimation} used to slide in the new content.
   */
  private static class SlideAnimation extends WidgetAnimation {
    /**
     * The {@link Element} holding the {@link Widget} with a lower index.
     */
    private Element container1 = null;

    /**
     * The {@link Element} holding the {@link Widget} with a higher index.
     */
    private Element container2 = null;

    /**
     * A boolean indicating whether container1 is growing or shrinking.
     */
    private boolean growing = false;

    /**
     * The fixed height of a {@link TabPanel} in pixels. If the {@link TabPanel}
     * does not have a fixed height, this will be set to -1.
     */
    private int fixedHeight = -1;

    @Override
    public void onCancel() {
      onComplete();
    }

    @Override
    public void onComplete() {
      if (growing) {
        onUpdate(1.0);
        DOM.setStyleAttribute(container1, "height", "100%");
        UIObject.setVisible(container1, true);
        UIObject.setVisible(container2, false);
        DOM.setStyleAttribute(container2, "height", "100%");
      } else {
        UIObject.setVisible(container1, false);
        DOM.setStyleAttribute(container1, "height", "100%");
        DOM.setStyleAttribute(container2, "height", "100%");
        UIObject.setVisible(container2, true);
      }
      container1 = null;
      container2 = null;
    }

    @Override
    public void onInstantaneousRun() {
      UIObject.setVisible(container1, growing);
      UIObject.setVisible(container2, !growing);
      container1 = null;
      container2 = null;
    }

    @Override
    public void onStart() {
      onUpdate(0.0);
      UIObject.setVisible(container1, true);
      UIObject.setVisible(container2, true);
    }

    @Override
    public void onUpdate(double progress) {
      if (!growing) {
        progress = 1.0 - progress;
      }

      // Container1 expands (shrinks) to its target height
      int height1;
      int height2;
      if (fixedHeight == -1) {
        height1 = (int) (progress * DOM.getElementPropertyInt(container1,
            "scrollHeight"));
        height2 = (int) ((1.0 - progress) * DOM.getElementPropertyInt(
            container2, "scrollHeight"));
      } else {
        height1 = (int) (progress * fixedHeight);
        height2 = fixedHeight - height1;
      }
      DOM.setStyleAttribute(container1, "height", height1 + "px");
      DOM.setStyleAttribute(container2, "height", height2 + "px");
    }

    /**
     * Switch to a new {@link Widget}.
     * 
     * @param oldWidget the {@link Widget} to hide
     * @param newWidget the {@link Widget} to show
     * @param animate true to animate, false to switch instantly
     */
    public void showWidget(Widget oldWidget, Widget newWidget, boolean animate) {
      // Immediately complete previous animation
      cancel();

      // Get the container and index of the new widget
      Element newContainer = getContainer(newWidget);
      int newIndex = DOM.getChildIndex(DOM.getParent(newContainer),
          newContainer);

      // If we aren't showing anything, don't bother with the animation
      if (oldWidget == null) {
        UIObject.setVisible(newContainer, true);
        return;
      }

      // Get the container and index of the old widget
      Element oldContainer = getContainer(oldWidget);
      int oldIndex = DOM.getChildIndex(DOM.getParent(oldContainer),
          oldContainer);

      // Figure out whether to grow or shrink the container
      if (newIndex > oldIndex) {
        container1 = oldContainer;
        container2 = newContainer;
        growing = false;
      } else {
        container1 = newContainer;
        container2 = oldContainer;
        growing = true;
      }

      // Figure out if we are in a fixed height situation
      fixedHeight = DOM.getElementPropertyInt(oldContainer, "offsetHeight");
      if (fixedHeight == oldWidget.getOffsetHeight()) {
        fixedHeight = -1;
      }

      // Start the animation
      if (animate) {
        run(350);
      } else {
        onInstantaneousRun();
      }
    }
  }

  /**
   * The {@link WidgetAnimation} used to slide in the new {@link Widget}.
   */
  private static SlideAnimation slideAnimation;

  /**
   * The the container {@link Element} around a {@link Widget}.
   * 
   * @param w the {@link Widget}
   * @return the container {@link Element}
   */
  private static Element getContainer(Widget w) {
    return DOM.getParent(w.getElement());
  }

  private boolean isAnimationEnabled = false;

  private Widget visibleWidget;

  /**
   * Creates an empty deck panel.
   */
  public DeckPanel() {
    setElement(DOM.createDiv());
  }

  /**
   * Adds the specified widget to the deck.
   * 
   * @param w the widget to be added
   */
  @Override
  public void add(Widget w) {
    Element container = DOM.createDiv();
    DOM.appendChild(getElement(), container);
    initChildWidget(w);
    initWidgetContainer(container);
    super.add(w, container);
  }

  /**
   * Gets the index of the currently-visible widget.
   * 
   * @return the visible widget's index
   */
  public int getVisibleWidget() {
    return getWidgetIndex(visibleWidget);
  }

  /**
   * Inserts a widget before the specified index.
   * 
   * @param w the widget to be inserted
   * @param beforeIndex the index before which it will be inserted
   * @throws IndexOutOfBoundsException if <code>beforeIndex</code> is out of
   *           range
   */
  public void insert(Widget w, int beforeIndex) {
    Element container = DOM.createDiv();
    DOM.insertChild(getElement(), container, beforeIndex);
    initChildWidget(w);
    initWidgetContainer(container);
    super.insert(w, container, beforeIndex, true);
  }

  /**
   * @see HasAnimation#isAnimationEnabled()
   */
  public boolean isAnimationEnabled() {
    return isAnimationEnabled;
  }

  @Override
  public boolean remove(Widget w) {
    Element container = getContainer(w);
    boolean removed = super.remove(w);
    if (removed) {
      resetChildWidget(w);

      DOM.removeChild(getElement(), container);
      if (visibleWidget == w) {
        visibleWidget = null;
      }
    }
    return removed;
  }

  /**
   * @see HasAnimation#setAnimationEnabled(boolean)
   */
  public void setAnimationEnabled(boolean enable) {
    isAnimationEnabled = enable;
  }

  /**
   * Shows the widget at the specified index. This causes the currently- visible
   * widget to be hidden.
   * 
   * @param index the index of the widget to be shown
   */
  public void showWidget(int index) {
    checkIndexBoundsForAccess(index);
    Widget oldWidget = visibleWidget;
    visibleWidget = getWidget(index);

    if (visibleWidget != oldWidget) {
      if (slideAnimation == null) {
        slideAnimation = new SlideAnimation();
      }
      slideAnimation.showWidget(oldWidget, visibleWidget, isAnimationEnabled
          && isAttached());
    }
  }

  /**
   * Set the widget's width and height to full.
   */
  private void initChildWidget(Widget w) {
    w.setSize("100%", "100%");
  }
  
  /**
   * Setup the container around the widget.
   */
  private void initWidgetContainer(Element container) {
    DOM.setStyleAttribute(container, "width", "100%");
    DOM.setStyleAttribute(container, "height", "100%");
    DOM.setStyleAttribute(container, "overflow", "hidden");
    DOM.setStyleAttribute(container, "padding", "0px");
    DOM.setStyleAttribute(container, "margin", "0px");
    UIObject.setVisible(container, false);
  }
  
  /**
   * Reset the dimensions of the widget when it is removed.
   */
  private void resetChildWidget(Widget w) {
    w.setSize("", "");
  }
}
