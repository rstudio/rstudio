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
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Event;

/**
 * A panel that adds user-positioned splitters between each of its child
 * widgets.
 * 
 * <p>
 * This panel is used in the same way as {@link DockLayoutPanel}, except that
 * its children's sizes are always specified in {@link Unit#PX} units, and each
 * pair of child widgets has a splitter between them that the user can drag.
 * </p>
 * 
 * <p>
 * This widget will <em>only</em> work in standards mode, which requires that
 * the HTML page in which it is run have an explicit &lt;!DOCTYPE&gt;
 * declaration.
 * </p>
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-SplitLayoutPanel { the panel itself }</li>
 * <li>.gwt-SplitLayoutPanel .gwt-SplitLayoutPanel-HDragger { horizontal dragger
 * }</li>
 * <li>.gwt-SplitLayoutPanel .gwt-SplitLayoutPanel-VDragger { vertical dragger }
 * </li>
 * </ul>
 * 
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.SplitLayoutPanelExample}
 * </p>
 */
public class SplitLayoutPanel extends DockLayoutPanel {

  class HSplitter extends Splitter {
    public HSplitter(Widget target, boolean reverse) {
      super(target, reverse);
      getElement().getStyle().setPropertyPx("width", SPLITTER_SIZE);
      setStyleName("gwt-SplitLayoutPanel-HDragger");
    }

    @Override
    protected int getAbsolutePosition() {
      return getAbsoluteLeft();
    }

    @Override
    protected int getEventPosition(Event event) {
      return event.getClientX();
    }

    @Override
    protected int getTargetPosition() {
      return target.getAbsoluteLeft();
    }

    @Override
    protected int getTargetSize() {
      return target.getOffsetWidth();
    }
  }

  abstract class Splitter extends Widget {
    protected final Widget target;

    private int offset;
    private boolean mouseDown;
    private Command layoutCommand;

    private final boolean reverse;
    private int minSize;

    public Splitter(Widget target, boolean reverse) {
      this.target = target;
      this.reverse = reverse;

      setElement(Document.get().createDivElement());
      sinkEvents(Event.ONMOUSEDOWN | Event.ONMOUSEUP | Event.ONMOUSEMOVE
          | Event.ONDBLCLICK);
    }

    @Override
    public void onBrowserEvent(Event event) {
      switch (event.getTypeInt()) {
        case Event.ONMOUSEDOWN:
          mouseDown = true;
          offset = getEventPosition(event) - getAbsolutePosition();
          Event.setCapture(getElement());
          event.preventDefault();
          break;

        case Event.ONMOUSEUP:
          mouseDown = false;
          Event.releaseCapture(getElement());
          event.preventDefault();
          break;

        case Event.ONMOUSEMOVE:
          if (mouseDown) {
            int size;
            if (reverse) {
              size = getTargetPosition() + getTargetSize()
                  - getEventPosition(event) - offset;
            } else {
              size = getEventPosition(event) - getTargetPosition() - offset;
            }
            setAssociatedWidgetSize(size);
            event.preventDefault();
          }
          break;
      }
    }

    public void setMinSize(int minSize) {
      this.minSize = minSize;
      LayoutData layout = (LayoutData) target.getLayoutData();

      // Try resetting the associated widget's size, which will enforce the new
      // minSize value.
      setAssociatedWidgetSize((int) layout.size);
    }

    protected abstract int getAbsolutePosition();

    protected abstract int getEventPosition(Event event);

    protected abstract int getTargetPosition();

    protected abstract int getTargetSize();

    private void setAssociatedWidgetSize(int size) {
      if (size < minSize) {
        size = minSize;
      }

      LayoutData layout = (LayoutData) target.getLayoutData();
      if (size == layout.size) {
        return;
      }

      layout.size = size;

      // Defer actually updating the layout, so that if we receive many
      // mouse events before layout/paint occurs, we'll only update once.
      if (layoutCommand == null) {
        layoutCommand = new Command() {
          public void execute() {
            layoutCommand = null;
            forceLayout();
          }
        };
        DeferredCommand.addCommand(layoutCommand);
      }
    }
  }

  class VSplitter extends Splitter {
    public VSplitter(Widget target, boolean reverse) {
      super(target, reverse);
      getElement().getStyle().setPropertyPx("height", SPLITTER_SIZE);
      setStyleName("gwt-SplitLayoutPanel-VDragger");
    }

    @Override
    protected int getAbsolutePosition() {
      return getAbsoluteTop();
    }

    @Override
    protected int getEventPosition(Event event) {
      return event.getClientY();
    }

    @Override
    protected int getTargetPosition() {
      return target.getAbsoluteTop();
    }

    @Override
    protected int getTargetSize() {
      return target.getOffsetHeight();
    }
  }

  private static final int SPLITTER_SIZE = 8;

  public SplitLayoutPanel() {
    super(Unit.PX);
    setStyleName("gwt-SplitLayoutPanel");
  }

  @Override
  public void insert(Widget child, Direction direction, double size, Widget before) {
    super.insert(child, direction, size, before);
    if (direction != Direction.CENTER) {
      insertSplitter(child, before);
    }
  }

  @Override
  public boolean remove(Widget child) {
    assert !(child instanceof Splitter) : "Splitters may not be directly removed";

    int idx = getWidgetIndex(child);
    if (super.remove(child)) {
      // Remove the associated splitter, if any.
      // Now that the widget is removed, idx is the index of the splitter.
      if (idx < getWidgetCount()) {
        // Call super.remove(), or we'll end up recursing.
        super.remove(getWidget(idx));
      }
      return true;
    }
    return false;
  }

  /**
   * Sets the minimum allowable size for the given widget.
   * 
   * <p>
   * Its associated splitter cannot be dragged to a position that would make it
   * smaller than this size. This method has no effect for the
   * {@link DockLayoutPanel.Direction#CENTER} widget.
   * </p>
   * 
   * @param child the child whose minimum size will be set
   * @param minSize the minimum size for this widget
   */
  public void setWidgetMinSize(Widget child, int minSize) {
    assertIsChild(child);
    Splitter splitter = getAssociatedSplitter(child);
    // The splitter is null for the center element. 
    if (splitter != null) {
      splitter.setMinSize(minSize);
    }
  }

  private Splitter getAssociatedSplitter(Widget child) {
    // If a widget has a next sibling, it must be a splitter, because the only
    // widget that *isn't* followed by a splitter must be the CENTER, which has
    // no associated splitter.
    int idx = getWidgetIndex(child);
    if (idx > -1 && idx < getWidgetCount() - 1) {
      Widget splitter = getWidget(idx + 1);
      assert splitter instanceof Splitter : "Expected child widget to be splitter";
      return (Splitter) splitter;
    }
    return null;
  }

  private void insertSplitter(Widget widget, Widget before) {
    assert getChildren().size() > 0 : "Can't add a splitter before any children";

    LayoutData layout = (LayoutData) widget.getLayoutData();
    Splitter splitter = null;
    switch (layout.direction) {
      case WEST:
        splitter = new HSplitter(widget, false);
        break;
      case EAST:
        splitter = new HSplitter(widget, true);
        break;
      case NORTH:
        splitter = new VSplitter(widget, false);
        break;
      case SOUTH:
        splitter = new VSplitter(widget, true);
        break;
      default:
        assert false : "Unexpected direction";
    }

    super.insert(splitter, layout.direction, SPLITTER_SIZE, before);
  }
}
