/*
 * Copyright 2009 Google Inc.
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

import com.google.gwt.dom.client.Document;
import com.google.gwt.layout.client.Layout;
import com.google.gwt.layout.client.Layout.Layer;

/**
 * A panel that lays its children out in arbitrary
 * {@link com.google.gwt.layout.client.Layout.Layer layers} using the
 * {@link Layout} class.
 * 
 * <p>
 * Whenever children are added to, or removed from, this panel, you must call
 * one of {@link #layout()}, {@link #layout(int)}, or
 * {@link #layout(int, com.google.gwt.layout.client.Layout.AnimationCallback)}
 * to update the panel's layout.
 * </p>
 * 
 * <p>
 * This widget will <em>only</em> work in standards mode, which requires that
 * the HTML page in which it is run have an explicit &lt;!DOCTYPE&gt;
 * declaration.
 * </p>
 * 
 * <p>
 * NOTE: This class is still very new, and its interface may change without
 * warning. Use at your own risk.
 * </p>
 * 
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.LayoutPanelExample}
 * </p>
 * 
 * TODO: implements IndexedPanel (I think)
 */
public class LayoutPanel extends ComplexPanel implements RequiresLayout,
    RequiresResize, ProvidesResize {

  private final Layout layout;

  /**
   * Creates an empty layout panel.
   */
  public LayoutPanel() {
    setElement(Document.get().createDivElement());
    layout = new Layout(getElement());
  }

  /**
   * Adds a widget to this panel.
   * 
   * <p>
   * By default, each child will fill the panel. To build more interesting
   * layouts, use {@link #getLayer(Widget)} to get the
   * {@link com.google.gwt.layout.client.Layout.Layer} associated with each
   * child, and set its layout constraints as desired.
   * </p>
   * 
   * @param widget the widget to be added
   */
  public void add(Widget widget) {
    insert(widget, getWidgetCount());
  }

  /**
   * Gets the {@link Layer} associated with the given widget. This layer may be
   * used to manipulate the child widget's layout constraints.
   * 
   * <p>
   * After you have made changes to any of the child widgets' constraints, you
   * must call one of the {@link RequiresLayout} methods for those changes to
   * be reflected visually.
   * </p>
   * 
   * @param child the child widget whose layer is to be retrieved
   * @return the associated layer
   */
  public Layout.Layer getLayer(Widget child) {
    assert child.getParent() == this :
      "The requested widget is not a child of this panel";
    return (Layout.Layer) child.getLayoutData();
  }

  /**
   * Inserts a widget before the specified index.
   * 
   * <p>
   * By default, each child will fill the panel. To build more interesting
   * layouts, use {@link #getLayer(Widget)} to get the
   * {@link com.google.gwt.layout.client.Layout.Layer} associated with each
   * child, and set its layout constraints as desired.
   * </p>
   * 
   * <p>
   * Inserting a widget in this way has no effect on the DOM structure, but can
   * be useful for other panels that wrap LayoutPanel to maintain insertion
   * order.
   * </p>
   * 
   * @param widget the widget to be inserted
   * @param beforeIndex the index before which it will be inserted
   * @throws IndexOutOfBoundsException if <code>beforeIndex</code> is out of
   *           range
   */
  public void insert(Widget widget, int beforeIndex) {
    // Detach new child.
    widget.removeFromParent();

    // Logical attach.
    getChildren().insert(widget, beforeIndex);

    // Physical attach.
    Layer layer = layout.attachChild(widget.getElement(), widget);
    widget.setLayoutData(layer);

    // Adopt.
    adopt(widget);
  }

  public void layout() {
    layout.layout();
  }

  public void layout(int duration) {
    layout.layout(duration);
  }

  public void layout(int duration, final Layout.AnimationCallback callback) {
    layout.layout(duration, new Layout.AnimationCallback() {
      public void onAnimationComplete() {
        // Chain to the passed callback.
        if (callback != null) {
          callback.onAnimationComplete();
        }
      }

      public void onLayout(Layer layer, double progress) {
        // Inform the child associated with this layer that its size may have
        // changed.
        Widget child = (Widget) layer.getUserObject();
        if (child instanceof RequiresResize) {
          ((RequiresResize) child).onResize();
        }

        // Chain to the passed callback.
        if (callback != null) {
          callback.onLayout(layer, progress);
        }
      }
    });
  }

  public void onResize() {
    for (Widget child : getChildren()) {
      if (child instanceof RequiresResize) {
        ((RequiresResize) child).onResize();
      }
    }
  }

  @Override
  public boolean remove(Widget w) {
    boolean removed = super.remove(w);
    if (removed) {
      layout.removeChild((Layer) w.getLayoutData());
    }
    return removed;
  }

  /**
   * Gets the {@link Layout} instance associated with this widget.
   * 
   * @return this widget's layout instance
   */
  protected Layout getLayout() {
    return layout;
  }

  @Override
  protected void onLoad() {
    layout.onAttach();
  }

  @Override
  protected void onUnload() {
    layout.onDetach();
  }
}
