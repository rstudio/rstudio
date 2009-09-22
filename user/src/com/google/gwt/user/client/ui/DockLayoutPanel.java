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
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.layout.client.Layout;
import com.google.gwt.layout.client.Layout.Layer;

/**
 * A panel that lays its child widgets out "docked" at its outer edges, and
 * allows its last widget to take up the remaining space in its center.
 * 
 * <p>
 * Whenever children are added to, or removed from, this panel, you must call
 * one of {@link #layout()}, {@link #layout(int)}, or
 * {@link #layout(int, com.google.gwt.layout.client.Layout.AnimationCallback)}
 * to update the panel's layout.
 * </p>
 * 
 * <p>
 * This widget will <em>only</em> work in standards mode, which requires
 * that the HTML page in which it is run have an explicit &lt;!DOCTYPE&gt;
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
 * {@example com.google.gwt.examples.DockLayoutPanelExample}
 * </p>
 * 
 * TODO(jgw): RTL support.
 */
public class DockLayoutPanel extends ComplexPanel implements RequiresLayout,
    RequiresResize, ProvidesResize {

  /**
   * Used in {@link DockLayoutPanel#add(Widget, Direction, double)} to specify
   * the direction in which a child widget will be added.
   */
  public enum Direction {
    NORTH, EAST, SOUTH, WEST, CENTER, LINE_START, LINE_END
  }

  /**
   * Layout data associated with each widget.
   */
  protected static class LayoutData {
    public Direction direction;
    public double oldSize, size;
    public double originalSize;
    public boolean hidden;
    public Layer layer;

    public LayoutData(Direction direction, double size, Layer layer) {
      this.direction = direction;
      this.size = size;
      this.layer = layer;
    }
  }

  private final Unit unit;
  private Widget center;
  private final Layout layout;

  /**
   * Creates an empty dock panel.
   * 
   * @param unit the unit to be used for layout
   */
  public DockLayoutPanel(Unit unit) {
    this.unit = unit;

    setElement(Document.get().createDivElement());
    layout = new Layout(getElement());
  }

  /**
   * Adds a widget at the center of the dock. No further widgets may be added
   * after this one.
   * 
   * @param widget the widget to be added
   */
  @Override
  public void add(Widget widget) {
    insert(widget, Direction.CENTER, 0, null);
  }

  /**
   * Adds a widget to the east edge of the dock.
   * 
   * @param widget the widget to be added
   * @param size the child widget's size
   */
  public void addEast(Widget widget, double size) {
    insert(widget, Direction.EAST, size, null);
  }

  /**
   * Adds a widget to the north edge of the dock.
   * 
   * @param widget the widget to be added
   * @param size the child widget's size
   */
  public void addNorth(Widget widget, double size) {
    insert(widget, Direction.NORTH, size, null);
  }

  /**
   * Adds a widget to the south edge of the dock.
   * 
   * @param widget the widget to be added
   * @param size the child widget's size
   */
  public void addSouth(Widget widget, double size) {
    insert(widget, Direction.SOUTH, size, null);
  }

  /**
   * Adds a widget to the west edge of the dock.
   * 
   * @param widget the widget to be added
   * @param size the child widget's size
   */
  public void addWest(Widget widget, double size) {
    insert(widget, Direction.WEST, size, null);
  }

  /**
   * Gets the container element associated with the given child widget.
   * 
   * <p>
   * The container element is created by the {@link Layout} class. This should
   * be used with certain styles, such as {@link Style#setZIndex(int)}, that
   * must be applied to the container, rather than directly to the child widget.
   * </p>
   * 
   * TODO(jgw): Is this really the best way to do this?
   * 
   * @param widget the widget whose container element is to be retrieved
   * @return the widget's container element
   */
  public Element getContainerElementFor(Widget widget) {
    assertIsChild(widget);
    return ((LayoutData) widget.getLayoutData()).layer.getContainerElement();
  }

  /**
   * Gets the layout direction of the given child widget.
   * 
   * @param w the widget to be queried
   * @return the widget's layout direction, or <code>null</code> if it is not a
   *         child of this panel
   */
  public Direction getWidgetDirection(Widget w) {
    if (w.getParent() != this) {
      return null;
    }
    return ((LayoutData) w.getLayoutData()).direction;
  }

  /**
   * Adds a widget to the east edge of the dock, inserting it before an
   * existing widget.
   * 
   * @param widget the widget to be added
   * @param size the child widget's size
   * @param before the widget before which to insert the new child, or
   *          <code>null</code> to append
   */
  public void insertEast(Widget widget, double size, Widget before) {
    insert(widget, Direction.EAST, size, before);
  }

  /**
   * Adds a widget to the north edge of the dock, inserting it before an
   * existing widget.
   * 
   * @param widget the widget to be added
   * @param size the child widget's size
   * @param before the widget before which to insert the new child, or
   *          <code>null</code> to append
   */
  public void insertNorth(Widget widget, double size, Widget before) {
    insert(widget, Direction.NORTH, size, before);
  }

  /**
   * Adds a widget to the south edge of the dock, inserting it before an
   * existing widget.
   * 
   * @param widget the widget to be added
   * @param size the child widget's size
   * @param before the widget before which to insert the new child, or
   *          <code>null</code> to append
   */
  public void insertSouth(Widget widget, double size, Widget before) {
    insert(widget, Direction.SOUTH, size, before);
  }

  /**
   * Adds a widget to the west edge of the dock, inserting it before an
   * existing widget.
   * 
   * @param widget the widget to be added
   * @param size the child widget's size
   * @param before the widget before which to insert the new child, or
   *          <code>null</code> to append
   */
  public void insertWest(Widget widget, double size, Widget before) {
    insert(widget, Direction.WEST, size, before);
  }

  public void layout() {
    layout(0);
  }

  public void layout(int duration) {
    layout(0, null);
  }

  public void layout(int duration, final Layout.AnimationCallback callback) {
    int left = 0, top = 0, right = 0, bottom = 0;

    for (Widget child : getChildren()) {
      LayoutData data = (LayoutData) child.getLayoutData();
      Layer layer = data.layer;

      switch (data.direction) {
        case NORTH:
          layer.setLeftRight(left, unit, right, unit);
          layer.setTopHeight(top, unit, data.size, unit);
          top += data.size;
          break;

        case SOUTH:
          layer.setLeftRight(left, unit, right, unit);
          layer.setBottomHeight(bottom, unit, data.size, unit);
          bottom += data.size;
          break;

        case WEST:
          layer.setTopBottom(top, unit, bottom, unit);
          layer.setLeftWidth(left, unit, data.size, unit);
          left += data.size;
          break;

        case EAST:
          layer.setTopBottom(top, unit, bottom, unit);
          layer.setRightWidth(right, unit, data.size, unit);
          right += data.size;
          break;

        case CENTER:
          layer.setLeftRight(left, unit, right, unit);
          layer.setTopBottom(top, unit, bottom, unit);
          break;
      }
    }

    layout.layout(duration, new Layout.AnimationCallback() {
      public void onAnimationComplete() {
        for (Widget child : getChildren()) {
          LayoutData data = (LayoutData) child.getLayoutData();
          if (data.size != data.oldSize) {
            data.oldSize = data.size;
            if (child instanceof RequiresResize) {
              ((RequiresResize) child).onResize();
            }
          }

          if (callback != null) {
            callback.onAnimationComplete();
          }
        }
      }

      public void onLayout(Layer layer, double progress) {
        Widget child = (Widget) layer.getUserObject();
        if (child instanceof RequiresResize) {
          ((RequiresResize) child).onResize();
        }

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
      // Clear the center widget.
      if (w == center) {
        center = null;
      }

      LayoutData data = (LayoutData) w.getLayoutData();
      layout.removeChild(data.layer);
    }

    return removed;
  }

  protected Widget getCenter() {
    return center;
  }

  protected Unit getUnit() {
    return unit;
  }

  /**
   * Adds a widget to the specified edge of the dock. If the widget is already a
   * child of this panel, this method behaves as though {@link #remove(Widget)}
   * had already been called.
   * 
   * @param widget the widget to be added
   * @param direction the widget's direction in the dock
   * @param before the widget before which to insert the new child, or
   *          <code>null</code> to append
   */
  protected void insert(Widget widget, Direction direction, double size,
      Widget before) {
    assertIsChild(before);

    // Validation.
    if (before == null) {
      assert center == null : "No widget may be added after the CENTER widget";
    } else {
      assert direction != Direction.CENTER : "A CENTER widget must always be added last";
    }

    // Detach new child.
    widget.removeFromParent();

    // Logical attach.
    getChildren().add(widget);
    if (direction == Direction.CENTER) {
      center = widget;
    }

    // Physical attach.
    Layer layer = layout.attachChild(widget.getElement(),
        (before != null) ? before.getElement() : null, widget);
    LayoutData data = new LayoutData(direction, size, layer);
    widget.setLayoutData(data);

    // Adopt.
    adopt(widget);
  }

  @Override
  protected void onLoad() {
    layout.onAttach();
  }

  @Override
  protected void onUnload() {
    layout.onDetach();
  }

  private void assertIsChild(Widget widget) {
    assert (widget == null) || (widget.getParent() == this) : "The specified widget is not a child of this panel";
  }
}
